package com.neilab.plugins.tenacious

import com.neilab.plugins.tenacious.exception.CancelException
import com.neilab.plugins.tenacious.exception.PersistentException
import grails.gorm.DetachedCriteria
import grails.util.Holders
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
@Slf4j
class PersistentTaskData   {
    String id
    Integer priority = 0
    Integer attempts = 0
    String queue = "default"
    String handler
    String action
    String params
    String lastError
    Date runAt
    Date failedAt
    Boolean active = true

    Date dateCreated
    Date lastUpdated

    //  Date lockedAt
    //  String lockedBy

    private PersistentTask persistentTask

    static DetachedCriteria getIsActive() {
        def criteria = new DetachedCriteria(this).build {
            eq 'active', true
        }
    }

    PersistentTask getTask() {
        if(persistentTask)
            return persistentTask

        TenaciousFactoryService  tenaciousFactoryService =
                Holders.grailsApplication.mainContext.getBean("tenaciousFactoryService")
        def injectedTask = tenaciousFactoryService[handler]

        persistentTask =  injectedTask ?: Class.forName(handler).newInstance()
        return persistentTask
    }

    static mapping = {
        id generator: 'uuid', params: [separator: '-']
        lastError type: 'text'
        params type: 'text'
        active  index: 'handler_action_active_idx'
        handler index: 'handler_action_active_idx'
        action index: 'handler_action_active_idx'
        priority index: 'priority_runAt_idx'
        runAt index: 'priority_runAt_idx'
    }

    static constraints = {
        lastError nullable: true
        //lockedAt nullable: true
        //lockedBy nullable: true
        action nullable: true
        failedAt nullable: true
        runAt nullable: true
        dateCreated nullable: true
        lastUpdated nullable: true
    }

    static transients = ['task']

    def resume(Map extra=[:]) {
        Map options = [failOnError: false, flush: false] << extra
        PersistentTask persistentTask = (PersistentTask)options.task ?: this.task
        this.runAt = new Date()

        try {
            withNewTransaction { status ->
                if([false].contains(persistentTask.perform(this.parseJsonData()))) {
                    throw new PersistentException("Task returned false")
                }
            }
            this.failedAt = null
            this.attempts = Math.max(0,this.attempts ?: 1)
            this.active = false
        } catch (CancelException c) {
            this.lastError = parseStacktrace(c)
            this.attempts = Math.max(0,this.attempts) + 1
            this.failedAt = new Date()
            this.active = false
            if(persistentTask.maxAttempts && this.attempts > persistentTask.maxAttempts) {
                this.active = false
            }
            log.info("task cancelled with id: ${this.id} params: ${this.params}  exception message: '${c.message}'")
        } catch (PersistentException|Exception e) {
            this.lastError = parseStacktrace(e)
            this.attempts = Math.max(0,this.attempts) + 1
            this.failedAt = new Date()
            this.runAt = nextRunDate()
            if(persistentTask.maxAttempts && this.attempts > persistentTask.maxAttempts) {
                this.active = false
            }
            log.warn("failed execution of task id: ${this.id} params: ${this.params}  exception: ${e.stackTrace.join("\n\t")}")
        }

        try {
            return this.save(failOnError: options.failOnError, flush: options.flush)
        } catch(Exception e) {
            log.error("Unable to save persistent job state failure with id: ${this.id} because: ${e.stackTrace.join("\n\t")}")
        }
    }

    private  Date nextRunDate() {
        Integer n = this.attempts ?: 0
        return  DateTime.now().plusSeconds((5+n)*4).toDate()
    }

    private Map parseJsonData() {
        def jsonSlurper = new JsonSlurper()
        def object = this.params ? jsonSlurper.parseText(this.params) : [:]
        return object instanceof  Map ? object : [:]
    }

    private String parseStacktrace(Exception e) {
        StringWriter errors = new StringWriter()
        e.printStackTrace(new PrintWriter(errors))
        return e.toString()
    }
}

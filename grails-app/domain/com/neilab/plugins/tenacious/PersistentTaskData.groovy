package com.neilab.plugins.tenacious

import com.neilab.plugins.tenacious.exception.*
import grails.compiler.GrailsCompileStatic
import grails.gorm.DetachedCriteria
import grails.util.Holders
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
@Slf4j
@GrailsCompileStatic
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

        TenaciousFactoryService  tenaciousFactoryService = (TenaciousFactoryService)  Holders.grailsApplication.mainContext.getBean("tenaciousFactoryService")
        def injectedTask = tenaciousFactoryService[handler]

        persistentTask = (PersistentTask) (injectedTask instanceof PersistentTask ? injectedTask : Class.forName(handler).newInstance())
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


    private  Date nextRunDate() {
        Integer n = this.attempts ?: 0
        return  DateTime.now().plusSeconds((5+n)*4).toDate()
    }

    Map getParameterMap() {
        def jsonSlurper = new JsonSlurper()
        def object = this.params ? jsonSlurper.parseText(this.params) : [:]
        return (Map)(object instanceof  Map ? object : [:])
    }

}

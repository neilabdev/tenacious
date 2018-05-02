package com.neilab.plugins.tenacious

import groovy.json.JsonSlurper
import org.joda.time.DateTime

class PersistentTaskData   {

    Integer priority = 0
    Integer attempts = 0
    String queue = "default"
    String handler
    String params
    String lastError
    Date runAt
    Date failedAt
    Boolean active = true
  //  Date lockedAt
 //   String lockedBy

    private persistentTask

    PersistentTask getTask() {
        if(persistentTask)
            return persistentTask
        persistentTask =  Class.forName(handler).newInstance()
        return persistentTask
    }

    static transients = ['task']

    static mapping = {
        lastError type: 'text'
        params type: 'text'
    }

    static constraints = {
        lastError nullable: true
        //lockedAt nullable: true
        //lockedBy nullable: true
        failedAt nullable: true
        runAt nullable: true
    }


    def resume(Map extra=[:]) {
        Map options = [failOnError: false] << extra
        PersistentTask persistentTask = (PersistentTask)options.task ?: this.task
        this.runAt = new Date()
        try {
            if([false].contains(persistentTask.perform(this.parseJsonData()))) {
                throw new PersistentException("Tasked returned false")
            }
            handler = null
            failedAt = null
            active = false
        } catch (PersistentException|Exception e) {
            this.handler = parseStacktrace(e)
            this.attempts = Math.max(0,this.attempts) + 1
            this.failedAt = new Date()
            this.runAt = nextRunDate()

            if( persistentTask.maxAttempts && this.attempts > persistentTask.maxAttempts) {
                this.active = false
            }
        }

        return this.save(failOnError: options.failOnError)
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

package com.neilab.plugins.tenacious

import com.neilab.plugins.tenacious.util.TenaciousUtil
import grails.util.GrailsClassUtils
import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import org.joda.time.DateTime

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import groovy.json.*

trait PersistentWorker <T extends PersistentWorker<T>>  {
    Integer maxAttempts
    String  queueName

    def execute(context) {
        runTasks()
    }

    def execute() {
        this.execute(null)
    }

    static def run() {
        throw new UnsupportedOperationException("Unable to execute method which should have been dynamically overridden")
    }

    void runTasks() {
        Date now = new Date()
        Map config = TenaciousUtil.getStaticPropertyValue(this.getClass(),"tenacious",Map,[:])
        Integer ma =  maxAttempts ?: config.maxAttempts
        String qn = queueName ?: config.queueName

        def taskData = PersistentTaskData.where {
            if (ma) {
                attempts < ma
            }

            if(qn) {
                queue == qn
            }

            runAt == null || runAt < now

            active == true
        }.list(order:"desc", sort:"priority").eachWithIndex { PersistentTaskData task, int i ->
            task.resume()
        }
    }

    static def scheduleTask(Map<String, Object> params = [:], Class<PersistentTask> taskClass, boolean immediate = false) {
        this.scheduleTask(params, taskClass.newInstance(), immediate)
    }

    static def scheduleTask(Map<String, Object> params = [:], PersistentTask task, boolean immediate = false) {
        TenaciousUtil.scheduleTask(params,task,null,immediate)
    }

    private String parseStacktrace(Exception e) {
        StringWriter errors = new StringWriter()
        e.printStackTrace(new PrintWriter(errors))
        return e.toString()
    }
}

//class MyTask implements PersistentTask {}
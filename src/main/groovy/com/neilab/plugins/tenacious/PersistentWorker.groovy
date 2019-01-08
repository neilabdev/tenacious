package com.neilab.plugins.tenacious

import com.neilab.plugins.tenacious.util.TenaciousUtil
import com.neilab.plugins.tenacious.TenaciousService
import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import org.joda.time.DateTime

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import groovy.json.*
import static  grails.util.Holders.*

trait PersistentWorker<T extends PersistentWorker<T>> {
    Integer maxAttempts = 7
    Integer maxJobs = 250
    Long sleepInterval = 0
    String queueName
    def tenaciousService

    def execute(context) {
        runTasks()
    }

    def execute() {
        this.execute(null)
    }

    def initWork() {

    }

    def beforeWork() {

    }

    def afterWork() {

    }

    def beforeTask(PersistentTaskData data) {

    }

    def afterTask(PersistentTaskData data) {

    }

    static def run(Map params=[:]) {
        throw new UnsupportedOperationException("Unable to execute method which should have been dynamically overridden")
    }

    void runTasks(Map params = [:]) {
        def ts = applicationContext.getBean("tenaciousService") //as TenaciousService
        ts?.performTasks(params,this) //TenaciousUtil.performTasks(params,this)
    }



    static def scheduleTask(Map<String, Object> params = [:], Class<PersistentTask> taskClass, boolean immediate = false) {
        this.scheduleTask(params,taskClass,(String)null,immediate)
    }

    static def scheduleTask(Map<String, Object> params = [:], Class<PersistentTask> taskClass,String action, boolean immediate = false) {
        //TODO:Refactor
        def ts = applicationContext.getBean("tenaciousService")  as TenaciousService
        ts?.scheduleTask(params, taskClass,action, immediate)
    }

    static def scheduleTask(Map<String, Object> params = [:], PersistentTask task, String action , boolean immediate = false) {
        //TODO:Refactor
        TenaciousUtil.scheduleTask(params, task,action, null, immediate)
    }

    private String parseStacktrace(Exception e) {
        StringWriter errors = new StringWriter()
        e.printStackTrace(new PrintWriter(errors))
        return e.toString()
    }
}

//class MyTask implements PersistentTask {}
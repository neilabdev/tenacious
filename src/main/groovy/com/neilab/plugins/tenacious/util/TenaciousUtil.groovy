package com.neilab.plugins.tenacious.util

import com.neilab.plugins.tenacious.PersistentTask
import com.neilab.plugins.tenacious.PersistentTaskData
import com.neilab.plugins.tenacious.PersistentWorker
import grails.util.GrailsClassUtils
import groovy.json.JsonOutput
import org.joda.time.DateTime

class TenaciousUtil {

    static def getStaticPropertyValue(Class clazz, String propertyName, Class type, def defaultValue= null) {
        def value = GrailsClassUtils.getStaticPropertyValue(clazz,propertyName)

        if(type.isAssignableFrom(value.getClass())) {
            return value
        }

        return defaultValue
    }

    static def scheduleTask(Map<String, Object> params = [:], PersistentTask task, String queue = null, boolean immediate = false) { //TODO: MOVE TO SERVICE
        Boolean processedTask = false
        //TODO: Throw excption if task in wrong queue
        Map config = getStaticPropertyValue(task.getClass(),"tenacious",Map,[:])
        PersistentTaskData taskData = new PersistentTaskData(handler: task.getClass().name)
        taskData.priority = task.priority ?: config.priority ?: 1
        taskData.queue = task.queueName ?: config.queueName ?: queue ?: "default"
        //TODO: If no queue specified, it should probably be on the on the class upon which it was scheduled
        if(task.minDelay?.intValue() > 0)
            taskData.runAt = DateTime.now().plusSeconds(task.minDelay).toDate()
        taskData.params = JsonOutput.toJson(params ?: [:])

        return  immediate ? taskData.resume(task: task) : taskData.save()
    }


    static void performTasks(Map params, PersistentWorker worker) { //TODO: Move to Service
        def options = [failOnError: true, flush: false] << params
        Date now = new Date()
        Map config = TenaciousUtil.getStaticPropertyValue(worker.getClass(), "tenacious", Map, [:])
        Integer ma = worker.maxAttempts ?: config.maxAttempts
        String qn = worker.queueName ?: config.queueName

        def taskData = PersistentTaskData.where {
            if (ma) {
                attempts < ma
            }

            if (qn) {
                queue == qn
            }

            runAt == null || runAt < now

            active == true
        }.list(order: "desc", sort: "priority")

        worker.beforeWork()

        for (task in taskData) {
            PersistentTaskData.withTransaction { status ->
                PersistentTaskData t = task //.lock()
                worker.beforeTask(task)
                task.resume(failOnError: options.failOnError, flush: options.flush)
                worker.afterTask(task)
            }
        }

        worker.afterWork()
    }

    static void performTasks(Map params, Class<PersistentWorker> workerClass) {
        performTasks(params, workerClass.newInstance())
    }
}

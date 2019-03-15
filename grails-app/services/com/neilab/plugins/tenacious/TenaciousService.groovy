package com.neilab.plugins.tenacious

import com.neilab.plugins.tenacious.util.TenaciousUtil
import grails.transaction.Transactional
import groovy.util.logging.Slf4j
import org.joda.time.DateTime

import java.util.concurrent.TimeUnit
import groovy.json.JsonOutput

@Slf4j
@Transactional
class TenaciousService {
    def tenaciousFactoryService
    //def transactional = false

    void performTasks(Map params, PersistentWorker worker) {
        def options = [failOnError: true, flush: false] << params
        Date now = new Date()
        Map config = TenaciousUtil.getStaticPropertyValue(worker.getClass(), "tenacious", Map, [:])
        Integer ma = worker.maxAttempts ?: config.maxAttempts
        String qn = worker.queueName ?: config.queueName

        try {
            worker.initWork()
            def taskData = PersistentTaskData.createCriteria().list(order: "desc", sort: "priority", max: worker.maxJobs, {
                if (ma instanceof Integer && ma > 0) {
                    lt("attempts", ma)
                }

                if (qn) {
                    eq("queue", qn)
                }

                or {
                    isNull("runAt")
                    lt("runAt", now)
                }

                eq("active", true)

                lock true
            })

            worker.beforeWork()

            for (task in taskData) {
                PersistentTaskData.withNewTransaction { status ->
                    PersistentTaskData t = task //.lock()
                    worker.beforeTask(task)
                    task.resume(failOnError: options.failOnError, flush: options.flush)
                    worker.afterTask(task)
                    status.flush()
                }

                if ((worker.restInterval ?: 0) > 0) {
                    TimeUnit.MILLISECONDS.sleep(worker.restInterval)
                }
            }
            worker.afterWork()
        } catch (Exception e) {
            log.error("Unable to performTasks because: ${e.stackTrace.join('\n\t')}")
        } //try
    }

    void performTasks(Map params, Class<PersistentWorker> workerClass) {
        PersistentWorker worker = workerClass.newInstance()
        performTasks(params, worker)
    }

    def scheduleTask(Map<String, Object> params = [:], Class<PersistentTask> taskClass, boolean immediate = false) {
        this.scheduleTask(params, taskClass, null, immediate)
    }

    def scheduleTask(Map<String, Object> params = [:], Class<PersistentTask> taskClass, String action, boolean immediate = false) {
        def artifactInstance = tenaciousFactoryService[taskClass.name] ?: taskClass.newInstance()
        this.scheduleTask(params, (PersistentTask) artifactInstance, action, immediate)
    }

    def scheduleTask(Map<String, Object> params = [:], PersistentTask task, boolean immediate = false) {
        this.scheduleTask(params, task, null, null, immediate)
    }

    def scheduleTask(Map<String, Object> params = [:], PersistentTask task, String actionName  , String queue  , boolean immediate = false) { //TODO: MOVE TO SERVICE
        Boolean processedTask = false
        //TODO: Throw excption if task in wrong queue
        String taskClassName = task.getClass().name
        Map config = TenaciousUtil.getStaticPropertyValue(task.getClass(),"tenacious",Map,[:])
        PersistentTaskData existingTaskData = null
        PersistentTaskData taskData =   (actionName ? (existingTaskData =
                PersistentTaskData.where {
                    handler == taskClassName
                    action == actionName
                    active == true
                }.get()) : null) ?: new PersistentTaskData(handler: taskClassName)

        taskData.action = actionName
        taskData.priority = task.priority ?: config.priority ?: 1
        taskData.queue = task.queueName ?: config.queueName ?: queue ?: "default"
        taskData.attempts = Math.max(0, taskData.attempts - 1)
        //TODO: If no queue specified, it should probably be on the on the class upon which it was scheduled
        if(!existingTaskData && task.minDelay?.intValue() > 0)
            taskData.runAt = DateTime.now().plusSeconds(task.minDelay).toDate()
        taskData.params = JsonOutput.toJson(params ?: [:])

        return immediate ?   taskData.resume(task: task) : taskData.save()
    }

    def scheduleTask(Map<String, Object> params = [:], PersistentTask task, String action, boolean immediate = false) {
        this.scheduleTask(params, task, action, null, immediate)
    }
}

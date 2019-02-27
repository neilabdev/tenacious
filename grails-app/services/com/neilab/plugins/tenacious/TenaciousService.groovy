package com.neilab.plugins.tenacious

import com.neilab.plugins.tenacious.util.TenaciousUtil
import grails.transaction.Transactional
import org.apache.commons.logging.impl.SLF4JLog

import java.util.concurrent.TimeUnit

@Transactional
class TenaciousService {
    def tenaciousFactoryService
    def transactional = false

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
                PersistentTaskData.withTransaction { status ->
                    PersistentTaskData t = task //.lock()
                    worker.beforeTask(task)
                    task.resume(failOnError: options.failOnError, flush: options.flush)
                    worker.afterTask(task)
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
        TenaciousUtil.scheduleTask(params, task, null, null, immediate)
    }

    def scheduleTask(Map<String, Object> params = [:], PersistentTask task, String action, boolean immediate = false) {
        TenaciousUtil.scheduleTask(params, task, action, null, immediate)
    }

}

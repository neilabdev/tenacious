package com.neilab.plugins.tenacious

import com.neilab.plugins.tenacious.util.TenaciousUtil
import grails.transaction.Transactional

@Transactional
class TenaciousService {

    void performTasks(Map params, PersistentWorker worker) {
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

    void performTasks(Map params, Class<PersistentWorker> workerClass) {
        PersistentWorker worker = workerClass.newInstance()
        performTasks(params, worker)
    }


    def scheduleTask(Map<String, Object> params = [:], Class<PersistentTask> taskClass, boolean immediate = false) {
        this.scheduleTask(params, taskClass.newInstance(), immediate)
    }

    def scheduleTask(Map<String, Object> params = [:], PersistentTask task, boolean immediate = false) {
        TenaciousUtil.scheduleTask(params, task, null, immediate)
    }

}
package com.neilab.plugins.tenacious

import com.neilab.plugins.tenacious.util.*
import com.neilab.plugins.tenacious.exception.*
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
import org.springframework.transaction.annotation.Propagation
import grails.gorm.transactions.*
import java.util.concurrent.TimeUnit
import groovy.json.JsonOutput

//@GrailsCompileStatic
@Slf4j
@Transactional
class TenaciousService {
    def tenaciousFactoryService

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected PersistentTaskData runTask(Map params = [:], PersistentWorker worker, PersistentTaskData task) {
        def options = [:] << params
        worker.beforeTask(task)
        resumeTask(task)
        worker.afterTask(task)
        return task
    }

    private def resumeTask(Map extra = [:], PersistentTaskData task) {
        Map options = [failOnError: false, flush: false] << extra
        PersistentTask persistentTask = (PersistentTask) (options.task instanceof PersistentTask ? options.task : task.task)
        Boolean enableSavepoint = Holders.grailsApplication.config.getProperty("tenacious.dataSource.savePoint", Boolean, false)
        task.runAt = new Date()

        try {

            PersistentTaskData.withTransaction { status ->
                def savePoint = enableSavepoint ? status.createSavepoint() : null
                try {
                    if ([false].contains(persistentTask.perform(task.parameterMap))) {
                        throw new PersistentException("${task.handler}: task returned false")
                    }
                } catch (Exception e) {
                    if (savePoint)
                        status.rollbackToSavepoint(savePoint)
                    else
                        status.setRollbackOnly()
                    throw e
                }
            }

            task.failedAt = null
            task.attempts = Math.max(0, task.attempts ?: 1)
            task.active = false
        } catch (CancelException c) {
            task.lastError = TenaciousUtil.parseStacktrace(c)
            task.attempts = Math.max(0, task.attempts) + 1
            task.failedAt = new Date()
            task.active = false
            log.info("task cancelled with id: ${task.id} params: ${task.params}  exception message: '${c.message}'")
        } catch (Exception e) {
            task.lastError = TenaciousUtil.parseStacktrace(e)
            task.attempts = Math.max(0, task.attempts) + 1
            task.failedAt = new Date()
            task.runAt = task.nextRunDate()

            if (persistentTask.maxAttempts && task.attempts > persistentTask.maxAttempts) {
                task.active = false
            }

            log.warn("failed execution of task id: ${task.id} params: ${task.params}  exception: ${e.stackTrace.join("\n\t")}")
        }

        return task
    }

    void performTasks(Map params, PersistentWorker worker) {
        def options = [failOnError: true, flush: false] << params
        Date now = new Date()
        Map config = (Map) TenaciousUtil.getStaticPropertyValue(worker.getClass(), "tenacious", Map, [:])
        Integer ma = (Integer) (worker.maxAttempts ?: config.maxAttempts as Integer)
        String qn = worker.queueName ?: config.queueName
        Integer max_jobs = worker.maxJobs

        try {
            worker.initWork()
            List<PersistentTaskData> taskData =
                    PersistentTaskData.createCriteria().list(order: "desc", sort: "priority", max: max_jobs, {
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

            for (t in taskData) {
                runTask(options, worker, t).save(failOnError: options.failOnError, flush: options.flush)

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

    def scheduleTask(Map<String, Object> params = [:], PersistentTask task, String actionName, String queue, boolean immediate = false) {
        //TODO: Throw excption if task in wrong queue
        String taskClassName = task.getClass().name
        Map config = (Map) TenaciousUtil.getStaticPropertyValue(task.getClass(), "tenacious", Map, [:])
        PersistentTaskData existingTaskData = null
        PersistentTaskData taskData = (actionName ? (existingTaskData =
                PersistentTaskData.where {
                    handler == taskClassName
                    action == actionName
                    active == true
                }.get()) : null) ?: new PersistentTaskData(handler: taskClassName)

        task.config()
        taskData.action = actionName
        taskData.priority = (Integer) (task.priority ?: config.priority ?: 1)
        taskData.queue = task.queueName ?: config.queueName ?: queue ?: "default"
        taskData.attempts = Math.max(0, taskData.attempts - 1)
        //TODO: If no queue specified, it should probably be on the on the class upon which it was scheduled
        if (!existingTaskData && task.minDelay?.intValue() > 0)
            taskData.runAt = DateTime.now().plusSeconds(task.minDelay).toDate()
        taskData.params = JsonOutput.toJson(params ?: [:])

        return immediate ?  resumeTask(taskData,task: task).save() : taskData.save()
    }

    def scheduleTask(Map<String, Object> params = [:], PersistentTask task, String action, boolean immediate = false) {
        this.scheduleTask(params, task, action, null, immediate)
    }
}

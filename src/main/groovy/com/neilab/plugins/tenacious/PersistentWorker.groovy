package com.neilab.plugins.tenacious

import groovy.transform.CompileStatic
import org.quartz.JobExecutionContext

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import groovy.json.*

trait PersistentWorker {
    Integer maxAttempts
    String  queueName

    def execute(JobExecutionContext context) {
        // println context.mergedJobDataMap.get('foo')
        //ExecutorService executor = Executors.newFixedThreadPool(4);

        def taskData = PersistentTaskData.where {
            if (maxAttempts) {
                attempts < maxAttempts
            }

            if(queueName) {
                queue == queueName
            }

            active == true
        }.order("priority", "DESC")

        for (row in taskData) {
            row.resume()
        }
    }

    static def scheduleTask(Map<String, Object> params = [:], Class<PersistentTask> taskClass, boolean immediate = false) {
        this.scheduleTask(params, taskClass.newInstance(), immediate)
    }

    static def scheduleTask(Map<String, Object> params = [:], PersistentTask task, boolean immediate = false) {
        Boolean processedTask = false
        PersistentTaskData taskData = new PersistentTaskData(handler: task.getClass().name)
        taskData.priority = task.priority
        taskData.queue = task.queueName
        taskData.params = JsonOutput.toJson(params ?: [:])


        if(immediate) {
            processedTask = taskData.resume(task: task)
        } else {
            taskData.save()
        }

        return taskData
    }

    private String parseStacktrace(Exception e) {
        StringWriter errors = new StringWriter()
        e.printStackTrace(new PrintWriter(errors))
        return e.toString()
    }
}

//class MyTask implements PersistentTask {}
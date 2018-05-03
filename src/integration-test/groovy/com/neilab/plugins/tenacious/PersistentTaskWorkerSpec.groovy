package com.neilab.plugins.tenacious

import com.neilab.plugins.tenacious.test.SampleJob
import com.neilab.plugins.tenacious.test.SampleTask
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
//import org.springframework.test.annotation.Rollback
import spock.lang.*

@Integration
@Rollback
class PersistentTaskWorkerSpec extends Specification  {

    void "task can be scheduled"() {
        given: "scheduled jobs"
            def task = [
                    sample1: SampleJob.scheduleTask(SampleTask, title:"sample 1",result:true),
                    sample2: SampleJob.scheduleTask(SampleTask,title:"sample 2",result:false)
            ]
            def total_count = PersistentTaskData.count()
            def active_count = [before_run: PersistentTaskData.isActive.count() ]
        when: "processing jobs"
            SampleJob.run(flush:true) //FIXME: Fix, while it works live, no inserts happen here despite correct save :(
            /* def first_row = PersistentTaskData.findById(1) //NOTE: This causes updated
            first_row.lastError = "foo"
            first_row.active = false
            first_row.save(flush:true) */
            active_count.after_run = PersistentTaskData.isActive.count()
        then: "then the successful job should not be active"
            active_count.after_run  == 1
            active_count.before_run == 2
            total_count == task.size()
    }

    protected void flushAndClear() {
        PersistentTaskData.withSession { session ->
            session.flush()
            session.clear()
        }
    }
}

package com.neilab.plugins.tenacious
import com.neilab.plugins.tenacious.test.*
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.grails.orm.hibernate.cfg.HibernateUtils

//import grails.test.mixin.integration.Integration
//import grails.transaction.Rollback
//import org.springframework.test.annotation.Rollback
import spock.lang.*

@Integration
@Rollback
class PersistentTaskWorkerSpec extends Specification  {
    def tenaciousService
    void "task can be scheduled"() {
        given: "scheduled jobs"
            def task = [
                    sample1: SampleJob.scheduleTask(SampleTask,title:"sample 1",result:true),
                    sample2: SampleJob.scheduleTask(SampleTask,title:"sample 2",result:false)
            ]
        and:
            flushAndClear() // without this, total_count == 0
        when:
            def total_count = PersistentTaskData.count()
            def active_count = [before_run: PersistentTaskData.isActive.count() ]
        then:
            total_count == 2
            active_count.before_run == 2
        when: "processing jobs"
            tenaciousService.performTasks(SampleJob, flush:false)
        and:
            flushAndClear()
        and:
            active_count.after_run =  PersistentTaskData.isActive.count()
        then: "then the successful job should not be active"
            active_count.after_run  == 1
            active_count.before_run == 2
            total_count == task.size()
    }

    protected def flushAndClear(Closure closure=null) {

        PersistentTaskData.withSession { session ->
            def  result = closure?.call()
            session.flush()
            session.clear()
            result
        }
    }
}

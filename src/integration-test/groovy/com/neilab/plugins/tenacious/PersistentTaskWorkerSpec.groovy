package com.neilab.plugins.tenacious
import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.*
import grails.transaction.Rollback
import spock.lang.*

@Integration
@Rollback
class PersistentTaskWorkerSpec extends Specification  {

    void "task can be scheduled"() {
        given:
            def scheduledTak = SampleJob.scheduleTask(SampleTask,param1:"test1",param2:"test2")
        when: ""
            def count = PersistentTaskData.count()

        then: ""
            count == 1
    }
}

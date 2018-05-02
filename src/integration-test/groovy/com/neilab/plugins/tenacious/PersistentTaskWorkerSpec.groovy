package com.neilab.plugins.tenacious

import com.neilab.plugins.tenacious.test.SampleJob
import com.neilab.plugins.tenacious.test.SampleTask
import grails.test.mixin.integration.Integration
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
            SampleJob.run()
        then: ""
            count == 1
    }
}

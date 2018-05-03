package com.neilab.plugins.tenacious.test

import com.neilab.plugins.tenacious.PersistentTask

class SampleTask implements PersistentTask {

    static tenacious = [maxAttempts: 1, queue: "default"]
    @Override
    def perform(Map params) {
        //System.out.println("perfrom: params = ${params}")
        return params.result
    }
}

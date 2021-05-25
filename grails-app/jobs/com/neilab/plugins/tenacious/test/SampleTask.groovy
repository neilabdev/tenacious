package com.neilab.plugins.tenacious.test

import com.neilab.plugins.tenacious.*

class SampleTask implements PersistentTask {

    static tenacious = [maxAttempts: 1, queue: "default"]

    @Override
    def perform(Map params) {
        return params.result
    }
}

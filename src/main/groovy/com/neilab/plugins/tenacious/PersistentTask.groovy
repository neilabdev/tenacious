package com.neilab.plugins.tenacious

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

trait PersistentTask {

    String queueName //= "default"
    Integer priority //= 1
    Integer maxAttempts = 5
    Integer maxRuntime //= 0
    Integer minDelay //= 0

    def config() {}
    def perform(Map params=[:]) {
        return false
    }
}


package com.neilab.plugins.tenacious

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

trait PersistentTask { // or PersistentTaskAbility

    String queueName = "default"
    Integer priority = 1
    Integer maxAttempts = 3
    Integer maxRuntime = 0
    Integer minDelay = 0

    def perform(Map params=[:]) {
      //  ExecutorService executor = Executors.newFixedThreadPool(4);
        return false
    }
}


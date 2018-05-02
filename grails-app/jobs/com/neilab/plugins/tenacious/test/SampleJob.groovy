package com.neilab.plugins.tenacious.test

import com.neilab.plugins.tenacious.PersistentWorker

class SampleJob implements PersistentWorker  {
    {
        this.maxAttempts = 3
        this.queueName = "default"
    }

    static tenacious = [maxAttempts: 1, queue: "default"]
}

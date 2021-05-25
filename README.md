# Tenacious



Tenacious  is a Job scheduler allowing transparent performance and retrying of task until successful or exhausted,  even across application restart, and was modeled and inspired by the equivalent rails plugin  *DelayedJob*.

Installation
------------

Add The Following to your `build.gradle`:

```groovy
dependencies {
    compile ':tenacious:1.0.0'
}
```

## Usage

Using *Tenacious* lingo, a *(Persistent)Worker* performs *Task* at a particular *interval* and *attempts* via a predefined  *Quarz* Job which inherits the *PersistentWorker* trait, which implements Tenacious behavior which adds default execution and various callbacks for task management. Example usage is below:

> grails-app/jobs/TenaciousJob.groovy
```groovy

import com.neilab.plugins.tenacious.*

class MyTenaciousJob implements PersistentWorker {
    def concurrent = false  // Quartz config option
    static tenacious = [ 
            queueName: "nameOfQueue", maxAttempts: 7, maxJobs: 250
    ] // takes precedence of  default initialization below if specified. OPTIONAL
    
    { // default initialization 
        queueName = "nameOfQueue" //if specified, only process jobs in this queue
        maxAttempts = 7 // maximum number of times a job can fail before giving
        maxJobs = 250 // maximum jobs to run per iteration
        restInterval = 0  // duration of rest between each run
    }

    @Override
    def initWork() {  
        // called once to init values before querying for multiple tasks. Settings can be updated here and takes precedence of above settings as its run after.
        maxAttempts = 10
        restInterval = 50  
    }

    @Override
    def beforeWork() {
       // called once before processing tasks returned
    }

    @Override
    def afterWork() {
       // called after all tasks have been processed
    }

    @Override
    def afterTask(PersistentTaskData data) {
        // called after a particular task has run.
        log.info("finished running task queue: ${data.queue} handler:${data.handler} action:${data.action} attempts: ${data.attempts} max: ${this.maxAttempts} active: active=${data.active} params:${data.params} error:${data.lastError}")
    }
}
```
## Scheduling Tasks
### Defining a Task
The first thing you must do before scheduling a task is to define by implementing the *PersistentTask* trait in the *tasks* directory. Thereafter onced scheduled, the *perform* method will be run until successful or *maxAttempts have exceeded. A successfull execution of *perform* occurs when no exception is thrown OR the function doesn't return *false*
> grails-app/tasks/CommentCreationTask.groovy
```groovy

class CommentCreationTask implements PersistentTask {
    def commentService 
    {
        maxAttempts = 3
    }
    
    @Override
    def perform(Map params) {
        User u = User.findbyId(params.user)  
        Article a = Article.findById(params.article)
        commentService.createComment(user:u, article: a, message: params.text) // communicates with external service, throws exception if fails
        return true // returning false fails attempt, as does throwing exception. Returning anything other than false is a successful task
    }
}
```


### Scheduling A Task

Once A Worker has been defined it can be scheduled to run. You may do this by executing the *scheduleTask* method on the *TenaciousService* or your custom *Worker* class.

For Example:

```groovy
MyTenaciousJob.scheduleTask(CommentCreationTask,user:user.id,article:article.id,text:"sample 2")
```

OR

```groovy
class MyControllerOrService {
    def tenaciousService
    
    def myMethodOrAction() {
        tenaciousService.scheduleTask(CommentCreationTask,user:user.id,article:article.id,text:"sample 2")
    }
}
```
## To Do

* More documentation and examples

## License

*Tenacious* is available under the MIT license. See the LICENSE file for more info.
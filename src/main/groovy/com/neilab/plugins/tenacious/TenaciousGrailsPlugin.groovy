package com.neilab.plugins.tenacious
import com.neilab.plugins.tenacious.artefact.TenaciousArtefactHandler
import grails.plugins.*

class TenaciousGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.0.0 > *"
    // resources that are excluded from plugin packaging
    def artefacts = [com.neilab.plugins.tenacious.artefact.TenaciousArtefactHandler]
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "**/com/neilab/plugins/tenacious/test/**"
    ]

    def watchedResources = [
            "file:./grails-app/tasks/**/*Task.groovy",
            "file:../../plugins/*/tasks/**/*Task.groovy",
            "file:./grails-app/services/**/*Service.groovy"
    ]

    // TODO Fill in these fields
    def title = "Tenacious" // Headline display name of the plugin
    def author = "ghost"
    def authorEmail = ""
    def description = '''\
Brief summary/description of the plugin.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/tenacious"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    //def dependsOn = [quartz: "* > 2.0"]
    def loadAfter = ['quartz']
    def observe = ['quartz']

    Closure doWithSpring() {
        { ->
            // TODO Implement runtime spring config (optional)
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)

        for (a in grailsApplication.getArtefacts("Job")) {
            if (PersistentWorker.isAssignableFrom(a.clazz)) { //}  a.clazz instanceof PersistentWorker) {
                a.clazz.metaClass.static.run = { Map params = [:] ->
                    String persistentWorkerClassName = a.clazz.name
                    PersistentWorker w = Class.forName(persistentWorkerClassName).newInstance()
                    w.runTasks(params)
                }
            }
        }

        for (ay in grailsApplication.getArtefacts("Task")) {
            //System.out.println("running task ${ay}")
        }

    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {

        if(event.source) {
            if (event.application.isArtefactOfType(TenaciousArtefactHandler.TYPE, event.source)) {
                def oldClass = event.application.getTaskClass(event.source.name)
                event.application.addArtefact(TenaciousArtefactHandler.TYPE, event.source)

                // Reload subclasses
                event.application.taskClasses.each {
                    if (it?.clazz != event.source && oldClass.clazz.isAssignableFrom(it?.clazz)) {
                        def newClass = event.application.classLoader.reloadClass(it.clazz.name)
                        event.application.addArtefact(TenaciousArtefactHandler.TYPE, newClass)
                    }
                }
            }
        }

    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }

    private boolean hasHibernate(manager) {
        manager?.hasGrailsPlugin("hibernate") ||
                manager?.hasGrailsPlugin("hibernate3") ||
                manager?.hasGrailsPlugin("hibernate4") ||
                manager?.hasGrailsPlugin("hibernate5")
    }
}

package com.neilab.plugins.tenacious

import grails.plugins.*
import static grails.plugins.quartz.GrailsJobClassConstants.*;

class  TenaciousGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.0.0 > *"
    // resources that are excluded from plugin packaging

    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "**/com/neilab/plugins/tenacious/test/**"
    ]

    // TODO Fill in these fields
    def title = "Tenacious" // Headline display name of the plugin
    def author = "Your name"
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

    Closure doWithSpring() { {->
            // TODO Implement runtime spring config (optional)
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)

        for (a in grailsApplication.getArtefacts("Job")) {
            if (PersistentWorker.isAssignableFrom(a.clazz) ) { //}  a.clazz instanceof PersistentWorker) {
                a.clazz.metaClass.static.run = {  ->
                    String persistentWorkerClassName = a.clazz.name
                    PersistentWorker w = Class.forName(persistentWorkerClassName).newInstance()
                    w.runTasks()

                     System.out.println("running Job ${a.clazz.name}")
                }
            }
        }

    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}

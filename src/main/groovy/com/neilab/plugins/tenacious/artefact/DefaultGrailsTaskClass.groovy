package com.neilab.plugins.tenacious.artefact

import org.grails.core.AbstractInjectableGrailsClass

class DefaultGrailsTaskClass extends AbstractInjectableGrailsClass implements  GrailsTaskClass {


    DefaultGrailsTaskClass(Class<?> clazz) {
        super(clazz, "Task")
    }
}

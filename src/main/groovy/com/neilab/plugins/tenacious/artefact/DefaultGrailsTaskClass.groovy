package com.neilab.plugins.tenacious.artefact

import grails.core.DefaultGrailsClass
import org.grails.core.AbstractInjectableGrailsClass

class DefaultGrailsTaskClass extends AbstractInjectableGrailsClass implements  GrailsTaskClass {


    DefaultGrailsTaskClass(Class<?> clazz) {
        super(clazz, TenaciousArtefactHandler.TYPE)
    }
/*
    @Override
    def perform(Map params) {
        getMetaClass().invokeMethod(getReferenceInstance(), "perform", [params]);
        return null
    } */
}

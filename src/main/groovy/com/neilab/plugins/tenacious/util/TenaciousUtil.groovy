package com.neilab.plugins.tenacious.util

import com.neilab.plugins.tenacious.*
import com.neilab.plugins.tenacious.PersistentTaskData
import com.neilab.plugins.tenacious.PersistentWorker
import grails.util.*
import grails.util.GrailsClassUtils
import groovy.json.JsonOutput
import org.joda.time.DateTime

class TenaciousUtil {

    static def getStaticPropertyValue(Class clazz, String propertyName, Class type, def defaultValue= null) {
        def value = GrailsClassUtils.getStaticPropertyValue(clazz,propertyName)

        if(type.isAssignableFrom(value.getClass())) {
            return value
        }

        return defaultValue
    }


    static String parseStacktrace(Exception e) {
        StringWriter errors = new StringWriter()
        e.printStackTrace(new PrintWriter(errors))
        return e.toString()
    }

}

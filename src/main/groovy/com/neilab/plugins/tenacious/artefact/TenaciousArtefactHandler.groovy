package com.neilab.plugins.tenacious.artefact

import com.neilab.plugins.tenacious.artefact.DefaultGrailsTaskClass
import com.neilab.plugins.tenacious.artefact.GrailsTaskClass
import grails.core.ArtefactHandlerAdapter

import java.util.regex.Pattern

import static org.grails.io.support.GrailsResourceUtils.GRAILS_APP_DIR
import static org.grails.io.support.GrailsResourceUtils.REGEX_FILE_SEPARATOR
class TenaciousArtefactHandler extends  ArtefactHandlerAdapter {

    static final String TYPE = "Task"

    public static Pattern TASK_PATH_PATTERN = Pattern.compile(".+" + REGEX_FILE_SEPARATOR + GRAILS_APP_DIR + REGEX_FILE_SEPARATOR + "tasks" + REGEX_FILE_SEPARATOR + "(.+)\\.(groovy)")

    TenaciousArtefactHandler() {
        super(TYPE, GrailsTaskClass.class, DefaultGrailsTaskClass.class, TYPE)
    }

    @Override
    boolean isArtefactClass(Class clazz) {
        if (!super.isArtefactClass(clazz))
            return false
        return clazz.methods.find {it.name == "perform" } != null
    }
}

package com.neilab.plugins.tenacious

import grails.transaction.Transactional
import grails.spring.BeanBuilder

@Transactional
class TenaciousFactoryService {
    def grailsApplication

    private Map <String,Class> cache = [:]

    protected def getPrototypeInstanceOf(def className) {
        Class taskClass = Class.forName(className)
        BeanBuilder beanBuilder = new BeanBuilder(grailsApplication.mainContext, grailsApplication.classLoader)
        String beanName = "prototype_${taskClass.simpleName}"

        beanBuilder.beans {
            "$beanName"(taskClass) { bean ->
                bean.autowire = 'byName'
            }
        }

        def applicationContext = beanBuilder.createApplicationContext()
        def instance = applicationContext.getBean(beanName)

        cache[className] = taskClass

        return instance
    }

    protected instantiateTask(taskClass ) {
        createBeanBuilder().with {
            beans {
                handler(taskClass) {
                    // autowire the handler so it can use services etc.
                    it.autowire = true
                }
            }
            createApplicationContext().getBean('handler')
        }
    }

    def getAt(String className) {
        def existingTask =  cache.containsKey(className) ?
                cache[className] : grailsApplication.getArtefact("Task",className)
        if(existingTask) {
            return getPrototypeInstanceOf(className)
        }
        return null
    }
}

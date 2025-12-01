package me.lucko.shadow

import kotlin.annotation.Target

/**
 * Marks a method on a [Shadow] that targets a static method or field.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Static 

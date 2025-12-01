package me.lucko.shadow

import kotlin.annotation.Target

/**
 * Marks that a method on a [Shadow] should map to a field on the target class.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Field 

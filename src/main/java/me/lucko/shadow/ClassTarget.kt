package me.lucko.shadow

import java.util.*
import java.util.function.Function
import kotlin.annotation.Target
import kotlin.reflect.KClass

/**
 * Defines a class target with a constant, known value.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ClassTarget(
    /**
     * Gets the value.
     *
     * @return the value
     */
    @get:NonNull val value: KClass<*>
) {
    companion object {
        /**
         * A [TargetResolver] for the [ClassTarget] annotation.
         */
        val RESOLVER: TargetResolver = object : TargetResolver {
            @Throws(ClassNotFoundException::class)
            override fun lookupClass(shadowClass: Class<out Shadow>): Optional<Class<*>> {
                val annotation = shadowClass.getAnnotation(ClassTarget::class.java) ?: return Optional.empty()
                return Optional.of(annotation.value.java)
            }
        }
    }
}

package me.lucko.shadow

import java.lang.reflect.Method
import java.util.*
import me.lucko.shadow.NonNull

/**
 * Defines a class, method or field target with a constant, known value.
 */
@kotlin.annotation.Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class Target(
    /**
     * Gets the value.
     *
     * @return the value
     */
    @get:NonNull val value: String
) {
    companion object {
        /**
         * A [TargetResolver] for the [Target] annotation.
         */
        val RESOLVER: TargetResolver = object : TargetResolver {
            @Throws(ClassNotFoundException::class)
            override fun lookupClass(shadowClass: Class<out Shadow>): Optional<Class<*>> {
                val annotation = shadowClass.getAnnotation(Target::class.java) ?: return Optional.empty()
                return Optional.of(Class.forName(annotation.value))
            }

            override fun lookupMethod(
                shadowMethod: Method,
                shadowClass: Class<out Shadow>,
                targetClass: Class<*>
            ): Optional<String> {
                return Optional.ofNullable(shadowMethod.getAnnotation(Target::class.java))
                    .map { it.value }
            }

            override fun lookupField(
                shadowMethod: Method,
                shadowClass: Class<out Shadow>,
                targetClass: Class<*>
            ): Optional<String> {
                return Optional.ofNullable(shadowMethod.getAnnotation(Target::class.java))
                    .map { it.value }
            }
        }
    }
}

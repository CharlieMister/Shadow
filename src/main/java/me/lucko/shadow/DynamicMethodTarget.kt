package me.lucko.shadow

import java.lang.reflect.Method
import java.util.*
import kotlin.annotation.Target
import kotlin.reflect.KClass

/**
 * Defines a method target with a dynamic value, calculated on demand by a function.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class DynamicMethodTarget(
    /**
     * Gets the loading function class.
     *
     *
     * An instance of the function is retrieved/constructed on demand by the implementation in
     * the following order.
     *
     *
     *
     *  * a static method named `getInstance` accepting no parameters and returning an instance of the implementation.
     *  * via a single enum constant, if the loading function class is an enum following the enum singleton pattern.
     *  * a static field named `instance` with the same type as and containing an instance of the implementation.
     *  * a static field named `INSTANCE` with the same type as and containing an instance of the implementation.
     *  * a no-args constructor
     *
     *
     *
     * Values defined for this property should be aware of this, and ensure an instance can be
     * retrieved/constructed.
     *
     * @return the loading function class
     */
    @get:NonNull val value: KClass<out Function>
) {
    /**
     * A functional interface encapsulating the target value computation.
     */
    fun interface Function {
        /**
         * Computes the target method for the given `shadowMethod`.
         *
         * @param shadowMethod the shadow method to compute a method target for
         * @param shadowClass the class defining the shadow method
         * @param targetClass the target class. the resultant method target should resolve for this class.
         * @return the target
         */
        @NonNull
        fun computeMethod(
            shadowMethod: Method,
            shadowClass: Class<out Shadow>,
            targetClass: Class<*>
        ): String
    }

    companion object {
        /**
         * A [TargetResolver] for the [DynamicMethodTarget] annotation.
         */
        val RESOLVER: TargetResolver = object : TargetResolver {
            override fun lookupMethod(
                shadowMethod: Method,
                shadowClass: Class<out Shadow>,
                targetClass: Class<*>
            ): Optional<String> {
                val annotation = shadowMethod.getAnnotation(DynamicMethodTarget::class.java)
                    ?: return Optional.empty()

                val function = Reflection.getInstance(
                    Function::class.java,
                    annotation.value.java
                )
                return Optional.of(function.computeMethod(shadowMethod, shadowClass, targetClass))
            }
        }
    }
}

package me.lucko.shadow

import java.util.*
import kotlin.annotation.Target
import kotlin.reflect.KClass

/**
 * Defines a class target with a dynamic value, calculated on demand by a function.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DynamicClassTarget(
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
         * Computes the target class for the given `shadowClass`.
         *
         * @param shadowClass the shadow class to compute a target for
         * @return the target
         * @throws ClassNotFoundException if the resultant target class cannot be loaded
         */
        @NonNull
        @Throws(ClassNotFoundException::class)
        fun computeClass(shadowClass: Class<out Shadow>): Class<*>
    }

    companion object {
        /**
         * A [TargetResolver] for the [DynamicClassTarget] annotation.
         */
        val RESOLVER: TargetResolver = object : TargetResolver {
            @Throws(ClassNotFoundException::class)
            override fun lookupClass(shadowClass: Class<out Shadow>): Optional<Class<*>> {
                val annotation = shadowClass.getAnnotation(DynamicClassTarget::class.java)
                    ?: return Optional.empty()

                val function = Reflection.getInstance(
                    Function::class.java,
                    annotation.value.java
                )
                return Optional.of(function.computeClass(shadowClass))
            }
        }
    }
}

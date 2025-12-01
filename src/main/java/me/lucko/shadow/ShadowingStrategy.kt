package me.lucko.shadow

import kotlin.reflect.KClass

/**
 * Defines the strategy to use when wrapping and unwrapping (shadow) objects.
 */
@kotlin.annotation.Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class ShadowingStrategy(
    /**
     * Gets the [Wrapper] function.
     *
     * An instance of the function is retrieved/constructed on demand by the implementation in
     * the following order:
     *  * a static method named `getInstance`
     *  * an enum singleton instance
     *  * a static field named `instance`
     *  * a static field named `INSTANCE`
     *  * a no-args constructor
     */
    val wrapper: KClass<out Wrapper> = Wrapper::class,

    /**
     * Gets the [Unwrapper] function.
     *
     * Uses the same resolution rules as [wrapper].
     */
    val unwrapper: KClass<out Unwrapper> = Unwrapper::class
) {
    /**
     * Wraps objects to [Shadow]s.
     */
    fun interface Wrapper {
        /**
         * Wraps the given [unwrapped] object to a shadow of [expectedType], using [shadowFactory].
         */
        @Throws(Exception::class)
        fun wrap(unwrapped: Any?, expectedType: Class<*>, shadowFactory: ShadowFactory): Any?
    }

    /**
     * Unwraps [Shadow]s to underlying objects.
     */
    interface Unwrapper {
        /**
         * Unwraps the given [wrapped] object to a non-shadow object.
         */
        @Throws(Exception::class)
        fun unwrap(wrapped: Any?, expectedType: Class<*>, shadowFactory: ShadowFactory): Any?

        /**
         * Unwraps a class type from a shadow type to its target type.
         */
        fun unwrap(wrappedClass: Class<*>, shadowFactory: ShadowFactory): Class<*>

        /**
         * Unwraps all of the given [wrapped] objects to non-shadow objects.
         */
        @Throws(Exception::class)
        fun unwrapAll(
            wrapped: Array<out Any?>?,
            expectedTypes: Array<Class<*>>,
            shadowFactory: ShadowFactory
        ): Array<Any?> {
            if (wrapped == null) {
                return emptyArray()
            }
            require(wrapped.size == expectedTypes.size) { "wrapped.length != expectedTypes.length" }

            val unwrapped = arrayOfNulls<Any>(wrapped.size)
            for (i in wrapped.indices) {
                unwrapped[i] = unwrap(wrapped[i], expectedTypes[i], shadowFactory)
            }
            @Suppress("UNCHECKED_CAST")
            return unwrapped as Array<Any?>
        }

        /**
         * Unwraps all of the given [wrapped] classes to their target types.
         */
        @Throws(Exception::class)
        fun unwrapAll(
            wrapped: Array<Class<*>>,
            shadowFactory: ShadowFactory
        ): Array<Class<*>> {
            val unwrapped = arrayOfNulls<Class<*>>(wrapped.size)
            for (i in wrapped.indices) {
                unwrapped[i] = unwrap(wrapped[i], shadowFactory)
            }
            @Suppress("UNCHECKED_CAST")
            return unwrapped as Array<Class<*>>
        }
    }

    /**
     * A (un)wrapper which wraps and unwraps shadow objects.
     *
     * Applied by default when a [ShadowingStrategy] is not defined.
     */
    enum class ForShadows : Wrapper, Unwrapper {
        INSTANCE;

        override fun wrap(unwrapped: Any?, expectedType: Class<*>, shadowFactory: ShadowFactory): Any? {
            if (unwrapped == null) {
                return null
            }

            if (Shadow::class.java.isAssignableFrom(expectedType)) {
                @Suppress("UNCHECKED_CAST")
                return shadowFactory.shadow(expectedType as Class<out Shadow>, unwrapped)
            }

            return unwrapped
        }

        override fun unwrap(wrapped: Any?, expectedType: Class<*>, shadowFactory: ShadowFactory): Any? {
            if (wrapped == null) {
                return null
            }

            if (wrapped is Shadow) {
                return wrapped.shadowTarget
            }

            return wrapped
        }

        override fun unwrap(wrappedClass: Class<*>, shadowFactory: ShadowFactory): Class<*> {
            return shadowFactory.getTargetClass(wrappedClass)
        }
    }

    /**
     * A (un)wrapper which wraps and unwraps one-dimensional shadow arrays.
     */
    enum class ForShadowArrays : Wrapper, Unwrapper {
        INSTANCE;

        override fun wrap(unwrapped: Any?, expectedType: Class<*>, shadowFactory: ShadowFactory): Any? {
            if (unwrapped == null) {
                return null
            }

            val unwrappedType = unwrapped.javaClass
            if (!unwrappedType.isArray) {
                throw RuntimeException("Object to be wrapped is not an array: $unwrappedType")
            }

            if (!expectedType.isArray) {
                throw RuntimeException("Expected type is not an array: $expectedType")
            }

            val wrappedArrayComponentType = expectedType.componentType
                ?: throw RuntimeException("Expected type has no component type: $expectedType")

            if (!Shadow::class.java.isAssignableFrom(wrappedArrayComponentType)) {
                throw RuntimeException("Expected type is not an array of shadow components: $wrappedArrayComponentType")
            }

            @Suppress("UNCHECKED_CAST")
            val unwrappedArray = unwrapped as Array<Any?>
            @Suppress("UNCHECKED_CAST")
            val wrappedArray = java.lang.reflect.Array.newInstance(
                wrappedArrayComponentType,
                unwrappedArray.size
            ) as Array<Any?>

            for (i in unwrappedArray.indices) {
                val o = unwrappedArray[i]
                if (o != null) {
                    @Suppress("UNCHECKED_CAST")
                    wrappedArray[i] = shadowFactory.shadow(
                        wrappedArrayComponentType as Class<out Shadow>,
                        o
                    )
                }
            }

            return wrappedArray
        }

        override fun unwrap(wrapped: Any?, expectedType: Class<*>, shadowFactory: ShadowFactory): Any? {
            if (wrapped == null) {
                return null
            }

            val wrappedType = wrapped.javaClass
            if (!wrappedType.isArray) {
                throw RuntimeException("Object to be unwrapped is not an array: $wrappedType")
            }

            if (!expectedType.isArray) {
                throw RuntimeException("Expected type is not an array: $expectedType")
            }

            val wrappedArrayComponentType = wrappedType.componentType
                ?: throw RuntimeException("Wrapped type has no component type: $wrappedType")

            if (!Shadow::class.java.isAssignableFrom(wrappedArrayComponentType)) {
                throw RuntimeException("Wrapped type is not an array of shadow components: $wrappedArrayComponentType")
            }

            @Suppress("UNCHECKED_CAST")
            val wrappedArray = wrapped as Array<Any?>
            @Suppress("UNCHECKED_CAST")
            val unwrappedArray = java.lang.reflect.Array.newInstance(
                expectedType.componentType
                    ?: throw RuntimeException("Expected type has no component type: $expectedType"),
                wrappedArray.size
            ) as Array<Any?>

            for (i in wrappedArray.indices) {
                val o = wrappedArray[i]
                if (o != null) {
                    unwrappedArray[i] = (o as Shadow).shadowTarget
                }
            }

            return unwrappedArray
        }

        override fun unwrap(wrappedClass: Class<*>, shadowFactory: ShadowFactory): Class<*> {
            if (!wrappedClass.isArray) {
                throw RuntimeException("Object to be unwrapped is not an array: $wrappedClass")
            }

            val unwrappedComponentType = shadowFactory.getTargetClass(
                wrappedClass.componentType
                    ?: throw RuntimeException("Wrapped type has no component type: $wrappedClass")
            )
            return java.lang.reflect.Array.newInstance(unwrappedComponentType, 0).javaClass
        }
    }
}

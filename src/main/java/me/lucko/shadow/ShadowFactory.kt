package me.lucko.shadow

import me.lucko.shadow.ShadowingStrategy.ForShadows
import me.lucko.shadow.ShadowingStrategy.Unwrapper
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.*
import java.util.function.Function
import java.util.function.Supplier

/**
 * Creates instances of [Shadow] interfaces.
 */
open class ShadowFactory {
    /**
     * Constructs a new shadow factory.
     */
    @NonNull
    private val shadows: LoadingMap<Class<out Shadow>, ShadowDefinition> =
        LoadingMap.of<Class<out Shadow>, ShadowDefinition>(
            Function { shadowClass: Class<out Shadow> -> this.initShadow(shadowClass) })

    @get:NonNull
    @NonNull
    internal val targetLookup: TargetLookup = TargetLookup()

    /**
     * Creates a shadow for the given object.
     *
     * @param shadowClass the class of the shadow definition
     * @param handle the handle object
     * @param <T> the shadow type
     * @return the shadow instance
    </T> */
    fun <T : Shadow> shadow(@NonNull shadowClass: Class<T>, @NonNull handle: Any): @NonNull T {
        Objects.requireNonNull(shadowClass, "shadowClass")
        Objects.requireNonNull(handle, "handle")

        // register the shadow first
        val shadowDefinition: ShadowDefinition = this.shadows.get(shadowClass)!!

        // ensure the target class of the shadow is assignable from the handle class
        val targetClass = shadowDefinition.targetClass
        require(targetClass.isAssignableFrom(handle.javaClass)) {
            "Target class " + targetClass.getName() + " is not assignable from handle class " + handle.javaClass.getName()
        }

        // return a proxy instance
        return createProxy(shadowClass, ShadowInvocationHandler(this, shadowDefinition, handle))
    }

    /**
     * Creates a static shadow for the given class.
     *
     * @param shadowClass the class of the shadow definition
     * @param <T> the shadow type
     * @return the shadow instance
    </T> */
    fun <T : Shadow> staticShadow(@NonNull shadowClass: Class<T>): @NonNull T {
        Objects.requireNonNull(shadowClass, "shadowClass")

        // register the shadow first
        val shadowDefinition = this.shadows.get(shadowClass)!!

        // return a proxy instance
        return createProxy(shadowClass, ShadowInvocationHandler(this, shadowDefinition, null))
    }

    /**
     * Creates a shadow for the given object, by invoking a constructor on the shadows
     * target.
     *
     * @param shadowClass the class of the shadow definition
     * @param args the arguments to pass to the constructor
     * @param <T> the shadow type
     * @return the shadow instance
    </T> */
    fun <T : Shadow> constructShadow(@NonNull shadowClass: Class<T>, @NonNull vararg args: Any?): @NonNull T {
        return constructShadow(shadowClass, ForShadows.INSTANCE, *args)
    }

    /**
     * Creates a shadow for the given object, by invoking a constructor on the shadows
     * target.
     *
     * @param shadowClass the class of the shadow definition
     * @param unwrapper the unwrapper to use
     * @param args the arguments to pass to the constructor
     * @param <T> the shadow type
     * @return the shadow instance
    </T> */
    fun <T : Shadow> constructShadow(
        @NonNull shadowClass: Class<T>,
        unwrapper: @NonNull Unwrapper,
        @NonNull vararg args: Any?
    ): @NonNull T {
        Objects.requireNonNull(shadowClass, "shadowClass")

        // register the shadow first
        val shadowDefinition: ShadowDefinition = this.shadows[shadowClass]!!

        @Suppress("UNCHECKED_CAST")
        val argumentTypes: Array<Class<*>> = ShadowInvocationHandler.getArgumentTypes(args as Array<Any?>, null)
        val unwrappedParameterTypes: Array<Class<*>>
        val unwrappedArguments: Array<Any?>
        try {
            unwrappedParameterTypes = unwrapper.unwrapAll(argumentTypes, this)
            @Suppress("UNCHECKED_CAST")
            unwrappedArguments = unwrapper.unwrapAll(args as Array<Any?>, unwrappedParameterTypes, this)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        val unwrappedArgumentTypes: Array<Class<*>> =
            ShadowInvocationHandler.getArgumentTypes(unwrappedArguments, unwrappedParameterTypes)

        val targetConstructor = shadowDefinition.findTargetConstructor(unwrappedArgumentTypes)

        val newInstance: Any?
        try {
            newInstance = targetConstructor.invokeWithArguments(*unwrappedArguments)
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }

        // create a shadow for the new instance
        return shadow(shadowClass, newInstance)
    }

    /**
     * Registers a target resolver with the shadow factory.
     *
     * @param targetResolver the resolver
     */
    fun registerTargetResolver(@NonNull targetResolver: TargetResolver?) {
        this.targetLookup.registerResolver(targetResolver)
    }

    @NonNull
    private fun initShadow(@NonNull shadowClass: Class<out Shadow>): ShadowDefinition {
        try {
            return ShadowDefinition(
                this,
                shadowClass,
                this.targetLookup.lookupClass(shadowClass)
                    .orElseThrow(Supplier {
                        IllegalStateException("Shadow class " + shadowClass.getName() + " does not have a defined target class.")
                    })
            )
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("Class not found for shadow " + shadowClass.getName(), e)
        }
    }

    @NonNull
    fun getTargetClass(@NonNull shadowClass: Class<*>): Class<*> {
        val definition = this.shadows.getIfPresent(shadowClass)
        return if (definition == null) shadowClass else definition.targetClass
    }

    companion object {
        private val INSTANCE = ShadowFactory()

        /**
         * Returns a shared static [ShadowFactory] instance.
         *
         * @return a shared instance
         */
        fun global(): ShadowFactory {
            return INSTANCE
        }

        private fun <T> createProxy(
            @NonNull interfaceType: Class<T>,
            @NonNull handler: InvocationHandler
        ): @NonNull T {
            val classLoader = interfaceType.getClassLoader()
            return interfaceType.cast(Proxy.newProxyInstance(classLoader, arrayOf<Class<*>>(interfaceType), handler))
        }
    }
}

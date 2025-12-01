package me.lucko.shadow

import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Base implementation of [TargetResolver] that delegates to the default + other registered
 * resolvers.
 */
internal class TargetLookup : TargetResolver {
    @NonNull
    private val resolvers: MutableList<TargetResolver> = CopyOnWriteArrayList(
        Arrays.asList(
            ClassTarget.Companion.RESOLVER,
            Target.Companion.RESOLVER,
            DynamicClassTarget.Companion.RESOLVER,
            DynamicMethodTarget.Companion.RESOLVER,
            DynamicFieldTarget.Companion.RESOLVER,
            FuzzyFieldTargetResolver.Companion.INSTANCE
        )
    )

    fun registerResolver(@NonNull targetResolver: TargetResolver?) {
        Objects.requireNonNull(targetResolver, "targetResolver")
        if (!this.resolvers.contains(targetResolver)) {
            this.resolvers.add(0, targetResolver!!)
        }
    }

    @NonNull
    @Throws(ClassNotFoundException::class)
    override fun lookupClass(shadowClass: Class<out Shadow>): Optional<Class<*>> {
        for (resolver in this.resolvers) {
            val result = resolver.lookupClass(shadowClass)
            if (result.isPresent) {
                return result
            }
        }
        return Optional.empty()
    }

    @NonNull
    override fun lookupMethod(
        shadowMethod: Method,
        shadowClass: Class<out Shadow>,
        targetClass: Class<*>
    ): Optional<String> {
        for (resolver in this.resolvers) {
            val result = resolver.lookupMethod(shadowMethod, shadowClass, targetClass)
            if (result.isPresent) {
                return result
            }
        }
        return Optional.empty()
    }

    @NonNull
    override fun lookupField(
        shadowMethod: Method,
        shadowClass: Class<out Shadow>,
        targetClass: Class<*>
    ): Optional<String> {
        for (resolver in this.resolvers) {
            val result = resolver.lookupField(shadowMethod, shadowClass, targetClass)
            if (result.isPresent) {
                return result
            }
        }
        return Optional.empty()
    }
}

package me.lucko.shadow.bukkit

import me.lucko.shadow.NonNull
import me.lucko.shadow.Shadow
import me.lucko.shadow.TargetResolver
import java.lang.reflect.Method
import java.util.*
import java.util.function.Function

/**
 * Defines a class, method or field target with a value that varies between package versions.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class ObfuscatedTarget(
    /**
     * Gets the mappings.
     *
     * @return the mappings
     */
    vararg val value: Mapping
) {
    companion object {
        /**
         * A [TargetResolver] for the [ObfuscatedTarget] annotation.
         */
        val RESOLVER: TargetResolver = object : TargetResolver {
            @Throws(ClassNotFoundException::class)
            override fun lookupClass(shadowClass: Class<out Shadow>): Optional<Class<*>> {
                val className =
                    Optional.ofNullable(shadowClass.getAnnotation(ObfuscatedTarget::class.java))
                        .flatMap { annotation: ObfuscatedTarget? ->
                            Arrays.stream(annotation!!.value)
                                .filter { mapping: Mapping? -> PackageVersion.runtimeVersion() === mapping!!.version }
                                .findFirst()
                        }
                        .map { mapping: Mapping? -> mapping!!.value }
                        .orElse(null)

                if (className == null) {
                    return Optional.empty()
                }
                return Optional.of(Class.forName(className))
            }

            override fun lookupMethod(
                shadowMethod: Method,
                shadowClass: Class<out Shadow>,
                targetClass: Class<*>
            ): Optional<String> {
                return Optional.ofNullable(
                    shadowMethod.getAnnotation(ObfuscatedTarget::class.java)
                )
                    .flatMap { annotation: ObfuscatedTarget? ->
                        Arrays.stream(annotation!!.value)
                            .filter { mapping: Mapping? -> PackageVersion.runtimeVersion() === mapping!!.version }
                            .findFirst()
                    }
                    .map { mapping: Mapping? -> mapping!!.value }
            }

            override fun lookupField(
                shadowMethod: Method,
                shadowClass: Class<out Shadow>,
                targetClass: Class<*>
            ): Optional<String> {
                return Optional.ofNullable(
                    shadowMethod.getAnnotation(ObfuscatedTarget::class.java)
                )
                    .flatMap { annotation: ObfuscatedTarget? ->
                        Arrays.stream(annotation!!.value)
                            .filter { mapping: Mapping? -> PackageVersion.runtimeVersion() === mapping!!.version }
                            .findFirst()
                    }
                    .map { mapping: Mapping? -> mapping!!.value }
            }
        }
    }
}

package me.lucko.shadow.bukkit

import me.lucko.shadow.NonNull
import me.lucko.shadow.Shadow
import me.lucko.shadow.TargetResolver
import java.util.*

/**
 * Defines a class target relative to the versioned 'net.minecraft.server' package.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class NmsClassTarget(
    /**
     * Gets the value of the class, relative to the versioned package.
     *
     * @return the value
     */
    @get:NonNull val value: String
) {
    companion object {
        /**
         * A [TargetResolver] for the [NmsClassTarget] annotation.
         */
        val RESOLVER: TargetResolver = object : TargetResolver {
            @Throws(ClassNotFoundException::class)
            override fun lookupClass(shadowClass: Class<out Shadow>): Optional<Class<*>> {
                val annotation = shadowClass.getAnnotation(NmsClassTarget::class.java)
                    ?: return Optional.empty()

                return Optional.of(PackageVersion.runtimeVersion().nmsClass(annotation.value))
            }
        }
    }
}

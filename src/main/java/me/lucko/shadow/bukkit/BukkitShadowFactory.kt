package me.lucko.shadow.bukkit

import me.lucko.shadow.NonNull
import me.lucko.shadow.ShadowFactory

/**
 * An extension of [ShadowFactory] with pre-registered target resolvers for
 * [NmsClassTarget], [ObcClassTarget] and [ObfuscatedTarget] annotations.
 */
class BukkitShadowFactory : ShadowFactory() {
    init {
        registerTargetResolver(NmsClassTarget.Companion.RESOLVER)
        registerTargetResolver(ObcClassTarget.Companion.RESOLVER)
        registerTargetResolver(ObfuscatedTarget.Companion.RESOLVER)
    }

    companion object {
        private val INSTANCE = BukkitShadowFactory()

        @NonNull
        fun global(): BukkitShadowFactory {
            return INSTANCE
        }
    }
}

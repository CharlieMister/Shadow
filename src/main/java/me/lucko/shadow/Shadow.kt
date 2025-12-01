package me.lucko.shadow

/**
 * Marks a "shadow" interface.
 *
 *
 * [Shadow]s are implemented at runtime by the [ShadowFactory].
 */
interface Shadow {
    @get:NonNull
    val shadowClass: Class<out Shadow>

    @get:Nullable
    val shadowTarget: Any?
}

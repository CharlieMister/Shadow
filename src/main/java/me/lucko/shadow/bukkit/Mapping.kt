package me.lucko.shadow.bukkit

/**
 * Represents a target value for a specific [PackageVersion].
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Mapping(
    /**
     * Gets the package version.
     *
     * @return the package version
     */
    val version: PackageVersion,
    /**
     * Gets the target value.
     *
     * @return the value
     */
    val value: String
)

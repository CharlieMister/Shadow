package me.lucko.shadow.bukkit

import me.lucko.shadow.NonNull
import org.bukkit.Bukkit
import java.util.*

/**
 * An enumeration of CraftBukkit package versions.
 */
enum class PackageVersion @JvmOverloads constructor(useModern: Boolean = false) {
    NONE {
        @NonNull
        override fun getPackageComponent(): String {
            return "."
        }
    },
    v1_7_R1,
    v1_7_R2,
    v1_7_R3,
    v1_7_R4,
    v1_8_R1,
    v1_8_R2,
    v1_8_R3,
    v1_9_R1,
    v1_9_R2,
    v1_10_R1,
    v1_11_R1,
    v1_12_R1,
    v1_13_R1,
    v1_13_R2,
    v1_14_R1,
    v1_15_R1,
    v1_16_R1,
    v1_16_R2,
    v1_16_R3,
    v1_17_R1(true),
    v1_18_R1(true),
    v1_18_R2(true),
    v1_19_R1(true),
    v1_19_R2(true),
    v1_19_R3(true),
    v1_20_R1(true),
    v1_20_R2(true),
    v1_20_R3(true),
    ;

    @NonNull
    private val nmsPrefix: String

    @NonNull
    private val obcPrefix: String

    @NonNull
    protected open fun getPackageComponent(): String {
        return ".$name."
    }

    /**
     * Prepends the versioned NMS prefix to the given class name
     *
     * @param className the name of the class
     * @return the full class name
     */
    @NonNull
    fun nms(@NonNull className: String): String {
        Objects.requireNonNull<String?>(className, "className")
        return this.nmsPrefix + className
    }

    /**
     * Prepends the versioned NMS prefix to the given class name
     *
     * @param className the name of the class
     * @return the class represented by the full class name
     */
    @NonNull
    @Throws(ClassNotFoundException::class)
    fun nmsClass(@NonNull className: String): Class<*> {
        return Class.forName(nms(className))
    }

    /**
     * Prepends the versioned OBC prefix to the given class name
     *
     * @param className the name of the class
     * @return the full class name
     */
    @NonNull
    fun obc(@NonNull className: String): String {
        Objects.requireNonNull<String?>(className, "className")
        return this.obcPrefix + className
    }

    /**
     * Prepends the versioned OBC prefix to the given class name
     *
     * @param className the name of the class
     * @return the class represented by the full class name
     */
    @NonNull
    @Throws(ClassNotFoundException::class)
    fun obcClass(@NonNull className: String): Class<*> {
        return Class.forName(obc(className))
    }

    private fun checkComparable(other: PackageVersion?) {
        Objects.requireNonNull<PackageVersion?>(other, "other")
        require(this !== PackageVersion.NONE) { "this cannot be NONE" }
        require(other !== PackageVersion.NONE) { "other cannot be NONE" }
    }

    /**
     * Gets if this version comes before the `other` version.
     *
     * @param other the other version
     * @return if it comes before
     */
    fun isBefore(@NonNull other: PackageVersion): Boolean {
        checkComparable(other)
        return this.ordinal < other.ordinal
    }

    /**
     * Gets if this version comes after the `other` version.
     *
     * @param other the other version
     * @return if it comes after
     */
    fun isAfter(@NonNull other: PackageVersion): Boolean {
        checkComparable(other)
        return this.ordinal > other.ordinal
    }

    /**
     * Gets if this version is the same as or comes before the `other` version.
     *
     * @param other the other version
     * @return if it comes before or is the same
     */
    fun isBeforeOrEq(@NonNull other: PackageVersion): Boolean {
        checkComparable(other)
        return this.ordinal <= other.ordinal
    }

    /**
     * Gets if this version is the same as or comes after the `other` version.
     *
     * @param other the other version
     * @return if it comes after or is the same
     */
    fun isAfterOrEq(@NonNull other: PackageVersion): Boolean {
        checkComparable(other)
        return this.ordinal >= other.ordinal
    }

    init {
        val nmsBase = "net.minecraft.server"
        val nmsModernBase = "net.minecraft."
        val obcBase = "org.bukkit.craftbukkit"

        this.nmsPrefix = if (useModern) nmsModernBase else (nmsBase + getPackageComponent())
        this.obcPrefix = obcBase + getPackageComponent()
    }

    companion object {
        private val RUNTIME_VERSION_STRING: String
        private val RUNTIME_VERSION: PackageVersion?

        init {
            var serverVersion = ""
            // check we're dealing with a "CraftServer" and that the server isn't non-versioned.
            val server: Class<*> = Bukkit.getServer().javaClass
            if (server.getSimpleName() == "CraftServer" && server.getName() != "org.bukkit.craftbukkit.CraftServer") {
                val obcPackage = server.getPackage().getName()
                // check we're dealing with a craftbukkit implementation.
                if (obcPackage.startsWith("org.bukkit.craftbukkit.")) {
                    // return the package version.
                    serverVersion = obcPackage.substring("org.bukkit.craftbukkit.".length)
                }
            }
            RUNTIME_VERSION_STRING = serverVersion

            var runtimeVersion: PackageVersion? = null
            if (RUNTIME_VERSION_STRING.isEmpty()) {
                runtimeVersion = PackageVersion.NONE
            } else {
                try {
                    runtimeVersion = valueOf(serverVersion)
                } catch (e: IllegalArgumentException) {
                    // ignore
                }
            }
            RUNTIME_VERSION = runtimeVersion
        }

        /**
         * Gets the package version for the current runtime server instance.
         *
         * @return the package version of the current runtime
         */
        @NonNull
        fun runtimeVersion(): PackageVersion {
            checkNotNull(RUNTIME_VERSION) { "Unknown package version: " + RUNTIME_VERSION_STRING }
            return RUNTIME_VERSION
        }
    }
}


package me.lucko.shadow

import java.lang.reflect.Method
import java.util.*

/**
 * Resolves the concrete target classes, methods and fields for shadow types.
 *
 * The default implementations mirror the upstream Java defaults and just return empty.
 */
interface TargetResolver {
    /**
     * Attempts to find the corresponding target class for the given shadow class.
     *
     * @param shadowClass the shadow class
     * @return the target, if any
     * @throws ClassNotFoundException if the resultant target class cannot be loaded
     */
    @Throws(ClassNotFoundException::class)
    fun lookupClass(shadowClass: Class<out Shadow>): Optional<Class<*>> {
        return Optional.empty()
    }

    /**
     * Attempts to find the corresponding target method name for the given shadow method.
     *
     * @param shadowMethod the shadow method to lookup a target method for
     * @param shadowClass the class defining the shadow method
     * @param targetClass the target class. the resultant method should resolve for this class.
     * @return the target, if any
     */
    fun lookupMethod(
        shadowMethod: Method,
        shadowClass: Class<out Shadow>,
        targetClass: Class<*>
    ): Optional<String> {
        return Optional.empty()
    }

    /**
     * Attempts to find the corresponding target field name for the given shadow method.
     *
     * @param shadowMethod the shadow method to lookup a target field for
     * @param shadowClass the class defining the shadow method
     * @param targetClass the target class. the resultant field should resolve for this class.
     * @return the target, if any
     */
    fun lookupField(
        shadowMethod: Method,
        shadowClass: Class<out Shadow>,
        targetClass: Class<*>
    ): Optional<String> {
        return Optional.empty()
    }
}

package me.lucko.shadow

import java.lang.Byte
import java.lang.Double
import java.lang.Float as JFloat
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.Array
import kotlin.Boolean
import kotlin.Char
import kotlin.Int
import kotlin.Long
import kotlin.Short
import kotlin.String

internal object BeanUtils {
    /**
     * Find an accessible method that matches the given name and has compatible parameters.
     *
     *
     * Compatible parameters mean that every method parameter is assignable from
     * the given parameters. In other words, it finds a method with the given name
     * that will take the parameters given.
     *
     *
     * This method is slightly undeterministic since it loops
     * through methods names and return the first matching method.
     *
     *
     * This method can match primitive parameter by passing in wrapper classes.
     * For example, a `Boolean` will match a primitive `boolean`
     * parameter.
     *
     * @param clazz          find method in this class
     * @param methodName     find method with this name
     * @param parameterTypes find method with compatible parameters
     * @return The accessible method
     */
    fun getMatchingMethod(clazz: Class<*>, methodName: String, parameterTypes: Array<Class<*>>): Method? {
        // try exact match
        try {
            val method = clazz.getDeclaredMethod(methodName, *parameterTypes)
            Reflection.ensureAccessible(method)
            return method
        } catch (e: NoSuchMethodException) {
            // ignore
        }

        // search through all methods
        var bestMatch: Method? = null
        var bestMatchCost = JFloat.MAX_VALUE

        search@ for (method in clazz.getDeclaredMethods()) {
            if (method.getName() != methodName) {
                continue
            }

            // compare parameters
            val methodsParams = method.getParameterTypes()
            if (methodsParams.size != parameterTypes.size) {
                continue
            }

            for (i in methodsParams.indices) {
                if (!isAssignmentCompatible(methodsParams[i], parameterTypes[i])) {
                    continue@search
                }
            }

            val cost = getTotalTransformationCost(parameterTypes, method.parameterTypes)
            if (cost < bestMatchCost) {
                bestMatch = method
                bestMatchCost = cost
            }
        }

        if (bestMatch == null && clazz.superclass != null) {
            bestMatch = getMatchingMethod(clazz.superclass, methodName, parameterTypes)
        }

        if (bestMatch == null && clazz.interfaces != null) {
            val interfaces = clazz.interfaces
            for (i in interfaces) {
                bestMatch = getMatchingMethod(i, methodName, parameterTypes)
                if (bestMatch != null) {
                    break
                }
            }
        }

        return bestMatch
    }

    fun getMatchingConstructor(clazz: Class<*>, parameterTypes: Array<Class<*>>): Constructor<*>? {
        // try exact match
        try {
            val constructor: Constructor<*> = clazz.getDeclaredConstructor(*parameterTypes)
            Reflection.ensureAccessible(constructor)
            return constructor
        } catch (e: NoSuchMethodException) {
            // ignore
        }

        // search through all methods
        var bestMatch: Constructor<*>? = null
        var bestMatchCost = JFloat.MAX_VALUE

        search@ for (constructor in clazz.declaredConstructors) {
            // compare parameters
            val methodsParams = constructor.parameterTypes
            if (methodsParams.size != parameterTypes.size) {
                continue
            }

            for (n in methodsParams.indices) {
                if (!isAssignmentCompatible(methodsParams[n], parameterTypes[n])) {
                    continue@search
                }
            }

            val cost = getTotalTransformationCost(parameterTypes, constructor.parameterTypes)
            if (cost < bestMatchCost) {
                bestMatch = constructor
                bestMatchCost = cost
            }
        }

        return bestMatch
    }

    /**
     * Returns the sum of the object transformation cost for each class in the source
     * argument list.
     *
     * @param srcArgs  The source arguments
     * @param destArgs The destination arguments
     * @return The total transformation cost
     */
    private fun getTotalTransformationCost(srcArgs: Array<Class<*>>, destArgs: Array<Class<*>>): Float {
        var totalCost = 0.0f
        for (i in srcArgs.indices) {
            val srcClass: Class<*> = srcArgs[i]
            val destClass: Class<*> = destArgs[i]
            totalCost += getObjectTransformationCost(srcClass, destClass)
        }

        return totalCost
    }

    /**
     * Gets the number of steps required needed to turn the source class into the
     * destination class. This represents the number of steps in the object hierarchy
     * graph.
     *
     * @param srcClass  The source class
     * @param destClass The destination class
     * @return The cost of transforming an object
     */
    private fun getObjectTransformationCost(srcClass: Class<*>?, destClass: Class<*>): Float {
        var srcClass = srcClass
        var cost = 0.0f
        while (srcClass != null && destClass != srcClass) {
            if (destClass.isPrimitive()) {
                val destClassWrapperClazz = getPrimitiveWrapper(destClass)
                if (destClassWrapperClazz != null && destClassWrapperClazz == srcClass) {
                    cost += 0.25f
                    break
                }
            }
            if (destClass.isInterface() && isAssignmentCompatible(destClass, srcClass)) {
                // slight penalty for interface match.
                // we still want an exact match to override an interface match, but
                // an interface match should override anything where we have to get a
                // superclass.
                cost += 0.25f
                break
            }
            cost++
            srcClass = srcClass.getSuperclass()
        }

        /*
         * If the destination class is null, we've travelled all the way up to
         * an Object match. We'll penalize this by adding 1.5 to the cost.
         */
        if (srcClass == null) {
            cost += 1.5f
        }

        return cost
    }

    /**
     *
     * Determine whether a type can be used as a parameter in a method invocation.
     * This method handles primitive conversions correctly.
     *
     *
     * In order words, it will match a `Boolean` to a `boolean`,
     * a `Long` to a `long`,
     * a `Float` to a `float`,
     * a `Integer` to a `int`,
     * and a `Double` to a `double`.
     * Now logic widening matches are allowed.
     * For example, a `Long` will not match a `int`.
     *
     * @param parameterType    the type of parameter accepted by the method
     * @param parameterization the type of parameter being tested
     * @return true if the assignment is compatible.
     */
    private fun isAssignmentCompatible(parameterType: Class<*>, parameterization: Class<*>): Boolean {
        // try plain assignment
        if (parameterType.isAssignableFrom(parameterization)) {
            return true
        }

        if (parameterType.isPrimitive()) {
            val parameterWrapperClazz = getPrimitiveWrapper(parameterType)
            if (parameterWrapperClazz != null) {
                return parameterWrapperClazz == parameterization
            }
        }

        return false
    }

    private fun getPrimitiveWrapper(primitiveType: Class<*>?): Class<*>? {
        if (primitiveType == Integer.TYPE) {
            return Int::class.java
        }
        if (primitiveType == java.lang.Long.TYPE) {
            return Long::class.java
        }
        if (primitiveType == java.lang.Boolean.TYPE) {
            return Boolean::class.java
        }
        if (primitiveType == Byte.TYPE) {
            return kotlin.Byte::class.java
        }
        if (primitiveType == Character.TYPE) {
            return Char::class.java
        }
        if (primitiveType == JFloat.TYPE) {
            return kotlin.Float::class.java
        }
        if (primitiveType == Double.TYPE) {
            return kotlin.Double::class.java
        }
        if (primitiveType == java.lang.Short.TYPE) {
            return Short::class.java
        }
        if (primitiveType == Void.TYPE) {
            return Void::class.java
        }
        return null
    }
}

package me.lucko.shadow

import java.lang.invoke.MethodHandles
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException

/**
 * A utility for constructing private method handles.
 */
internal object PrivateMethodHandles {
    @NonNull
    private val LOOKUP_CONSTRUCTOR: Constructor<MethodHandles.Lookup>

    init {
        try {
            LOOKUP_CONSTRUCTOR =
                MethodHandles.Lookup::class.java.getDeclaredConstructor(Class::class.java, Int::class.javaPrimitiveType)
            LOOKUP_CONSTRUCTOR.setAccessible(true)
        } catch (e: NoSuchMethodException) {
            throw ExceptionInInitializerError(e)
        }
    }

    /**
     * Returns a [lookup object][MethodHandles.Lookup] with full capabilities to emulate all
     * supported bytecode behaviors, including private access, on a target class.
     *
     * @param targetClass the target class
     * @return a lookup object for the target class, with private access
     */
    fun forClass(@NonNull targetClass: Class<*>?): @NonNull MethodHandles.Lookup {
        try {
            return LOOKUP_CONSTRUCTOR.newInstance(
                targetClass,
                MethodHandles.Lookup.PUBLIC or MethodHandles.Lookup.PRIVATE or MethodHandles.Lookup.PROTECTED or MethodHandles.Lookup.PACKAGE
            )
        } catch (e: InstantiationException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException(e)
        }
    }
}

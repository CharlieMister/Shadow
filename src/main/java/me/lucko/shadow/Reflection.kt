package me.lucko.shadow

import java.lang.reflect.*
import java.lang.reflect.Field

internal object Reflection {
    fun ensureStatic(member: Member) {
        require(Modifier.isStatic(member.getModifiers()))
    }

    fun <T : Any> getInstance(@NonNull returnType: Class<T>, @NonNull implementationType: Class<out T>): @NonNull T {
        try {
            val getInstanceMethod = implementationType.getDeclaredMethod("getInstance")
            ensureStatic(getInstanceMethod)
            require(getInstanceMethod.getParameterCount() == 0)
            require(returnType.isAssignableFrom(getInstanceMethod.getReturnType()))
            ensureAccessible(getInstanceMethod)
            return getInstanceMethod.invoke(null) as T
        } catch (e: Exception) {
            // ignore
        }

        if (implementationType.isEnum()) {
            val enumConstants = implementationType.enumConstants
            if (enumConstants != null && enumConstants.size == 1) {
                return enumConstants[0]
            }
        }

        try {
            val instanceField = implementationType.getDeclaredField("instance")
            ensureStatic(instanceField)
            require(returnType.isAssignableFrom(instanceField.getType()))
            ensureAccessible(instanceField)
            return instanceField.get(null) as T
        } catch (e: Exception) {
            // ignore
        }

        try {
            val instanceField = implementationType.getDeclaredField("INSTANCE")
            ensureStatic(instanceField)
            require(returnType.isAssignableFrom(instanceField.getType()))
            ensureAccessible(instanceField)
            return instanceField.get(null) as T
        } catch (e: Exception) {
            // ignore
        }

        try {
            val constructor: Constructor<out T> = implementationType.getDeclaredConstructor()
            ensureAccessible(constructor)
            return constructor.newInstance()
        } catch (e: Exception) {
            // ignore
        }

        throw RuntimeException("Unable to obtain an instance of " + implementationType.getName())
    }

    fun ensureAccessible(accessibleObject: AccessibleObject) {
        if (!accessibleObject.isAccessible()) {
            accessibleObject.setAccessible(true)
        }
    }

    fun ensureModifiable(field: Field) {
        if (Modifier.isFinal(field.getModifiers())) {
            try {
                val modifierField = Field::class.java.getDeclaredField("modifiers")
                modifierField.setAccessible(true)
                modifierField.setInt(field, field.getModifiers() and Modifier.FINAL.inv())
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            } catch (e: NoSuchFieldException) {
                throw RuntimeException(e)
            }
        }
    }

    fun findField(searchClass: Class<*>, fieldName: String): Field? {
        var searchClass = searchClass
        var field: Field? = null
        do {
            try {
                field = searchClass.getDeclaredField(fieldName)
            } catch (ignored: NoSuchFieldException) {
                searchClass = searchClass.getSuperclass()
            }
        } while (field == null && searchClass != Any::class.java)
        return field
    }
}

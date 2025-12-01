package me.lucko.shadow

import java.lang.invoke.MethodHandle
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Arrays
import java.util.function.Function

/**
 * Represents a processed [Shadow] definition.
 */
internal class ShadowDefinition(
    private val shadowFactory: ShadowFactory,
    val shadowClass: Class<out Shadow>,
    val targetClass: Class<*>
) {

    // caches
    private val methods: LoadingMap<MethodInfo, TargetMethod> =
        LoadingMap.of(Function { methodInfo: MethodInfo -> loadTargetMethod(methodInfo) })

    private val fields: LoadingMap<FieldInfo, TargetField> =
        LoadingMap.of(Function { fieldInfo: FieldInfo -> loadTargetField(fieldInfo) })

    private val constructors: LoadingMap<ConstructorInfo, MethodHandle> =
        LoadingMap.of(Function { constructorInfo: ConstructorInfo -> loadTargetConstructor(constructorInfo) })

    fun findTargetMethod(shadowMethod: Method, argumentTypes: Array<Class<*>>): TargetMethod {
        return methods[MethodInfo(shadowMethod, argumentTypes, shadowMethod.isAnnotationPresent(Static::class.java))]!!
    }

    fun findTargetField(shadowMethod: Method): TargetField {
        return fields[FieldInfo(shadowMethod, shadowMethod.isAnnotationPresent(Static::class.java))]!!
    }

    fun findTargetConstructor(argumentTypes: Array<Class<*>>): MethodHandle {
        return constructors[ConstructorInfo(argumentTypes)]!!
    }

    private fun loadTargetMethod(methodInfo: MethodInfo): TargetMethod {
        val shadowMethod = methodInfo.shadowMethod
        val methodName = shadowFactory.targetLookup
            .lookupMethod(shadowMethod, shadowClass, targetClass)
            .orElseGet { shadowMethod.name }

        val method = BeanUtils.getMatchingMethod(targetClass, methodName, methodInfo.argumentTypes)
            ?: throw RuntimeException(NoSuchMethodException("${targetClass.name}.$methodName"))

        if (methodInfo.isStatic && !Modifier.isStatic(method.modifiers)) {
            throw RuntimeException("Shadow method $shadowMethod is marked as static, but the target method $method is not.")
        }
        if (!methodInfo.isStatic && Modifier.isStatic(method.modifiers)) {
            throw RuntimeException("Shadow method $shadowMethod is not marked as static, but the target method $method is.")
        }

        Reflection.ensureAccessible(method)

        return try {
            TargetMethod(method)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }
    }

    private fun loadTargetField(fieldInfo: FieldInfo): TargetField {
        val shadowMethod = fieldInfo.shadowMethod

        val fieldName = shadowFactory.targetLookup
            .lookupField(shadowMethod, shadowClass, targetClass)
            .orElseGet { shadowMethod.name }

        val field = Reflection.findField(targetClass, fieldName)
            ?: throw RuntimeException(NoSuchFieldException("${targetClass.name}#$fieldName"))

        if (fieldInfo.isStatic && !Modifier.isStatic(field.modifiers)) {
            throw RuntimeException("Shadow method $shadowMethod is marked as static, but the target field $field is not.")
        }
        if (!fieldInfo.isStatic && Modifier.isStatic(field.modifiers)) {
            throw RuntimeException("Shadow method $shadowMethod is not marked as static, but the target field $field is.")
        }

        Reflection.ensureAccessible(field)
        Reflection.ensureModifiable(field)

        return try {
            TargetField(field)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }
    }

    private fun loadTargetConstructor(constructorInfo: ConstructorInfo): MethodHandle {
        val constructor: Constructor<*>? =
            BeanUtils.getMatchingConstructor(targetClass, constructorInfo.argumentTypes)
        if (constructor == null) {
            throw RuntimeException(
                NoSuchMethodException(
                    "${targetClass.name}.<init> - ${Arrays.toString(constructorInfo.argumentTypes)}"
                )
            )
        }

        Reflection.ensureAccessible(constructor)

        return try {
            PrivateMethodHandles.forClass(constructor.declaringClass).unreflectConstructor(constructor)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }
    }

    private class MethodInfo(
        val shadowMethod: Method,
        val argumentTypes: Array<Class<*>>,
        val isStatic: Boolean
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false

            val that = other as MethodInfo
            return shadowMethod == that.shadowMethod
        }

        override fun hashCode(): Int {
            return shadowMethod.hashCode()
        }
    }

    private class FieldInfo(val shadowMethod: Method, val isStatic: Boolean) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false

            val that = other as FieldInfo
            return shadowMethod == that.shadowMethod
        }

        override fun hashCode(): Int {
            return shadowMethod.hashCode()
        }
    }

    private class ConstructorInfo(val argumentTypes: Array<Class<*>>) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false

            val that = other as ConstructorInfo
            return Arrays.equals(argumentTypes, that.argumentTypes)
        }

        override fun hashCode(): Int {
            return Arrays.hashCode(argumentTypes)
        }
    }

    internal class TargetField(field: Field) {
        private val field: Field
        private val getter: MethodHandle
        private val setter: MethodHandle

        init {
            this.field = field
            val lookup = PrivateMethodHandles.forClass(field.declaringClass)
            this.getter = lookup.unreflectGetter(field)
            this.setter = lookup.unreflectSetter(field)
        }

        fun underlyingField(): Field {
            return field
        }

        fun getterHandle(): MethodHandle {
            return getter
        }

        fun setterHandle(): MethodHandle {
            return setter
        }
    }

    internal class TargetMethod(method: Method) {
        private val method: Method
        private val handle: MethodHandle

        init {
            this.method = method
            this.handle = PrivateMethodHandles.forClass(method.declaringClass).unreflect(method)
        }

        fun underlyingMethod(): Method {
            return method
        }

        fun handle(): MethodHandle {
            return handle
        }
    }
}

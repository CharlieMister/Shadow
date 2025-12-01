package me.lucko.shadow

import me.lucko.shadow.ShadowingStrategy.ForShadows
import me.lucko.shadow.ShadowingStrategy.Unwrapper
import me.lucko.shadow.ShadowingStrategy.Wrapper
import java.lang.invoke.MethodHandle
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.Objects

/**
 * Invocation handler for [Shadow]s.
 */
internal class ShadowInvocationHandler(
    private val shadowFactory: ShadowFactory,
    private val shadow: ShadowDefinition,
    private val handle: Any?
) : InvocationHandler {

    @Throws(Throwable::class)
    override fun invoke(shadowInstance: Any?, shadowMethod: Method, argsIn: Array<Any?>?): Any? {
        var args = argsIn

        // implement methods in Shadow
        if (shadowMethod == GET_SHADOW_TARGET_METHOD) {
            return handle
        }
        if (shadowMethod == GET_SHADOW_CLASS_METHOD) {
            return shadow.shadowClass
        }

        // implement some Object methods
        if (shadowMethod == OBJECT_TOSTRING_METHOD) {
            return "Shadow(shadowClass=${shadow.shadowClass}, targetClass=${shadow.targetClass}, target=$handle)"
        }
        if (shadowMethod == OBJECT_EQUALS_METHOD) {
            val otherObject = args!![0]
            if (otherObject === this) {
                return true
            }
            if (otherObject !is Shadow) {
                return false
            }
            val other = otherObject
            return shadow.shadowClass == other.shadowClass && Objects.equals(handle, other.shadowTarget)
        }
        if (shadowMethod == OBJECT_HASHCODE_METHOD) {
            return shadow.shadowClass.hashCode() xor Objects.hashCode(handle)
        }

        // just execute default methods on the proxy object itself
        if (shadowMethod.isDefault) {
            val declaringClass = shadowMethod.declaringClass
            return PrivateMethodHandles.forClass(declaringClass)
                .unreflectSpecial(shadowMethod, declaringClass)
                .bindTo(shadowInstance)
                .invokeWithArguments(*(args ?: emptyArray()))
        }

        if (args == null) {
            args = emptyArray()
        }

        val returnValue: Any?

        if (shadowMethod.isAnnotationPresent(Field::class.java)) {
            val targetField = shadow.findTargetField(shadowMethod)

            if (args!!.isEmpty()) {
                // getter
                returnValue = bindWithHandle(targetField.getterHandle(), shadowMethod).invoke()
            } else if (args.size == 1) {
                // setter
                val setter = bindWithHandle(targetField.setterHandle(), shadowMethod)
                val unwrapper = getUnwrapper(shadowMethod)
                val unwrappedType = unwrapper.unwrap(shadowMethod.parameterTypes[0], shadowFactory)
                val value = unwrapper.unwrap(args[0], unwrappedType, shadowFactory)
                setter.invokeWithArguments(value)

                returnValue = if (shadowMethod.returnType == Void.TYPE) {
                    null
                } else {
                    // allow chaining
                    handle
                }
            } else {
                throw IllegalStateException(
                    "Unable to determine accessor type (getter/setter) for " +
                        shadow.targetClass.name + "#" + shadowMethod.name
                )
            }
        } else {
            // assume method target
            val unwrapper = getUnwrapper(shadowMethod)
            val unwrappedParameterTypes =
                unwrapper.unwrapAll(shadowMethod.parameterTypes, shadowFactory)
            val unwrappedArguments =
                unwrapper.unwrapAll(args!!, unwrappedParameterTypes, shadowFactory)
            val unwrappedArgumentTypes = getArgumentTypes(unwrappedArguments, unwrappedParameterTypes)

            val targetMethod = shadow.findTargetMethod(shadowMethod, unwrappedArgumentTypes)
            returnValue = bindWithHandle(targetMethod.handle(), shadowMethod)
                .invokeWithArguments(*unwrappedArguments)
        }

        val wrapper: Wrapper = getWrapper(shadowMethod)
        return wrapper.wrap(returnValue, shadowMethod.returnType, shadowFactory)
    }

    private fun bindWithHandle(methodHandle: MethodHandle, annotatedElement: AnnotatedElement): MethodHandle {
        return if (annotatedElement.isAnnotationPresent(Static::class.java)) {
            methodHandle
        } else {
            if (handle == null) {
                throw IllegalStateException("Cannot call non-static method from a static shadow instance.")
            }
            methodHandle.bindTo(handle)
        }
    }

    companion object {
        private val GET_SHADOW_TARGET_METHOD: Method
        private val GET_SHADOW_CLASS_METHOD: Method
        private val OBJECT_TOSTRING_METHOD: Method
        private val OBJECT_EQUALS_METHOD: Method
        private val OBJECT_HASHCODE_METHOD: Method

        init {
            try {
                GET_SHADOW_TARGET_METHOD = Shadow::class.java.getMethod("getShadowTarget")
                GET_SHADOW_CLASS_METHOD = Shadow::class.java.getMethod("getShadowClass")
                OBJECT_TOSTRING_METHOD = Any::class.java.getMethod("toString")
                OBJECT_EQUALS_METHOD = Any::class.java.getMethod("equals", Any::class.java)
                OBJECT_HASHCODE_METHOD = Any::class.java.getMethod("hashCode")
            } catch (e: NoSuchMethodException) {
                throw ExceptionInInitializerError(e)
            }
        }

        fun getArgumentTypes(arguments: Array<Any?>, fallback: Array<Class<*>>?): Array<Class<*>> {
            val types = arrayOfNulls<Class<*>>(arguments.size)
            for (i in arguments.indices) {
                val arg = arguments[i]
                types[i] = if (arg == null) {
                    fallback?.get(i) ?: Any::class.java
                } else {
                    arg.javaClass
                }
            }
            @Suppress("UNCHECKED_CAST")
            return types as Array<Class<*>>
        }

        private fun getWrapper(shadowMethod: Method): Wrapper {
            val shadowingStrategy = shadowMethod.getAnnotation(ShadowingStrategy::class.java)
            val wrapper: Wrapper
            wrapper = if (shadowingStrategy == null || shadowingStrategy.wrapper.java == Wrapper::class.java) {
                ForShadows.INSTANCE
            } else {
                Reflection.getInstance(Wrapper::class.java, shadowingStrategy.wrapper.java)
            }
            return wrapper
        }

        private fun getUnwrapper(shadowMethod: Method): Unwrapper {
            val shadowingStrategy = shadowMethod.getAnnotation(ShadowingStrategy::class.java)
            val unwrapper: Unwrapper
            unwrapper = if (shadowingStrategy == null || shadowingStrategy.unwrapper.java == Unwrapper::class.java) {
                ForShadows.INSTANCE
            } else {
                Reflection.getInstance(Unwrapper::class.java, shadowingStrategy.unwrapper.java)
            }
            return unwrapper
        }
    }
}

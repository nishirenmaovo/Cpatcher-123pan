package io.github.cpatcher.arch

import java.lang.reflect.Field
import java.lang.reflect.Method

object ReflectUtil {
    fun getAllFields(clazz: Class<*>): List<Field> {
        val fields = mutableListOf<Field>()
        var c: Class<*>? = clazz
        while (c != null && c != Any::class.java) {
            fields.addAll(c.declaredFields)
            c = c.superclass
        }
        return fields
    }

    fun findField(clazz: Class<*>, fieldName: String): Field? {
        return getAllFields(clazz).firstOrNull { it.name == fieldName }
    }

    fun setFieldValue(target: Any, fieldName: String, value: Any?) {
        val field = findField(target.javaClass, fieldName)
        field?.isAccessible = true
        field?.set(target, value)
    }

    fun getFieldValue(target: Any, fieldName: String): Any? {
        val field = findField(target.javaClass, fieldName)
        field?.isAccessible = true
        return field?.get(target)
    }

    fun classExists(className: String, classLoader: ClassLoader? = null): Boolean {
        return try {
            if (classLoader != null) Class.forName(className, false, classLoader)
            else Class.forName(className)
            true
        } catch (_: ClassNotFoundException) { false }
    }
}

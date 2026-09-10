package io.github.cpatcher.arch

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

private const val TAG = "Cpatcher"

fun logI(msg: String) { Log.i(TAG, msg) }
fun logE(msg: String) { Log.e(TAG, msg) }
fun logE(msg: String, tr: Throwable?) { Log.e(TAG, msg, tr) }

fun findClass(className: String): Class<*> {
    return XposedHelpers.findClass(className, null)
}

fun findClass(className: String, classLoader: ClassLoader): Class<*> {
    return XposedHelpers.findClass(className, classLoader)
}

fun Class<*>.hookAfter(
    methodName: String,
    vararg parameterTypes: Any,
    callback: (XC_MethodHook.MethodHookParam) -> Unit
) {
    try {
        val args = mutableListOf<Any?>()
        args.addAll(parameterTypes)
        args.add(object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try { callback(param) } catch (_: Throwable) { }
            }
        })
        XposedHelpers.findAndHookMethod(this, methodName, *args.toTypedArray())
    } catch (t: Throwable) {
        Log.e("Cpatcher", "hookAfter failed: ${this.name}.$methodName - ${t.message}")
    }
}

fun Class<*>.hookBefore(
    methodName: String,
    vararg parameterTypes: Any,
    callback: (XC_MethodHook.MethodHookParam) -> Unit
) {
    try {
        val args = mutableListOf<Any?>()
        args.addAll(parameterTypes)
        args.add(object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                try { callback(param) } catch (_: Throwable) { }
            }
        })
        XposedHelpers.findAndHookMethod(this, methodName, *args.toTypedArray())
    } catch (t: Throwable) {
        Log.e("Cpatcher", "hookBefore failed: ${this.name}.$methodName - ${t.message}")
    }
}

fun hookAfter(
    className: String,
    methodName: String,
    vararg parameterTypes: Any,
    callback: (XC_MethodHook.MethodHookParam) -> Unit
) {
    findClass(className).hookAfter(methodName, *parameterTypes, callback = callback)
}

fun hookBefore(
    className: String,
    methodName: String,
    vararg parameterTypes: Any,
    callback: (XC_MethodHook.MethodHookParam) -> Unit
) {
    findClass(className).hookBefore(methodName, *parameterTypes, callback = callback)
}

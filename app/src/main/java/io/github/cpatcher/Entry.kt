package io.github.cpatcher

import android.app.Activity
import android.widget.Toast
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.logE
import io.github.cpatcher.arch.logI
import io.github.cpatcher.handlers.Cloud123Handler
import io.github.cpatcher.handlers.XingtuHandler

class Entry : IXposedHookLoadPackage {

    companion object {
        private val HANDLER_REGISTRY: Map<String, () -> IHook> = mapOf(
            "com.mfcloudcalculate.networkdisk" to { Cloud123Handler() },
            "com.xt.retouch" to { XingtuHandler() },
        )
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val handlerFactory = HANDLER_REGISTRY[lpparam.packageName] ?: return

        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Activity", lpparam.classLoader, "onCreate", "android.os.Bundle",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val activity = param.thisObject as Activity
                            Toast.makeText(activity, "Cpatcher 模块已加载: ${lpparam.packageName}", Toast.LENGTH_LONG).show()
                        } catch (_: Throwable) { }
                    }
                }
            )
            logI("Cpatcher: Debug Toast hook installed for ${lpparam.packageName}")
        } catch (e: Throwable) {
            logE("Cpatcher: Failed to install debug Toast - ${e.message}")
        }

        try {
            logI("Cpatcher: Loading handler for ${lpparam.packageName}")
            val handler = handlerFactory()
            handler.loadPackageParam = lpparam
            handler.onHook()
            logI("Cpatcher: Handler init success for ${lpparam.packageName}")
        } catch (e: Throwable) {
            logE("Cpatcher: Handler init failed for ${lpparam.packageName}", e)
        }
    }
}

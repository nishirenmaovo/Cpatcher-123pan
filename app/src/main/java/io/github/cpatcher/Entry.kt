package io.github.cpatcher

import de.robv.android.xposed.IXposedHookLoadPackage
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
            logI("Cpatcher: Loading handler for ${lpparam.packageName}")
            val handler = handlerFactory()
            handler.loadPackageParam = lpparam
            handler.onHook()
        } catch (e: Exception) {
            logE("Cpatcher: Handler init failed for ${lpparam.packageName}", e)
        }
    }
}

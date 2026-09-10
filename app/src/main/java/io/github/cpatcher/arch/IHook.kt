package io.github.cpatcher.arch

import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

abstract class IHook {

    lateinit var loadPackageParam: XC_LoadPackage.LoadPackageParam

    /**
     * 使用目标应用的 ClassLoader 查找类。
     * 成员方法覆盖顶层 findClass，确保加载目标应用的类。
     */
    fun findClass(className: String): Class<*> {
        return XposedHelpers.findClass(className, loadPackageParam.classLoader)
    }

    abstract fun onHook()
}

package io.github.cpatcher.arch

import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * 所有应用 Handler 的基类
 */
abstract class IHook {

    lateinit var loadPackageParam: XC_LoadPackage.LoadPackageParam

    abstract fun onHook()
}

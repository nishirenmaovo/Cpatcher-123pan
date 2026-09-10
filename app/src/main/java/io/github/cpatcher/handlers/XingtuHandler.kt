package io.github.cpatcher.handlers

import android.app.Activity
import android.widget.Toast
import io.github.cpatcher.arch.ClassScanner
import io.github.cpatcher.arch.DebugLog
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.hookAfter
import io.github.cpatcher.arch.hookBefore
import io.github.cpatcher.arch.findClass

class XingtuHandler : IHook() {

    companion object {
        private const val TAG = "Xingtu"
        private const val TARGET_PACKAGE = "com.xt.retouch"
    }

    private var hookSuccessCount = 0
    private var hookFailCount = 0

    override fun onHook() {
        if (loadPackageParam.packageName != TARGET_PACKAGE) return
        DebugLog.d(TAG, "===== 醒图模块启动 =====")
        toast("醒图: 模块启动")
        applyGenericHooks()
        scanAndHookVipMethods()
        scanAndHookLockMethods()
        scanAndHookWatermarkMethods()
        DebugLog.d(TAG, "===== 模块初始化完成: 成功$hookSuccessCount 失败$hookFailCount =====")
        toast("醒图: 完成 成功$hookSuccessCount 失败$hookFailCount")
    }

    private fun toast(msg: String) {
        try {
            val activityThreadClass = findClass("android.app.ActivityThread")
            val current = activityThreadClass.getMethod("currentActivityThread").invoke(null)
            val app = current.javaClass.getMethod("getApplication").invoke(current) as android.app.Application
            Toast.makeText(app, msg, Toast.LENGTH_LONG).show()
        } catch (_: Throwable) { }
    }

    private fun applyGenericHooks() {
        DebugLog.d(TAG, "--- 应用通用 Hook ---")
        try {
            findClass("android.app.Activity").hookAfter("onCreate", "android.os.Bundle") { param ->
                val activity = param.thisObject as Activity
                val cn = activity.javaClass.name.lowercase()
                if ((cn.contains("vip") || cn.contains("member") || cn.contains("pay") || cn.contains("recharge") || cn.contains("purchase")) && (cn.contains("activity") || cn.contains("dialog") || cn.contains("popup") || cn.contains("page"))) {
                    activity.finish()
                    DebugLog.d(TAG, "  VIP/支付Activity已finish: ${activity.javaClass.name}")
                }
            }
            hookSuccessCount++
            DebugLog.d(TAG, "  VIP/支付Activity拦截 Hook成功")
        } catch (e: Throwable) { hookFailCount++; DebugLog.e(TAG, "  VIP/支付Activity拦截失败: ${e.message}") }
    }

    private fun scanAndHookVipMethods() {
        DebugLog.d(TAG, "--- 扫描 VIP 方法 ---")
        try {
            val apkPath = loadPackageParam.appInfo.sourceDir
            val vipMethodNames = setOf("isVip", "isMember", "isSvip", "isPremium", "isPayUser", "hasVip", "isVipUser", "isVipMember", "isUserVip", "isUserMember", "isUserSvip", "isUserPremium")
            val booleanMethods = ClassScanner.findMethods(apkPath = apkPath, classLoader = loadPackageParam.classLoader, packagePrefix = "com.xt.retouch", methodNames = vipMethodNames, returnType = "boolean", paramCount = 0)
            var count = 0
            booleanMethods.forEach { info ->
                try {
                    findClass(info.className).hookAfter(info.methodName) { param -> param.result = true }
                    count++; hookSuccessCount++
                    DebugLog.d(TAG, "  VIP方法 Hook成功: ${info.className}.${info.methodName}() = true")
                } catch (e: Throwable) { hookFailCount++; DebugLog.e(TAG, "  VIP方法 Hook失败: ${info.className}.${info.methodName} - ${e.message}") }
            }
            val levelMethodNames = setOf("getVipLevel", "getMemberLevel", "getVipType", "getMemberType")
            val levelMethods = ClassScanner.findMethods(apkPath = apkPath, classLoader = loadPackageParam.classLoader, packagePrefix = "com.xt.retouch", methodNames = levelMethodNames, returnType = "int", paramCount = 0)
            levelMethods.forEach { info ->
                try {
                    findClass(info.className).hookAfter(info.methodName) { param -> param.result = 2 }
                    count++; hookSuccessCount++
                    DebugLog.d(TAG, "  VIP等级 Hook成功: ${info.className}.${info.methodName}() = 2")
                } catch (e: Throwable) { hookFailCount++; DebugLog.e(TAG, "  VIP等级 Hook失败: ${info.className}.${info.methodName} - ${e.message}") }
            }
            DebugLog.d(TAG, "  扫描VIP方法完成: 共 Hook $count 个")
            toast("醒图: 扫描到VIP方法 $count 个")
        } catch (e: Throwable) { DebugLog.e(TAG, "  扫描VIP方法异常: ${e.message}", e) }
    }

    private fun scanAndHookLockMethods() {
        DebugLog.d(TAG, "--- 扫描素材锁定方法 ---")
        try {
            val apkPath = loadPackageParam.appInfo.sourceDir
            val lockMethodNames = setOf("isLock", "isLocked", "isVipLock", "needVip", "isNeedVip", "isVipMaterial", "isVipFilter", "isLockMaterial", "isLockFilter", "isPay", "isNeedPay", "isVipOnly")
            val methods = ClassScanner.findMethods(apkPath = apkPath, classLoader = loadPackageParam.classLoader, packagePrefix = "com.xt.retouch", methodNames = lockMethodNames, returnType = "boolean", paramCount = 0)
            var count = 0
            methods.forEach { info ->
                try {
                    findClass(info.className).hookAfter(info.methodName) { param -> param.result = false }
                    count++; hookSuccessCount++
                    DebugLog.d(TAG, "  锁定方法 Hook成功: ${info.className}.${info.methodName}() = false")
                } catch (e: Throwable) { hookFailCount++; DebugLog.e(TAG, "  锁定方法 Hook失败: ${info.className}.${info.methodName} - ${e.message}") }
            }
            DebugLog.d(TAG, "  扫描锁定方法完成: 共 Hook $count 个")
        } catch (e: Throwable) { DebugLog.e(TAG, "  扫描锁定方法异常: ${e.message}", e) }
    }

    private fun scanAndHookWatermarkMethods() {
        DebugLog.d(TAG, "--- 扫描水印方法 ---")
        try {
            val apkPath = loadPackageParam.appInfo.sourceDir
            val watermarkMethodNames = setOf("addWatermark", "showWatermark", "hasWatermark", "isWatermark", "needWatermark", "getWatermark", "isAddWatermark", "isShowWatermark")
            val methods = ClassScanner.findMethods(apkPath = apkPath, classLoader = loadPackageParam.classLoader, packagePrefix = "com.xt.retouch", methodNames = watermarkMethodNames, returnType = "boolean", paramCount = 0)
            var count = 0
            methods.forEach { info ->
                try {
                    findClass(info.className).hookAfter(info.methodName) { param -> param.result = false }
                    count++; hookSuccessCount++
                    DebugLog.d(TAG, "  水印方法 Hook成功: ${info.className}.${info.methodName}() = false")
                } catch (e: Throwable) { hookFailCount++; DebugLog.e(TAG, "  水印方法 Hook失败: ${info.className}.${info.methodName} - ${e.message}") }
            }
            DebugLog.d(TAG, "  扫描水印方法完成: 共 Hook $count 个")
        } catch (e: Throwable) { DebugLog.e(TAG, "  扫描水印方法异常: ${e.message}", e) }
    }
}

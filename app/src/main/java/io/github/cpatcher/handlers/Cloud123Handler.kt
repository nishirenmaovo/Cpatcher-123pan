package io.github.cpatcher.handlers

import android.app.Activity
import android.view.View
import android.widget.Toast
import io.github.cpatcher.arch.ClassScanner
import io.github.cpatcher.arch.DebugLog
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.hookAfter
import io.github.cpatcher.arch.hookBefore
import io.github.cpatcher.arch.findClass

class Cloud123Handler : IHook() {

    companion object {
        private const val TAG = "Cloud123"
        private const val TARGET_PACKAGE = "com.mfcloudcalculate.networkdisk"
    }

    private var hookSuccessCount = 0
    private var hookFailCount = 0

    override fun onHook() {
        if (loadPackageParam.packageName != TARGET_PACKAGE) return
        DebugLog.clear()
        DebugLog.d(TAG, "===== 123云盘模块启动 =====")
        toast("123云盘: 模块启动")
        applyGenericHooks()
        scanAndHookVipMethods()
        DebugLog.d(TAG, "===== 模块初始化完成: 成功$hookSuccessCount 失败$hookFailCount =====")
        toast("123云盘: 完成 成功$hookSuccessCount 失败$hookFailCount")
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
                if ((cn.contains("splash") || cn.contains("welcome") || cn.contains("guide")) && (cn.contains("ad") || cn.contains("advert") || cn.contains("adver"))) {
                    activity.finish()
                    DebugLog.d(TAG, "  开屏广告Activity已finish: ${activity.javaClass.name}")
                }
            }
            hookSuccessCount++
            DebugLog.d(TAG, "  开屏广告拦截 Hook成功")
        } catch (e: Throwable) { hookFailCount++; DebugLog.e(TAG, "  开屏广告拦截失败: ${e.message}") }
        val adSdks = listOf("com.bytedance.sdk.openadsdk.TTAdNative", "com.bytedance.sdk.openadsdk.TTAdSdk", "com.qq.e.ads.nativ.NativeAD", "com.qq.e.ads.banner2.UnifiedBannerAD", "com.qq.e.ads.splash.SplashAD", "com.baidu.mobads.SplashAd", "com.baidu.mobads.BaiduNativeManager", "com.kwad.sdk.api.KsAdSDK", "com.kwad.sdk.api.KsSplashScreenAd")
        var adCount = 0
        adSdks.forEach { className ->
            try { findClass(className).hookBefore("loadAd") { param -> param.result = null }; adCount++ } catch (_: Throwable) { }
        }
        hookSuccessCount += adCount
        DebugLog.d(TAG, "  广告SDK拦截: $adCount 个")
        try {
            findClass("android.view.View").hookBefore("setVisibility", "int") { param ->
                val view = param.thisObject as View
                val cn = view.javaClass.name.lowercase()
                if (cn.contains("ad") || cn.contains("banner") || cn.contains("feed") || cn.contains("advert")) {
                    if ((param.args[0] as Int) != View.GONE) { param.args[0] = View.GONE; DebugLog.d(TAG, "  广告View已隐藏: ${view.javaClass.name}") }
                }
            }
            hookSuccessCount++
            DebugLog.d(TAG, "  广告View隐藏 Hook成功")
        } catch (e: Throwable) { hookFailCount++; DebugLog.e(TAG, "  广告View隐藏失败: ${e.message}") }
    }

    private fun scanAndHookVipMethods() {
        DebugLog.d(TAG, "--- 扫描 VIP 方法 ---")
        try {
            val apkPath = loadPackageParam.appInfo.sourceDir
            val vipMethodNames = setOf("isVip", "isMember", "isSvip", "isPremium", "isPayUser", "hasVip", "isVipUser", "isVipMember", "isUserVip", "isUserMember", "isUserSvip", "isUserPremium")
            val booleanMethods = ClassScanner.findMethods(apkPath = apkPath, classLoader = loadPackageParam.classLoader, packagePrefix = "com.mfcloudcalculate", methodNames = vipMethodNames, returnType = "boolean", paramCount = 0)
            var count = 0
            booleanMethods.forEach { info ->
                try {
                    findClass(info.className).hookAfter(info.methodName) { param -> param.result = true }
                    count++; hookSuccessCount++
                    DebugLog.d(TAG, "  VIP方法 Hook成功: ${info.className}.${info.methodName}() = true")
                } catch (e: Throwable) { hookFailCount++; DebugLog.e(TAG, "  VIP方法 Hook失败: ${info.className}.${info.methodName} - ${e.message}") }
            }
            val levelMethodNames = setOf("getVipLevel", "getMemberLevel", "getVipType", "getMemberType")
            val levelMethods = ClassScanner.findMethods(apkPath = apkPath, classLoader = loadPackageParam.classLoader, packagePrefix = "com.mfcloudcalculate", methodNames = levelMethodNames, returnType = "int", paramCount = 0)
            levelMethods.forEach { info ->
                try {
                    findClass(info.className).hookAfter(info.methodName) { param -> param.result = 2 }
                    count++; hookSuccessCount++
                    DebugLog.d(TAG, "  VIP等级 Hook成功: ${info.className}.${info.methodName}() = 2")
                } catch (e: Throwable) { hookFailCount++; DebugLog.e(TAG, "  VIP等级 Hook失败: ${info.className}.${info.methodName} - ${e.message}") }
            }
            DebugLog.d(TAG, "  扫描VIP方法完成: 共 Hook $count 个")
            toast("123云盘: 扫描到VIP方法 $count 个")
        } catch (e: Throwable) { DebugLog.e(TAG, "  扫描VIP方法异常: ${e.message}", e) }
    }
}

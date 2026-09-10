package io.github.cpatcher.handlers

import android.app.Activity
import android.view.View
import android.widget.Toast
import io.github.cpatcher.arch.DebugLog
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.ObfsMethodInfo
import io.github.cpatcher.arch.hookAfter
import io.github.cpatcher.arch.hookBefore
import io.github.cpatcher.arch.findClass
import io.github.cpatcher.arch.createObfsTable
import io.github.cpatcher.arch.toObfsInfo
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Modifier

class Cloud123Handler : IHook() {

    companion object {
        private const val TAG = "Cloud123"
        private const val TABLE_VERSION = 2
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

        try {
            DebugLog.d(TAG, "开始 DexKit 指纹扫描...")
            val obfsTable = createObfsTable("cloud123", TABLE_VERSION) { bridge -> buildObfsTable(bridge) }
            DebugLog.d(TAG, "指纹表构建成功，共 ${obfsTable.size} 条")
            toast("123云盘: 指纹匹配成功 ${obfsTable.size} 条")
            applyPrecisionHooks(obfsTable)
        } catch (e: Exception) {
            DebugLog.e(TAG, "DexKit 指纹失败: ${e.message}", e)
            toast("123云盘: 指纹失败，使用通用模式")
        }

        DebugLog.d(TAG, "===== 模块初始化完成: 成功$hookSuccessCount 失败$hookFailCount =====")
        toast("123云盘: 完成 成功$hookSuccessCount 失败$hookFailCount")
    }

    private fun toast(msg: String) {
        try {
            val activityThreadClass = findClass("android.app.ActivityThread")
            val current = activityThreadClass.getMethod("currentActivityThread").invoke(null)
            val app = current.javaClass.getMethod("getApplication").invoke(current) as android.app.Application
            Toast.makeText(app, msg, Toast.LENGTH_LONG).show()
        } catch (_: Exception) { }
    }

    private fun applyGenericHooks() {
        DebugLog.d(TAG, "--- 应用通用 Hook ---")

        val vipMethods = listOf("isVip", "isMember", "isSvip", "isPremium", "isPayUser", "hasVip", "getVip", "isVipUser")
        val packages = listOf("com.mfcloudcalculate.networkdisk", "com.mfcloudcalculate.networkdisk.user", "com.mfcloudcalculate.networkdisk.mine", "com.mfcloudcalculate.networkdisk.bean", "com.mfcloudcalculate.networkdisk.model", "com.mfcloudcalculate.networkdisk.vip", "com.mfcloudcalculate.networkdisk.member", "com.mfcloudcalculate.networkdisk.account", "com.mfcloudcalculate.networkdisk.data")

        var vipHookCount = 0
        packages.forEach { pkg ->
            vipMethods.forEach { methodName ->
                try {
                    val clazz = findClass("$pkg.UserInfo")
                    clazz.hookAfter(methodName) { param -> param.result = true }
                    vipHookCount++
                    DebugLog.d(TAG, "  VIP方法 Hook成功: $pkg.UserInfo.$methodName()")
                } catch (_: Exception) { }
            }
        }
        hookSuccessCount += vipHookCount
        DebugLog.d(TAG, "  通用VIP方法 Hook: $vipHookCount 个")

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
        } catch (e: Exception) {
            hookFailCount++
            DebugLog.e(TAG, "  开屏广告拦截失败: ${e.message}")
        }

        val adSdks = listOf("com.bytedance.sdk.openadsdk.TTAdNative", "com.bytedance.sdk.openadsdk.TTAdSdk", "com.qq.e.ads.nativ.NativeAD", "com.qq.e.ads.banner2.UnifiedBannerAD", "com.qq.e.ads.splash.SplashAD", "com.baidu.mobads.SplashAd", "com.baidu.mobads.BaiduNativeManager", "com.kwad.sdk.api.KsAdSDK", "com.kwad.sdk.api.KsSplashScreenAd")
        var adCount = 0
        adSdks.forEach { className ->
            try { findClass(className).hookBefore("loadAd") { param -> param.result = null }; adCount++ } catch (_: Exception) { }
        }
        hookSuccessCount += adCount
        DebugLog.d(TAG, "  广告SDK拦截: $adCount 个")

        try {
            findClass("android.view.View").hookBefore("setVisibility", "int") { param ->
                val view = param.thisObject as View
                val cn = view.javaClass.name.lowercase()
                if (cn.contains("ad") || cn.contains("banner") || cn.contains("feed") || cn.contains("advert")) {
                    if ((param.args[0] as Int) != View.GONE) {
                        param.args[0] = View.GONE
                        DebugLog.d(TAG, "  广告View已隐藏: ${view.javaClass.name}")
                    }
                }
            }
            hookSuccessCount++
            DebugLog.d(TAG, "  广告View隐藏 Hook成功")
        } catch (e: Exception) {
            hookFailCount++
            DebugLog.e(TAG, "  广告View隐藏失败: ${e.message}")
        }
    }

    private fun buildObfsTable(bridge: DexKitBridge): Map<String, Any> {
        val table = mutableMapOf<String, Any>()
        val isVipMethod = bridge.findMethod { matcher { usingStrings("isVip", "isMember", "isSvip", "vip"); returnType = "boolean"; modifiers = Modifier.PUBLIC } }.firstOrNull()
        if (isVipMethod != null) { table["is_vip"] = isVipMethod.toObfsInfo(); DebugLog.d(TAG, "  指纹命中 isVip: ${isVipMethod.className}.${isVipMethod.methodName}") }
        val vipDialogMethod = bridge.findMethod { matcher { usingStrings("vip_dialog", "member_pay", "open_vip", "upgrade", "vip_pay"); returnType = "void" } }.firstOrNull()
        if (vipDialogMethod != null) { table["vip_dialog"] = vipDialogMethod.toObfsInfo(); DebugLog.d(TAG, "  指纹命中 VIP弹窗: ${vipDialogMethod.className}.${vipDialogMethod.methodName}") }
        return table
    }

    private fun applyPrecisionHooks(obfsTable: Map<String, Any>) {
        obfsTable["is_vip"]?.let { info ->
            try {
                val mi = info as ObfsMethodInfo
                findClass(mi.className).hookAfter(mi.memberName) { param -> param.result = true }
                hookSuccessCount++
                DebugLog.d(TAG, "  精确Hook isVip成功: ${mi.className}.${mi.memberName}")
            } catch (e: Exception) { hookFailCount++; DebugLog.e(TAG, "  精确Hook isVip失败: ${e.message}") }
        }
        obfsTable["vip_dialog"]?.let { info ->
            try {
                val mi = info as ObfsMethodInfo
                findClass(mi.className).hookBefore(mi.memberName) { param -> param.result = null }
                hookSuccessCount++
                DebugLog.d(TAG, "  精确Hook VIP弹窗成功: ${mi.className}.${mi.memberName}")
            } catch (e: Exception) { hookFailCount++; DebugLog.e(TAG, "  精确Hook VIP弹窗失败: ${e.message}") }
        }
    }
}

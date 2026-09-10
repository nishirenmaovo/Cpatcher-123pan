package io.github.cpatcher.handlers

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.ObfsMethodInfo
import io.github.cpatcher.arch.hookAfter
import io.github.cpatcher.arch.hookBefore
import io.github.cpatcher.arch.logI
import io.github.cpatcher.arch.logE
import io.github.cpatcher.arch.findClass
import io.github.cpatcher.arch.createObfsTable
import io.github.cpatcher.arch.toObfsInfo
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Modifier

class Cloud123Handler : IHook() {

    companion object {
        private const val TABLE_VERSION = 1
        private const val TARGET_PACKAGE = "com.mfcloudcalculate.networkdisk"
        private const val KEY_SPLASH_AD_ACTIVITY = "splash_ad_activity"
        private const val KEY_SPLASH_AD_SHOW = "splash_ad_show_method"
        private const val KEY_AD_VIEW_CONTAINER = "ad_view_container_class"
        private const val KEY_AD_LOAD_METHOD = "ad_load_method"
        private const val KEY_USER_INFO_MODEL = "user_info_model_class"
        private const val KEY_IS_VIP_METHOD = "is_vip_method"
        private const val KEY_VIP_DIALOG_SHOW = "vip_dialog_show_method"
        private const val KEY_PAY_LAUNCH_METHOD = "pay_launch_method"
    }

    override fun onHook() {
        if (loadPackageParam.packageName != TARGET_PACKAGE) return
        logI("${this::class.simpleName}: Target package matched, starting initialization...")
        val obfsTable = try {
            createObfsTable("cloud123", TABLE_VERSION) { bridge -> buildObfsTable(bridge) }
        } catch (e: Exception) {
            logE("${this::class.simpleName}: Fingerprint resolution failed - ${e.message}")
            applyFallbackHooks()
            return
        }
        try {
            applyRemoveAdsHooks(obfsTable)
            applyVipEnhanceHooks(obfsTable)
            logI("${this::class.simpleName}: All hooks applied successfully")
        } catch (e: Exception) {
            logE("${this::class.simpleName}: Hook application failed - ${e.message}")
            applyFallbackHooks()
        }
    }

    private fun buildObfsTable(bridge: DexKitBridge): Map<String, Any> {
        val table = mutableMapOf<String, Any>()
        val splashActivity = bridge.findClass {
            matcher { usingStrings("splash", "ad", "skip") }
        }.firstOrNull()
        if (splashActivity != null) table[KEY_SPLASH_AD_ACTIVITY] = splashActivity.name
        val splashShowMethod = bridge.findMethod {
            matcher {
                usingStrings("ad_show", "splash_ad", "count_down")
                returnType = "void"
                modifiers = Modifier.PUBLIC
            }
        }.firstOrNull()
        if (splashShowMethod != null) table[KEY_SPLASH_AD_SHOW] = splashShowMethod.toObfsInfo()
        val adContainerClass = bridge.findClass {
            matcher { usingStrings("banner_ad", "feed_ad", "ad_position") }
        }.firstOrNull()
        if (adContainerClass != null) table[KEY_AD_VIEW_CONTAINER] = adContainerClass.name
        val adLoadMethod = bridge.findMethod {
            matcher {
                usingStrings("loadAd", "ad_load", "request_ad")
                returnType = "void"
                paramTypes("android.content.Context")
            }
        }.firstOrNull()
        if (adLoadMethod != null) table[KEY_AD_LOAD_METHOD] = adLoadMethod.toObfsInfo()
        val userInfoClass = bridge.findClass {
            matcher {
                usingStrings("vipLevel", "isVip", "vipExpire", "memberLevel")
                modifiers = Modifier.PUBLIC
            }
        }.firstOrNull()
        if (userInfoClass != null) table[KEY_USER_INFO_MODEL] = userInfoClass.name
        val isVipMethod = bridge.findMethod {
            matcher {
                usingStrings("isVip", "isMember", "isSvip", "vip")
                returnType = "boolean"
                modifiers = Modifier.PUBLIC
            }
        }.firstOrNull()
        if (isVipMethod != null) table[KEY_IS_VIP_METHOD] = isVipMethod.toObfsInfo()
        val vipDialogMethod = bridge.findMethod {
            matcher {
                usingStrings("vip_dialog", "member_pay", "open_vip", "upgrade")
                returnType = "void"
            }
        }.firstOrNull()
        if (vipDialogMethod != null) table[KEY_VIP_DIALOG_SHOW] = vipDialogMethod.toObfsInfo()
        val payMethod = bridge.findMethod {
            matcher {
                usingStrings("pay", "purchase", "recharge", "alipay", "wxpay")
                returnType = "void"
                modifiers = Modifier.PUBLIC or Modifier.STATIC
            }
        }.firstOrNull()
        if (payMethod != null) table[KEY_PAY_LAUNCH_METHOD] = payMethod.toObfsInfo()
        logI("${this::class.simpleName}: Fingerprint table built with ${table.size} entries")
        return table
    }

    private fun applyRemoveAdsHooks(obfsTable: Map<String, Any>) {
        var hookCount = 0
        obfsTable[KEY_SPLASH_AD_ACTIVITY]?.let { className ->
            findClass(className as String).hookAfter("onCreate") { param ->
                (param.thisObject as Activity).finish()
                logI("${this::class.simpleName}: Splash ad activity finished")
            }
            hookCount++
        }
        obfsTable[KEY_SPLASH_AD_SHOW]?.let { info ->
            val mi = info as ObfsMethodInfo
            findClass(mi.className).hookBefore(mi.memberName) { param ->
                param.result = null
                logI("${this::class.simpleName}: Splash ad show blocked")
            }
            hookCount++
        }
        obfsTable[KEY_AD_VIEW_CONTAINER]?.let { className ->
            findClass(className as String).hookAfter("onAttachedToWindow") { param ->
                val view = param.thisObject as View
                view.visibility = View.GONE
                (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                    it.width = 0; it.height = 0; view.layoutParams = it
                }
                logI("${this::class.simpleName}: Ad container hidden")
            }
            hookCount++
        }
        obfsTable[KEY_AD_LOAD_METHOD]?.let { info ->
            val mi = info as ObfsMethodInfo
            findClass(mi.className).hookBefore(mi.memberName) { param ->
                param.result = null
                logI("${this::class.simpleName}: Ad load blocked")
            }
            hookCount++
        }
        blockCommonAdSdks()
        logI("${this::class.simpleName}: Remove-ads hooks applied ($hookCount targeted + fallback)")
    }

    private fun blockCommonAdSdks() {
        val adSdks = listOf(
            "com.bytedance.sdk.openadsdk.TTAdNative",
            "com.bytedance.sdk.openadsdk.TTAdSdk",
            "com.qq.e.ads.nativ.NativeAD",
            "com.qq.e.ads.banner2.UnifiedBannerAD",
            "com.qq.e.ads.splash.SplashAD",
            "com.baidu.mobads.SplashAd",
            "com.baidu.mobads.BaiduNativeManager",
            "com.kwad.sdk.api.KsAdSDK",
            "com.kwad.sdk.api.KsSplashScreenAd",
        )
        adSdks.forEach { className ->
            try { findClass(className).hookBefore("loadAd") { param -> param.result = null } }
            catch (_: Exception) { }
        }
    }

    private fun applyVipEnhanceHooks(obfsTable: Map<String, Any>) {
        var hookCount = 0
        obfsTable[KEY_IS_VIP_METHOD]?.let { info ->
            val mi = info as ObfsMethodInfo
            findClass(mi.className).hookAfter(mi.memberName) { param ->
                param.result = true
                logI("${this::class.simpleName}: isVip() forced to true")
            }
            hookCount++
        }
        obfsTable[KEY_USER_INFO_MODEL]?.let { className ->
            val clazz = findClass(className as String)
            clazz.methods.forEach { method ->
                if (method.returnType.name == className && method.parameterTypes.isEmpty() && Modifier.isPublic(method.modifiers)) {
                    method.isAccessible = true
                    hookAfter(clazz.name, method.name) { param -> param.result?.let { injectVipFields(it) } }
                }
            }
            hookCount++
        }
        obfsTable[KEY_VIP_DIALOG_SHOW]?.let { info ->
            val mi = info as ObfsMethodInfo
            findClass(mi.className).hookBefore(mi.memberName) { param ->
                param.result = null
                logI("${this::class.simpleName}: VIP payment dialog blocked")
            }
            hookCount++
        }
        obfsTable[KEY_PAY_LAUNCH_METHOD]?.let { info ->
            val mi = info as ObfsMethodInfo
            findClass(mi.className).hookBefore(mi.memberName) { param ->
                param.result = null
                logI("${this::class.simpleName}: Payment launch blocked")
            }
            hookCount++
        }
        applyGenericVipHooks()
        logI("${this::class.simpleName}: VIP enhance hooks applied ($hookCount targeted + fallback)")
    }

    private fun injectVipFields(userInfo: Any) {
        val clazz = userInfo.javaClass
        val vipKeywords = listOf("vip", "member", "svip", "premium", "level")
        clazz.declaredFields.forEach { field ->
            val fieldName = field.name.lowercase()
            if (vipKeywords.any { fieldName.contains(it) }) {
                field.isAccessible = true
                try {
                    when (field.type) {
                        Boolean::class.java -> field.setBoolean(userInfo, true)
                        Int::class.java -> field.setInt(userInfo, if (fieldName.contains("level")) 2 else 1)
                        Long::class.java -> {
                            if (fieldName.contains("expire") || fieldName.contains("time"))
                                field.setLong(userInfo, System.currentTimeMillis() + 315360000000L)
                        }
                        String::class.java -> {
                            if (fieldName.contains("level")) field.set(userInfo, "svip")
                            else if (fieldName.contains("expire")) field.set(userInfo, "2099-12-31")
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }

    private fun applyGenericVipHooks() {
        val methods = listOf("isVip", "isMember", "isSvip", "isPremium", "isPayUser", "hasVip")
        val packages = listOf(
            "com.mfcloudcalculate.networkdisk.user",
            "com.mfcloudcalculate.networkdisk.mine",
            "com.mfcloudcalculate.networkdisk.bean",
            "com.mfcloudcalculate.networkdisk.model",
        )
        methods.forEach { methodName ->
            packages.forEach { pkg ->
                try { findClass("$pkg.UserInfo").hookAfter(methodName) { param -> param.result = true } }
                catch (_: Exception) { }
            }
        }
    }

    private fun applyFallbackHooks() {
        logI("${this::class.simpleName}: Using fallback hook strategy")
        findClass("android.app.Activity").hookAfter("onCreate") { param ->
            val activity = param.thisObject as Activity
            val cn = activity.javaClass.name.lowercase()
            if (cn.contains("splash") && cn.contains("ad")) {
                activity.finish()
                logI("${this::class.simpleName}: Fallback - splash ad activity finished")
            }
        }
        blockCommonAdSdks()
        applyGenericVipHooks()
        logI("${this::class.simpleName}: Fallback hooks applied")
    }
}

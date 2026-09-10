package io.github.cpatcher.handlers

import android.app.Activity
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

class XingtuHandler : IHook() {

    companion object {
        private const val TABLE_VERSION = 1
        private const val TARGET_PACKAGE = "com.xt.retouch"
        private const val KEY_USER_INFO_MODEL = "user_info_model_class"
        private const val KEY_IS_VIP_METHOD = "is_vip_method"
        private const val KEY_VIP_LEVEL_METHOD = "vip_level_method"
        private const val KEY_VIP_DIALOG_SHOW = "vip_dialog_show_method"
        private const val KEY_PAY_LAUNCH_METHOD = "pay_launch_method"
        private const val KEY_MATERIAL_VIP_CHECK = "material_vip_check_method"
        private const val KEY_FILTER_VIP_CHECK = "filter_vip_check_method"
        private const val KEY_EXPORT_WATERMARK_CHECK = "export_watermark_check_method"
        private const val KEY_EXPORT_VIP_CHECK = "export_vip_check_method"
    }

    override fun onHook() {
        if (loadPackageParam.packageName != TARGET_PACKAGE) return
        logI("${this::class.simpleName}: Target package matched, starting initialization...")
        val obfsTable = try {
            createObfsTable("xingtu", TABLE_VERSION) { bridge -> buildObfsTable(bridge) }
        } catch (e: Exception) {
            logE("${this::class.simpleName}: Fingerprint resolution failed - ${e.message}")
            applyFallbackHooks()
            return
        }
        try {
            applyVipStatusHooks(obfsTable)
            applyPaymentBlockHooks(obfsTable)
            applyMaterialUnlockHooks(obfsTable)
            applyExportHooks(obfsTable)
            logI("${this::class.simpleName}: All hooks applied successfully")
        } catch (e: Exception) {
            logE("${this::class.simpleName}: Hook application failed - ${e.message}")
            applyFallbackHooks()
        }
    }

    private fun buildObfsTable(bridge: DexKitBridge): Map<String, Any> {
        val table = mutableMapOf<String, Any>()
        val userInfoClass = bridge.findClass { matcher { usingStrings("vipLevel", "isVip", "vipExpire", "memberLevel", "isSvip"); modifiers = Modifier.PUBLIC } }.firstOrNull()
        if (userInfoClass != null) table[KEY_USER_INFO_MODEL] = userInfoClass.name
        val isVipMethod = bridge.findMethod { matcher { usingStrings("isVip", "isMember", "isSvip", "vip"); returnType = "boolean"; modifiers = Modifier.PUBLIC } }.firstOrNull()
        if (isVipMethod != null) table[KEY_IS_VIP_METHOD] = isVipMethod.toObfsInfo()
        val vipLevelMethod = bridge.findMethod { matcher { usingStrings("vipLevel", "getVipLevel", "memberLevel", "getMemberLevel"); returnType = "int"; modifiers = Modifier.PUBLIC } }.firstOrNull()
        if (vipLevelMethod != null) table[KEY_VIP_LEVEL_METHOD] = vipLevelMethod.toObfsInfo()
        val vipDialogMethod = bridge.findMethod { matcher { usingStrings("vip_dialog", "member_pay", "open_vip", "upgrade_vip", "vip_pay"); returnType = "void" } }.firstOrNull()
        if (vipDialogMethod != null) table[KEY_VIP_DIALOG_SHOW] = vipDialogMethod.toObfsInfo()
        val payMethod = bridge.findMethod { matcher { usingStrings("pay", "purchase", "recharge", "alipay", "wxpay", "iap"); returnType = "void"; modifiers = Modifier.PUBLIC or Modifier.STATIC } }.firstOrNull()
        if (payMethod != null) table[KEY_PAY_LAUNCH_METHOD] = payMethod.toObfsInfo()
        val materialCheckMethod = bridge.findMethod { matcher { usingStrings("isVipMaterial", "materialVip", "isLock", "materialLock", "needVip"); returnType = "boolean" } }.firstOrNull()
        if (materialCheckMethod != null) table[KEY_MATERIAL_VIP_CHECK] = materialCheckMethod.toObfsInfo()
        val filterCheckMethod = bridge.findMethod { matcher { usingStrings("isVipFilter", "filterVip", "filterLock", "isFilterVip"); returnType = "boolean" } }.firstOrNull()
        if (filterCheckMethod != null) table[KEY_FILTER_VIP_CHECK] = filterCheckMethod.toObfsInfo()
        val watermarkMethod = bridge.findMethod { matcher { usingStrings("watermark", "addWatermark", "showWatermark", "hasWatermark"); returnType = "boolean" } }.firstOrNull()
        if (watermarkMethod != null) table[KEY_EXPORT_WATERMARK_CHECK] = watermarkMethod.toObfsInfo()
        val exportVipMethod = bridge.findMethod { matcher { usingStrings("exportVip", "saveVip", "hdExport", "highQualityExport"); returnType = "boolean" } }.firstOrNull()
        if (exportVipMethod != null) table[KEY_EXPORT_VIP_CHECK] = exportVipMethod.toObfsInfo()
        logI("${this::class.simpleName}: Fingerprint table built with ${table.size} entries")
        return table
    }

    private fun applyVipStatusHooks(obfsTable: Map<String, Any>) {
        var count = 0
        obfsTable[KEY_IS_VIP_METHOD]?.let { info ->
            val mi = info as ObfsMethodInfo
            findClass(mi.className).hookAfter(mi.memberName) { param -> param.result = true; logI("${this::class.simpleName}: isVip() forced to true") }
            count++
        }
        obfsTable[KEY_VIP_LEVEL_METHOD]?.let { info ->
            val mi = info as ObfsMethodInfo
            findClass(mi.className).hookAfter(mi.memberName) { param -> param.result = 2; logI("${this::class.simpleName}: vipLevel forced to 2 (SVIP)") }
            count++
        }
        obfsTable[KEY_USER_INFO_MODEL]?.let { className ->
            val clazz = findClass(className as String)
            clazz.methods.forEach { method ->
                if (method.returnType.name == className && method.parameterTypes.isEmpty() && Modifier.isPublic(method.modifiers)) {
                    method.isAccessible = true
                    hookAfter(clazz.name, method.name) { param -> param.result?.let { injectVipFields(it) } }
                }
            }
            count++
        }
        logI("${this::class.simpleName}: VIP status hooks applied ($count)")
    }

    private fun applyPaymentBlockHooks(obfsTable: Map<String, Any>) {
        var count = 0
        obfsTable[KEY_VIP_DIALOG_SHOW]?.let { info ->
            val mi = info as ObfsMethodInfo
            findClass(mi.className).hookBefore(mi.memberName) { param -> param.result = null; logI("${this::class.simpleName}: VIP dialog blocked") }
            count++
        }
        obfsTable[KEY_PAY_LAUNCH_METHOD]?.let { info ->
            val mi = info as ObfsMethodInfo
            findClass(mi.className).hookBefore(mi.memberName) { param -> param.result = null; logI("${this::class.simpleName}: Payment launch blocked") }
            count++
        }
        logI("${this::class.simpleName}: Payment block hooks applied ($count)")
    }

    private fun applyMaterialUnlockHooks(obfsTable: Map<String, Any>) {
        var count = 0
        obfsTable[KEY_MATERIAL_VIP_CHECK]?.let { info ->
            val mi = info as ObfsMethodInfo
            findClass(mi.className).hookAfter(mi.memberName) { param -> param.result = false; logI("${this::class.simpleName}: Material VIP check bypassed") }
            count++
        }
        obfsTable[KEY_FILTER_VIP_CHECK]?.let { info ->
            val mi = info as ObfsMethodInfo
            findClass(mi.className).hookAfter(mi.memberName) { param -> param.result = false; logI("${this::class.simpleName}: Filter VIP check bypassed") }
            count++
        }
        logI("${this::class.simpleName}: Material unlock hooks applied ($count)")
    }

    private fun applyExportHooks(obfsTable: Map<String, Any>) {
        var count = 0
        obfsTable[KEY_EXPORT_WATERMARK_CHECK]?.let { info ->
            val mi = info as ObfsMethodInfo
            findClass(mi.className).hookAfter(mi.memberName) { param -> param.result = false; logI("${this::class.simpleName}: Watermark disabled") }
            count++
        }
        obfsTable[KEY_EXPORT_VIP_CHECK]?.let { info ->
            val mi = info as ObfsMethodInfo
            findClass(mi.className).hookAfter(mi.memberName) { param -> param.result = true; logI("${this::class.simpleName}: Export VIP check bypassed") }
            count++
        }
        logI("${this::class.simpleName}: Export hooks applied ($count)")
    }

    private fun injectVipFields(userInfo: Any) {
        val clazz = userInfo.javaClass
        val vipKeywords = listOf("vip", "member", "svip", "premium", "level", "expire")
        clazz.declaredFields.forEach { field ->
            val fieldName = field.name.lowercase()
            if (vipKeywords.any { fieldName.contains(it) }) {
                field.isAccessible = true
                try {
                    when (field.type) {
                        Boolean::class.java -> field.setBoolean(userInfo, true)
                        Int::class.java -> field.setInt(userInfo, if (fieldName.contains("level")) 2 else 1)
                        Long::class.java -> { if (fieldName.contains("expire") || fieldName.contains("time")) field.setLong(userInfo, System.currentTimeMillis() + 315360000000L) }
                        String::class.java -> { if (fieldName.contains("level")) field.set(userInfo, "svip") else if (fieldName.contains("expire")) field.set(userInfo, "2099-12-31") }
                    }
                } catch (_: Exception) { }
            }
        }
    }

    private fun applyFallbackHooks() {
        logI("${this::class.simpleName}: Using fallback hook strategy")
        val vipMethods = listOf("isVip", "isMember", "isSvip", "isPremium", "isPayUser", "hasVip")
        val packages = listOf("com.xt.retouch.user", "com.xt.retouch.mine", "com.xt.retouch.bean", "com.xt.retouch.model", "com.xt.retouch.vip", "com.xt.retouch.member")
        vipMethods.forEach { methodName ->
            packages.forEach { pkg ->
                try { findClass("$pkg.UserInfo").hookAfter(methodName) { param -> param.result = true } }
                catch (_: Exception) { }
            }
        }
        findClass("android.app.Activity").hookAfter("onCreate", "android.os.Bundle") { param ->
            val activity = param.thisObject as Activity
            val cn = activity.javaClass.name.lowercase()
            if ((cn.contains("vip") || cn.contains("member") || cn.contains("pay")) && (cn.contains("dialog") || cn.contains("activity") || cn.contains("popup"))) {
                activity.finish()
                logI("${this::class.simpleName}: Fallback - VIP/pay activity finished")
            }
        }
        logI("${this::class.simpleName}: Fallback hooks applied")
    }
}

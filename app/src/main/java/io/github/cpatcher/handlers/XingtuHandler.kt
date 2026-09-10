package io.github.cpatcher.handlers

import android.app.Activity
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

class XingtuHandler : IHook() {

    companion object {
        private const val TAG = "Xingtu"
        private const val TABLE_VERSION = 2
        private const val TARGET_PACKAGE = "com.xt.retouch"
    }

    private var hookSuccessCount = 0
    private var hookFailCount = 0

    override fun onHook() {
        if (loadPackageParam.packageName != TARGET_PACKAGE) return
        DebugLog.d(TAG, "===== 醒图模块启动 =====")
        toast("醒图: 模块启动")
        applyGenericHooks()
        try {
            DebugLog.d(TAG, "开始 DexKit 指纹扫描...")
            val obfsTable = createObfsTable("xingtu", TABLE_VERSION) { bridge -> buildObfsTable(bridge) }
            DebugLog.d(TAG, "指纹表构建成功，共 ${obfsTable.size} 条")
            toast("醒图: 指纹匹配成功 ${obfsTable.size} 条")
            applyPrecisionHooks(obfsTable)
        } catch (e: Throwable) {
            DebugLog.e(TAG, "DexKit 指纹失败: ${e.message}", e)
            toast("醒图: 指纹失败，使用通用模式")
        }
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
        val vipMethods = listOf("isVip", "isMember", "isSvip", "isPremium", "isPayUser", "hasVip", "getVip", "isVipUser", "isVipMember", "getMemberLevel", "getVipLevel", "isUserVip", "isUserMember", "isUserSvip", "isUserPremium")
        val packages = listOf("com.xt.retouch", "com.xt.retouch.user", "com.xt.retouch.mine", "com.xt.retouch.bean", "com.xt.retouch.model", "com.xt.retouch.vip", "com.xt.retouch.member", "com.xt.retouch.account", "com.xt.retouch.data", "com.xt.retouch.entity", "com.xt.retouch.info")
        var vipHookCount = 0
        packages.forEach { pkg ->
            vipMethods.forEach { methodName ->
                val classNames = listOf("$pkg.UserInfo", "$pkg.User", "$pkg.MemberInfo", "$pkg.AccountInfo", "$pkg.VipInfo", "$pkg.UserBean", "$pkg.UserModel", "$pkg.Profile")
                classNames.forEach { className ->
                    try {
                        val clazz = findClass(className)
                        if (methodName == "getVipLevel" || methodName == "getMemberLevel") {
                            clazz.hookAfter(methodName) { param -> param.result = 2 }
                        } else {
                            clazz.hookAfter(methodName) { param -> param.result = true }
                        }
                        vipHookCount++
                        DebugLog.d(TAG, "  VIP方法 Hook成功: $className.$methodName()")
                    } catch (_: Throwable) { }
                }
            }
        }
        hookSuccessCount += vipHookCount
        DebugLog.d(TAG, "  通用VIP方法 Hook: $vipHookCount 个")
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
        val lockMethods = listOf("isLock", "isLocked", "isVipLock", "needVip", "isNeedVip", "isVipMaterial", "isVipFilter", "isLockMaterial", "isLockFilter")
        val materialPackages = listOf("com.xt.retouch", "com.xt.retouch.material", "com.xt.retouch.filter", "com.xt.retouch.bean", "com.xt.retouch.model", "com.xt.retouch.entity", "com.xt.retouch.data", "com.xt.retouch.item")
        var lockCount = 0
        materialPackages.forEach { pkg ->
            lockMethods.forEach { methodName ->
                val classNames = listOf("$pkg.Material", "$pkg.Filter", "$pkg.MaterialBean", "$pkg.FilterBean", "$pkg.MaterialInfo", "$pkg.FilterInfo", "$pkg.Item", "$pkg.Resource")
                classNames.forEach { className ->
                    try { findClass(className).hookAfter(methodName) { param -> param.result = false }; lockCount++; DebugLog.d(TAG, "  素材锁定 Hook成功: $className.$methodName()") } catch (_: Throwable) { }
                }
            }
        }
        hookSuccessCount += lockCount
        DebugLog.d(TAG, "  素材/滤镜锁定 Hook: $lockCount 个")
        val watermarkMethods = listOf("addWatermark", "showWatermark", "hasWatermark", "isWatermark", "needWatermark", "getWatermark", "watermark")
        val exportPackages = listOf("com.xt.retouch", "com.xt.retouch.export", "com.xt.retouch.save", "com.xt.retouch.editor", "com.xt.retouch.util", "com.xt.retouch.utils")
        var wmCount = 0
        exportPackages.forEach { pkg ->
            watermarkMethods.forEach { methodName ->
                val classNames = listOf("$pkg.ExportHelper", "$pkg.SaveHelper", "$pkg.ImageUtil", "$pkg.ImageUtils", "$pkg.ExportUtil", "$pkg.SaveUtil", "$pkg.EditHelper", "$pkg.PhotoHelper")
                classNames.forEach { className ->
                    try { findClass(className).hookAfter(methodName) { param -> param.result = false }; wmCount++; DebugLog.d(TAG, "  水印 Hook成功: $className.$methodName()") } catch (_: Throwable) { }
                }
            }
        }
        hookSuccessCount += wmCount
        DebugLog.d(TAG, "  导出水印 Hook: $wmCount 个")
    }

    private fun buildObfsTable(bridge: DexKitBridge): Map<String, Any> {
        val table = mutableMapOf<String, Any>()
        val isVipMethod = bridge.findMethod { matcher { usingStrings("isVip", "isMember", "isSvip", "vip"); returnType = "boolean"; modifiers = Modifier.PUBLIC } }.firstOrNull()
        if (isVipMethod != null) { table["is_vip"] = isVipMethod.toObfsInfo(); DebugLog.d(TAG, "  指纹命中 isVip: ${isVipMethod.className}.${isVipMethod.methodName}") }
        val vipDialogMethod = bridge.findMethod { matcher { usingStrings("vip_dialog", "member_pay", "open_vip", "upgrade_vip", "vip_pay"); returnType = "void" } }.firstOrNull()
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
            } catch (e: Throwable) { hookFailCount++; DebugLog.e(TAG, "  精确Hook isVip失败: ${e.message}") }
        }
        obfsTable["vip_dialog"]?.let { info ->
            try {
                val mi = info as ObfsMethodInfo
                findClass(mi.className).hookBefore(mi.memberName) { param -> param.result = null }
                hookSuccessCount++
                DebugLog.d(TAG, "  精确Hook VIP弹窗成功: ${mi.className}.${mi.memberName}")
            } catch (e: Throwable) { hookFailCount++; DebugLog.e(TAG, "  精确Hook VIP弹窗失败: ${e.message}") }
        }
    }
}

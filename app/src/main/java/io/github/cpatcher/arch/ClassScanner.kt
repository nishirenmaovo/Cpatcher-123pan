package io.github.cpatcher.arch

import dalvik.system.DexFile

object ClassScanner {

    data class MethodInfo(
        val className: String,
        val methodName: String,
        val returnType: String,
        val paramCount: Int
    )

    fun findMethods(
        apkPath: String,
        classLoader: ClassLoader,
        packagePrefix: String,
        methodNames: Set<String>,
        returnType: String? = null,
        paramCount: Int? = null
    ): List<MethodInfo> {
        val results = mutableListOf<MethodInfo>()
        var dexFile: DexFile? = null
        try {
            dexFile = DexFile(apkPath)
            val entries = dexFile.entries()
            var scanned = 0
            while (entries.hasMoreElements()) {
                val className = entries.nextElement()
                if (!className.startsWith(packagePrefix)) continue
                scanned++
                try {
                    val clazz = Class.forName(className, false, classLoader)
                    clazz.declaredMethods.forEach { method ->
                        if (method.name in methodNames) {
                            if (returnType != null && method.returnType.name != returnType) return@forEach
                            if (paramCount != null && method.parameterTypes.size != paramCount) return@forEach
                            results.add(MethodInfo(className, method.name, method.returnType.name, method.parameterTypes.size))
                        }
                    }
                } catch (_: Throwable) { }
            }
            DebugLog.d("Scanner", "扫描了 $scanned 个类（前缀=$packagePrefix），找到 ${results.size} 个匹配方法")
            results.forEach { info ->
                DebugLog.d("Scanner", "  匹配: ${info.className}.${info.methodName}() -> ${info.returnType}")
            }
        } catch (e: Throwable) {
            DebugLog.e("Scanner", "DexFile扫描失败: ${e.message}")
        } finally {
            try { dexFile?.close() } catch (_: Throwable) { }
        }
        return results
    }
}

package io.github.cpatcher.arch

import de.robv.android.xposed.XposedHelpers
import org.json.JSONObject
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.MethodData
import java.io.File

data class ObfsMethodInfo(
    val className: String,
    val memberName: String,
    val paramTypes: List<String> = emptyList()
) {
    fun toMap(): Map<String, String> = mapOf("className" to className, "memberName" to memberName, "paramTypes" to paramTypes.joinToString(","))
    companion object {
        fun fromMap(map: Map<String, String>): ObfsMethodInfo = ObfsMethodInfo(
            className = map["className"] ?: "",
            memberName = map["memberName"] ?: "",
            paramTypes = map["paramTypes"]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        )
    }
}

fun MethodData.toObfsInfo(): ObfsMethodInfo = ObfsMethodInfo(
    className = this.className,
    memberName = this.methodName,
    paramTypes = this.paramTypeNames ?: emptyList()
)

fun createObfsTable(
    tableName: String,
    version: Int,
    apkPath: String,
    block: (DexKitBridge) -> Map<String, Any>
): Map<String, Any> {
    val cacheFile = getCacheFile(tableName, version)
    val cached = loadFromCache(cacheFile)
    if (cached != null) {
        logI("ObfsUtils: Loaded fingerprint table '$tableName' v$version from cache")
        return cached
    }
    logI("ObfsUtils: Building fingerprint table '$tableName' v$version ...")
    val table = DexKitBridge.create(apkPath).use { bridge -> block(bridge) }
    saveToCache(cacheFile, table)
    logI("ObfsUtils: Fingerprint table '$tableName' v$version built with ${table.size} entries")
    return table
}

private fun getCacheFile(tableName: String, version: Int): File {
    return File("/sdcard/cpatcher_cache_${tableName}_$version.json")
}

private fun loadFromCache(file: File): Map<String, Any>? {
    return try {
        if (!file.exists()) return null
        val json = JSONObject(file.readText())
        val result = mutableMapOf<String, Any>()
        json.keys().forEach { key ->
            val value = json.get(key)
            if (value is JSONObject) {
                val map = mutableMapOf<String, String>()
                value.keys().forEach { k -> map[k] = value.getString(k) }
                result[key] = ObfsMethodInfo.fromMap(map)
            } else {
                result[key] = value
            }
        }
        result
    } catch (e: Throwable) {
        logE("ObfsUtils: Failed to load cache - ${e.message}")
        null
    }
}

private fun saveToCache(file: File, table: Map<String, Any>) {
    try {
        val json = JSONObject()
        table.forEach { (key, value) ->
            when (value) {
                is ObfsMethodInfo -> json.put(key, JSONObject(value.toMap()))
                is String -> json.put(key, value)
                else -> json.put(key, value.toString())
            }
        }
        file.writeText(json.toString())
    } catch (e: Throwable) {
        logE("ObfsUtils: Failed to save cache - ${e.message}")
    }
}

package io.github.cpatcher.arch

import de.robv.android.xposed.XposedHelpers

object ExtraField {
    private const val PREFIX = "cpatcher_extra_"

    fun set(target: Any, key: String, value: Any?) {
        XposedHelpers.setAdditionalInstanceField(target, PREFIX + key, value)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(target: Any, key: String): T? {
        return XposedHelpers.getAdditionalInstanceField(target, PREFIX + key) as? T
    }

    fun remove(target: Any, key: String) {
        XposedHelpers.removeAdditionalInstanceField(target, PREFIX + key)
    }
}

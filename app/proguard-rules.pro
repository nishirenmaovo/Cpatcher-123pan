# Xposed 模块 ProGuard 规则
-keep class io.github.cpatcher.Entry { *; }
-keep class io.github.cpatcher.arch.** { *; }
-keep class io.github.cpatcher.handlers.** { *; }
-keep class org.luckypray.dexkit.** { *; }
-dontwarn de.robv.android.xposed.**

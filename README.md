# Cpatcher-123pan — 123云盘 Xposed 模块

基于 Cpatcher 框架的 123云盘去广告 + VIP 本地增强 Xposed 模块。

## 功能
- **去广告**：开屏广告跳过、广告 View 隐藏、广告加载拦截、四大广告 SDK 兜底拦截
- **VIP 本地增强**：VIP 状态标识修改、付费弹窗拦截、支付入口拦截、用户信息字段注入

> VIP 功能仅修改客户端本地 UI 显示，服务端控制的存储空间、下载速度等无法绕过。

## 构建

### GitHub Actions（本仓库已配置）
Push 到 main 分支后自动构建，APK 在 Actions → Artifacts 中下载。

### 本地构建
```bash
gradle assembleDebug
```
输出：`app/build/outputs/apk/debug/app-debug.apk`

## 安装激活
1. 安装 APK
2. LSPosed Manager → 模块 → 启用 Cpatcher-123pan
3. 作用域勾选 123云盘（com.mfcloudcalculate.networkdisk）
4. 强制停止 123云盘后重开

## 调试
```bash
adb logcat -s Cpatcher:V
```

## 免责声明
仅供学习与研究用途。
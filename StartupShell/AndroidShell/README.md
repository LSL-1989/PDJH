# Hospital Queue WebView Shell

候诊区大屏、诊室门边屏、自助签到终端的 Android WebView 壳工程，实现规范见 [`../docs/android-shell-spec.md`](../docs/android-shell-spec.md)（与仓库 [`docs/android/android-shell-spec.md`](../../docs/android/android-shell-spec.md) 对齐）。

## 三种安装包（Product Flavor）

| Gradle Flavor | `getTerminalType()` | WebView 首页 | 屏幕方向 |
|---------------|---------------------|-------------|---------|
| `screen` | `SCREEN` | `{baseUrl}/screen/` | 横屏 |
| `clinic` | `CLINIC` | `{baseUrl}/clinic/` | 竖屏 |
| `checkin` | `CHECKIN` | `{baseUrl}/checkin/` | 竖屏 |

## JS Bridge

页面加载后注入 `window.AndroidNative`：

```javascript
window.AndroidNative.getDeviceId()      // XXXX-XXXX-XXXX-XXXX（≤20，[A-Z0-9-]），SharedPreferences 持久化
window.AndroidNative.getTerminalType()  // SCREEN / CLINIC / CHECKIN（安装包写死）
window.AndroidNative.getServerBase()    // 内网服务根 URL，空则 H5 使用相对路径 /api
```

非法旧值（如历史 `HJ-{UUID}`）在启动时会自动作废并重新生成。

## Kiosk 能力

- 全屏、屏幕常亮、开机自启
- Flavor 锁定横/竖屏（不随传感器旋转）
- 双击返回键退出维护模式
- **长按屏幕**打开服务地址配置（保存后重载；可恢复默认）
- 断网自动重连（5 秒轮询 + 网络恢复回调）
- WebView 默认缓存策略

## 签名（Release 上线）

1. 生成密钥库（只需一次，妥善备份 `.jks` 与密码）：

```powershell
cd d:\AiProject\QueueNumberCalling\StartupShell\AndroidShell
mkdir keystore -Force
keytool -genkeypair -v `
  -keystore keystore\pdjh-shell.jks `
  -alias pdjh-shell `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -storepass 你的仓库密码 -keypass 你的密钥密码 `
  -dname "CN=PDJH Shell, OU=Hospital, O=Gocent, L=City, ST=Province, C=CN"
```

2. 复制并填写本地配置（勿提交仓库）：

```powershell
copy keystore.properties.example keystore.properties
# 编辑 keystore.properties：storeFile / storePassword / keyAlias / keyPassword
```

3. 再执行 `assembleXxxRelease`，产出即为已签名 APK。  
   若没有 `keystore.properties`，Release 仍可编译，但不会自动签名。

## 构建

需要 **JDK 17** 与 **Android SDK 34**（需已安装 `platforms;android-34` 与 `build-tools;35.0.0`）。

版本矩阵（与 Android Studio Meerkat+ / 新版 SDK 兼容）：

| 组件 | 版本 |
|------|------|
| Gradle | 8.11.1 |
| Android Gradle Plugin | 8.10.1 |
| Kotlin | 2.0.21 |

在 Android Studio 中打开本目录，Sync 后选择 Flavor 运行。命令行示例：

```bash
# 候诊大屏
./gradlew :app:assembleScreenRelease

# 门边屏
./gradlew :app:assembleClinicRelease

# 签到
./gradlew :app:assembleCheckinRelease
```

Debug 包：

```bash
./gradlew :app:assembleScreenDebug
```

若 Sync 仍失败，请在 SDK Manager 安装 **Android SDK Platform 34** 与 **Build-Tools 35.0.0**；国内网络可在 `settings.gradle.kts` 中保留阿里云镜像。

### 关于 `SDK XML version 4` 警告

本机若安装了 **Android SDK Platform 36.1**（新版 Android Studio 默认会装），其元数据格式为 XML v4，而当前 AGP 在扫描 SDK 目录时仍报「只支持到 v3」。**不影响编译**，可任选其一消除：

1. **推荐**：SDK Manager → SDK Platforms → 取消勾选 **Android 36.1**（本项目 `compileSdk = 34`，不需要 36.1）
2. SDK Manager → SDK Tools → 安装 **Android SDK Command-line Tools (latest)**
3. 忽略该警告，继续开发

默认服务地址在 `gradle.properties` 的 `SERVER_BASE`（默认 `http://queue.hospital.local`），也可在 `app/build.gradle.kts` 中覆盖。各 Flavor 实际加载：

| Flavor | 首页 URL |
|--------|----------|
| screen | `{SERVER_BASE}/screen/` |
| clinic | `{SERVER_BASE}/clinic/` |
| checkin | `{SERVER_BASE}/checkin/` |

运行时：长按屏幕 →「服务地址配置」写入 SharedPreferences（`ServerConfig`），或点「恢复默认」清除覆盖。

## 包名

| Flavor | Application ID |
|--------|----------------|
| screen | `com.hospital.queue.shell.screen` |
| clinic | `com.hospital.queue.shell.clinic` |
| checkin | `com.hospital.queue.shell.checkin` |

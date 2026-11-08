# 安卓 WebView Shell 规范

> **职责：** 为候诊区大屏、诊室门边屏、自助签到终端提供统一设备指纹与终端类型；H5 通过 JS Bridge 完成双线绑定。  
> **消费者：** 现场安卓设备厂商 / 实施工程师（本仓库不含原生工程，仅规范与 H5 Mock）。

## 1. Shell 变体

| Shell 安装包 | `getTerminalType()` | WebView 首页 | 屏幕方向 |
|-------------|---------------------|-------------|---------|
| 候诊大屏壳 | `SCREEN` | `{baseUrl}/screen/` | 横屏 |
| 门边屏壳 | `CLINIC` | `{baseUrl}/clinic/` | 竖屏 |
| 签到壳 | `CHECKIN` | `{baseUrl}/checkin/` | 竖屏 |

> 原生实现：`StartupShell/AndroidShell`（三 Flavor）。

## 2. JS Bridge（必须注入）

```javascript
window.AndroidNative = {
  /**
   * Shell 首次启动生成并持久化。
   * 约束：最终长度 ≤20；仅数字、大写字母与连字符「-」（例：A3F9-K2M7-Q1WP-8X4C，19 位）。
   * 禁止 HJ-{UUID} 等超长格式；H5 会对超长/非法字符做规范化截断，但仍应以本规范生成为准。
   */
  getDeviceId() { return "A3F9-K2M7-Q1WP-8X4C"; },

  /** 安装包写死，不可由 H5 修改 */
  getTerminalType() { return "SCREEN"; }, // 或 CLINIC / CHECKIN

  /** 可选：内网服务根 URL，空则 H5 使用相对路径 /api */
  getServerBase() { return "http://queue.hospital.local"; }
};
```

- **禁止**使用 WiFi MAC 作为 deviceCode（Android 6+ 限制，不可靠）。
- `getDeviceId()` 存入 `SharedPreferences`；清除应用数据后视为新设备；**须满足 ≤20 位且仅 `[A-Z0-9-]`**。
- 废弃旧接口 `getMacAddress()`，统一使用 `getDeviceId()`。

## 3. Kiosk 要求

- 全屏、屏幕常亮、开机自启
- SCREEN 锁横屏；CLINIC / CHECKIN 锁竖屏
- 禁用系统返回键退出（或双击退出维护模式）
- 长按可配置服务根地址并重载（实现见 `StartupShell/AndroidShell`）
- 断网自动重连；WebView 缓存 H5 静态资源

## 4. H5 绑定流程（双线）

1. 启动后读取 `localStorage.pdjh_device_code` 或 Shell `getDeviceId()`
2. 调用 `GET /api/Terminal/Resolve?deviceCode=&terminalType=`
3. **路径 A：** 404 → 用户确认 → `POST /api/Terminal/Register` → 待配置待机轮询
4. **路径 B：** 用户手工录入后台 deviceCode → 存 localStorage → Resolve
5. `code=200` 且 `status=READY` 后进入业务页

## 5. 开发 Mock

无 Shell 时，各 H5 工程在 dev 环境自动调用 `installAndroidNativeMock(terminalType)`（见 `packages/common/src/androidNative.mock.js`）。

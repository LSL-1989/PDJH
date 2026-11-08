# 欢匠 PDJH - HJ叫号器（WPF）

**.NET Framework 4.8 + WPF** 无边框通用叫号客户端。门诊诊室、药房窗口、检验窗口、医技/体检检查室共用同一套程序；登录后按上岗窗口 `businessType` 切换文案与药房专有能力（配药中、扫码叫号）。

内部工程名为 `Gocent.CallStation`（历史名 `Gocent.Caller`）。

## 功能

1. **登录**（360×300）：医院名称、工号/手机号、密码显隐、进入叫号器、忘记密码申请
2. **首次/重置后登录**：提示是否修改密码
3. **确认上岗**：选择诊室 / 窗口 / 检查室（按场景名词切换）
4. **列表版**（300×800）：当前呼叫、在线灯（WebSocket）、呼叫下一位 / 重呼 / 过号 / 完成、等候（点名）/ 已完成 / 过号
5. **药房模式**：在列表能力上增加配药中列表、配药完成兜底、扫码叫号；精简悬浮窗同样可用
6. **体检增强**：当前呼叫展示项目名与下一站（`examItemName` / `examNextHint`，复用 `GetDocCallList`）
7. **精简版**：可拖拽圆形悬浮窗（门诊/药房/检验/体检），悬停：完成 / 呼叫下一位 / 重呼 / 过号 / 切换 / 退出

## 后端接口

| 接口 | 说明 |
|------|------|
| `POST /Doctor/Login` | 工号或手机号登录（校验统一 `users` 表 BCrypt）；返回 `stationMode`：CLINIC / PHARMACY / LAB / EXAM |
| `POST /Doctor/ChangePassword` | 改密（写入 `users`） |
| `POST /Doctor/RequestPasswordReset` | 忘记密码申请 |
| `POST /Doctor/AdminResetPassword` | 后台管理员重置 |
| `POST /Doctor/BindSittingRoom` | 确认上岗窗口 |
| `POST /Doctor/LeaveSittingRoom` | 退出离岗 |
| `DoctorCall/*` | 叫号状态与播报 |
| `ws://.../api/pdjh-ws-native` | 原生 STOMP（无 SockJS） |

默认密码：`123456`（新建档案 / 管理员重置后，须改密）。

## 开发

用 **Visual Studio 2022/2026** 打开 `Gocent.CallStation.sln`，目标框架 **.NET Framework 4.8**。

```text
ApiBaseUrl / WsUrl 见 Gocent.CallStation/App.config
```

启动前确保 `pdjh-server` 已运行（默认 `8081`）。

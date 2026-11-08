# 欢匠 PDJH - HJ候诊区大屏

**HJ候诊区大屏**（仓库目录 `HJ候诊区大屏`）是候诊区公共显示大屏（**SCREEN 终端专用**）与院长效能看板前端。

> 诊室门边屏（CLINIC）请使用独立项目 **HJ诊室门边屏终端**（`/clinic/`）。架构说明见 [docs/architecture/显示屏终端分层说明.md](../docs/architecture/显示屏终端分层说明.md)。

## 核心能力

1. **等候区综合大屏** — `/default`、`/default-mul`、`pdjh3`～`pdjh8`、`/GlobalHub` 等模板路由（`/pdjh1` 已兼容重定向至 `/default`）
2. **院长效能看板** — `/directorDashboard`
3. **WebSocket (STOMP)** — 实时叫号广播与轮询兜底

## 技术栈

- **Vue 3** + **Vite 8** + **Element Plus**（按需引入 + pdjh 模板懒加载）
- **Echarts 5**（CDN）
- **STOMP / SockJS**

## 运行

```bash
npm install
npm run dev    # http://localhost:5173/screen/
npm run build  # 输出 dist/ → 部署至 Nginx /screen
```

开发环境 API 代理：`VITE_DEV_PROXY_TARGET`（默认 `http://127.0.0.1:8081`）。

## 终端绑定

- Android Shell：`terminalType=SCREEN`，首页 `{baseUrl}/screen/`
- 绑定流程：`Terminal/Register` → 后台屏幕管理 → `Terminal/Resolve` → 按 `templateRoute` 进入模板

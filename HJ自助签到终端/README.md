# 欢匠 PDJH - HJ自助签到终端

**HJ自助签到终端**（仓库目录 `HJ自助签到终端`）是“欢匠智慧医院全场景排队叫号系统”的门诊/医技现场自助签到前端（Vue 3 H5）。

## 场景定位

提供给就诊患者到院后基于 LBS 距离或聚合扫码完成的一键排队入列操作服务。这是大体检排队流量洪峰的首道分流闸门。

## 核心架构演进 (Phase 11 新增)

1. **打通预约时间槽壁垒（Time Slot Verification）**
   - 系统支持携带 `appointmentStartTime` 和 `appointmentEndTime` 双时间域的预约令牌签到。
   - 包含严格的“过早签到”拒绝机制（禁止超过早于预设容忍度 30 分钟报到），保护在场候诊秩序。

2. **过号智能衰减 (SLA Penalty)**
   - 若患者迟到且超过预约结束边界的宽限期（通常为 30 分钟），签到端请求后台引擎时，后台将自动剥夺其最高优先权（将 `isAppointment=1` 重置为 `0`）。
   - 保障履约者利益，实现“高保真签到”生态。

## 运行与编译

发布路径固定为 **`/checkin/`**（与安卓签到壳 `WEB_PATH`、Nginx `location /checkin/` 一致）。

```bash
npm install

# 必须占住 5174（strictPort）；被占用时会直接失败，不会偷偷改到 5176
npm run dev -- --host
# 平板/壳请打开：http://<本机IP>:5174/checkin/
# 错误示例：http://192.168.0.75:5174/checkin/

npm run build
# dist/ → 部署到 Nginx /usr/share/nginx/html/checkin/（对外 URL: {host}/checkin/）
```

安卓签到壳首页为 `{SERVER_BASE}/checkin/`。`SERVER_BASE` 填服务根（勿带 `/checkin`）；本地联调示例：`http://192.168.0.75:5174`。

路由与门边屏一致使用 **Hash**（`#/bind`、`#/signin`）。

若仍白屏：看页面是否出现红色错误文字；并确认浏览器地址是 **5174** 而不是 5176。

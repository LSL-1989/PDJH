# 分诊台护士工作站（HJ护士工作站）

门诊诊区护士 Web 工作台：告警驱动、签到兜底、队列干预（不代叫号）。

## 开发

```bash
npm install
npm run dev
```

默认 `http://127.0.0.1:5175`，API 代理至 `http://127.0.0.1:8081/api`。

## 部署

构建产物部署至 Nginx `/nurse` 路径。

## API

统一走 `src/api/nurseDesk.js` → `POST /api/NurseDesk/*`

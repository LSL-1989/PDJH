# 欢匠·序流排队分诊管理系统 — 项目文档中心

本目录为 **QueueNumberCalling**  monorepo 的权威文档归档位置。产品说明、业务规则、部署方案、数据库设计、接口对接及评估报告均集中于此，便于检索与版本同步。

**完整单册（产品 + 架构 + 全模块用法 + 全表字段 + 全接口）：** [欢匠序流排队分诊管理系统-完整说明书.md](./欢匠序流排队分诊管理系统-完整说明书.md)（2026-08-28 按当前代码整理，不依赖跳转其它文档即可阅读）。

**最后整理日期：** 2026-10-20

### 子项目目录

| 项目 | 仓库路径 |
|------|---------|
| **HJ管理后台** | `HJ管理后台/Gocent.Manager`（若仍为 `HJ后台/Gocent.PDJH`，请先停止 dev 后执行 `scripts/rename-HJ管理后台.ps1`） |
| **HJ叫号器** | `HJ叫号器` |
| **HJ自助签到终端** | `HJ自助签到终端` |
| **HJ护士工作站** | `HJ护士工作站` |
| **HJ候诊区大屏** | `HJ候诊区大屏`（Vue3 + Vite，SCREEN 专用） |
| **HJ诊室门边屏终端** | `HJ诊室门边屏终端` |

---

## 文档维护约定

> **任何业务调整、规则变更、接口变更、部署方式变更或新终端上线，必须同步更新本目录下对应文档。**

| 变更类型 | 应更新的文档 |
|---------|-------------|
| 产品定位、终端能力、对外宣传 | [product/产品说明书.md](product/产品说明书.md) |
| 排队规则、分诊算法、配置项 | [business/排队规则业务逻辑分析.md](business/排队规则业务逻辑分析.md) |
| 新终端 / 模块需求 | [business/requirements/](business/requirements/) 下新增或修订 |
| 组件部署、Nginx 路由、CI/CD | [architecture/项目部署架构说明.md](architecture/项目部署架构说明.md)、[architecture/显示屏终端分层说明.md](architecture/显示屏终端分层说明.md)、[deployment/实施与部署方案.md](deployment/实施与部署方案.md)、[deployment/显示屏终端升级检查清单.md](deployment/显示屏终端升级检查清单.md) |
| HIS/LIS/PACS 对接方式 | [integration/医院接口对接实施方案说明.md](integration/医院接口对接实施方案说明.md) |
| 表结构、字段、命名约定 | [database/数据库设计说明书.md](database/数据库设计说明书.md)、[database/SCHEMA_NAMING.md](database/SCHEMA_NAMING.md) |
| 后台权限、菜单、登录 | [backend/RBAC.md](backend/RBAC.md) |
| 管理后台 UI 规范 | [architecture/管理系统UI设计规范与组件库架构.md](architecture/管理系统UI设计规范与组件库架构.md) |
| 联调 / 压测 / 评估结论 | [reports/](reports/) 下对应报告（或新建报告并在此索引） |

**提交代码时建议：** 若本次 PR 涉及业务行为变化，在 PR 描述中注明「已更新 docs/xxx」。

---

## 文档索引

### 产品与业务

| 文档 | 说明 |
|------|------|
| [product/产品说明书.md](product/产品说明书.md) | 产品白皮书：定位、终端矩阵、技术亮点、场景覆盖 |
| [欢匠序流排队分诊管理系统-功能说明.pdf](./欢匠序流排队分诊管理系统-功能说明.pdf) | 已落地功能清单：按终端与模块列出功能名称和功能说明（2026-10-20） |
| [欢匠序流排队分诊管理系统-功能说明.docx](./欢匠序流排队分诊管理系统-功能说明.docx) | 同上内容的 Word 版，便于继续修改 |
| [business/排队规则业务逻辑分析.md](business/排队规则业务逻辑分析.md) | 四条业务线（门诊/药房/医技/体检）排队规则与配置范式 |
| [business/requirements/HJ诊室门边屏终端-需求说明书.md](business/requirements/HJ诊室门边屏终端-需求说明书.md) | HJ诊室门边屏终端（仓库目录 `HJ诊室门边屏终端`）功能与非功能需求 |

### 架构与部署

| 文档 | 说明 |
|------|------|
| [architecture/项目部署架构说明.md](architecture/项目部署架构说明.md) | Monorepo 组件清单、Nginx 路由、独立构建与 CI/CD 建议 |
| [architecture/管理系统UI设计规范与组件库架构.md](architecture/管理系统UI设计规范与组件库架构.md) | HJ管理后台 Design System 与组件规范 |
| [deployment/实施与部署方案.md](deployment/实施与部署方案.md) | 现场实施 SOP：基础设施、后端/前端发布、UAT 验收 |

### 集成与数据

| 文档 | 说明 |
|------|------|
| [integration/医院接口对接实施方案说明.md](integration/医院接口对接实施方案说明.md) | HIS 对接四种模式（HTTP / Webhook / JDBC / Edge Agent） |
| [database/数据库设计说明书.md](database/数据库设计说明书.md) | 核心 ER 与表结构说明 |
| [database/SCHEMA_NAMING.md](database/SCHEMA_NAMING.md) | 表名/列名约定与 DDL 权威来源 |
| [backend/RBAC.md](backend/RBAC.md) | 后台菜单、角色、用户与登录流程 |

### 评估报告（归档）

| 文档 | 说明 |
|------|------|
| [reports/项目规范性与完整性评估报告.md](reports/项目规范性与完整性评估报告.md) | 代码库结构与服务完整度评估 |
| [reports/核心分诊与呼叫调度API测试报告.md](reports/核心分诊与呼叫调度API测试报告.md) | DoctorCallController 等核心 API 测试 |
| [reports/Vue前端矩阵连贯性评估报告.md](reports/Vue前端矩阵连贯性评估报告.md) | HJ叫号器与 HJ候诊区大屏 WebSocket 链路评估 |

### 开发辅助

| 文档 | 说明 |
|------|------|
| [development/HJ体检H5-启动说明.md](development/HJ体检H5-启动说明.md) | 体检 H5 本地启动与端口说明 |

各子工程 `README.md`（如 `pdjh-server/README.md`、`HJ自助签到终端/README.md`（HJ自助签到终端））仍保留在对应目录，侧重**本地开发与构建**；与业务/实施相关的说明以本 `docs/` 目录为准。

---

## 仓库组件与文档对应关系

| 目录 | 角色 | 相关文档 |
|------|------|---------|
| `pdjh-server` | Java 核心调度引擎 | database/*、backend/RBAC、integration/*、business/* |
| `HJ管理后台/Gocent.Manager` | **HJ管理后台**（Web） | architecture/UI 规范、backend/RBAC |
| `HJ叫号器` | **HJ叫号器**（WPF） | product/产品说明书、business/排队规则 |
| `HJ候诊区大屏` | **HJ候诊区大屏** | deployment/*、architecture/* |
| `HJ诊室门边屏终端` | **HJ诊室门边屏终端** | business/requirements/HJ诊室门边屏终端-* |
| `HJ自助签到终端` | **HJ自助签到终端** | deployment/* |
| `HJ体检H5` | 体检导检 | business/排队规则、development/* |
| `HJ官网` | 静态官网 | architecture/项目部署架构说明 |
| `scripts/` | SQL 种子与运维脚本 | database/SCHEMA_NAMING |
| `Nginx-Config/` | 示例 Nginx 配置 | architecture/*、deployment/* |

---

## 资源目录

- [assets/](assets/) — 产品截图、架构图等静态资源（引用时使用相对路径，如 `../assets/xxx.png`）

# 欢匠·序流管理系统 (HJ管理后台) - UI 视觉设计交互规范体系

> **文档位置：** `docs/architecture/` · UI 规范变更请同步更新本文档。

基于现代企业级 SaaS (B2B Admin Dashboard) 及医疗级应用的严苛标准，本规范体系（Design System）旨在为 **HJ管理后台**（仓库目录 `HJ管理后台/Gocent.Manager`）注入 **“Tech Blue (科技医疗蓝)”** 核心基因。

系统整体调性：**专业、沉稳、高效、克制**。拒绝夸张的动效，利用“轻量玻璃拟态 (Micro-Glassmorphism)”搭配“清晰的视知觉层级 (Visual Hierarchy)”，使得操作人员在高负荷工作下依然不易产生视觉疲劳。同时完全满足 **WCAG AA 级** 的无障碍高对比度视读标准。

---

## 🎨 视觉基石系统 (Design Foundations)

### 1. 结构化调色板 (Color System)
**医疗科技蓝 (Primary Colors - Brand)** 
系统主视觉、品牌核心色，传达安全与信赖。
- `--color-primary-100`: `#E0F2FE` (浅蓝背景)
- `--color-primary-500`: `#0EA5E9` (次要行动按钮)
- `--color-primary-600`: `#0284C7` (**核心主轴色 / 强按钮主体**)
- `--color-primary-900`: `#0C4A6E` (极深蓝，用于极强视觉强调)

**支撑中性灰 (Neutral Palette)**
用于全局容器描边、排版及底色，传达极简层级。
- `--color-slate-50`:  `#F8FAFC` (全屏外层底色，柔和不刺眼)
- `--color-slate-200`: `#E2E8F0` (模块分割线 / 表格边框)
- `--color-slate-500`: `#64748B` (次要辅助文字 / 提示信息)
- `--color-slate-800`: `#1E293B` (主标题 / 全局正文文字，确保 WCAG AA 对比度)

**语义告警色 (Semantic Colors)**
用于医疗风险控制，严防业务误判。
- `--color-success`: `#10B981` (翠绿 - 状态正常、服务可用)
- `--color-warning`: `#F59E0B` (亮橙 - 将要过号、中位警告)
- `--color-error`  : `#EF4444` (警红 - 设备掉线、极端超载、阻塞警报)
- `--color-info`   : `#6366F1` (靛蓝 - 常规数据提示)

### 2. 现代多格字体版式 (Typography System)
抛弃传统的宋体排版，采用易于在高清液晶屏幕上识别的黑字体簇。
- **全局字体架构 (Font Family)**: `'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif`
- **等宽数字体 (Metrics Font)**: 强烈建议全局数据大屏及数据表格内的所有“人数排队数字、流水编号”强制启用 `'JetBrains Mono', 'DIN Alternate', monospace`，以保证时间、数字宽度齐平对齐。
- **阶梯字号 (Font Scale)**: 
  - `12px` - 表格角标、微小辅助备注
  - `14px` - **标准体层 (Base Text)** 全局表单、默认按钮
  - `16px` - 二级标题、强提示弹窗
  - `20px` - 一级页头标题、模态框主轴

### 3. 数学等比呼吸间距 (Spacing System)
全系统的 `padding` 和 `margin` 强制收敛至基于 `4px` 为底数的倍率矩阵，告别无序的间距。
- `space-2` (8px), `space-4` (16px), `space-6` (24px - 卡片主流默认内边距)

---

## 🧱 原子与大分子组件架构 (Component Library)

### 1. 表单与交互矩阵 (Form Elements)
- **输入框 (Input)**: 统一带 `border-slate-200` 线性描边。在 `&:focus` 时，激发出 `box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15)` 浅蓝晕圈包裹，杜绝突兀的 `outline` 粗黑线。
- **按钮体系 (Buttons)**:
  - **首要动作 (Primary)**: `bg-primary-600` 结合白字，并附带平滑的 200ms `hover:translate-y-px` 提拉微动效。
  - **次要动作 (Secondary)**: 纯白底色配灰色描边 `border-slate-200` 和 `slate-700` 文字。
  - **红线核爆 (Danger)**: 用于“重置科室”、“全量强刷”等高危销毁按钮，必须使用 `error` 红色，配合极高对比的白色文案。

### 2. 卡片与骨架布局 (Containers)
- **卡片 (Card)**: 所有的设置板块强制包裹在白色的 Card 中，附以 `--shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05)` 阴影。鼠标划过时触发由 `sm` 提升为 `md` 深度的浮动感知。
- **抽屉页 (Drawer / Modal)**: 所有新增医生、科室复杂级联表配置项应当使用屏幕右侧划出的 Drawer，并施加 `backdrop-blur` 底部全屏背景模糊，迫使运营人员专注填写。

---

## 💻 响应式及多端可用性方案 (Responsive Strategy)

### 自适应排位格 (Layout Patterns)
针对不同接诊台以及运营电脑的屏幕，设定强自适应断点：
- **运营大屏 (>= 1440px)**: 允许横向展示 4-5 个功能聚合分析块卡片。
- **标准工作本 (1024px ~ 1366px)**: 中规中矩的 3 栏栅格响应展示。
- **管理随行 Pad (768px ~ 1024px)**: 表格响应为可水平滚动的 Flex 或隐藏非必须数据列（如：备注、全长流水线代码），仅暴露状态、医生标识和操作按钮组。

---

## ♿ 医疗高容错与无障碍规避 (Accessibility Standards)

在忙碌甚至重压的门诊后台调度中，**“不犯错”永远高于“炫酷”**。

1. **防患交互 (Error Prevention)**: 关键流转（比如强制某排队状态跳变，或修改基础叫号规则配置）之前，强制出带再次确认拦截的 `Dialog` 面板，而非即点即存！
2. **纯粹对比度 (Contrast Ratio)**: 所有在蓝底色 (`primary-600`) 上方的文字或图标，对比度必须 >= 4.5:1 (通过 WCAG AA 标准)，确保有白内障的资深医生依然可见。
3. **沉浸式聚焦指示 (Focus Management)**: 操作医生如若使用键盘 <kbd>Tab</kbd> 流流转表单时，整个网页的可视聚焦圈必须逻辑连贯，不能漂移。

**UI Designer**: Antigravity UI Specialist
**规范交付日期**: 2026-04-01
**后续对接进程**: 已随时准备向 Vue/CSS/Tailwind 研发工程师递交开发落盘指引。

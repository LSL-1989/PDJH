# 欢匠 PDJH - 核心排队推算引擎与总线 (Core Engine)

本项目是主导整个“智慧医院分诊与叫号场景”的 Java Spring Boot 后端超级引擎。

## 系统架构与能力 (全场景打通)

经历 Phase 11 大型重构后，本引擎已从“结构松散的单体叫号玩具”，晋升具备生产级医院中间件能力。

1. **状态机驱动与时间闭环计步器**
   - 底层 `CallList` 实现从 `enqueue` -> `attending` (`startTime`) -> `completed` (`endDate`) 的完美生命周期落点。
   - 所有引擎层面的动作自带防并发悲观锁及超时熔断判定。

2. **智能体检算路调度中心 (Smart Routing Engine)**
   - 设计了极其复杂的科室依赖关系校验图谱 (Depends_on_ids)。
   - 自主调度未挂起且依赖已释放的空闲科室派发给病患。

3. **突发灾难重入网关 (Emergency Re-Routing)**
   - 暴露 `/suspendDept` 管理端接口，在遇到物理空间停电时，**瞬间挂起并踢出当前科室所有等待用户**，引发大规模连带性异步切流算路编排，将海量被踢患者无缝引流至全院其他可做项目的空闲检验科。
   - 暴露 `/forceRoute` 护士端霸权接口，处理晕血、残障用户的强行就诊特例（附魔 `isAppointment=2` 空降兵特权）。

4. **预检高保真签到网关**
   - 彻底杜绝使用旧日过时预约码（不在 Time Slot 内或迟到超过 30 分钟）换取首发叫号优先权的霸王行径，实行 `isAppointment` 属性的实时动态衰减。

5. **院长监控效能舱只读分析节点 (SLA OLAP)**
   - 使用异步及单独抽取 `DashboardService` 隔离常规队列事务操作对大盘看板产生锁表和影响。支持全院超时警报。

6. **全时空微信 SPI 挂载 (Wechat Notifications)**
   - 提供标准化的 `MessageNotificationService`，使本排队系统与外部“第三方智慧医院中台”保持松耦合。支持候诊即将过号提醒与下一个科室接力导流。

## 本地开发指南

- 技术栈：Spring Boot 2 / 3, Hibernate JPA, MySQL 8+.
- **运行配置**：配置 `-Dspring-boot.run.profiles=dev` 参数以开发者模式拉起。
- **构建方式**：`mvn clean package`
- **项目文档**：业务、部署、数据库与 RBAC 说明见仓库根目录 [docs/](../docs/README.md)。

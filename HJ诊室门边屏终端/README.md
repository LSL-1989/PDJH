# 欢匠 PDJH - HJ诊室门边屏终端

**HJ诊室门边屏终端**（仓库目录 `HJ诊室门边屏终端`）是各诊室门口的 9:16 竖屏展示端，用于展示值班信息、当前办理与等候患者（姓名脱敏）。

完整需求说明见 [docs/business/requirements/HJ诊室门边屏终端-需求说明书.md](../docs/business/requirements/HJ诊室门边屏终端-需求说明书.md)。

## 运行

```bash
npm install
npm run dev
```

启动后通过 `?deviceCode=xxx` 绑定物理终端（与 HJ自助签到终端相同的 `deviceCode` 机制）。

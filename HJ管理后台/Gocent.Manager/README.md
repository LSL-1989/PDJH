# 欢匠 PDJH - HJ管理后台 (Smart Hospital Admin Portal)

**HJ管理后台**（目录 `HJ管理后台/Gocent.Manager`）是整个「欢匠智慧医院全场景排队叫号系统」的超级管理员管控中心，衍生自 vue-admin-template。

This project is the super-administrator control center for the entire "欢匠 Smart Hospital Global Queue & Calling System," derived from the vue-admin-template framework.

## Core Capabilities

1. **Global Data Dictionary Maintenance:** Managing hospital branches, departments, doctors' profiles, and macro queueing policies.
2. **Medical Tech Workflow Editor (Added in Phase 11):** Empower hospital staff to define tree-structured medical dependencies (e.g., blood extraction must precede an ultrasound scan) to support intelligent physical exam routing.
3. **Privilege Authentication Issue:** Management of QR codes and accounts capable of VIP intervention and emergency force routing.

## Development

```bash
npm install
npm run dev
```
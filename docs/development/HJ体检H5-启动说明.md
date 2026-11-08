# 体检H5 启动说明

> **文档位置：** `docs/development/` · 端口或启动方式变更请同步更新本文档。

## 启动方式

### 方式1: 双击启动脚本
- Windows: 双击 `start.bat`
- PowerShell: 右键 `start.ps1` 选择"使用PowerShell运行"

### 方式2: 命令行启动
```bash
# 在项目目录下执行
npm run dev
```

### 方式3: 直接使用vite
```bash
npx vite --host 0.0.0.0 --port 5173
```

## 访问地址

- **本地访问**: http://localhost:5173
- **局域网访问**: http://192.168.0.28:5173

## 项目信息

- **技术栈**: Vue 3 + Vite + TypeScript + Vant UI
- **开发端口**: 5173
- **预览端口**: 4173

## 其他命令

```bash
# 构建生产版本
npm run build

# 预览构建结果
npm run preview

# 类型检查
npx vue-tsc --noEmit
```

## 注意事项

- 确保已安装 Node.js (推荐 v18+)
- 首次运行需执行 `npm install` 安装依赖
- 开发服务器会自动热更新代码
- API请求已配置代理到 http://127.0.0.1:8080/api

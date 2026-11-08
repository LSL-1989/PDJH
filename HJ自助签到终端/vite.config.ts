import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const rootDir = path.dirname(fileURLToPath(import.meta.url))
const monorepoRoot = path.resolve(rootDir, '..')
const commonDir = path.resolve(monorepoRoot, 'packages/common/src')

/**
 * 签到 H5 Vite 配置（对齐门边屏可跑通的精简结构）。
 * 消费者：安卓签到壳 WebView、平板 Chrome 联调。
 */
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_DEV_PROXY_TARGET || env.DEV_PROXY_TARGET || 'http://127.0.0.1:8081'
  const port = Number(env.DEV_PORT || 5174)

  return {
    // 与安卓壳 WEB_PATH、Nginx 一致：{SERVER_BASE}/checkin/
    base: '/checkin/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@pdjh/common': commonDir
      }
    },
    optimizeDeps: {
      // 预打包 common，避免安卓 Chrome 加载 /@fs/D:/... 盘符 URL 失败
      include: ['@pdjh/common/terminalBinding', '@pdjh/common/androidNative.mock']
    },
    server: {
      port,
      // 端口被占时禁止跳到 5176，避免壳/平板仍访问 5174 导致「改了却空白」
      strictPort: true,
      host: true,
      allowedHosts: true,
      fs: {
        allow: [monorepoRoot]
      },
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true
        }
      }
    },
    preview: {
      port,
      strictPort: true,
      host: true,
      allowedHosts: true,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true
        }
      }
    }
  }
})

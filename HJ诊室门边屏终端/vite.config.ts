import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8081'

  return {
    base: '/clinic/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@pdjh/common': path.resolve(__dirname, '../packages/common/src')
      }
    },
    server: {
      port: 5175,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true
        }
      }
    }
  }
})

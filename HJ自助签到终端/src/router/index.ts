import { createRouter, createWebHashHistory } from 'vue-router'
import SignIn from '../views/SignIn.vue'
import BindingGuide from '../views/BindingGuide.vue'
import {
  getStoredDeviceCode,
  getShellTerminalType,
  setStoredDeviceCode,
  resolveTerminal,
  TERMINAL_STATUS,
  installAndroidNativeMock
} from '../utils/terminalBinding'
import { IS_DEV } from '../config/env'

/** URL ?deviceId= 或 ?deviceCode= 写入本地绑定 */
function syncDeviceCodeFromQuery(query: Record<string, unknown>) {
  const raw = query.deviceId ?? query.deviceCode
  const fromQuery = (Array.isArray(raw) ? raw[0] : raw)?.toString().trim()
  if (fromQuery) setStoredDeviceCode(fromQuery)
}

/** 防止网络慢时路由守卫挂起导致白屏 */
function withTimeout<T>(promise: Promise<T>, ms: number, fallback: T): Promise<T> {
  return Promise.race([
    promise,
    new Promise<T>((resolve) => setTimeout(() => resolve(fallback), ms))
  ])
}

const router = createRouter({
  // 对齐门边屏：Hash 路由在 Android WebView 更稳，不依赖 History API / 路径回退
  history: createWebHashHistory(),
  routes: [
    { path: '/bind', name: 'Bind', component: BindingGuide },
    {
      path: '/signin',
      name: 'SignIn',
      component: SignIn,
      alias: '/'
    }
  ]
})

/**
 * 打开页面时检测设备编号与绑定状态：
 * - 未绑定 → /bind 绑定引导
 * - 已绑定 READY → 签到页
 * - PENDING / FROZEN / 其他 → /bind 由 BindingGuide 展示对应状态
 */
router.beforeEach(async (to, _from, next) => {
  if (to.path === '/bind') {
    next()
    return
  }

  try {
    syncDeviceCodeFromQuery(to.query as Record<string, unknown>)
    if (IS_DEV) {
      installAndroidNativeMock('CHECKIN')
    }

    /** 仅用户确认过的本地编号参与自动解析；本机 Shell ID 不自动绑定 */
    const code = getStoredDeviceCode()
    if (!code) {
      next('/bind')
      return
    }

    const terminalType = getShellTerminalType('CHECKIN')
    if (terminalType !== 'CHECKIN') {
      next('/bind')
      return
    }

    const result = await withTimeout(
      resolveTerminal(code, terminalType),
      8000,
      { ok: false, status: 'UNKNOWN', code: 500, msg: '网络超时', data: null }
    )

    if (result.ok && result.status === TERMINAL_STATUS.READY) {
      next()
      return
    }

    if (result.status === TERMINAL_STATUS.FROZEN) {
      next({ path: '/bind', query: { frozen: '1' } })
      return
    }

    next('/bind')
  } catch (err) {
    console.error('[router] terminal resolve failed:', err)
    next('/bind')
  }
})

export default router

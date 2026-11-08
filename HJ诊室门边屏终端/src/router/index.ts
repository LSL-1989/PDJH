import { createRouter, createWebHashHistory } from 'vue-router'
import ClinicScreen from '../views/ClinicScreen.vue'
import BindingGuide from '../views/BindingGuide.vue'
import {
  getStoredDeviceCode,
  getShellTerminalType,
  setStoredDeviceCode,
  resolveTerminal,
  TERMINAL_STATUS,
  installAndroidNativeMock
} from '../utils/terminalBinding'
import { isClinicPreview } from '../utils/clinicPreview'

/** 管理后台预览：query preview=1，或首屏已写入的只读标记 */
function isPreviewNavigation(query: Record<string, unknown>) {
  if (isClinicPreview()) return true
  const raw = query.preview
  const flag = (Array.isArray(raw) ? raw[0] : raw)?.toString()
  return flag === '1' || flag === 'true'
}

/** URL ?deviceCode= 写入本地绑定（与 resolveDeviceCodeFromRoute 一致） */
function syncDeviceCodeFromQuery(query: Record<string, unknown>) {
  const raw = query.deviceCode
  const fromQuery = (Array.isArray(raw) ? raw[0] : raw)?.toString().trim()
  if (fromQuery) setStoredDeviceCode(fromQuery)
}

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/bind', name: 'Bind', component: BindingGuide },
    { path: '/', name: 'ClinicScreen', component: ClinicScreen }
  ]
})

/** 防止网络慢时路由守卫挂起导致白屏 */
function withTimeout<T>(promise: Promise<T>, ms: number, fallback: T): Promise<T> {
  return Promise.race([
    promise,
    new Promise<T>((resolve) => setTimeout(() => resolve(fallback), ms))
  ])
}

/**
 * 打开页面时检测设备编号与绑定状态：
 * - 未绑定 → /bind 绑定引导
 * - 已绑定 READY → 诊室展示页
 * - PENDING / FROZEN / 其他 → /bind 由 BindingGuide 展示对应状态
 */
router.beforeEach(async (to, _from, next) => {
  if (isPreviewNavigation(to.query as Record<string, unknown>)) {
    next()
    return
  }

  if (to.path === '/bind') {
    next()
    return
  }

  if (import.meta.env.DEV) {
    installAndroidNativeMock('CLINIC')
  }

  syncDeviceCodeFromQuery(to.query as Record<string, unknown>)

  /** 仅用户确认过的本地编号参与自动解析；本机 Shell ID 不自动绑定 */
  const code = getStoredDeviceCode()
  if (!code) {
    next('/bind')
    return
  }

  const terminalType = getShellTerminalType('CLINIC')
  if (terminalType !== 'CLINIC') {
    next('/bind')
    return
  }

  try {
    const result = await withTimeout(
      resolveTerminal(code, terminalType),
      8000,
      { ok: false, status: 'UNKNOWN', code: 500, msg: '网络超时', data: null }
    )

    if (result.ok && result.status === TERMINAL_STATUS.READY) {
      if (result.data?.terminalType === 'SCREEN') {
        next({ path: '/bind', query: { wrongShell: '1' } })
        return
      }
      next()
      return
    }

    if (result.status === TERMINAL_STATUS.FROZEN) {
      next({ path: '/bind', query: { frozen: '1' } })
      return
    }

    // PENDING、NOT_REGISTERED、网络异常等 → 绑定页按状态展示
    next('/bind')
  } catch {
    next('/bind')
  }
})

export default router

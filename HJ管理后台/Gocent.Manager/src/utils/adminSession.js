/**
 * 管理后台空闲会话（本地活动时间）。
 * 消费者：permission 路由守卫、request 拦截器、user 登录/退出。
 * 30 分钟与后端 AdminSessionService.IDLE_MINUTES 一致；轮询不写这个时间。
 */

export const ADMIN_IDLE_MS = 30 * 60 * 1000
const ACTIVE_KEY = 'admin_last_active_at'
/** 距上次用户操作不足该时长时，下一次业务请求才带 X-Touch 续期 */
export const TOUCH_WINDOW_MS = 20 * 1000

export function markAdminActive() {
  localStorage.setItem(ACTIVE_KEY, String(Date.now()))
}

export function clearAdminActivity() {
  localStorage.removeItem(ACTIVE_KEY)
}

export function lastAdminActiveAt() {
  const t = Number(localStorage.getItem(ACTIVE_KEY) || 0)
  return Number.isFinite(t) ? t : 0
}

/** 没有活动记录时不在本地判过期，交给后端 GetInfo（兼容本次上线前已登录的会话） */
export function isAdminIdleExpired() {
  const t = lastAdminActiveAt()
  if (!t) return false
  return Date.now() - t >= ADMIN_IDLE_MS
}

export function isPublicAdminPath(path) {
  if (!path) return false
  return path === '/login' || path === '/h5' || path.indexOf('/Singin') === 0
}

/** 登录、登出本身不能被空闲拦截挡住 */
export function isSessionFreeUrl(url) {
  return /\/User\/(LoginUser|GetUserInfo|LogOut)(\?|$)/.test(url || '')
}

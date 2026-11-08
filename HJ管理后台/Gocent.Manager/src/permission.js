import router from './router'
import store from './store'
import { ElMessage as Message } from 'element-plus'
import NProgress from 'nprogress' // progress bar
import 'nprogress/nprogress.css' // progress bar style
import { getToken } from '@/utils/auth' // get token from cookie
import getPageTitle from '@/utils/get-page-title'
import { isPathAllowed, resolveMenuTitle } from '@/utils/permissionRoutes'
import { isAdminIdleExpired, isPublicAdminPath, markAdminActive } from '@/utils/adminSession'
import { expireAdminSession } from '@/utils/request'
import { touchSession } from '@/api/user'

NProgress.configure({ showSpinner: false }) // NProgress Configuration

router.beforeEach(async(to, from, next) => {
  var code = to.params.code
  // 路由白名单
  const whiteList = ['/login', `/Singin/${code}`, `/Singin2/${code}`, `/Singin0/${code}`, '/h5'] // no redirect whitelist

  // start progress bar
  NProgress.start()

  // set page title（优先菜单管理中的 PageShowName）
  const pageTitle = resolveMenuTitle(to, store.state.user.menuTitleMap) || to.meta.title
  document.title = getPageTitle(pageTitle)

  // determine whether the user has logged in
  const hasToken = getToken()

  if (hasToken) {
    // console.log("从哪来？",from.path,"去哪？",to.path);
    if (to.path === '/login') {
      // 如果已登录，重定向到主页、
      // console.log("如果已登录，请重定向到主页");
      next({ path: '/admin' })
      NProgress.done()
    } else if (isPublicAdminPath(to.path)) {
      next()
    } else if (isAdminIdleExpired()) {
      await expireAdminSession(undefined, false)
      next(`/login?redirect=${to.path}`)
      NProgress.done()
    } else {
      // 否则去看看有没有用户名
      // console.log("如果在地址栏上直接输入地址去后端 那么检查有没有用户名");
      const hasGetUserInfo = store.getters.name
      if (hasGetUserInfo) {
        const allowedKeys = store.state.user.allowedMenuKeys
        if (allowedKeys && allowedKeys.size > 0 && !isPathAllowed(to, allowedKeys)) {
          Message.warning('无权限访问该页面')
          next({ path: '/dashboard', replace: true })
          NProgress.done()
          return
        }
        next()
      } else {
        try {
          // console.log("没有用户名 那就触发getinfo");
          // 要是没有用户名 去请求一个
          await store.dispatch('user/getInfo')
          const allowedKeys = store.state.user.allowedMenuKeys
          if (allowedKeys && allowedKeys.size > 0 && !isPathAllowed(to, allowedKeys)) {
            Message.warning('无权限访问该页面')
            next({ path: '/dashboard', replace: true })
            NProgress.done()
            return
          }
          next({ ...to, replace: true })
        } catch (error) {
          const msg = error && error.message ? String(error.message) : ''
          if (msg.indexOf('登录已过期') !== -1) {
            await expireAdminSession(msg, false)
            next(`/login?redirect=${to.path}`)
            NProgress.done()
            return
          }
          // 删除令牌并转到登录页面重新登录
          await store.dispatch('user/resetToken')
          Message.error(error || 'Has Error')
          next(`/login?redirect=${to.path}`)
          NProgress.done()
        }
      }
    }
  } else {
    /* has no token*/

    // next()
    if (whiteList.indexOf(to.path) !== -1) {
      // in the free login whitelist, go directly
      next()
    } else {
      // other pages that do not have permission to access are redirected to the login page
      next(`/login?redirect=${to.path}`)
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  // finish progress bar
  NProgress.done()
})

// 已在业务页内切换菜单也算操作；纯轮询不经过这里，因此不会把会话续活
router.afterEach((to) => {
  if (getToken() && !isPublicAdminPath(to.path) && !isAdminIdleExpired()) {
    markAdminActive()
  }
})

let lastTouchSent = 0
function noteUserActivity() {
  const path = router.currentRoute && router.currentRoute.value ? router.currentRoute.value.path : ''
  if (!getToken() || isPublicAdminPath(path) || isAdminIdleExpired()) return
  markAdminActive()
  const now = Date.now()
  if (now - lastTouchSent < 60000) return
  lastTouchSent = now
  touchSession().catch(() => { lastTouchSent = 0 })
}

if (!window.__adminIdleWatch) {
  window.__adminIdleWatch = true
  ;['pointerdown', 'keydown', 'touchstart', 'wheel'].forEach((name) => {
    window.addEventListener(name, noteUserActivity, { passive: true })
  })
  setInterval(() => {
    const path = router.currentRoute && router.currentRoute.value ? router.currentRoute.value.path : ''
    if (!getToken() || isPublicAdminPath(path)) return
    if (isAdminIdleExpired()) expireAdminSession()
  }, 10000)
}

import axios from 'axios'
import { ElMessageBox as MessageBox, ElMessage as Message } from 'element-plus'
import store from '@/store'
import router from '@/router'
import { isAdminIdleExpired, isPublicAdminPath, isSessionFreeUrl, lastAdminActiveAt, TOUCH_WINDOW_MS } from '@/utils/adminSession'

/** 从接口响应或 axios 异常中解析可读错误信息（后端统一字段为 msg） */
export function getApiErrorMessage(error, fallback = '请求失败') {
  if (!error) return fallback
  if (error.message && error.message !== 'Network Error') {
    return error.message
  }
  const data = error.response && error.response.data
  if (data && (data.msg || data.message)) {
    return data.msg || data.message
  }
  if (error.message === 'Network Error') {
    return '无法连接服务器，请确认后端已启动'
  }
  return fallback
}

// create an axios instance
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API, // url = base url + request url
  timeout: 10000 // request timeout    //最大超时时间 10秒
})

let sessionExpireTask = null

/**
 * 清掉本地登录态。navigate 为 false 时只清状态，由路由守卫 next 去登录页。
 * 重复调用共用同一次清理，避免刷新和接口同时过期时弹两次。
 */
export function expireAdminSession(message, navigate = true) {
  if (sessionExpireTask) return sessionExpireTask
  const path = router.currentRoute && router.currentRoute.value ? router.currentRoute.value.path : ''
  if (isPublicAdminPath(path)) return Promise.resolve()
  const redirect = router.currentRoute && router.currentRoute.value ? router.currentRoute.value.fullPath : '/'
  sessionExpireTask = store.dispatch('user/resetToken').then(() => {
    Message.warning(message || '登录已过期，请重新登录')
    if (navigate) router.replace(`/login?redirect=${redirect}`)
  }).finally(() => {
    setTimeout(() => { sessionExpireTask = null }, 800)
  })
  return sessionExpireTask
}

// 请求拦截器
service.interceptors.request.use(
  config => {
    const url = config.url || ''
    const path = router.currentRoute && router.currentRoute.value ? router.currentRoute.value.path : ''
    if (isSessionFreeUrl(url) || isPublicAdminPath(path)) {
      return config
    }
    if (store.getters.token) {
      if (isAdminIdleExpired()) {
        expireAdminSession()
        return Promise.reject(new Error('登录已过期，请重新登录'))
      }
      config.headers['X-Token'] = store.getters.token
      const activeAt = lastAdminActiveAt()
      const recent = activeAt && (Date.now() - activeAt < TOUCH_WINDOW_MS)
      if (config.touchSession || recent) {
        config.headers['X-Touch'] = '1'
      }
    }
    return config
  },
  error => {
    // 处理请求错误
    console.log(error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  /**
   * 如果您想获取http信息，如标题或状态
   * 请返回 response => response
  */

  /**
    *通过自定义代码确定请求状态
    *这里只是一个例子
    *您还可以通过HTTP状态代码来判断状态
   */
  response => {
    const res = response.data

    // console.log("响应拦截器接收到的数据",response);
    // 如果返回的状态码不是200
    if (res.code !== 200 && res.code !== 400) {
      // if (condition) {

      // }

      // Message({
      //   message: res.msg || '服务器错误！',
      //   type: 'error',
      //   duration: 5 * 1000
      // })
      // 庐山市签到机端不需要
      // 50008: Illegal token; 50012: Other clients logged in; 50014: Token expired;
      if (res.code === 50014) {
        const url = (response.config && response.config.url) || ''
        expireAdminSession(res.msg, !/\/User\/GetInfo/.test(url))
        return Promise.reject(new Error(res.msg || '登录已过期，请重新登录'))
      }
      if (res.code === 50008 || res.code === 50012) {
        // to re-login
        MessageBox.confirm('You have been logged out, you can cancel to stay on this page, or log in again', 'Confirm logout', {
          confirmButtonText: 'Re-Login',
          cancelButtonText: 'Cancel',
          type: 'warning'
        }).then(() => {
          store.dispatch('user/resetToken').then(() => {
            location.reload()
          })
        })
      }
      return Promise.reject(new Error(res.msg || res.message || '请求失败'))
    } else {
      return res
    }
  },
  error => {
    console.log('响应拦截器发生错误', 'err' + error) // for debug
    return Promise.reject(new Error(getApiErrorMessage(error)))
  }
)

export default service

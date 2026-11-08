/**
 * 护士工作站 axios 封装
 * 消费者：api/auth.js、api/nurseDesk.js
 * 约定：业务 ResultData code===400 为软失败（返回 body、不 reject），由登录/Desk 等调用方自行提示
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useSessionStore } from '../stores/session'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

/** 业务 400：不弹全局错误、不 reject，交给调用方 */
function isSoftFail(res) {
  return res && res.success === false && res.code === 400
}

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.success === false && !isSoftFail(res)) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  (error) => {
    const res = error.response?.data
    // 兼容仍返回 HTTP 400 的业务体（如未重启的旧后端）
    if (isSoftFail(res)) {
      return res
    }
    const msg = res?.msg || res?.message || error.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  }
)

export function nursePayload(extra = {}) {
  const session = useSessionStore()
  const payload = {
    operatorUserNo: session.userNo,
    token: session.token
  }
  if (session.zoneId) payload.zoneId = session.zoneId
  return { ...payload, ...extra }
}

export default service

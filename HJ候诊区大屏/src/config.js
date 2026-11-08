/** 候诊区大屏 API / WebSocket 根路径（Vue3 + Vite）；请求路径保留 /api 前缀 */
export const baseURL = ''
export const signalrURL = import.meta.env.DEV
  ? 'http://127.0.0.1:8081/api/pdjh-ws'
  : `${window.location.origin}/api/pdjh-ws`

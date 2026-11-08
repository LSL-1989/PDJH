import axios from 'axios'
import { baseURL } from '@/config'
import { isScreenPreview } from '@/utils/screenPreview'

axios.defaults.baseURL = baseURL
axios.defaults.headers.post['Content-Type'] = 'application/json; charset=utf-8'

/** 预览窗禁止打到现场的写接口：叫号消费、设备登记/解析、屏幕增删改 */
const PREVIEW_WRITE_PATH = /\/(CallMsg\/EditCallMsg|CallMsg\/DocCallMsg|Terminal\/Register|Terminal\/Resolve|DoctorCall\/|Screens\/(UpdateScreen|AddScreen|DelScreen))/i

axios.interceptors.request.use((config) => {
  if (!isScreenPreview() || !PREVIEW_WRITE_PATH.test(String(config.url || ''))) {
    return config
  }
  config.adapter = () => Promise.resolve({
    data: { success: true, code: 200, data: null, msg: 'preview-readonly' },
    status: 200,
    statusText: 'OK',
    headers: {},
    config
  })
  return config
})

const http = {
  get(url, params) {
    return axios.get(url, params).then((res) => res)
  },
  post(url, params) {
    return axios.post(url, params).then((res) => res)
  }
}

export default http

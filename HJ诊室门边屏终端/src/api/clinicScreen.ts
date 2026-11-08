import axios from 'axios'
import type { ApiResult, ClinicDisplayData } from '../types/clinic'

import { getStoredDeviceCode, setStoredDeviceCode } from '../utils/terminalBinding'
import { isClinicPreview } from '../utils/clinicPreview'

export function resolveDeviceCodeFromRoute(
  queryCode?: string | string[] | null | (string | null)[]
): string {
  const raw = Array.isArray(queryCode) ? queryCode[0] : queryCode
  const fromQuery = typeof raw === 'string' ? raw.trim() : ''
  if (fromQuery) {
    if (!isClinicPreview()) setStoredDeviceCode(fromQuery)
    return fromQuery
  }
  if (isClinicPreview()) return ''
  const fromStorage = getStoredDeviceCode()
  if (fromStorage) return fromStorage
  const legacy = localStorage.getItem('pdjh_clinic_device_code')?.trim()
  if (legacy) {
    setStoredDeviceCode(legacy)
    return legacy
  }
  const fromEnv = (import.meta.env.VITE_DEVICE_ID as string | undefined)?.trim()
  return fromEnv || ''
}

export function mapApiError(code: number | undefined, msg: string): import('../types/clinic').StandbyReason {
  if (code === 404) return 'not_registered'
  if (code === 428) return 'pending'
  if (code === 403) {
    if (msg.includes('冻结') || msg.includes('停用')) return 'frozen'
    if (msg.includes('绑定') || msg.includes('诊室')) return 'not_bound'
    return 'frozen'
  }
  return 'unknown'
}

export async function fetchClinicDisplay(deviceCode: string): Promise<{
  ok: true
  data: ClinicDisplayData
} | {
  ok: false
  reason: import('../types/clinic').StandbyReason
  message: string
}> {
  try {
    const preview = isClinicPreview()
    const res = await axios.get<ApiResult<ClinicDisplayData>>('/api/ClinicScreen/GetDisplay', {
      params: preview ? { deviceCode, preview: true } : { deviceCode },
      timeout: 10000
    })
    const body = res.data
    if (body.success && body.data) {
      return { ok: true, data: body.data }
    }
    const msg = body.msg || body.message || '加载失败'
    return { ok: false, reason: mapApiError(body.code, msg), message: msg }
  } catch (err) {
    if (axios.isAxiosError(err) && err.response?.data) {
      const body = err.response.data as ApiResult<unknown>
      const msg = body.msg || body.message || '请求失败'
      return { ok: false, reason: mapApiError(body.code ?? err.response.status, msg), message: msg }
    }
    return { ok: false, reason: 'network_error', message: '网络连接失败，请检查网络' }
  }
}

/**
 * 签到终端状态展示（sign_device）
 * - 配置状态 status：0 停用 / 1 启用 / 2 待配置（由后台按绑定自动推算，仅停用可人工操作）
 * - 连接状态 online：终端 5 分钟内 Register/Resolve/GetDeviceConfig 视为已连接
 * 消费者：SignDeviceManages
 */
import { getOnlineStatusLabel, getOnlineTagType } from '@/utils/screenState'

export { getOnlineStatusLabel, getOnlineTagType }

/** 归一化为 0 | 1 | 2 */
export function normalizeSignStatus(status) {
  const n = Number(status)
  if (n === 1) return 1
  if (n === 2) return 2
  return 0
}

/** 配置状态文案；优先使用后端 statusModel */
export function getSignStatusLabel(status, statusModel) {
  if (statusModel) return statusModel
  const s = normalizeSignStatus(status)
  if (s === 1) return '启用'
  if (s === 2) return '待配置'
  return '停用'
}

/** 配置状态 Tag 颜色 */
export function getSignStatusTagType(status) {
  const s = normalizeSignStatus(status)
  if (s === 1) return 'success'
  if (s === 2) return 'warning'
  return 'info'
}

/** 是否处于停用 */
export function isSignDisabled(status) {
  return normalizeSignStatus(status) === 0
}

/** 设备编号输入：仅保留数字、大写字母与连字符，字母转大写 */
export function sanitizeDeviceCodeInput(value) {
  return String(value || '')
    .toUpperCase()
    .replace(/[^A-Z0-9-]/g, '')
}

/** 归一化 API 返回的签到终端行 */
export function normalizeSignDeviceRow(row) {
  if (!row) return row
  const status = normalizeSignStatus(row.status)
  return {
    ...row,
    status,
    statusModel: row.statusModel || getSignStatusLabel(status),
    configComplete: row.configComplete != null ? row.configComplete : row.ConfigComplete,
    online: row.online != null ? row.online : row.Online,
    deviceCode: row.deviceCode || row.DeviceCode || ''
  }
}

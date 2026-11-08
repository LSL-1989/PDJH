/**
 * 屏幕设备状态展示（doc_screen）
 * - 配置状态 state：0 冻结 / 1 已启用 / 2 待配置（由后台按绑定与字段完整性自动推算，仅冻结可人工操作）
 * - 连接状态 online：终端 5 分钟内 Register/Resolve/GetDisplay 视为已连接，与配置是否完整无关
 * 消费者：ScreenManages、screenConfig、clinicConfig
 */

/** 归一化为 0 | 1 | 2 */
export function normalizeScreenState(state) {
  const n = Number(state)
  if (n === 1) return 1
  if (n === 2) return 2
  return 0
}

/** 配置状态文案；优先使用后端 stateModel */
export function getScreenStateLabel(state, stateModel) {
  if (stateModel) return stateModel
  const s = normalizeScreenState(state)
  if (s === 1) return '已启用'
  if (s === 2) return '待配置'
  return '冻结'
}

/** 配置状态 Tag 颜色 */
export function getScreenStateTagType(state) {
  const s = normalizeScreenState(state)
  if (s === 1) return 'success'
  if (s === 2) return 'warning'
  return 'info'
}

/** 连接状态文案 */
export function getOnlineStatusLabel(online, deviceCode) {
  if (online === true) return '已连接'
  const code = (deviceCode || '').trim()
  if (code) return '未连接'
  return '未上线'
}

/** 连接状态 Tag 颜色 */
export function getOnlineTagType(online, deviceCode) {
  if (online === true) return 'success'
  const code = (deviceCode || '').trim()
  if (code) return 'warning'
  return 'info'
}

/** 卡片标题栏 class（ScreenManages，按配置状态） */
export function getScreenStateHeaderClass(state) {
  const s = normalizeScreenState(state)
  if (s === 1) return 'is-active'
  if (s === 2) return 'is-pending'
  return 'is-frozen'
}

/** 设备列表图标背景（按配置状态） */
export function getScreenStateGradient(state) {
  const s = normalizeScreenState(state)
  if (s === 1) return 'linear-gradient(135deg, #4285F4 0%, #34A853 100%)'
  if (s === 2) return 'linear-gradient(135deg, #FB8C00 0%, #F9A825 100%)'
  return 'linear-gradient(135deg, #78909C 0%, #455A64 100%)'
}

/** 是否处于冻结（配置状态） */
export function isScreenFrozen(state) {
  return normalizeScreenState(state) === 0
}

/** 归一化 API 返回的屏幕行，统一 state / configComplete / online */
export function normalizeScreenRow(row) {
  if (!row) return row
  const state = normalizeScreenState(row.state != null ? row.state : row.State)
  return {
    ...row,
    state,
    State: state,
    configComplete: row.configComplete != null ? row.configComplete : row.ConfigComplete,
    online: row.online != null ? row.online : row.Online,
    stateModel: row.stateModel || row.StateModel || getScreenStateLabel(state),
    deviceCode: row.deviceCode || row.DeviceCode || ''
  }
}

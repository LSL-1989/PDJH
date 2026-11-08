/**
 * 候诊区大屏绑定与 ScreensInput 字段兼容
 * 消费者：pdjhScreenMixin、各 pdjh 模板 Main/Header
 */

/** camelCase ↔ PascalCase 字段对（后端 Jackson camelCase，模板仍读 PascalCase） */
const SCREEN_FIELD_ALIASES = [
  ['id', 'ID'],
  ['name', 'Name'],
  ['state', 'State'],
  ['callCode', 'CallCode'],
  ['callNum', 'CallNum'],
  ['callContent', 'CallContent'],
  ['maxNum', 'MaxNum'],
  ['bulletin', 'Bulletin'],
  ['scrollTime', 'ScrollTime'],
  ['subtitle', 'Subtitle'],
  ['rate', 'Rate'],
  ['dataMode', 'DataMode'],
  ['isNum', 'IsNum'],
  ['isAudio', 'IsAudio'],
  ['deviceCode', 'DeviceCode'],
  ['deptInfo', 'DeptInfo'],
  ['roomInfo', 'RoomInfo'],
  ['stateModel', 'StateModel'],
  ['contentConfig', 'ContentConfig'],
  ['deptType', 'DeptType'],
  ['terminalType', 'TerminalType'],
  ['templateRoute', 'TemplateRoute'],
  ['voiceChannel', 'VoiceChannel']
]

/**
 * 将 GetScreensInput 结果补齐 PascalCase / camelCase 双键，供遗留模板使用。
 * @param {object|null|undefined} raw
 * @returns {object}
 */
export function normalizeScreensInput(raw) {
  if (!raw || typeof raw !== 'object') return raw || {}
  const out = { ...raw }
  for (const [camel, pascal] of SCREEN_FIELD_ALIASES) {
    const val = out[pascal] != null ? out[pascal] : out[camel]
    if (val !== undefined) {
      out[camel] = val
      out[pascal] = val
    }
  }
  return out
}

/** 解析大屏绑定的轮播项（优先物理诊室 roomInfo，兼容旧 deptInfo） */
export function parseScreenBindingList(screensInput) {
  if (!screensInput) return []
  const bindingStr =
    screensInput.roomInfo ||
    screensInput.RoomInfo ||
    screensInput.deptInfo ||
    screensInput.DeptInfo ||
    ''
  if (!bindingStr) return []
  if (!screensInput.roomInfo && screensInput.deptInfo) {
    console.warn('[Screen] 未配置 roomInfo，回退使用 deptInfo')
  }
  return bindingStr.split(';').reduce((acc, part) => {
    if (!part) return acc
    const pieces = part.split('|')
    if (pieces.length >= 2) {
      acc.push({ Code: pieces[0], DeptName: pieces[1] })
    }
    return acc
  }, [])
}

export function resolveScreenIdFromRoute(route) {
  const q = route && route.query ? route.query : {}
  return q.screenId || q.ScreensID || q.screensID || '1EECAEBE-B6C3-454E-800E-A5A66DD045BD'
}

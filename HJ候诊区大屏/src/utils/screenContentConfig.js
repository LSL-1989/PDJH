/**
 * 候诊区大屏显示内容配置（与后台 screenConfig/index.vue DEFAULT_CFG 保持一致）
 * 数据来源：GetScreensInput.contentConfig ← sys_config.SCREEN_CFG_{屏幕ID}
 */

import { desensitizeName } from '@pdjh/common/desensitize'

export const DEFAULT_SCREEN_CFG = {
  layout: '2COL',
  displayFields: ['QUEUE_NO', 'PATIENT_NAME', 'DOCTOR_NAME', 'OFFICE_NAME'],
  nameDesensitize: true,
  fontSize: 32,
  themeColor: '#1A73E8',
  darkMode: false,
  marqueeEnabled: true,
  marqueeText: '如有特殊情况，请告知导诊护士。就诊时请携带就诊卡及相关资料。',
  marqueeSpeed: 'normal',
  welcomeTitle: '欢迎来到本医院，祝您早日康复',
  mediaEnabled: false,
  mediaType: 'IMAGE',
  mediaIntervalSec: 8,
  mediaUrls: [],
  voiceEnabled: true,
  voiceTemplate: '{号码}号，{姓名}，请至{诊室}就诊',
  voiceVolume: 80,
  voiceLang: 'zh-CN-female'
}

const MARQUEE_SPEED_SEC = { slow: 25, normal: 15, fast: 8 }

const LAYOUT_PAGE = {
  '1COL': { waiting: 6, past: 4 },
  '2COL': { waiting: 7, past: 6 },
  '3COL': { waiting: 12, past: 6 },
  FULL: { waiting: 4, past: 0 }
}

/** 解析 contentConfig（对象或 JSON 字符串），缺省项补 DEFAULT_SCREEN_CFG */
export function parseContentConfig(raw) {
  if (!raw) {
    return { ...DEFAULT_SCREEN_CFG, displayFields: [...DEFAULT_SCREEN_CFG.displayFields], mediaUrls: [] }
  }
  let obj = raw
  if (typeof raw === 'string') {
    try {
      obj = JSON.parse(raw)
    } catch (e) {
      return { ...DEFAULT_SCREEN_CFG, displayFields: [...DEFAULT_SCREEN_CFG.displayFields], mediaUrls: [] }
    }
  }
  return {
    ...DEFAULT_SCREEN_CFG,
    ...obj,
    displayFields: Array.isArray(obj.displayFields) ? obj.displayFields : [...DEFAULT_SCREEN_CFG.displayFields],
    mediaUrls: Array.isArray(obj.mediaUrls) ? obj.mediaUrls : []
  }
}

/** 根节点主题 class（layout / 深浅色） */
export function buildScreenThemeClasses(cfg) {
  const c = parseContentConfig(cfg)
  const layout = (c.layout || '2COL').toLowerCase()
  return [
    'screen-content-root',
    `screen-layout-${layout}`,
    c.darkMode ? 'screen-theme-dark' : 'screen-theme-light'
  ]
}

/**
 * 写入字号、主题色、字幕速度。
 * 必须写在带 screen-content-root 的模板根上：index.html 外层 #app 上的变量
 * 会被该根节点样式里的同名变量盖住，主题色就会一直停在 #1A73E8。
 */
export function applyScreenThemeVars(cfg, rootEl) {
  const c = parseContentConfig(cfg)
  let root = null
  if (rootEl && rootEl.classList && rootEl.classList.contains('screen-content-root')) {
    root = rootEl
  } else if (typeof document !== 'undefined') {
    root = document.querySelector('.screen-content-root')
  }
  if (!root) root = rootEl || (typeof document !== 'undefined' ? document.documentElement : null)
  if (!root || !root.style) return
  root.style.setProperty('--screen-font-size', `${c.fontSize || 32}px`)
  root.style.setProperty('--screen-theme-color', c.themeColor || '#1A73E8')
  const duration = MARQUEE_SPEED_SEC[c.marqueeSpeed] || MARQUEE_SPEED_SEC.normal
  root.style.setProperty('--screen-marquee-duration', `${duration}s`)
}

export function showDisplayField(field, cfg) {
  const c = parseContentConfig(cfg)
  return c.displayFields.includes(field)
}

export function formatPatientName(name, cfg) {
  if (!name) return ''
  const c = parseContentConfig(cfg)
  if (!c.nameDesensitize) return name
  return desensitizeName(name)
}

/**
 * 底部滚动字幕（公告）。
 * 优先屏幕管理「公告内容」bulletin；无公告时再用显示配置 marqueeText / 默认文案。
 * 避免 contentConfig 缺省合并 DEFAULT.marqueeText 后盖住已配置的公告。
 */
export function resolveMarqueeText(cfg, screensInput) {
  const c = parseContentConfig(cfg)
  if (!c.marqueeEnabled) return ''
  const si = screensInput || {}
  const bulletin = String(si.Bulletin || si.bulletin || '').trim()
  if (bulletin) return bulletin
  const marquee = String(c.marqueeText || '').trim()
  if (marquee) return marquee
  return '欢迎来到系统，请遵守次序，一医一患，文明就诊。'
}

/**
 * 顶栏欢迎标题。
 * 将「本医院」替换为屏幕/诊区名称（ScreensInput.Name）；无自定义文案时生成带诊区名的默认欢迎语。
 */
export function resolveWelcomeTitle(cfg, screensInput) {
  const c = parseContentConfig(cfg)
  const si = screensInput || {}
  const areaName = String(si.Name || si.name || '').trim()
  const raw = String(
    c.welcomeTitle || si.Subtitle || si.subtitle || ''
  ).trim()

  if (areaName) {
    if (raw.includes('本医院')) {
      return raw.replace(/本医院/g, areaName)
    }
    if (!raw || raw === DEFAULT_SCREEN_CFG.welcomeTitle) {
      return `欢迎来到${areaName}，祝您早日康复`
    }
  }
  if (raw) return raw
  return areaName
}

export function isMarqueeVisible(cfg) {
  return parseContentConfig(cfg).marqueeEnabled !== false
}

export function isVoiceEnabled(cfg, screensInput) {
  const c = parseContentConfig(cfg)
  if (c.voiceEnabled === false) return false
  const si = screensInput || {}
  const legacy = si.IsAudio != null ? si.IsAudio : si.isAudio
  if (legacy === '0' || legacy === 0) return false
  return true
}

export function getLayoutPageSize(cfg, section) {
  const layout = parseContentConfig(cfg).layout || '2COL'
  const sizes = LAYOUT_PAGE[layout] || LAYOUT_PAGE['2COL']
  return sizes[section] != null ? sizes[section] : LAYOUT_PAGE['2COL'][section]
}

export function applyLayoutToStaff(cfg, vm) {
  if (!vm) return
  const waiting = getLayoutPageSize(cfg, 'waiting')
  const past = getLayoutPageSize(cfg, 'past')
  if (vm.WaitingStaff) vm.WaitingStaff.Page = waiting
  if (vm.PastStaff) vm.PastStaff.Page = past
}

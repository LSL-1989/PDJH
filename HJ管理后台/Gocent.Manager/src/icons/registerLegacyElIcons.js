import { h } from 'vue'

/**
 * 把历史 `el-icon-*` 名称注册为组件，供 Element Plus 的 button/input `icon` 属性解析。
 * 实际字形仍走 element-icons 字体（styles/element-icons.scss）。
 * names 由迁移脚本根据源码扫描写入。
 */
const ICON_NAMES = [
  'el-icon-search', 'el-icon-plus', 'el-icon-edit', 'el-icon-delete', 'el-icon-refresh',
  'el-icon-connection', 'el-icon-link', 'el-icon-key', 'el-icon-document-copy', 'el-icon-s-operation',
  'el-icon-monitor', 'el-icon-arrow-left', 'el-icon-arrow-right', 'el-icon-check', 'el-icon-close',
  'el-icon-user', 'el-icon-date', 'el-icon-time', 'el-icon-warning', 'el-icon-warning-outline',
  'el-icon-bell', 'el-icon-tickets', 'el-icon-sort', 'el-icon-edit-outline', 'el-icon-location-information',
  'el-icon-chat-dot-square', 'el-icon-microphone', 'el-icon-mobile-phone', 'el-icon-guide',
  'el-icon-document', 'el-icon-refresh-left', 'el-icon-s-custom', 'el-icon-s-data', 'el-icon-s-tools',
  'el-icon-s-shop', 'el-icon-s-cooperation', 'el-icon-video-camera', 'el-icon-office-building',
  'el-icon-odometer', 'el-icon-full-screen', 'el-icon-first-aid-kit', 'el-icon-cpu', 'el-icon-set-up',
  'el-icon-box', 'el-icon-data-analysis', 'el-icon-c-scale-to-original', 'el-icon-circle-check',
  'el-icon-top', 'el-icon-bottom', 'el-icon-lock', 'el-icon-caret-bottom', 'el-icon-loading'
]

export function registerLegacyElIcons(app) {
  ICON_NAMES.forEach(name => {
    app.component(name, {
      name,
      setup() {
        return () => h('i', { class: name })
      }
    })
  })
}

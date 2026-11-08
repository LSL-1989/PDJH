/**
 * 后台菜单图标目录：本地 SVG 雪碧图在前，Element 字体图标在后。
 * 消费者：SvgSelect、IconPickerInput、侧栏 MenuItem、菜单管理列表。
 * 依赖：icons/svg、icons/elementIconNames.js（对应 element-icons 字体）。
 */
import fontIcons from './elementIconNames'

const req = require.context('./svg', false, /\.svg$/)

const svgIcons = req.keys()
  .map(key => {
    const matched = key.match(/\.\/(.*)\.svg/)
    return matched ? matched[1] : ''
  })
  .filter(Boolean)
  .sort((a, b) => a.localeCompare(b))

/** 是否为 Element 字体图标（侧栏用 i.el-icon-* 渲染，不是 svg 雪碧图） */
export function isFontIcon(name) {
  return typeof name === 'string' && name.startsWith('el-icon-')
}

/** 选择器里展示的短名；字体图标去掉 el-icon- 前缀 */
export function iconLabel(name) {
  return isFontIcon(name) ? name.slice('el-icon-'.length) : name
}

export const menuIcons = [...svgIcons, ...fontIcons]

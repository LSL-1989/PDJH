import SvgIcon from '@/components/SvgIcon'
import IconPickerInput from '@/components/IconPickerInput'
import TableActionColumn from '@/components/TableActionColumn'
import TableActionIcon from '@/components/TableActionIcon'
import FormDrawer from '@/components/FormDrawer'

/**
 * 注册全局组件与 SVG 雪碧图。
 * 消费者：main.js
 */
export function registerIcons(app) {
  app.component('svg-icon', SvgIcon)
  app.component('icon-picker-input', IconPickerInput)
  app.component('table-action-column', TableActionColumn)
  app.component('table-action-icon', TableActionIcon)
  app.component('form-drawer', FormDrawer)

  const req = require.context('./svg', false, /\.svg$/)
  const requireAll = requireContext => requireContext.keys().map(requireContext)
  requireAll(req)
}

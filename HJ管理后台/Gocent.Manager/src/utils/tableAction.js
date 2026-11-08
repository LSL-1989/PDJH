/** 单个图标按钮占位（含 tooltip 包裹与点击热区） */
const ACTION_BTN_WIDTH = 32
/** 按钮间距 */
const ACTION_BTN_GAP = 4
/** 单元格左右内边距合计 */
const ACTION_CELL_PADDING = 20

/**
 * 列表操作列宽度计算（图标按钮专用）
 * @param {number} buttonCount 操作按钮数量
 * @returns {number} 列宽（px）
 */
export function tableActionColWidth(buttonCount) {
  const count = Math.max(1, Number(buttonCount) || 1)
  return ACTION_CELL_PADDING + count * ACTION_BTN_WIDTH + (count - 1) * ACTION_BTN_GAP
}

/**
 * 科室 API 字段归一化（DepartmentOutput 为 PascalCase：ID/Name，与 doctor 等 camelCase 不一致）
 * 消费者：DoctorProfile、doctorSchedule 等科室下拉
 */

/** 单条科室转为 { id, name, ... } */
export function normalizeDepartmentRow(row) {
  if (!row) return null
  const id = row.id || row.ID
  if (!id) return null
  return {
    ...row,
    id,
    name: row.name || row.Name || row.deptName || ''
  }
}

/** 科室列表归一化，过滤无 id 项 */
export function normalizeDepartmentList(list) {
  if (!Array.isArray(list)) return []
  return list.map(normalizeDepartmentRow).filter(Boolean)
}

/** id -> 科室名称映射 */
export function buildDepartmentNameMap(list) {
  const map = {}
  normalizeDepartmentList(list).forEach(d => {
    map[d.id] = d.name || d.id
  })
  return map
}

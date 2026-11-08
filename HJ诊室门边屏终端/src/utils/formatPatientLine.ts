import type { ClinicPatientRow } from '../types/clinic'

/** 初复诊标识：REVISIT 为复诊，其余为初诊 */
export function visitTypeLabel(triageState?: string): string {
  return triageState === 'REVISIT' ? '复' : '初'
}

/** 展示用序号，优先 showNo 中的数字部分，否则用 xh 补零 */
export function queueDisplayNo(showNo: string | undefined, xh: number): string {
  const trimmed = (showNo || '').trim()
  if (trimmed) {
    const numMatch = trimmed.match(/\d+/)
    if (numMatch) return numMatch[0].padStart(2, '0')
    return trimmed
  }
  return String(xh || 0).padStart(2, '0')
}

/** 紧凑脱敏姓名：去掉空格，统一为「张*花」形态 */
export function compactPatientName(name: string | undefined): string {
  if (!name) return ''
  return name.replace(/\s+/g, '').replace(/\*+/g, '*')
}

/**
 * 患者单行展示：初 09 张*花（空格分隔）
 * 入参 patientName 已由后端按配置脱敏；空姓名时仅输出类型与序号
 */
export function formatPatientLine(patient: Pick<ClinicPatientRow, 'showNo' | 'xh' | 'patientName' | 'triageState'>): string {
  const type = visitTypeLabel(patient.triageState)
  const no = queueDisplayNo(patient.showNo, patient.xh)
  const name = compactPatientName(patient.patientName)
  if (!name) return `${type} ${no}`
  return `${type} ${no} ${name}`
}

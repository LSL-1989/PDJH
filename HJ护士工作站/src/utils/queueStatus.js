/**
 * 队列/诊室状态色与图标（色盲友好：颜色 + 图标双编码）
 * 消费者：RoomCardGrid、AlertInbox、ActionPanel
 */

export const STATUS = {
  normal: { color: '#43A047', bg: '#E8F5E9', label: '正常', icon: 'CircleCheck' },
  active: { color: '#1E88E5', bg: '#E3F2FD', label: '就诊中', icon: 'VideoPlay' },
  warning: { color: '#FB8C00', bg: '#FFF3E0', label: '警告', icon: 'Warning' },
  danger: { color: '#E53935', bg: '#FFEBEE', label: '异常', icon: 'CircleClose' },
  inactive: { color: '#9E9E9E', bg: '#F5F5F5', label: '未激活', icon: 'Remove' }
}

/** 诊室开放状态 */
export function roomStatusMeta(room, deptSuspended) {
  if (deptSuspended) return { ...STATUS.warning, label: '科室停诊' }
  const open = room.state === 1
  if (!open) return { ...STATUS.inactive, label: '关闭' }
  if (room.congested) return { ...STATUS.danger, label: '拥堵' }
  return { ...STATUS.normal, label: '开放' }
}

/** 告警严重度 */
export function alertSeverityMeta(severity) {
  if (severity >= 3) return STATUS.danger
  if (severity >= 2) return STATUS.warning
  return STATUS.active
}

/** 告警类型 */
export function alertTypeMeta(alertType) {
  const map = {
    OVERTIME: STATUS.danger,
    PASSED: STATUS.warning,
    UNARRIVED: STATUS.warning,
    MISSING_ROOM: STATUS.warning,
    DEPT_SUSPENDED: STATUS.danger
  }
  return map[alertType] || STATUS.active
}

export const ALERT_TYPE_LABELS = {
  OVERTIME: '超时等候',
  PASSED: '过号',
  UNARRIVED: '未签到',
  MISSING_ROOM: '缺诊室',
  DEPT_SUSPENDED: '科室停诊'
}

export const PATIENT_STATE_LABELS = {
  0: '等候',
  1: '呼叫中',
  2: '完成',
  3: '过号',
  9: '作废'
}

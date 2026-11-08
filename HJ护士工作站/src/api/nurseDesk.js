import request, { nursePayload } from '../utils/request'

function post(path, data = {}) {
  return request.post(`/NurseDesk/${path}`, nursePayload(data))
}

export const getSession = (zoneId) => post('GetSession', zoneId ? { zoneId } : {})
/** 登录页：当日排班 / 常驻 / 授权诊区；与 GetSession 分开以免未选定就拉诊室 */
export const listDutyZones = () => post('ListDutyZones')
/** 当班中途切换诊区；入参 zoneId，写 SWITCH_ZONE 审计 */
export const switchZone = (payload) => post('SwitchZone', payload)
export const getOverview = () => post('GetOverview')
export const getAlerts = () => post('GetAlerts')
export const searchPatient = (keyword) => post('SearchPatient', { keyword })
export const manualSignIn = (payload) => post('ManualSignIn', payload)
export const bindOfficeRoom = (payload) => post('BindOfficeRoom', payload)
export const revive = (payload) => post('Revive', payload)
export const manualCutIn = (payload) => post('ManualCutIn', payload)
export const forceSwap = (payload) => post('ForceSwap', payload)
export const setFrozen = (payload) => post('SetFrozen', payload)
export const markHisRefunded = (payload) => post('MarkHisRefunded', payload)
export const suspendDept = (payload) => post('SuspendDept', payload)
export const resumeDept = (payload) => post('ResumeDept', payload)
/** 诊室故障批量换房 */
export const migrateRoom = (payload) => post('MigrateRoom', payload)
export const getActionLogs = (page = 1, pageSize = 20) =>
  post('GetActionLogs', { page, pageSize })

/**
 * 诊室队列与代呼叫，直接复用叫号器的 DoctorCall，不另建护士专用接口。
 * state：0 等候，1 就诊中，4 完成，6 过号。
 */
export const getRoomQueue = (deptId, officeRoomId, state) =>
  request.get('/DoctorCall/GetDocCallList', {
    params: { deptid: deptId, state, officeRoomId }
  })

/** 改排队状态。calllistid / state 与叫号器 EditCallMsgState 一致 */
export const editCallState = (calllistid, state, officeRoomId) =>
  request.post('/DoctorCall/EditCallMsgState', { calllistid, state, officeRoomId })

/** 触发大屏语音。msg 形如「排队单ID|诊室名」 */
export const docCallMsg = (msg, code) =>
  request.post('/DoctorCall/DocCallMsg', { msg, code: code || '' })

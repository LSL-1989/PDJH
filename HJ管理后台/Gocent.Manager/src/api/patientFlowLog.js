/**
 * 患者全流程操作日志 API
 * 消费者：patientFlowLog 列表页与时间轴抽屉
 */
import request from '@/utils/request'

/** 分页查询；keyword / eventType / patientCode / callListId / startTime / endTime */
export function queryPatientFlowLog(query) {
  return request({
    url: '/api/PatientFlowLog/Query',
    method: 'post',
    data: query
  })
}

/** 按排队单 ID 拉取时间轴节点 */
export function timelineByCallList(callListId) {
  return request({
    url: '/api/PatientFlowLog/TimelineByCallList',
    method: 'post',
    data: { callListId }
  })
}

/** 按患者编号拉取时间轴 */
export function timelineByPatient(patientCode, startTime, endTime) {
  const data = { patientCode }
  if (startTime) data.startTime = startTime
  if (endTime) data.endTime = endTime
  return request({
    url: '/api/PatientFlowLog/TimelineByPatient',
    method: 'post',
    data
  })
}

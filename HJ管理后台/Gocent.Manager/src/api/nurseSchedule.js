/**
 * 护士当班排班 API（nurse_schedule 表）
 * 消费者：nurseSchedule 排班页；与 DoctorSchedule 分开（对象是 users 而非 doctor）
 */
import request from '@/utils/request'

/** 列出排班。date 查单日；startDate 与 endDate 同时传入查一周（闭区间） */
export function listNurseSchedules(data) {
  return request({
    url: '/api/NurseSchedule/List',
    method: 'post',
    data
  })
}

/**
 * 保存当班。单条 userNo + scheduleDate；批量 userNos + scheduleDates。
 * 同一护士同一天只保留本次班次。
 */
export function saveNurseSchedule(data) {
  return request({
    url: '/api/NurseSchedule/Save',
    method: 'post',
    data
  })
}

/** 按 id 删除一条排班 */
export function deleteNurseSchedule(id) {
  return request({
    url: '/api/NurseSchedule/Delete',
    method: 'post',
    data: { id }
  })
}

/**
 * 医生出诊排班 API（doctor_schedule）
 * 消费者：出诊排班甘特页、医生管理查看排班抽屉
 * 班次编码来自排班类型字典，不在这里写死班次名称
 */
import request from '@/utils/request'

/**
 * 查询排班。
 * 周视图传 weekStart（周一 yyyy-MM-dd）；月历传 startDate + endDate。可选 deptId。
 * 查看某医生全部排班只传 doctorId，与周/月明细范围不同，仍走本接口。
 */
export function listDoctorSchedules(params) {
  return request({
    url: '/api/DoctorSchedule/list',
    method: 'get',
    params
  })
}

/** 曾经排过班的医生 id，供列表过滤和下拉；不带周明细，因此不并进 list */
export function listScheduledDoctorIds() {
  return request({
    url: '/api/DoctorSchedule/scheduledDoctorIds',
    method: 'get'
  })
}

/**
 * 保存排班。
 * shifts 覆盖所选日期的全部班次。stopFlag=1 表示该午别停诊，不写诊室和号源。
 * 只传 shiftType 且为停诊类编码（如 OFF）时，仍会清掉当天其他班次。
 * 日期用 scheduleDate，或多天 scheduleDates。
 */
export function saveDoctorSchedule(data) {
  return request({
    url: '/api/DoctorSchedule/save',
    method: 'post',
    data
  })
}

/** 删除排班。shiftType 有值时只删该班次，否则删当天全部 */
export function deleteDoctorScheduleDay(data) {
  return request({
    url: '/api/DoctorSchedule/delete',
    method: 'post',
    data
  })
}

/**
 * 按范围清除某医生排班，返回清除条数。
 * scope：TODAY 当日，FROM_TODAY 当日及以后，ALL 全部。
 */
export function clearDoctorSchedules(doctorId, scope) {
  return request({
    url: '/api/DoctorSchedule/deleteByDoctor',
    method: 'post',
    data: { doctorId, scope: scope || 'ALL' }
  })
}

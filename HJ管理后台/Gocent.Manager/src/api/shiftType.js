/**
 * 医生出诊班次字典 API（dict_shift_type）
 * 消费者：系统管理排班类型页、出诊排班班次下拉、医生管理查看排班
 * 与 DoctorSchedule/save 不同：这里只维护可选班次，不写排班记录
 */
import request from '@/utils/request'

/** 全部班次，含停用；按 sortOrder 排序 */
export function getShiftTypes() {
  return request({
    url: '/api/ShiftType/GetAll',
    method: 'post'
  })
}

/** 新增或修改。data: { id?, code, name, startTime, endTime, tagStyle, sortOrder, enabled, exclusiveFlag } */
export function saveShiftType(data) {
  return request({
    url: '/api/ShiftType/Save',
    method: 'post',
    data
  })
}

/** 按 id 删除；仍被排班使用时 success=false */
export function deleteShiftType(id) {
  return request({
    url: '/api/ShiftType/Delete',
    method: 'post',
    data: { id }
  })
}

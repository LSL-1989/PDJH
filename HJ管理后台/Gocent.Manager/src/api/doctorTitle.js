/**
 * 医生职称字典 API（dict_doctor_title）
 * 消费者：DoctorTitle 维护页、DoctorProfile 职称下拉
 * 与 Doctor/Save 不同：这里只维护可选名称，不写医生档案
 */
import request from '@/utils/request'

/** 全部职称，含停用；按 sortOrder 排序 */
export function getDoctorTitles() {
  return request({
    url: '/api/DoctorTitle/GetAll',
    method: 'post'
  })
}

/** 新增或修改。data: { id?, name, sortOrder, enabled } */
export function saveDoctorTitle(data) {
  return request({
    url: '/api/DoctorTitle/Save',
    method: 'post',
    data
  })
}

/** 按 id 删除；仍被医生使用时 success=false */
export function deleteDoctorTitle(id) {
  return request({
    url: '/api/DoctorTitle/Delete',
    method: 'post',
    data: { id }
  })
}

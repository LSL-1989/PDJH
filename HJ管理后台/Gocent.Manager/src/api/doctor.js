/**
 * 医生主数据 API（doctor 表，含展示档案字段）
 * 消费者：DoctorProfile、doctorSchedule 下拉、HIS 同步
 */
import request from '@/utils/request'

/** 分页查询；query: { Index, Page, workNo, name } */
export function getDoctorPageList(query) {
  return request({
    url: '/api/Doctor/GetPageList',
    method: 'post',
    data: query
  })
}

/** 获取全部医生（排班页等）。deptIds 为 doctor_dept 所属科室，供排班按科室筛诊室 */
export function getAllDoctors() {
  return request({
    url: '/api/Doctor/GetAll',
    method: 'post'
  })
}

/** 按工号查询；404 表示工号可用（新增校验） */
export function getDoctorByWorkNo(workNo) {
  return request({
    url: '/api/Doctor/GetByWorkNo',
    method: 'post',
    data: { workNo }
  })
}

/** 新增/更新医生 */
export function saveDoctor(data) {
  return request({
    url: '/api/Doctor/Save',
    method: 'post',
    data
  })
}

/** 删除医生 */
export function deleteDoctor(id) {
  return request({
    url: '/api/Doctor/Delete',
    method: 'post',
    data: { id }
  })
}

/**
 * 管理员重置叫号器密码为默认值（123456）并强制下次登录改密
 * 与 User/ResetPassWord（后台用户改自己密码）不同
 */
export function adminResetDoctorPassword(id) {
  return request({
    url: '/api/Doctor/AdminResetPassword',
    method: 'post',
    data: { id }
  })
}

/** 上传头像，返回相对路径 */
export function uploadDoctorAvatar(formData) {
  return request({
    url: '/api/Doctor/UploadAvatar',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data: formData
  })
}

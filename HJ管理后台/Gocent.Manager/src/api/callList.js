import request from '@/utils/request'

// export function getDoctorCall(){
//    return request({
//         url:'/api/DoctorCall/GetDoctorCall',
//         method:'post'
//    })
// }

// 条件查询获取队列信息
export function getQueryCallList(QueryInput) {
  return request({
    url: '/api/DoctorCall/GetCallList',
    method: 'post',
    data: QueryInput
  })
}
// 查询某个科室的数据，可以使用父科室ID
export function GetDeptCallList(data) {
  return request({
    url: '/api/DoctorCall/GetDeptCallList',
    method: 'post',
    data
  })
}

// 根据诊室id拿到下面所有的医生
export function getDoctorByDeptID(DeptID) {
  return request({
    url: '/api/DoctorCall/GetDoctorByDeptID',
    method: 'post',
    params: { DeptID }
  })
}
// 登记队列
export function Register(Call) {
  return request({
    url: '/api/DoctorCall/Register',
    method: 'post',
    data: Call
  })
}

// 删除队列信息
export function removeCall(id) {
  return request({
    url: '/api/DoctorCall/RemoveCall',
    method: 'post',
    params: { id }
  })
}

// 修改队列信息
export function editorCall(EditorCall) {
  return request({
    url: '/api/DoctorCall/EditorCall',
    method: 'post',
    data: EditorCall
  })
}

// 批量删除
export function batchRemove(input) {
  return request({
    url: '/api/DoctorCall/BatchRemove',
    method: 'post',
    data: input
  })
}
// 一键清除
export function oneclickClear() {
  return request({
    url: '/api/DoctorCall/OneclickClear',
    method: 'post'
  })
}

// 调整队列排序
export function sortCall(input) {
  return request({
    url: '/api/DoctorCall/SortCall',
    method: 'post',
    data: input
  })
}


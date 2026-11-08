import request from '@/utils/request'

/** 编号规则（call_auto_num）API；消费者：AutoNumManages */

// 获取
export function getAutoNum() {
  return request({
    url: '/api/CallAutoNum/GetAutoNum',
    method: 'post'
  })
}

// 新增
export function createAutoNum(input) {
  return request({
    url: '/api/CallAutoNum/CreateAutoNum',
    method: 'post',
    data: input
  })
}

// 修改
export function editorAutoNum(input) {
  return request({
    url: '/api/CallAutoNum/EditorAutoNum',
    method: 'post',
    data: input
  })
}

// 删除
export function removeAutoNum(GID) {
  return request({
    url: '/api/CallAutoNum/RemoveAutoNum',
    method: 'post',
    params: { GID }
  })
}

/** 按当前角色主任工作台权限，从 call_list 同步当日发号序号；token 必填，autoDate 可选 yyyy-MM-dd */
export function syncAutoNum(data) {
  return request({
    url: '/api/CallAutoNum/SyncAutoNum',
    method: 'post',
    data
  })
}


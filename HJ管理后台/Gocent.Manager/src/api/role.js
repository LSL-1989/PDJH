import request from '@/utils/request'

// 获取所有角色
export function getRole() {
  return request({
    url: '/api/Role/GetRole',
    method: 'post'
  })
}

// 根据角色id 获取权限
export function getPermission(RoleId) {
  return request({
    url: '/api/Role/GetPermission',
    method: 'post',
    params: { RoleId }
  })
}

// 修改角色
export function editorRole(input) {
  return request({
    url: '/api/Role/EditorRole',
    method: 'post',
    data: input
  })
}

// 保存权限
export function commitPermission(input) {
  return request({
    url: '/api/Role/CommitPermission',
    method: 'post',
    // params:{Pages,RoleId}
    data: input

  })
}

// 新增角色
export function createRole(input) {
  return request({
    url: '/api/Role/CreateRole',
    method: 'post',
    data: input

  })
}

// 删除角色
export function removeRole(RoleId) {
  return request({
    url: '/api/Role/RemoveRole',
    method: 'post',
    params: { RoleId }
  })
}


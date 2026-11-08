import request from '@/utils/request'

export function getAllOfficeRooms() {
  return request({
    url: '/api/officeRoom/list',
    method: 'post'
  })
}

export function saveOfficeRoom(data) {
  return request({
    url: '/api/officeRoom/save',
    method: 'post',
    data
  })
}

export function removeOfficeRoom(id) {
  return request({
    url: '/api/officeRoom/remove',
    method: 'post',
    data: { id }
  })
}

import request from '@/utils/request'

export function listSignDevices(data) {
  return request({
    url: '/api/SignDevice/list',
    method: 'post',
    data: data || {}
  })
}

export function saveSignDevice(data) {
  return request({
    url: '/api/SignDevice/save',
    method: 'post',
    data
  })
}

export function removeSignDevice(id) {
  return request({
    url: '/api/SignDevice/remove',
    method: 'post',
    data: { id }
  })
}

export function relevancySignDeviceAndRooms(data) {
  return request({
    url: '/api/SignDevice/RelevancySignDeviceAndRooms',
    method: 'post',
    data
  })
}

export function getSignDeviceConfig(deviceCode) {
  return request({
    url: '/api/SignDevice/GetDeviceConfig',
    method: 'get',
    params: { deviceCode }
  })
}

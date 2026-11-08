import request from '@/utils/request'

export function listVoiceDevices() {
  return request({
    url: '/api/VoiceDevice/list',
    method: 'post',
    data: {}
  })
}

export function saveVoiceDevice(data) {
  return request({
    url: '/api/VoiceDevice/save',
    method: 'post',
    data
  })
}

export function removeVoiceDevice(id) {
  return request({
    url: '/api/VoiceDevice/remove',
    method: 'post',
    data: { id }
  })
}

/** 绑定多个诊室 + 诊区 */
export function relevancyVoiceScopes(data) {
  return request({
    url: '/api/VoiceDevice/RelevancyScopes',
    method: 'post',
    data
  })
}

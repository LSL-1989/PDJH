import request from '@/utils/request'

export function getAllTasks() {
  return request({
    url: '/api/implement/getAllTasks',
    method: 'get'
  })
}

export function startTask(data) {
  return request({
    url: '/api/implement/startTask',
    method: 'post',
    data
  })
}

export function finishTask(data) {
  return request({
    url: '/api/implement/finishTask',
    method: 'post',
    data
  })
}

export function revertTask(data) {
  return request({
    url: '/api/implement/revertTask',
    method: 'post',
    data
  })
}

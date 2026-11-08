import request from '@/utils/request'

export function getDashboardStats() {
  return request({ url: '/api/integration/monitor/dashboard', method: 'get' })
}

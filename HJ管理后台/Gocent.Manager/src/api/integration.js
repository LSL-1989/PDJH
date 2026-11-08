/**
 * 集成网关配置 API（连接、端点、映射、试跑、模板）
 * 消费者：数据源接口配置页
 */
import request from '@/utils/request'
import store from '@/store'

function operatorHeaders() {
  const userNo = store.state.user && store.state.user.UserNo
  return userNo ? { 'X-User-No': userNo } : {}
}

export function getConnections() {
  return request({ url: '/api/integration/config/connections', method: 'get' })
}
export function saveConnection(data) {
  return request({ url: '/api/integration/config/connection', method: 'post', data, headers: operatorHeaders() })
}
export function deleteConnection(id) {
  return request({ url: `/api/integration/config/connection/${id}`, method: 'delete', headers: operatorHeaders() })
}
export function pingConnection(id) {
  return request({ url: `/api/integration/config/connection/${id}/ping`, method: 'get' })
}

export function getEndpoints(connectionId) {
  return request({ url: `/api/integration/config/endpoints/${connectionId}`, method: 'get' })
}
export function saveEndpoint(data) {
  return request({ url: '/api/integration/config/endpoint', method: 'post', data, headers: operatorHeaders() })
}
export function deleteEndpoint(id) {
  return request({ url: `/api/integration/config/endpoint/${id}`, method: 'delete', headers: operatorHeaders() })
}

export function getMappings(endpointId) {
  return request({ url: `/api/integration/config/mappings/${endpointId}`, method: 'get' })
}
export function saveMapping(data) {
  return request({ url: '/api/integration/config/mapping', method: 'post', data, headers: operatorHeaders() })
}
export function deleteMapping(id) {
  return request({ url: `/api/integration/config/mapping/${id}`, method: 'delete', headers: operatorHeaders() })
}

export function getEndpointHistory(endpointId) {
  return request({ url: `/api/integration/config/history/endpoint/${endpointId}`, method: 'get' })
}

export function rollbackEndpoint(historyId) {
  return request({ url: `/api/integration/config/history/rollback/${historyId}`, method: 'post', headers: operatorHeaders() })
}

/** 按端点已保存的映射整表试跑。mockData：HTTP/WEBHOOK 为源 JSON，JDBC 为列名对象或数组。 */
export function dryRunMapping(data) {
  return request({ url: '/api/integration/config/debug/dry-run', method: 'post', data })
}

export function getMarketplaceTemplates() {
  return request({ url: '/api/integration/marketplace/templates', method: 'get' })
}

export function importMarketplaceTemplate(templateId, connectionId) {
  return request({
    url: `/api/integration/marketplace/import/${templateId}?connectionId=${connectionId}`,
    method: 'post',
    headers: operatorHeaders()
  })
}

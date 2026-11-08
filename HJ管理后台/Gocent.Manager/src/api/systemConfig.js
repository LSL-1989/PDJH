import request from '@/utils/request'

export function getAllConfigs() {
  return request({
    url: '/api/SystemConfig/GetAllConfigs',
    method: 'get'
  })
}

export function getConfigByKey(key) {
  return request({
    url: '/api/SystemConfig/GetConfigByKey',
    method: 'get',
    params: { key }
  })
}

export function saveConfig(data) {
  return request({
    url: '/api/SystemConfig/SaveConfig',
    method: 'post',
    data
  })
}

export function deleteConfig(data) {
  return request({
    url: '/api/SystemConfig/DeleteConfig',
    method: 'post',
    data
  })
}

/**
 * 查询 sys_action_log。与「业务事件日志」页同一接口，不另造端点。
 * 消费者：规则中心变更历史（logType=RULE_CHANGE）
 */
export function queryActionLogs(data) {
  return request({
    url: '/api/SystemActionLog/Query',
    method: 'post',
    data
  })
}

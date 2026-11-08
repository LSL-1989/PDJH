/**
 * 终端 Resolve API（供实施调试）
 * 消费者：实施人员验证 deviceCode 绑定状态
 */
import request from '@/utils/request'

/** 解析终端配置状态；terminalType: SCREEN | CLINIC | CHECKIN */
export function resolveTerminal(deviceCode, terminalType) {
  return request({
    url: '/Terminal/Resolve',
    method: 'get',
    params: { deviceCode, terminalType }
  })
}

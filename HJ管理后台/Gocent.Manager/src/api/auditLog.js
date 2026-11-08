/**
 * 外部网关审计日志 API（gw_audit_log）
 * 消费者：auditLog 查询页
 * 与 patientFlowLog / SystemActionLog 不同：只查集成网关调用记录
 */
import request from '@/utils/request'
import store from '@/store'

function operatorHeaders() {
  const userNo = store.state.user && store.state.user.UserNo
  return userNo ? { 'X-User-No': userNo } : {}
}

/** 分页查询。返回 data.list、data.total，以及筛选命中的 data.stats（total/success/fail/avgDuration） */
export function queryAuditLog(data) {
  return request({
    url: '/api/AuditLog/Query',
    method: 'post',
    data
  })
}

/** 按同一筛选条件导出全部命中（上限 2000 条，含请求/响应）。data.csv 为带 BOM 的文本 */
export function exportAuditLog(data) {
  return request({
    url: '/api/AuditLog/Export',
    method: 'post',
    data,
    headers: operatorHeaders(),
    timeout: 60000
  })
}

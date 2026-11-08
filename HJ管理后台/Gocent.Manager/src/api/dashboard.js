import request from '@/utils/request'

// 获取当日核心 SLA 指标 (今日算法调度、平均耗时等)
export function getOverviewMetrics() {
  return request({
    url: '/api/dashboard/getOverviewMetrics',
    method: 'get'
  })
}

// 获取超时警告、拥挤分流患者 (过号拦截 / 拥堵分流数)
export function getOvertimeAlerts(thresholdMinutes = 45) {
  return request({
    url: `/api/dashboard/getOvertimeAlerts?thresholdMinutes=${thresholdMinutes}`,
    method: 'get'
  })
}

// 获取门诊峰值追踪 (入列量和处理量折线图使用)
export function getPeakAnalysis(date) {
  return request({
    url: '/api/dashboard/getPeakAnalysis',
    method: 'get',
    params: date ? { date } : undefined
  })
}

// 获取科室积压热力矩阵 (中间四大管线雷达图使用)
export function getHeatmapMatrix() {
  return request({
    url: '/api/dashboard/heatmap-matrix',
    method: 'get'
  })
}

/** 门诊主任台：医生出诊实时看板 */
export function getOutpatientDoctorDutyBoard() {
  return request({
    url: '/api/dashboard/outpatient/doctor-duty-board',
    method: 'get'
  })
}

/** 门诊主任台：科室过号率排行 */
export function getOutpatientSkipRateRanking() {
  return request({
    url: '/api/dashboard/outpatient/skip-rate-ranking',
    method: 'get'
  })
}

/** 门诊主任台：今日号源配额总览 */
export function getOutpatientQuotaOverview() {
  return request({
    url: '/api/dashboard/outpatient/quota-overview',
    method: 'get'
  })
}

/** 后台首页待处理消息 */
export function getPendingSummary() {
  return request({
    url: '/api/dashboard/pending-summary',
    method: 'get'
  })
}

export function resolvePending(id) {
  return request({
    url: '/api/dashboard/pending-resolve',
    method: 'post',
    data: { id }
  })
}

export function getDepartments() {
  return request({
    url: '/api/dashboard/legacy/departments',
    method: 'get'
  })
}

export function getDepartmentQueueTime(input) {
  return request({
    url: '/api/dashboard/legacy/department-queue-time',
    method: 'post',
    data: input
  })
}

export function getDoctorCallCount(input) {
  return request({
    url: '/api/dashboard/legacy/doctor-call-count',
    method: 'post',
    data: input
  })
}

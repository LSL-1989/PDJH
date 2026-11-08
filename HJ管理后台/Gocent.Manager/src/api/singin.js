import request from '@/utils/request'

// 获取时间
export function GetDataTime() {
  return request({
    url: '/api/System/GetDataTime',
    method: 'get'
  })
}
// 获取签到端信息
export function GetSignInfoByCode(code) {
  return request({
    url: '/api/SignInfo/GetSignInfoByCode',
    method: 'get',
    params: { code }
  })
}

// 获取刷卡数据
export function GetCreditCardData(data) {
  return request({
    url: '/api/SignInfo/GetCreditCardData',
    method: 'post',
    data
  })
}

// 判断是否签到
export function VerifyPatientSign(data) {
  return request({
    url: '/api/SignInfo/VerifyPatientSign',
    method: 'post',
    data
  })
}

// 确认签到
export function ConfirmPatientSign(data) {
  return request({
    url: '/api/SignInfo/ConfirmPatientSign',
    method: 'post',
    data
  })
}

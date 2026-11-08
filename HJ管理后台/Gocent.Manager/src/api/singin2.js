import request from '@/utils/request'

// 获取时间
export function GetDataTime() {
  return request({
    url: '/api/System/GetDataTime',
    method: 'get'
  })
}
// 获取签到端信息
export function GetSignInfoBtnByCode(code) {
  return request({
    url: '/api/SignInfo/GetSignInfoBtnByCode',
    method: 'get',
    params: { code }
  })
}

// 获取刷卡数据
export function GetCreditCardBtnData(data) {
  return request({
    url: '/api/SignInfo/GetCreditCardBtnData',
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
export function ConfirmPatientBtnSign(data) {
  return request({
    url: '/api/SignInfo/ConfirmPatientBtnSign',
    method: 'post',
    data
  })
}

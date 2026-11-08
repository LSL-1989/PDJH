import request from '../utils/request'

export function loginUser(userNo, password) {
  return request.post('/User/LoginUser', {
    UserNo: userNo,
    Password: password,
    LoginScene: 'NURSE'
  })
}

/**
 * 忘记密码：登记重置申请，后台「用户管理」处理
 * @param {string} account 工号或手机号
 */
export function requestPasswordReset(account) {
  return request.post('/User/RequestPasswordReset', { account })
}

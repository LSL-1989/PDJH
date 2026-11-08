import request from '@/utils/request'

// 登录
export function login(data) {
  return request({
    url: '/api/User/LoginUser',
    method: 'post',
    data
  })
}

// 获取用户信息（打开/刷新页面：带 touchSession，未过期则续期）
export function getInfo(token) {
  return request({
    url: '/api/User/GetInfo',
    method: 'post',
    params: { token },
    touchSession: true
  })
}

/** 鼠标/键盘操作后续期，60 秒内由调用方节流；过期码 50014 由 request 拦截器回登录页 */
export function touchSession() {
  return request({
    url: '/api/User/TouchSession',
    method: 'post',
    touchSession: true
  })
}

// 获取用户诊室信息
export function GetUserClinic(data) {
  return request({
    url: '/api/User/GetUserClinic',
    method: 'post',
    data
  })
}

// 根据卡号 获取患者数据
export function getPatientByCardNo(PatientCardNo) {
  return request({
    url: '/api/User/GetPatientByCardNo',
    method: 'post',
    params: { PatientCardNo }
  })
}

// 登出
export function logout(userno) {
  return request({
    url: '/api/User/LogOut',
    method: 'post',
    params: { userno }
  })
}

// 新增用户
export function addUser(data) {
  console.log(data)
  return request({
    url: '/api/User/AddUser',
    method: 'post',
    data
  })
}

// 修改用户信息
export function editorUser(FromUser) {
  return request({
    url: '/api/User/EditorUser',
    method: 'post',
    data: FromUser
  })
}

// 删除用户
export function removeUser(UserNo) {
  console.log(UserNo)
  return request({
    url: '/api/User/RemoveUser',
    method: 'post',
    params: { UserNo }
  })
}

// 联合分页获取用户；keyword / roleId / loginScene / passwordResetRequested 筛选
export function getAllUser(query) {
  const payload = {
    pageindex: query.pageindex,
    pagesize: query.pagesize
  }
  const kw = query.keyword != null ? String(query.keyword).trim() : ''
  if (kw) payload.keyword = kw
  if (query.roleId) payload.roleId = query.roleId
  if (query.loginScene) payload.loginScene = query.loginScene
  if (query.passwordResetRequested === 1 || query.passwordResetRequested === '1') {
    payload.passwordResetRequested = 1
  }
  return request({
    url: '/api/User/GetUsers',
    method: 'post',
    data: payload
  })
}

/**
 * 管理员重置用户密码为默认值（123456）并强制下次登录改密
 * 与 ResetPassWord（用户自己改密需原密码）不同；消费者：maintenanceUser
 */
export function adminResetPassword(userNo) {
  return request({
    url: '/api/User/AdminResetPassword',
    method: 'post',
    data: { UserNo: userNo }
  })
}

/**
 * 忘记密码：登记重置申请（帐号/工号或手机号）
 * 消费者：护士站、药房等；后台用户管理可见
 */
export function requestPasswordReset(account) {
  return request({
    url: '/api/User/RequestPasswordReset',
    method: 'post',
    data: { account }
  })
}

// 重置密码（用户自己改密）
export function resetPassWord(ChangePassWord) {
  return request({
    url: '/api/User/ResetPassWord',
    method: 'post',
    data: ChangePassWord
  })
}


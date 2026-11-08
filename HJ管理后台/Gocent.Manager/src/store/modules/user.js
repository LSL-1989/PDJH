import { login, logout, getInfo, getAllUser, editorUser, removeUser, addUser, resetPassWord, getPatientByCardNo, GetUserClinic } from '@/api/user'
import { GetDataTime } from '@/api/singin'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { clearAdminActivity, markAdminActive } from '@/utils/adminSession'
import { resetRouter, asyncRoutes, constantRoutes } from '@/router'
import router from '@/router'
import { ElMessageBox as MessageBox, ElMessage as Message } from 'element-plus'
import Layout from '@/layout'
import { filterRoutesByPermission } from '@/utils/permissionRoutes'

const getDefaultState = () => {
  return {
    token: getToken(),
    name: '',
    avatar: '',
    UserNo: '',
    asyncRoutesDto: asyncRoutes,
    AllRoutes: [], // 侧边栏展示的路由（已按权限过滤）
    allowedMenuKeys: null, // Set<string>，用于路由守卫
    menuTitleMap: {} // PageUrl -> PageShowName，菜单管理修改后随登录/getInfo 刷新
  }
}

const state = getDefaultState()

// 唯一修改state的地方
const mutations = {
  // 重置state
  RESET_STATE: (state) => {
    Object.assign(state, getDefaultState())
  },
  // 存储token
  SET_TOKEN: (state, token) => {
    state.token = token
  },
  // 存储用户名
  SET_NAME: (state, name) => {
    state.name = name
  },
  // 存储用户头像
  SET_AVATAR: (state, avatar) => {
    state.avatar = avatar
  },
  // 存储用户账号
  SET_USERNO: (state, UserNo) => {
    state.UserNo = UserNo
  },
  // 存储全部计算好了的路由
  SET_ALLROUTES: (state, AllRoutes) => {
    state.AllRoutes = AllRoutes
  },
  SET_ALLOWED_MENU_KEYS: (state, keys) => {
    state.allowedMenuKeys = keys
  },
  SET_MENU_TITLE_MAP: (state, map) => {
    state.menuTitleMap = map || {}
  }
}

function applyPermissionMenus(commit, serverRoutes) {
  let routes = serverRoutes
  if (!routes || routes.length === 0) {
    routes = [
      { Id: 'M001', PageShowName: '工作台', PageUrl: 'dashboard', LevelId: 0, ParentPageId: '0' }
    ]
  }
  const { sidebarRoutes, allowedKeys, titleMap } = filterRoutesByPermission(constantRoutes, routes)
  commit('SET_ALLROUTES', sidebarRoutes)
  commit('SET_ALLOWED_MENU_KEYS', allowedKeys)
  commit('SET_MENU_TITLE_MAP', titleMap)
}

// 动作方法
const actions = {
  // 这里就是登陆业务
  login({ commit, state }, userInfo) {
    // 解析出用户名和密码
    const { UserNo, Password } = userInfo
    return new Promise((resolve, reject) => {
      login({ UserNo: UserNo.trim(), Password: Password, LoginScene: 'ADMIN' }).then(response => {
        console.log('查看返回值', response)
        // 本地存储签到端SignCode，目前未加密
        localStorage.setItem('SignCode', response.data.SignCode || '')
        let Routes = response.data.Routes || []
        console.log('服务端菜单路由数量:', Routes.length)

        // vuex存储token
        commit('SET_TOKEN', response.data.Token)
        // 本地持久化存储token
        setToken(response.data.Token)
        markAdminActive()
        // 设置账号
        commit('SET_USERNO', response.data.UserNo)
        // 设置名字
        commit('SET_NAME', response.data.UserName)

        // 设置头像
        commit('SET_AVATAR', 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif')

        applyPermissionMenus(commit, Routes)

        resolve()
      }).catch(error => {
        console.error('Login Promise Catch 捕获到异常:', error)
        reject(error)
      })
    })
  },

  // 获取用户信息
  getInfo({ commit, state }) {
    return new Promise((resolve, reject) => {
      getInfo(state.token).then(response => {
        const { data } = response
        if (!data) {
          return reject('Verification failed, please Login again.')
        }
        // 返回的信息包含 头像地址 介绍 用户名 角色

        // 设置账号
        commit('SET_USERNO', data.UserNo)
        // 设置名字
        commit('SET_NAME', data.UserName)
        // 设置头像
        commit('SET_AVATAR', 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif')

        applyPermissionMenus(commit, response.data.Routes || [])

        resolve(data)
      }).catch(error => {
        console.error('GetInfo Promise Catch 捕获到异常:', error)
        reject(error)
      })
    })
  },

  // 根据卡号 获取患者数据
  getPatientByCardNo({ }, PatientCardNo) {
    return new Promise((resolve, reject) => {
      getPatientByCardNo(PatientCardNo).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 根据用户ID获取用户数据
  GetUserClinic({ }, TokenInput) {
    return new Promise((resolve, reject) => {
      GetUserClinic(TokenInput).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 修改密码
  resetPassWord({ }, ChangePassWord) {
    return new Promise((resolve, reject) => {
      resetPassWord(ChangePassWord).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 修改用户信息
  editorUser({ }, FromUser) {
    return new Promise((resolve, reject) => {
      editorUser(FromUser).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 删除用户
  removeUser({ }, UserNo) {
    return new Promise((resolve, reject) => {
      removeUser(UserNo).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 新增用户
  addUser({ }, AddUser) {
    return new Promise((resolve, reject) => {
      addUser(AddUser).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 联合分页 获取用户信息
  getAllUser({ commit, state }, Page) {
    return new Promise((resolve, reject) => {
      getAllUser(Page).then(response => {
        const inner = response.data
        if (!inner) {
          return reject('Verification failed, please Login again.')
        }
        const list = Array.isArray(inner.data) ? inner.data : (Array.isArray(inner) ? inner : [])
        const total = inner.pageInfo && inner.pageInfo.totalCount != null
          ? inner.pageInfo.totalCount
          : list.length
        resolve({ data: list, pageInfo: { totalCount: total } })
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 登出
  logout({ commit, state }) {
    return new Promise((resolve, reject) => {
      logout(state.UserNo).then(() => {
        removeToken() // must remove  token  first
        clearAdminActivity()
        // resetRouter()
        commit('RESET_STATE')
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 删除token
  resetToken({ commit }) {
    return new Promise(resolve => {
      removeToken() // must remove  token  first
      clearAdminActivity()
      commit('RESET_STATE')
      resolve()
    })
  }
}

// 对比法 后端返回路由名字   定义一个函数 对比异步路由 计算出用户到底显示哪些异步路由
const computedAsyncReoutes = (AsyncRoutes, Routes) => {
  return AsyncRoutes.filter(item => {
    if (Routes.indexOf(item.name) != -1) {
      // 递归
      if (item.children && item.children.length) {
        item.children = computedAsyncReoutes(item.children, Routes)
      }
      return true
    }
  })
}

// 生成法 后端返回路由实体 定义一个函数 直接生成路由信息
const createAsyncReoutes = (Routes) => {
  const asyncR = []
  Routes.filter(item => {
    if (item.LevelId == 0) {
      const father = {
        path: `/${item.PageUrl}`,
        component: Layout,
        name: `${item.PageUrl}`,
        alwaysShow: true,
        hidden: !item.AlwaysShow,
        meta: { title: `${item.PageShowName}`, icon: `${item.Icon}`, noCache: !item.NoCache },
        children: []
      }
      Routes.filter(it => {
        if (it.ParentPageId.toLowerCase() == item.Id.toLowerCase()) {
          father.children.push({
            path: `${it.PageUrl}`,
            name: `${it.PageUrl}`,
            hidden: !item.AlwaysShow,
            meta: { title: `${it.PageShowName}` },
            component: getViews(it.PageUrl)
          })
        }
      })
      asyncR.push(father)
    }
  })
  return asyncR
}
// 生成组件路径
function getViews(path) {
  return () => import(`@/views/${path}/index.vue`)
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}


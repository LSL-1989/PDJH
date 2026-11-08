import { getRole, editorRole, getPermission, commitPermission, createRole, removeRole } from '@/api/role'

const getDefaultState = () => {
  return {
    // 角色数组
    RoleArry: []
  }
}
const state = getDefaultState()

// 唯一修改state的地方
const mutations = {
  // 存储角色
  SET_ROLES: (state, roles) => {
    state.RoleArry = roles
  }
}

// 动作方法
const actions = {

  // 获取所有的角色信息
  getRole({ commit }) {
    return new Promise((resolve, reject) => {
      getRole().then(response => {
        const { data } = response
        commit('SET_ROLES', data)
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 修改角色信息
  editorRole({ }, input) {
    return new Promise((resolve, reject) => {
      editorRole(input).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 根据角色id获取权限
  getPermission({ }, RoleId) {
    return new Promise((resolve, reject) => {
      getPermission(RoleId).then(response => {
        const { data } = response
        var Permission = []
        if (data) {
          data.filter(item => {
            item = item.toUpperCase()
            Permission.push(item)
          })
        }
        resolve(Permission)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 保存权限
  commitPermission({ }, { Pages, RoleId }) {
    return new Promise((resolve, reject) => {
      var input = {
        Pages: Pages,
        RoleId: RoleId
      }
      commitPermission(input).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 新增角色
  createRole({ }, input) {
    return new Promise((resolve, reject) => {
      createRole(input).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 删除角色
  romoveRole({ }, RoleId) {
    return new Promise((resolve, reject) => {
      removeRole(RoleId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}


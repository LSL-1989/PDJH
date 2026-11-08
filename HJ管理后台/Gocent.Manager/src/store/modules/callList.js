import { getDoctorByDeptID, getQueryCallList, Register, removeCall, editorCall, batchRemove, oneclickClear, sortCall, GetDeptCallList } from '@/api/callList'

const getDefaultState = () => {
  return {

  }
}
const state = getDefaultState()

const mutations = {

}

const actions = {

  // 登记排队
  Register({ }, Call) {
    return new Promise((resolve, reject) => {
      Register(Call).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 修改信息
  editorCall({ }, EditorCall) {
    return new Promise((resolve, reject) => {
      editorCall(EditorCall).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 删除队列信息
  removeCall({ }, id) {
    return new Promise((resolve, reject) => {
      removeCall(id).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 根据诊室ID 查询医生
  getDoctorByDeptID({ }, DeptID) {
    return new Promise((resolve, reject) => {
      getDoctorByDeptID(DeptID).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 条件查询
  getQueryCallList({ }, QueryInput) {
    return new Promise((resolve, reject) => {
      getQueryCallList(QueryInput).then(response => {
        const inner = response.data
        const list = inner && Array.isArray(inner.data) ? inner.data : []
        const total = inner && inner.pageInfo && inner.pageInfo.totalCount != null
          ? inner.pageInfo.totalCount
          : (inner && inner.TotalCount != null ? inner.TotalCount : 0)
        resolve({ data: list, pageInfo: { totalCount: total } })
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 条件查询
  GetDeptCallList({ }, QueryInput) {
    return new Promise((resolve, reject) => {
      GetDeptCallList(QueryInput).then(response => {
        const inner = response.data
        const list = inner && Array.isArray(inner.data) ? inner.data : []
        const total = inner && inner.pageInfo && inner.pageInfo.totalCount != null
          ? inner.pageInfo.totalCount
          : (inner && inner.TotalCount != null ? inner.TotalCount : 0)
        resolve({ data: list, pageInfo: { totalCount: total } })
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 批量删除
  batchRemove({ }, input) {
    return new Promise((resolve, reject) => {
      batchRemove(input).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 一键清除
  oneclickClear({ }) {
    return new Promise((resolve, reject) => {
      oneclickClear().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 排序
  sortCall({ }, input) {
    return new Promise((resolve, reject) => {
      sortCall(input).then(response => {
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

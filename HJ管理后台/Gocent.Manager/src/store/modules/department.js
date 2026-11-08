import { getAllDepartment, editorDepartment, createDepartment, removeDepartments, GetUserParentDeptID } from '@/api/department'

const ROOT_FATHER = '00000000-0000-0000-0000-000000000000'

const getDefaultState = () => {
  return {
    DepartmentArry: []
  }
}

const state = getDefaultState()

const mutations = {
  SET_DEPARTMENTARRY: (state, value) => {
    state.DepartmentArry = value
  }
}

/** 平铺列表 → 多层树（cascader：value/label/children） */
function buildCascaderTree(data) {
  if (!Array.isArray(data)) return []
  const byFather = {}
  data.forEach(item => {
    const fid = item.FatherID || item.fatherID || ROOT_FATHER
    if (!byFather[fid]) byFather[fid] = []
    byFather[fid].push(item)
  })
  function build(parentId) {
    const kids = byFather[parentId] || []
    return kids.map(item => {
      const id = item.ID || item.id
      const node = {
        value: id,
        label: item.Name || item.name,
        disabled: item.State != 1 && item.state != 1,
        Queue: item.Queue || item.queue,
        Type: item.Type || item.type,
        businessType: item.BusinessType || item.businessType,
        IsLeaf: item.IsLeaf != null ? item.IsLeaf : !(byFather[id] && byFather[id].length),
        children: build(id)
      }
      if (!node.children.length) {
        delete node.children
      }
      return node
    })
  }
  // 根：FatherID 为空 GUID，或 Type=1 且父为空的历史数据
  const roots = build(ROOT_FATHER)
  // 兼容：孤儿节点挂到根
  const allIds = new Set(data.map(d => d.ID || d.id))
  data.forEach(item => {
    const fid = item.FatherID || item.fatherID
    const id = item.ID || item.id
    if (fid && fid !== ROOT_FATHER && !allIds.has(fid)) {
      roots.push({
        value: id,
        label: item.Name || item.name,
        disabled: item.State != 1,
        Type: item.Type,
        IsLeaf: true
      })
    }
  })
  return roots
}

/** 表格树：保留原始字段 + children */
function buildTableTree(data) {
  if (!Array.isArray(data)) return []
  const map = {}
  data.forEach(item => {
    item.children = []
    map[item.ID] = item
  })
  const roots = []
  data.forEach(item => {
    const fid = item.FatherID
    if (fid && fid !== ROOT_FATHER && map[fid]) {
      map[fid].children.push(item)
    } else {
      roots.push(item)
    }
  })
  function prune(nodes) {
    nodes.forEach(n => {
      if (n.children && n.children.length) {
        prune(n.children)
      } else {
        delete n.children
      }
    })
  }
  prune(roots)
  return roots
}

const actions = {
  getAllDepartment({ commit }) {
    return new Promise((resolve, reject) => {
      getAllDepartment().then(response => {
        const { data } = response
        resolve(buildCascaderTree(data))
      }).catch(error => {
        reject(error)
      })
    })
  },

  GetUserParentDeptID({ }, DepartmentInput) {
    return new Promise((resolve, reject) => {
      GetUserParentDeptID(DepartmentInput).then(response => {
        const { data } = response
        const DepartmentArry = (data || []).map(item => ({
          value: item.ID,
          label: item.Name,
          Type: item.Type,
          Queue: item.Queue,
          IsLeaf: item.IsLeaf
        }))
        resolve(DepartmentArry)
      }).catch(error => {
        reject(error)
      })
    })
  },

  getAllDepartment3({ }) {
    return new Promise((resolve, reject) => {
      getAllDepartment().then(response => {
        resolve(response.data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  getAllDepartment2({ }) {
    return new Promise((resolve, reject) => {
      getAllDepartment().then(response => {
        resolve(buildTableTree(response.data || []))
      }).catch(error => {
        reject(error)
      })
    })
  },

  createDepartment({ }, AddDept) {
    return new Promise((resolve, reject) => {
      createDepartment(AddDept).then(response => {
        resolve(response.data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  editorDepartment({ }, EditorDept) {
    return new Promise((resolve, reject) => {
      editorDepartment(EditorDept).then(response => {
        resolve(response.data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  removeDepartments({ }, ID) {
    return new Promise((resolve, reject) => {
      removeDepartments(ID).then(response => {
        resolve(response.data)
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

export { buildCascaderTree, buildTableTree, ROOT_FATHER }

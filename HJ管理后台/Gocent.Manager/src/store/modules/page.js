import { getPage, editorPage, removePage, createPage } from '@/api/page'

/** 接口 data 可能为 null；行上 Id/ParentPageId 可能为空，避免 toLowerCase 抛错导致整页一直 loading */
function menuRows(response) {
  const rows = response && response.data
  return Array.isArray(rows) ? rows : []
}

function idEq(a, b, upper) {
  const x = a != null ? String(a) : ''
  const y = b != null ? String(b) : ''
  return upper ? x.toUpperCase() === y.toUpperCase() : x.toLowerCase() === y.toLowerCase()
}

const getDefaultState = () => {
  return {
    PageMenuArry: [],
    PageMenuArry2: [],
    PageMenuArry3: []
  }
}

const state = getDefaultState()

const mutations = {
  SET_PAGEMENUARRY: (state, value) => {
    state.PageMenuArry = value
  },
  SET_PAGEMENUARRY2: (state, value) => {
    state.PageMenuArry2 = value
  },
  SET_PAGEMENUARRY3: (state, value) => {
    state.PageMenuArry3 = value
  }
}

const actions = {
  // 获取菜单数据 计算父子级关系 用于表格
  getPage({ commit }) {
    return new Promise((reslove, reject) => {
      getPage().then(response => {
        const data = menuRows(response)
        var PageArry = []
        data.filter(item => {
          if (item.LevelId == 0) {
            item.children = []
            data.filter(item2 => {
              if (idEq(item.Id, item2.ParentPageId, false)) {
                item.children.push(item2)
              }
            })
            PageArry.push(item)
          }
        })
        commit('SET_PAGEMENUARRY', PageArry)
        reslove(PageArry)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 获取菜单数据 计算父子关系 用于级联选择器
  getPage2({ commit }) {
    return new Promise((reslove, reject) => {
      getPage().then(response => {
        const data = menuRows(response)
        var PageArry = []
        data.filter(item => {
          if (item.LevelId == 0) {
            const father = {
              value: (item.Id != null ? String(item.Id) : '').toUpperCase(),
              label: item.PageShowName,
              disabled: item.IsDelete,
              children: []
            }
            data.filter(item2 => {
              if (idEq(item.Id, item2.ParentPageId, true)) {
                const song = {
                  value: (item2.Id != null ? String(item2.Id) : '').toUpperCase(),
                  label: item2.PageShowName,
                  disabled: item2.IsDelete
                }
                father.children.push(song)
              }
            })
            PageArry.push(father)
          }
        })
        commit('SET_PAGEMENUARRY2', PageArry)
        reslove(PageArry)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 获取菜单数据 计算父子关系 用于权限目录树
  getPage3({ commit }) {
    return new Promise((resolve, reject) => {
      getPage().then(response => {
        const data = menuRows(response)
        var PageArry = []
        data.filter(item => {
          if (item.LevelId == 0) {
            const father = {
              id: (item.Id != null ? String(item.Id) : '').toUpperCase(),
              label: item.PageShowName,
              disabled: item.IsDelete,
              children: []
            }
            data.filter(item2 => {
              if (idEq(item.Id, item2.ParentPageId, true)) {
                const song = {
                  id: (item2.Id != null ? String(item2.Id) : '').toUpperCase(),
                  label: item2.PageShowName,
                  disabled: item2.IsDelete
                }
                father.children.push(song)
              }
            })
            PageArry.push(father)
          }
        })
        commit('SET_PAGEMENUARRY3', PageArry)
        resolve(PageArry)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 修改菜单
  editorPage({ }, input) {
    return new Promise((resolve, reject) => {
      editorPage(input).then(response => {
        const { data } = response

        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 删除菜单
  removePage({ }, id) {
    return new Promise((resolve, reject) => {
      removePage(id).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 新建菜单
  createPage({ }, input) {
    return new Promise((resolve, reject) => {
      createPage(input).then(response => {
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

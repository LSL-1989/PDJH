import { getDepartments, getDepartmentQueueTime, getDoctorCallCount } from '@/api/dashboard'
const getDefaultState = () => {
  return {

  }
}
const state = getDefaultState()
const mutations = {

}

const actions = {
  // 获取首页卡片上每个屏幕下每种诊室的排队人数
  getDepartments({}) {
    return new Promise((resolve, reject) => {
      getDepartments().then(response => {
        const { data } = response
        resolve(data)
      }).catch(erro => {
        reject(erro)
      })
    })
  },
  // 获取折线统计上每个时间段每个诊室的排队人数
  getDepartmentQueueTime({}, input) {
    return new Promise((resolve, reject) => {
      getDepartmentQueueTime(input).then(response => {
        const { data } = response
        var Series = []
        var legend = []
        var Queue = []
        data.filter(item => {
          var obj = {
            name: item.Name,
            type: 'line',
            data: [item.ZeroSpot, item.TowSpot, item.FourSpot, item.SexSpot, item.EightSpot, item.TenSpot, item.TwelveSpot, item.FourteenSpot, item.SixtenSpot, item.EighttenSpot, item.TwentySpot, item.TwentyTwoSpot]
          }
          Queue.push(item.sflbbigtype)
          legend.push(item.Name)
          Series.push(obj)
        })
        resolve({ legend, Series, Queue })
      }).catch(erro => {
        reject(erro)
      })
    })
  },
  // 统计报表 统计每个医生呼叫了多少人
  getDoctorCallCount({}, input) {
    return new Promise((resolve, reject) => {
      getDoctorCallCount(input).then((response) => {
        const inner = response.data
        const rows = inner && inner.data ? inner.data : []
        const pageInfo = inner && inner.pageInfo ? inner.pageInfo : { totalCount: 0 }
        resolve({ data: rows, pageInfo })
      }).catch((error) => {
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

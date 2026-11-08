import { GetScreens, GetScreensSingle, UpdateScreen, AddScreen, DelScreen, GetDepartments, GetHasassociatedDepartments, RelevancyScreenAndDepts, GetHasassociatedRooms, RelevancyScreenAndRooms } from '@/api/ScreenManages'

const getDefaultState = () => {
  return {
  }
}

const state = getDefaultState()

const mutations = {

}

const actions = {
  GetScreens({ }, ScreensInput) {
    return new Promise((resolve, reject) => {
      GetScreens(ScreensInput).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  GetScreensSingle({ }, ScreenInput) {
    return new Promise((resolve, reject) => {
      GetScreensSingle(ScreenInput).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  UpdateScreen({ }, ScreenInput) {
    return new Promise((resolve, reject) => {
      UpdateScreen(ScreenInput).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  AddScreen({ }, ScreenInput) {
    return new Promise((resolve, reject) => {
      AddScreen(ScreenInput).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  DelScreen({ }, ScreenInput) {
    return new Promise((resolve, reject) => {
      DelScreen(ScreenInput).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },

  GetDepartments({ }, ScreenInput) {
    return new Promise((resolve, reject) => {
      GetDepartments(ScreenInput).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  GetHasassociatedDepartments({ }, ScreenInput) {
    return new Promise((resolve, reject) => {
      GetHasassociatedDepartments(ScreenInput).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },

  RelevancyScreenAndDepts({ }, ScreenDeptMappingInput) {
    return new Promise((resolve, reject) => {
      RelevancyScreenAndDepts(ScreenDeptMappingInput).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  GetHasassociatedRooms({ }, ScreenInput) {
    return GetHasassociatedRooms(ScreenInput)
  },
  RelevancyScreenAndRooms({ }, input) {
    return RelevancyScreenAndRooms(input)
  }

}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}

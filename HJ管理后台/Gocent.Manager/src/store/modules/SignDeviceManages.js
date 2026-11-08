import { listSignDevices, saveSignDevice, removeSignDevice, relevancySignDeviceAndRooms } from '@/api/SignDeviceManages'
import { getAllOfficeRooms } from '@/api/officeRoom'

const state = {}

const mutations = {}

const actions = {
  listSignDevices(_, payload) {
    return listSignDevices(payload)
  },
  saveSignDevice(_, payload) {
    return saveSignDevice(payload)
  },
  removeSignDevice(_, id) {
    return removeSignDevice(id)
  },
  relevancySignDeviceAndRooms(_, payload) {
    return relevancySignDeviceAndRooms(payload)
  },
  getAllOfficeRooms() {
    return getAllOfficeRooms()
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}

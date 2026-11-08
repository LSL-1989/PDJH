import { defineStore } from 'pinia'

const TOKEN_KEY = 'nurse_desk_token'
const USER_KEY = 'nurse_desk_user'

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userNo: localStorage.getItem(USER_KEY) || '',
    userName: '',
    role: '',
    zoneId: '',
    zoneName: '',
    zoneSource: '',
    fallbackHint: false,
    authorizedZones: [],
    departments: [],
    rooms: []
  }),
  getters: {
    isLoggedIn: (s) => !!s.token && !!s.userNo,
    canSwitchZone: (s) => (s.authorizedZones || []).length > 1
  },
  actions: {
    setLogin(token, userNo, userName, role) {
      this.token = token
      this.userNo = userNo
      this.userName = userName || userNo
      this.role = role || ''
      this.zoneId = ''
      this.zoneName = ''
      this.zoneSource = ''
      this.fallbackHint = false
      this.authorizedZones = []
      this.departments = []
      this.rooms = []
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, userNo)
    },
    setSession(data) {
      if (!data) return
      this.zoneId = data.zoneId || ''
      this.zoneName = data.zoneName || ''
      this.zoneSource = data.zoneSource || ''
      this.fallbackHint = !!data.fallbackHint
      this.authorizedZones = data.authorizedZones || []
      this.departments = data.departments || []
      this.rooms = data.rooms || []
      if (data.operatorName) this.userName = data.operatorName
      if (data.operatorRole) this.role = data.operatorRole
    },
    logout() {
      this.token = ''
      this.userNo = ''
      this.userName = ''
      this.role = ''
      this.zoneId = ''
      this.zoneName = ''
      this.zoneSource = ''
      this.fallbackHint = false
      this.authorizedZones = []
      this.departments = []
      this.rooms = []
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})

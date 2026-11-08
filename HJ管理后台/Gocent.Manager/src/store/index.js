import { createStore } from 'vuex'
import getters from './getters'
import app from './modules/app'
import settings from './modules/settings'
import user from './modules/user'
import tagsView from './modules/tagsView'
import role from './modules/role'
import department from './modules/department'
import callList from './modules/callList'
import callAutoNum from './modules/callAutoNum'
import ScreenManages from './modules/ScreenManages'
import SignDeviceManages from './modules/SignDeviceManages'
import page from './modules/page'
import dashboard from './modules/dashboard'

const store = createStore({
  modules: {
    app,
    settings,
    tagsView,
    user,
    role,
    department,
    callList,
    callAutoNum,
    ScreenManages,
    SignDeviceManages,
    page,
    dashboard
  },
  getters
})

export default store

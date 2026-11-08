import { createApp } from 'vue'
import 'normalize.css/normalize.css'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/display.css'
import 'animate.css'
import '@/styles/index.scss'
import App from './App'
import store from './store'
import router from './router'
import * as echarts from 'echarts'
import moment from 'moment'
import axios from 'axios'
import { registerIcons } from '@/icons'
import { registerLegacyElIcons } from '@/icons/registerLegacyElIcons'
import bus from '@/utils/bus'
import '@/permission'

axios.get('/api/SystemConfig/GetClientManifest').then(res => {
  if (res.data && res.data.code === 200) {
    window.$sysConfig = res.data.data
  }
}).catch(() => {
  window.$sysConfig = {}
}).finally(() => {
  const app = createApp(App)
  app.config.globalProperties.$moment = moment
  app.config.globalProperties.$echarts = echarts
  app.config.globalProperties.$Bus = bus
  registerIcons(app)
  registerLegacyElIcons(app)
  app.use(store)
  app.use(router)
  app.use(ElementPlus, { locale: zhCn })
  app.mount('#app')
})

import { createApp } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import App from './App.vue'
import moment from 'moment'
import 'animate.css'
import './styles/screen-content.scss'
import http from './request/axios.config'
import router from './router/index'
import { startConnection } from './signalr/index'
import { installScreenPreviewGuard } from './utils/screenPreview'
import { RecycleScroller } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import { initViewportCssVars } from './composables/useScreenScale'

/** 首屏前写入视口 CSS 变量，供绑定页 ScreenFrame 缩放（对齐自助签到机） */
initViewportCssVars()
/** preview=1 时标记只读预览并静音，须早于 App 绑定引导 */
installScreenPreviewGuard()

const app = createApp(App)
app.component('RecycleScroller', RecycleScroller)
app.config.globalProperties.$proxy = startConnection()
app.config.globalProperties.$axios = http
app.config.globalProperties.$moment = moment
app.config.globalProperties.$message = ElMessage
app.config.globalProperties.$confirm = ElMessageBox.confirm
app.use(router)
app.mount('#app')

import { createApp } from 'vue'
import './index.css'
import App from './App.vue'
import router from './router'
import { initViewportCssVars } from './composables/useScreenScale'
import { installClinicPreviewGuard } from './utils/clinicPreview'

initViewportCssVars()
/** preview=1 时标记只读预览，须早于路由守卫里的设备解析 */
installClinicPreviewGuard()

createApp(App).use(router).mount('#app')

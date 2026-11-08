import { createApp } from 'vue'
import './styles/neumorphism.css'
import './index.css'
import App from './App.vue'
import router from './router'
import { initViewportCssVars } from './composables/useScreenScale'

/**
 * 入口对齐门边屏：不在此全局注册 vant（全量 vant 在部分安卓 Chrome 上会导致模块加载失败白屏）。
 * van 组件按需在页面内局部引入。
 */
initViewportCssVars()

createApp(App).use(router).mount('#app')

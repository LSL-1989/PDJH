import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { Toast, Dialog, Notify } from 'vant'

import 'vant/lib/index.css'

const app = createApp(App)

app.use(router)
app.use(Toast)
app.use(Dialog)
app.use(Notify)

app.mount('#app')

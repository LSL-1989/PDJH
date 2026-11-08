/**
 * 候诊区大屏（SCREEN）路由：/default、/default-mul、/default-mul-col、窗口屏 + 遗留 pdjh 模板
 * 诊室门边屏（CLINIC）请使用 HJ诊室门边屏终端 /clinic/
 */
import { createRouter, createWebHashHistory } from 'vue-router'

const lazy = (loader) => () => loader()

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/default' },
    { path: '/default', component: lazy(() => import('../common/default/index.vue')) },
    {
      path: '/default-mul',
      component: lazy(() => import('../common/default-mul/index.vue'))
    },
    {
      path: '/default-mul-col',
      component: lazy(() => import('../common/default-mul-col/index.vue'))
    },
    /** 兼容旧配置 templateRoute=/pdjh1 */
    { path: '/pdjh1', redirect: (to) => ({ path: '/default', query: to.query }) },
    {
      path: '/pdjh2',
      component: lazy(() => import('../common/pdjh2/index.vue')),
      meta: { legacy: true, note: 'legacy 门边屏模板，新部署请用 /clinic/' }
    },
    { path: '/pdjh3', component: lazy(() => import('../common/pdjh3/index.vue')) },
    { path: '/pdjh4', component: lazy(() => import('../common/pdjh4/index.vue')) },
    { path: '/pdjh5', component: lazy(() => import('../common/pdjh5/index.vue')) },
    { path: '/pdjh6', component: lazy(() => import('../common/pdjh6/index.vue')) },
    { path: '/pdjh7', component: lazy(() => import('../common/pdjh7/index.vue')) },
    { path: '/pdjh8', component: lazy(() => import('../common/pdjh8/index.vue')) },
    {
      path: '/pdjh9',
      component: lazy(() => import('../common/pdjh9/index.vue')),
      meta: { legacy: true, note: 'legacy 门边屏模板，新部署请用 /clinic/' }
    },
    { path: '/pharmacy-window', component: lazy(() => import('../common/pharmacy-window/index.vue')) },
    { path: '/lab-window', component: lazy(() => import('../common/lab-window/index.vue')) },
    { path: '/GlobalHub', component: lazy(() => import('../common/globalHub/index.vue')) },
    { path: '/directorDashboard', component: lazy(() => import('../common/directorDashboard/index.vue')) },
    { path: '/bind', component: lazy(() => import('../views/BindingGuide.vue')) },
    { path: '/voice', component: lazy(() => import('../views/VoicePlayer.vue')) },
    { path: '/wrong-terminal', component: lazy(() => import('../views/WrongTerminal.vue')) },
    { path: '/404', component: lazy(() => import('../common/error/index.vue')) },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/404',
      name: 'notFound'
    }
  ]
})

export default router

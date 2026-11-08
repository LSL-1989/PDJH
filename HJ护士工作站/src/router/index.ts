import { createRouter, createWebHashHistory } from 'vue-router'
import { useSessionStore } from '../stores/session.js'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'Desk',
    component: () => import('../views/Desk.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  const session = useSessionStore()
  if (!to.meta.public && !session.isLoggedIn) {
    return { name: 'Login' }
  }
  if (to.name === 'Login' && session.isLoggedIn) {
    return { name: 'Desk' }
  }
})

export default router

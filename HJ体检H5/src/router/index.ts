import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/exam',
      name: 'ExamDashboard',
      alias: '/',
      component: () => import('../views/ExamDashboard.vue')
    }
  ]
})

export default router

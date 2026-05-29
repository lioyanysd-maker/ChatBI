import { createRouter, createWebHistory } from 'vue-router'
import ChatBI from '../views/ChatBI.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'chatbi', component: ChatBI },
  ],
})

export default router

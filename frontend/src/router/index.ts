import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'

// 定义路由配置
const routes = [
  {
    path: '/',
    name: 'home',
    component: HomePage,
  },
  {
    path: '/chat/:chatId',
    name: 'chat',
    component: () => import('@/pages/ChatPage.vue'),
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/pages/AdminPage.vue'),
  },
  {
    path: '/apps',
    name: 'apps',
    component: () => import('@/pages/AppsPage.vue'),
  },
  {
    path: '/gomoku',
    name: 'gomoku-lobby',
    component: () => import('@/pages/GomokuLobbyPage.vue'),
  },
  {
    path: '/gomoku/:roomId',
    name: 'gomoku-game',
    component: () => import('@/pages/GomokuGamePage.vue'),
  },
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router

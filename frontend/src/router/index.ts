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
  {
    path: '/editor',
    name: 'editor-new',
    component: () => import('@/pages/EditorPage.vue'),
  },
  {
    path: '/editor/:docId',
    name: 'editor-room',
    component: () => import('@/pages/EditorPage.vue'),
  },
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 页面标题映射
const titleMap: Record<string, string> = {
  home: '聊天',
  chat: '聊天',
  admin: '管理后台',
  apps: '应用中心',
  'gomoku-lobby': '应用大厅',
  'gomoku-game': '五子棋对局',
  'editor-new': '协作编辑器',
  'editor-room': '协作编辑器',
}

router.afterEach((to) => {
  document.title = titleMap[to.name as string] ?? '聊天'
})

export default router

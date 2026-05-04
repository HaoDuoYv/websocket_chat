import { createRouter, createWebHistory } from 'vue-router'
import LandingPage from '@/pages/LandingPage.vue'

const routes = [
  {
    path: '/',
    name: 'landing',
    component: LandingPage,
  },
  {
    path: '/login',
    name: 'home',
    component: () => import('@/pages/HomePage.vue'),
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

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const titleMap: Record<string, string> = {
  landing: 'WebSocket Chat',
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

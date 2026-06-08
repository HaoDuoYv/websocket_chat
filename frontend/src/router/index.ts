import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'server-select',
    component: () => import('@/pages/ServerSelectPage.vue'),
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
  {
    path: '/ai/manage',
    name: 'ai-manage',
    component: () => import('@/pages/AiManagePage.vue'),
  },
  {
    path: '/ai/:assistantId',
    name: 'ai-chat',
    component: () => import('@/pages/AiChatView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const titleMap: Record<string, string> = {
  'server-select': 'WebSocket Chat',
  home: '聊天',
  chat: '聊天',
  apps: '应用中心',
  'gomoku-lobby': '应用大厅',
  'gomoku-game': '五子棋对局',
  'editor-new': '协作编辑器',
  'editor-room': '协作编辑器',
  'ai-manage': 'AI助手管理',
  'ai-chat': 'AI助手',
}

router.afterEach((to) => {
  const title = to.meta.title as string
  document.title = title || (titleMap[to.name as string] ?? '聊天')
})

export default router

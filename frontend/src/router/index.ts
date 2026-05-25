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
    path: '/admin/login',
    name: 'admin-login',
    component: () => import('@/views/admin/AdminLogin.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/AdminDashboard.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/AdminUsers.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'ai',
        name: 'admin-ai',
        component: () => import('@/views/admin/AdminAiConfig.vue'),
        meta: { title: 'AI 助手配置' }
      },
      {
        path: 'logs',
        name: 'admin-logs',
        component: () => import('@/views/admin/AdminLogs.vue'),
        meta: { title: '系统日志' }
      },
    ]
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
  landing: 'WebSocket Chat',
  home: '聊天',
  chat: '聊天',
  'admin-login': '管理员登录',
  'admin-dashboard': '管理后台 - 仪表盘',
  'admin-users': '管理后台 - 用户管理',
  'admin-ai': '管理后台 - AI配置',
  'admin-logs': '管理后台 - 系统日志',
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

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  adminLogin,
  adminLogout,
  banUser,
  clearLogs,
  getAdminSession,
  getOnlineUsers,
  getRecentLogs,
  getSystemMetrics,
  getUsers,
  renameUser,
  unbanUser,
  type AdminUser,
  type LogLine,
  type SystemMetrics
} from '@/api/admin'

const router = useRouter()
const metrics = ref<SystemMetrics | null>(null)
const logs = ref<LogLine[]>([])
const users = ref<AdminUser[]>([])
const onlineUserIds = ref<Set<string>>(new Set())
const error = ref('')
const isLoading = ref(true)
const isSubmitting = ref(false)
const autoRefresh = ref(true)
const adminSession = ref({ loggedIn: false, username: '' })
const loginForm = ref({ username: '', password: '' })
const renameDrafts = ref<Record<string, string>>({})
const banReasonDrafts = ref<Record<string, string>>({})
let refreshInterval: number | null = null

const isDarkTheme = ref(localStorage.getItem('theme') !== 'light')

const onlineCount = computed(() => users.value.filter(user => onlineUserIds.value.has(user.userId)).length)
const bannedCount = computed(() => users.value.filter(user => user.banned).length)

const enrichedUsers = computed(() => {
  return [...users.value].sort((a, b) => {
    const aOnline = onlineUserIds.value.has(a.userId)
    const bOnline = onlineUserIds.value.has(b.userId)
    if (aOnline !== bOnline) {
      return aOnline ? -1 : 1
    }
    if (a.banned !== b.banned) {
      return a.banned ? 1 : -1
    }
    return b.createdAt - a.createdAt
  })
})

const formatMemory = (mb: number): string => {
  if (mb >= 1024) {
    return (mb / 1024).toFixed(2) + ' GB'
  }
  return mb + ' MB'
}

const formatUptime = (seconds: number): string => {
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)

  if (days > 0) {
    return `${days}天 ${hours}小时 ${minutes}分钟`
  } else if (hours > 0) {
    return `${hours}小时 ${minutes}分钟`
  }
  return `${minutes}分钟`
}

const formatTimestamp = (timestamp: number): string => {
  if (!timestamp) return '--'
  return new Date(timestamp).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getLogLevelClass = (level: string): string => {
  const levelMap: Record<string, string> = {
    ERROR: isDarkTheme.value ? 'text-red-400 bg-red-400/10' : 'text-red-600 bg-red-100',
    WARN: isDarkTheme.value ? 'text-yellow-400 bg-yellow-400/10' : 'text-yellow-600 bg-yellow-100',
    INFO: isDarkTheme.value ? 'text-gray-400 bg-gray-400/10' : 'text-[#18181B] bg-gray-100',
    DEBUG: isDarkTheme.value ? 'text-gray-400 bg-gray-400/10' : 'text-gray-600 bg-gray-100'
  }
  return levelMap[level] || levelMap.INFO
}

const refreshData = async () => {
  const [metricsData, logsData, usersData, onlineUsersData] = await Promise.all([
    getSystemMetrics(),
    getRecentLogs(200),
    getUsers(),
    getOnlineUsers()
  ])
  metrics.value = metricsData
  logs.value = logsData
  users.value = usersData
  onlineUserIds.value = new Set((onlineUsersData.users || []).map(user => user.userId))
  usersData.forEach(user => {
    if (!(user.userId in renameDrafts.value)) {
      renameDrafts.value[user.userId] = user.username
    }
    if (!(user.userId in banReasonDrafts.value)) {
      banReasonDrafts.value[user.userId] = user.bannedReason || ''
    }
  })
}

const loadData = async () => {
  try {
    if (!adminSession.value.loggedIn) {
      const session = await getAdminSession()
      adminSession.value = session
      if (!session.loggedIn) {
        error.value = ''
        return
      }
    }

    await refreshData()
    error.value = ''
  } catch (err: any) {
    if (err.response?.status === 401) {
      adminSession.value = { loggedIn: false, username: '' }
      error.value = '请先登录管理员账号'
    } else if (err.response?.status === 403) {
      error.value = '访问被拒绝：您的IP不在白名单中'
    } else {
      error.value = '加载数据失败：' + (err.response?.data?.message || err.message || '未知错误')
    }
  } finally {
    isLoading.value = false
  }
}

const handleLogin = async () => {
  if (!loginForm.value.username.trim() || !loginForm.value.password.trim()) {
    error.value = '请输入管理员账号和密码'
    return
  }
  isSubmitting.value = true
  try {
    const response = await adminLogin(loginForm.value.username.trim(), loginForm.value.password)
    adminSession.value = { loggedIn: true, username: response.username }
    loginForm.value.password = ''
    await refreshData()
    error.value = ''
  } catch (err: any) {
    error.value = err.response?.data?.message || '登录失败'
  } finally {
    isSubmitting.value = false
  }
}

const handleLogout = async () => {
  isSubmitting.value = true
  try {
    await adminLogout()
    adminSession.value = { loggedIn: false, username: '' }
    metrics.value = null
    logs.value = []
    users.value = []
    onlineUserIds.value = new Set()
    error.value = ''
  } catch (err: any) {
    error.value = err.response?.data?.message || '退出失败'
  } finally {
    isSubmitting.value = false
  }
}

const handleRenameUser = async (user: AdminUser) => {
  const nextName = (renameDrafts.value[user.userId] || '').trim()
  if (!nextName) {
    error.value = '用户名不能为空'
    return
  }
  isSubmitting.value = true
  try {
    await renameUser(user.userId, nextName)
    await refreshData()
    error.value = ''
  } catch (err: any) {
    error.value = err.response?.data?.message || '修改用户名失败'
  } finally {
    isSubmitting.value = false
  }
}

const handleBanUser = async (user: AdminUser) => {
  isSubmitting.value = true
  try {
    await banUser(user.userId, (banReasonDrafts.value[user.userId] || '').trim())
    await refreshData()
    error.value = ''
  } catch (err: any) {
    error.value = err.response?.data?.message || '封禁失败'
  } finally {
    isSubmitting.value = false
  }
}

const handleUnbanUser = async (user: AdminUser) => {
  isSubmitting.value = true
  try {
    await unbanUser(user.userId)
    banReasonDrafts.value[user.userId] = ''
    await refreshData()
    error.value = ''
  } catch (err: any) {
    error.value = err.response?.data?.message || '解封失败'
  } finally {
    isSubmitting.value = false
  }
}

const handleClearLogs = async () => {
  if (!confirm('确定要清空日志缓存吗？')) return
  try {
    await clearLogs()
    logs.value = []
  } catch (err: any) {
    error.value = '清空日志失败：' + (err.response?.data?.message || err.message || '未知错误')
  }
}

const handleBack = () => {
  router.push('/login')
}

const startAutoRefresh = () => {
  if (refreshInterval || !adminSession.value.loggedIn) return
  refreshInterval = window.setInterval(() => {
    refreshData().catch(() => {})
  }, 3000)
}

const stopAutoRefresh = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
    refreshInterval = null
  }
}

const toggleAutoRefresh = () => {
  autoRefresh.value = !autoRefresh.value
  if (autoRefresh.value) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

const toggleTheme = () => {
  isDarkTheme.value = !isDarkTheme.value
  localStorage.setItem('theme', isDarkTheme.value ? 'dark' : 'light')
}

onMounted(async () => {
  await loadData()
  if (autoRefresh.value && adminSession.value.loggedIn) {
    startAutoRefresh()
  }
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<template>
  <div class="flex flex-col min-h-screen" :class="isDarkTheme ? 'bg-[#18181B]' : 'bg-gray-50'">
    <header class="px-6 py-4 flex justify-between items-center border-b" :class="isDarkTheme ? 'border-gray-700/50' : 'border-gray-200'">
      <div class="flex items-center gap-3">
        <button
          @click="handleBack"
          class="transition-colors text-sm"
          :class="isDarkTheme ? 'text-gray-400 hover:text-white' : 'text-gray-500 hover:text-gray-800'"
        >
          ← 返回
        </button>
        <div class="w-px h-4" :class="isDarkTheme ? 'bg-gray-600' : 'bg-gray-300'"></div>
        <h1 class="text-lg font-semibold" :class="isDarkTheme ? 'text-white' : 'text-gray-900'">管理员控制台</h1>
      </div>

      <div class="flex items-center gap-4">
        <button
          @click="toggleTheme"
          class="w-9 h-9 flex items-center justify-center rounded transition-colors"
          :class="isDarkTheme ? 'bg-gray-700 text-yellow-400 hover:bg-gray-600' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
          :title="isDarkTheme ? '切换到浅色模式' : '切换到深色模式'"
        >
          <svg v-if="isDarkTheme" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="5"/>
            <line x1="12" y1="1" x2="12" y2="3"/>
            <line x1="12" y1="21" x2="12" y2="23"/>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
            <line x1="1" y1="12" x2="3" y2="12"/>
            <line x1="21" y1="12" x2="23" y2="12"/>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
          </svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          </svg>
        </button>

        <template v-if="adminSession.loggedIn">
          <div class="text-sm" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-600'">
            当前管理员：{{ adminSession.username }}
          </div>
          <button
            @click="toggleAutoRefresh"
            class="flex items-center gap-2 px-3 py-1.5 rounded text-sm transition-colors"
            :class="autoRefresh
              ? (isDarkTheme ? 'bg-green-500/20 text-green-400' : 'bg-green-100 text-green-600')
              : (isDarkTheme ? 'bg-gray-700 text-gray-400' : 'bg-gray-200 text-gray-500')"
          >
            <span class="w-2 h-2 rounded-full" :class="autoRefresh ? 'bg-green-500 animate-pulse' : 'bg-gray-400'"></span>
            {{ autoRefresh ? '自动刷新中' : '自动刷新已关闭' }}
          </button>
          <button
            @click="loadData"
            class="px-3 py-1.5 text-white text-sm font-medium rounded transition-colors"
            :class="isDarkTheme ? 'bg-[#27272A] hover:bg-[#3F3F46]' : 'bg-[#18181B] hover:bg-[#27272A]'"
          >
            刷新
          </button>
          <button
            @click="handleLogout"
            class="px-3 py-1.5 text-sm rounded transition-colors"
            :disabled="isSubmitting"
            :class="isDarkTheme ? 'bg-red-500/20 text-red-400 hover:bg-red-500/30 disabled:opacity-60' : 'bg-red-100 text-red-600 hover:bg-red-200 disabled:opacity-60'"
          >
            退出
          </button>
        </template>
      </div>
    </header>

    <div v-if="error" class="px-4 py-3 border-b" :class="isDarkTheme ? 'bg-red-900/30 border-red-700/50' : 'bg-red-50 border-red-200'">
      <p class="text-sm" :class="isDarkTheme ? 'text-red-300' : 'text-red-600'">{{ error }}</p>
    </div>

    <div class="flex-1 p-6">
      <div v-if="isLoading" class="flex items-center justify-center h-[60vh]" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">
        加载中...
      </div>

      <div
        v-else-if="!adminSession.loggedIn"
        class="max-w-md mx-auto mt-16 p-6 rounded-xl"
        :class="isDarkTheme ? 'bg-[#27272A] border border-gray-700/50' : 'bg-white border border-gray-200 shadow-sm'"
      >
        <h2 class="text-xl font-semibold mb-2" :class="isDarkTheme ? 'text-white' : 'text-gray-900'">管理员登录</h2>
        <p class="text-sm mb-6" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">登录后可查看监控、日志和用户管理数据。</p>
        <form class="space-y-4" @submit.prevent="handleLogin">
          <div>
            <label class="block text-sm mb-2" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-700'">账号</label>
            <input
              v-model="loginForm.username"
              type="text"
              class="w-full px-3 py-2 rounded border outline-none"
              :class="isDarkTheme ? 'bg-[#18181B] border-gray-600 text-white' : 'bg-white border-gray-300 text-gray-900'"
            >
          </div>
          <div>
            <label class="block text-sm mb-2" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-700'">密码</label>
            <input
              v-model="loginForm.password"
              type="password"
              class="w-full px-3 py-2 rounded border outline-none"
              :class="isDarkTheme ? 'bg-[#18181B] border-gray-600 text-white' : 'bg-white border-gray-300 text-gray-900'"
            >
          </div>
          <button
            type="submit"
            class="w-full py-2 rounded text-white font-medium"
            :disabled="isSubmitting"
            :class="isDarkTheme ? 'bg-[#27272A] hover:bg-[#3F3F46] disabled:opacity-60' : 'bg-[#18181B] hover:bg-[#27272A] disabled:opacity-60'"
          >
            登录
          </button>
        </form>
      </div>

      <div v-else class="space-y-6">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div class="p-4 rounded-lg" :class="isDarkTheme ? 'bg-[#27272A]' : 'bg-white border border-gray-200'">
            <p class="text-sm" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">在线用户</p>
            <p class="text-2xl font-semibold mt-1" :class="isDarkTheme ? 'text-white' : 'text-gray-900'">{{ onlineCount }}</p>
          </div>
          <div class="p-4 rounded-lg" :class="isDarkTheme ? 'bg-[#27272A]' : 'bg-white border border-gray-200'">
            <p class="text-sm" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">用户总数</p>
            <p class="text-2xl font-semibold mt-1" :class="isDarkTheme ? 'text-white' : 'text-gray-900'">{{ users.length }}</p>
          </div>
          <div class="p-4 rounded-lg" :class="isDarkTheme ? 'bg-[#27272A]' : 'bg-white border border-gray-200'">
            <p class="text-sm" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">已封禁</p>
            <p class="text-2xl font-semibold mt-1" :class="isDarkTheme ? 'text-white' : 'text-gray-900'">{{ bannedCount }}</p>
          </div>
        </div>

        <div class="grid grid-cols-1 xl:grid-cols-3 gap-6">
          <div class="xl:col-span-1 space-y-4 overflow-y-auto">
            <h2 class="text-sm font-medium" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-700'">系统资源监控</h2>

            <div class="p-4 rounded-lg" :class="isDarkTheme ? 'bg-[#27272A]' : 'bg-white border border-gray-200'">
              <div class="flex items-center justify-between mb-3">
                <span class="text-sm" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">CPU 使用率</span>
                <span class="text-lg font-semibold" :class="isDarkTheme ? 'text-white' : 'text-gray-900'">{{ metrics ? metrics.cpuUsage.toFixed(1) : '--' }}%</span>
              </div>
              <div class="w-full h-2 rounded-full overflow-hidden" :class="isDarkTheme ? 'bg-gray-700' : 'bg-gray-200'">
                <div class="h-full transition-all duration-500 rounded-full" :class="metrics && metrics.cpuUsage > 80 ? 'bg-red-500' : (isDarkTheme ? 'bg-[#525252]' : 'bg-[#18181B]')" :style="{ width: (metrics ? metrics.cpuUsage : 0) + '%' }"></div>
              </div>
              <p class="text-xs mt-2" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">核心数: {{ metrics ? metrics.cpuCores : '--' }}</p>
            </div>

            <div class="p-4 rounded-lg" :class="isDarkTheme ? 'bg-[#27272A]' : 'bg-white border border-gray-200'">
              <div class="flex items-center justify-between mb-3">
                <span class="text-sm" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">内存使用率</span>
                <span class="text-lg font-semibold" :class="isDarkTheme ? 'text-white' : 'text-gray-900'">{{ metrics ? metrics.memoryUsage.toFixed(1) : '--' }}%</span>
              </div>
              <div class="w-full h-2 rounded-full overflow-hidden" :class="isDarkTheme ? 'bg-gray-700' : 'bg-gray-200'">
                <div class="h-full transition-all duration-500 rounded-full" :class="metrics && metrics.memoryUsage > 80 ? 'bg-red-500' : (isDarkTheme ? 'bg-[#525252]' : 'bg-[#18181B]')" :style="{ width: (metrics ? metrics.memoryUsage : 0) + '%' }"></div>
              </div>
              <div class="flex justify-between text-xs mt-2" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">
                <span>已用: {{ metrics ? formatMemory(metrics.usedMemory) : '--' }}</span>
                <span>总计: {{ metrics ? formatMemory(metrics.totalMemory) : '--' }}</span>
              </div>
            </div>

            <div class="p-4 rounded-lg" :class="isDarkTheme ? 'bg-[#27272A]' : 'bg-white border border-gray-200'">
              <span class="text-sm" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">系统运行时间</span>
              <p class="text-lg font-semibold mt-1" :class="isDarkTheme ? 'text-white' : 'text-gray-900'">{{ metrics ? formatUptime(metrics.uptime) : '--' }}</p>
            </div>
          </div>

          <div class="xl:col-span-2 space-y-6">
            <section>
              <div class="flex items-center justify-between mb-4">
                <h2 class="text-sm font-medium" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-700'">用户管理</h2>
                <span class="text-xs" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">在线状态按实时连接计算</span>
              </div>
              <div class="rounded-lg overflow-hidden" :class="isDarkTheme ? 'bg-[#1C1C1C] border border-gray-700/50' : 'bg-white border border-gray-200'">
                <div class="overflow-x-auto">
                  <table class="w-full text-sm">
                    <thead :class="isDarkTheme ? 'text-gray-400 bg-[#27272A]' : 'text-gray-500 bg-gray-50'">
                      <tr>
                        <th class="px-4 py-3 text-left">用户</th>
                        <th class="px-4 py-3 text-left">状态</th>
                        <th class="px-4 py-3 text-left">创建时间</th>
                        <th class="px-4 py-3 text-left">最后活动</th>
                        <th class="px-4 py-3 text-left">封禁原因</th>
                        <th class="px-4 py-3 text-left">操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-if="enrichedUsers.length === 0">
                        <td colspan="6" class="px-4 py-8 text-center" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">暂无用户</td>
                      </tr>
                      <tr v-for="adminUser in enrichedUsers" :key="adminUser.userId" class="border-t" :class="isDarkTheme ? 'border-gray-700/50' : 'border-gray-100'">
                        <td class="px-4 py-3 align-top">
                          <div class="space-y-2">
                            <div class="font-medium" :class="isDarkTheme ? 'text-white' : 'text-gray-900'">{{ adminUser.username }}</div>
                            <input
                              v-model="renameDrafts[adminUser.userId]"
                              type="text"
                              class="w-full px-3 py-2 rounded border outline-none"
                              :class="isDarkTheme ? 'bg-[#18181B] border-gray-600 text-white' : 'bg-white border-gray-300 text-gray-900'"
                            >
                          </div>
                        </td>
                        <td class="px-4 py-3 align-top">
                          <div class="space-y-2">
                            <span class="inline-flex items-center gap-2 px-2.5 py-1 rounded-full text-xs"
                              :class="onlineUserIds.has(adminUser.userId)
                                ? (isDarkTheme ? 'bg-green-500/20 text-green-400' : 'bg-green-100 text-green-600')
                                : (isDarkTheme ? 'bg-gray-700 text-gray-300' : 'bg-gray-100 text-gray-500')"
                            >
                              <span class="w-2 h-2 rounded-full" :class="onlineUserIds.has(adminUser.userId) ? 'bg-green-500' : 'bg-gray-400'"></span>
                              {{ onlineUserIds.has(adminUser.userId) ? '在线' : '离线' }}
                            </span>
                            <span v-if="adminUser.banned" class="inline-flex px-2.5 py-1 rounded-full text-xs" :class="isDarkTheme ? 'bg-red-500/20 text-red-400' : 'bg-red-100 text-red-600'">已封禁</span>
                          </div>
                        </td>
                        <td class="px-4 py-3 align-top" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-600'">{{ formatTimestamp(adminUser.createdAt) }}</td>
                        <td class="px-4 py-3 align-top" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-600'">{{ formatTimestamp(adminUser.lastSeen) }}</td>
                        <td class="px-4 py-3 align-top">
                          <textarea
                            v-model="banReasonDrafts[adminUser.userId]"
                            rows="2"
                            placeholder="可选：填写封禁原因"
                            class="w-full px-3 py-2 rounded border outline-none resize-none"
                            :class="isDarkTheme ? 'bg-[#18181B] border-gray-600 text-white placeholder:text-gray-500' : 'bg-white border-gray-300 text-gray-900 placeholder:text-gray-400'"
                          ></textarea>
                        </td>
                        <td class="px-4 py-3 align-top">
                          <div class="flex flex-col gap-2">
                            <button
                              @click="handleRenameUser(adminUser)"
                              class="px-3 py-2 rounded text-sm transition-colors"
                              :disabled="isSubmitting"
                              :class="isDarkTheme ? 'bg-[#18181B]/20 text-gray-400 hover:bg-[#18181B]/30 disabled:opacity-60' : 'bg-gray-100 text-[#18181B] hover:bg-gray-200 disabled:opacity-60'"
                            >
                              修改用户名
                            </button>
                            <button
                              v-if="!adminUser.banned"
                              @click="handleBanUser(adminUser)"
                              class="px-3 py-2 rounded text-sm transition-colors"
                              :disabled="isSubmitting"
                              :class="isDarkTheme ? 'bg-red-500/20 text-red-400 hover:bg-red-500/30 disabled:opacity-60' : 'bg-red-100 text-red-600 hover:bg-red-200 disabled:opacity-60'"
                            >
                              封禁用户
                            </button>
                            <button
                              v-else
                              @click="handleUnbanUser(adminUser)"
                              class="px-3 py-2 rounded text-sm transition-colors"
                              :disabled="isSubmitting"
                              :class="isDarkTheme ? 'bg-green-500/20 text-green-400 hover:bg-green-500/30 disabled:opacity-60' : 'bg-green-100 text-green-600 hover:bg-green-200 disabled:opacity-60'"
                            >
                              解封用户
                            </button>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </section>

            <section>
              <div class="flex items-center justify-between mb-4">
                <h2 class="text-sm font-medium" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-700'">实时日志监控 ({{ logs.length }} 条)</h2>
                <button
                  @click="handleClearLogs"
                  class="px-3 py-1.5 text-sm rounded transition-colors"
                  :class="isDarkTheme ? 'bg-red-500/20 text-red-400 hover:bg-red-500/30' : 'bg-red-100 text-red-600 hover:bg-red-200'"
                >
                  清空日志
                </button>
              </div>

              <div class="rounded-lg overflow-hidden flex flex-col h-[420px]" :class="isDarkTheme ? 'bg-[#1C1C1C] border border-gray-700/50' : 'bg-white border border-gray-200'">
                <div class="flex items-center gap-4 px-4 py-2 border-b text-xs" :class="isDarkTheme ? 'border-gray-700/50 text-gray-500' : 'border-gray-200 text-gray-500'">
                  <span class="w-16">级别</span>
                  <span class="w-24">时间</span>
                  <span>内容</span>
                </div>
                <div class="flex-1 overflow-y-auto p-2 space-y-1 font-mono text-sm">
                  <div v-if="logs.length === 0" class="flex items-center justify-center h-full" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">暂无日志</div>
                  <div
                    v-for="log in logs"
                    :key="log.lineNumber"
                    class="flex items-start gap-4 px-2 py-1.5 rounded"
                    :class="isDarkTheme ? 'hover:bg-gray-800/50' : 'hover:bg-gray-100'"
                  >
                    <span class="w-16 px-2 py-0.5 rounded text-xs font-medium text-center flex-shrink-0" :class="getLogLevelClass(log.level)">{{ log.level }}</span>
                    <span class="w-24 flex-shrink-0" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">{{ log.timestamp || '--:--:--' }}</span>
                    <span class="break-all" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-700'">{{ log.content }}</span>
                  </div>
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

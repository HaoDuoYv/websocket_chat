<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useGomokuWebSocket } from '@/composables/useGomokuWebSocket'
import type { GomokuRoom } from '@/composables/useGomokuWebSocket'

const router = useRouter()
const isDark = ref(localStorage.getItem('theme') === 'dark')

const {
  isConnected, rooms, gameState, currentRoomId,
  connect, disconnect, createRoom, joinRoom, spectateRoom
} = useGomokuWebSocket()

const isCreateDialogOpen = ref(false)
const isPasswordDialogOpen = ref(false)
const selectedRoomId = ref('')

const newRoomName = ref('')
const newRoomPassword = ref('')
const inputPassword = ref('')

const statusConfig: Record<string, { label: string; lightClass: string; darkClass: string }> = {
  WAITING: { label: '等待中', lightClass: 'bg-yellow-100 text-yellow-700', darkClass: 'bg-yellow-900/30 text-yellow-400' },
  PLAYING: { label: '对战中', lightClass: 'bg-green-100 text-green-700', darkClass: 'bg-green-900/30 text-green-400' },
  FINISHED: { label: '已结束', lightClass: 'bg-gray-100 text-gray-600', darkClass: 'bg-gray-700/50 text-gray-400' }
}

function getStatusBadge(status: string) {
  const config = statusConfig[status]
  if (!config) return { label: status, class: '' }
  return {
    label: config.label,
    class: isDark.value ? config.darkClass : config.lightClass
  }
}

function handleRoomClick(room: GomokuRoom) {
  if (room.playerCount >= 2) {
    spectateRoom(room.roomId)
    return
  }
  if (room.hasPassword) {
    selectedRoomId.value = room.roomId
    inputPassword.value = ''
    isPasswordDialogOpen.value = true
    return
  }
  joinRoom(room.roomId)
}

function handleCreateRoom() {
  if (!newRoomName.value.trim()) return
  createRoom(newRoomName.value.trim(), newRoomPassword.value.trim() || undefined)
  newRoomName.value = ''
  newRoomPassword.value = ''
  isCreateDialogOpen.value = false
}

function handlePasswordSubmit() {
  if (!inputPassword.value.trim()) return
  joinRoom(selectedRoomId.value, inputPassword.value.trim())
  isPasswordDialogOpen.value = false
  inputPassword.value = ''
}

function closeCreateDialog() {
  isCreateDialogOpen.value = false
  newRoomName.value = ''
  newRoomPassword.value = ''
}

function closePasswordDialog() {
  isPasswordDialogOpen.value = false
  inputPassword.value = ''
}

watch(gameState, (state) => {
  if (state && currentRoomId.value) {
    router.push(`/gomoku/${currentRoomId.value}`)
  }
})

onMounted(() => {
  const stored = localStorage.getItem('user')
  if (!stored) {
    router.push('/')
    return
  }
  try {
    const user = JSON.parse(stored)
    if (!user?.userId || !user?.username) {
      router.push('/')
      return
    }
    connect(user.userId, user.username)
  } catch {
    router.push('/')
  }
})

onUnmounted(() => {
  disconnect()
})
</script>

<template>
  <div class="min-h-screen transition-colors" :class="isDark ? 'bg-[#18181B] text-gray-100' : 'bg-white text-gray-800'">
    <!-- 顶部导航 -->
    <header class="border-b px-6 py-5" :class="isDark ? 'border-gray-800' : 'border-gray-100'">
      <div class="max-w-7xl mx-auto flex items-center gap-4">
        <button
          @click="router.push('/apps')"
          class="w-9 h-9 flex items-center justify-center rounded-xl transition-colors"
          :class="isDark ? 'hover:bg-gray-800 text-gray-400 hover:text-gray-200' : 'hover:bg-gray-100 text-gray-500 hover:text-gray-700'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="19" y1="12" x2="5" y2="12"/>
            <polyline points="12 19 5 12 12 5"/>
          </svg>
        </button>
        <div class="flex-1">
          <h1 class="text-lg font-semibold">五子棋对战大厅</h1>
        </div>
        <div class="flex items-center gap-3">
          <!-- 连接状态 -->
          <span class="flex items-center gap-1.5 text-xs" :class="isDark ? 'text-gray-400' : 'text-gray-500'">
            <span class="w-2 h-2 rounded-full" :class="isConnected ? 'bg-green-500' : 'bg-red-400'"></span>
            {{ isConnected ? '已连接' : '未连接' }}
          </span>
          <!-- 在线房间数 -->
          <span
            class="text-xs px-2.5 py-1 rounded-full"
            :class="isDark ? 'bg-gray-800 text-gray-400' : 'bg-gray-100 text-gray-500'"
          >
            {{ rooms.length }} 个房间
          </span>
          <!-- 创建房间按钮 -->
          <button
            @click="isCreateDialogOpen = true"
            class="px-4 py-2 text-xs font-medium rounded-xl bg-[#18181B] text-white hover:bg-[#27272A] transition-colors"
          >
            创建房间
          </button>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="max-w-7xl mx-auto px-6 py-8">
      <!-- 空状态 -->
      <div v-if="rooms.length === 0" class="flex flex-col items-center justify-center py-24">
        <div class="w-12 h-12 rounded-full flex items-center justify-center mb-4" :class="isDark ? 'bg-gray-800' : 'bg-gray-100'">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" :class="isDark ? 'text-gray-500' : 'text-gray-400'">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
          </svg>
        </div>
        <p class="text-sm" :class="isDark ? 'text-gray-500' : 'text-gray-400'">暂无房间，点击右上角创建一个吧</p>
      </div>

      <!-- 房间卡片网格 -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="room in rooms"
          :key="room.roomId"
          class="rounded-2xl border p-5 transition-all duration-200 hover:shadow-lg hover:-translate-y-0.5"
          :class="isDark ? 'border-gray-700 bg-[#27272A] hover:border-gray-600' : 'border-gray-200 bg-white hover:border-gray-300'"
        >
          <!-- 房间名称 + 状态标签 -->
          <div class="flex items-center gap-2 mb-3">
            <h3 class="font-medium text-sm truncate flex-1">{{ room.roomName }}</h3>
            <span
              :class="['inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium flex-shrink-0', getStatusBadge(room.status).class]"
            >
              {{ getStatusBadge(room.status).label }}
            </span>
          </div>

          <!-- 房间信息 -->
          <div class="flex items-center gap-4 text-xs mb-4" :class="isDark ? 'text-gray-400' : 'text-gray-500'">
            <span class="flex items-center gap-1">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
              {{ room.playerCount }}/2
            </span>
            <span v-if="room.hasPassword" class="flex items-center gap-1" title="需要密码">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </span>
            <span v-if="room.spectatorCount > 0" class="flex items-center gap-1">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              {{ room.spectatorCount }}
            </span>
          </div>

          <!-- 操作按钮 -->
          <button
            @click="handleRoomClick(room)"
            class="w-full px-3 py-2 text-xs font-medium rounded-xl transition-colors"
            :class="room.playerCount >= 2
              ? (isDark ? 'border border-gray-600 text-gray-300 hover:bg-gray-700' : 'border border-gray-300 text-gray-700 hover:bg-gray-50')
              : 'bg-[#18181B] text-white hover:bg-[#27272A]'"
          >
            {{ room.playerCount >= 2 ? '观战' : '加入对战' }}
          </button>
        </div>
      </div>
    </main>

    <!-- 创建房间对话框 -->
    <Teleport to="body">
      <div v-if="isCreateDialogOpen" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/50" @click="closeCreateDialog"></div>
        <div
          class="relative w-full max-w-md mx-4 rounded-2xl border p-6 shadow-xl"
          :class="isDark ? 'bg-[#27272A] border-gray-700' : 'bg-white border-gray-200'"
        >
          <h2 class="text-base font-semibold mb-5">创建房间</h2>
          <div class="space-y-4">
            <div>
              <label class="block text-xs font-medium mb-1.5" :class="isDark ? 'text-gray-300' : 'text-gray-700'">房间名称</label>
              <input
                v-model="newRoomName"
                type="text"
                placeholder="请输入房间名称"
                class="w-full px-3 py-2.5 text-sm rounded-xl border outline-none transition-colors"
                :class="isDark
                  ? 'bg-[#18181B] border-gray-600 text-gray-100 placeholder-gray-500 focus:border-gray-500'
                  : 'bg-gray-50 border-gray-200 text-gray-800 placeholder-gray-400 focus:border-gray-300'"
                @keyup.enter="handleCreateRoom"
              />
            </div>
            <div>
              <label class="block text-xs font-medium mb-1.5" :class="isDark ? 'text-gray-300' : 'text-gray-700'">密码（可选）</label>
              <input
                v-model="newRoomPassword"
                type="password"
                placeholder="留空则无密码"
                class="w-full px-3 py-2.5 text-sm rounded-xl border outline-none transition-colors"
                :class="isDark
                  ? 'bg-[#18181B] border-gray-600 text-gray-100 placeholder-gray-500 focus:border-gray-500'
                  : 'bg-gray-50 border-gray-200 text-gray-800 placeholder-gray-400 focus:border-gray-300'"
                @keyup.enter="handleCreateRoom"
              />
            </div>
          </div>
          <div class="flex gap-3 mt-6">
            <button
              @click="closeCreateDialog"
              class="flex-1 px-4 py-2.5 text-sm font-medium rounded-xl border transition-colors"
              :class="isDark
                ? 'border-gray-600 text-gray-300 hover:bg-gray-700'
                : 'border-gray-300 text-gray-700 hover:bg-gray-50'"
            >
              取消
            </button>
            <button
              @click="handleCreateRoom"
              :disabled="!newRoomName.trim()"
              class="flex-1 px-4 py-2.5 text-sm font-medium rounded-xl bg-[#18181B] text-white hover:bg-[#27272A] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              创建
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 密码输入对话框 -->
    <Teleport to="body">
      <div v-if="isPasswordDialogOpen" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/50" @click="closePasswordDialog"></div>
        <div
          class="relative w-full max-w-md mx-4 rounded-2xl border p-6 shadow-xl"
          :class="isDark ? 'bg-[#27272A] border-gray-700' : 'bg-white border-gray-200'"
        >
          <h2 class="text-base font-semibold mb-5">输入房间密码</h2>
          <input
            v-model="inputPassword"
            type="password"
            placeholder="请输入房间密码"
            class="w-full px-3 py-2.5 text-sm rounded-xl border outline-none transition-colors"
            :class="isDark
              ? 'bg-[#18181B] border-gray-600 text-gray-100 placeholder-gray-500 focus:border-gray-500'
              : 'bg-gray-50 border-gray-200 text-gray-800 placeholder-gray-400 focus:border-gray-300'"
            @keyup.enter="handlePasswordSubmit"
          />
          <div class="flex gap-3 mt-6">
            <button
              @click="closePasswordDialog"
              class="flex-1 px-4 py-2.5 text-sm font-medium rounded-xl border transition-colors"
              :class="isDark
                ? 'border-gray-600 text-gray-300 hover:bg-gray-700'
                : 'border-gray-300 text-gray-700 hover:bg-gray-50'"
            >
              取消
            </button>
            <button
              @click="handlePasswordSubmit"
              :disabled="!inputPassword.trim()"
              class="flex-1 px-4 py-2.5 text-sm font-medium rounded-xl bg-[#18181B] text-white hover:bg-[#27272A] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              加入
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

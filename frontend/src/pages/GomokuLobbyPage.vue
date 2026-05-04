<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useGomokuWebSocket } from '@/composables/useGomokuWebSocket'
import type { GomokuRoom } from '@/composables/useGomokuWebSocket'
import { useEditorWebSocket } from '@/composables/useEditorWebSocket'
import type { EditorRoom } from '@/composables/useEditorWebSocket'

const router = useRouter()
const isDark = ref(localStorage.getItem('theme') === 'dark')

// Gomoku WebSocket
const {
  isConnected: gomokuConnected, rooms: gomokuRooms, gameState, currentRoomId,
  connect: gomokuConnect, disconnect: gomokuDisconnect,
  createRoom: gomokuCreateRoom, joinRoom: gomokuJoinRoom, spectateRoom
} = useGomokuWebSocket()

// Editor WebSocket
const {
  isConnected: editorConnected, rooms: editorRooms,
  connectToLobby: editorConnectToLobby, disconnect: editorDisconnect,
  createRoom: editorCreateRoom, joinRoom: editorJoinRoom,
  setOnRoomJoined: editorSetOnRoomJoined
} = useEditorWebSocket()

const isCreateDialogOpen = ref(false)
const isPasswordDialogOpen = ref(false)
const selectedRoomId = ref('')
const selectedRoomType = ref<'gomoku' | 'editor'>('gomoku')

const newRoomName = ref('')
const newRoomPassword = ref('')
const inputPassword = ref('')
const newRoomType = ref<'gomoku' | 'editor'>('gomoku')

const editorJoinedDocId = ref('')

interface UnifiedRoom {
  id: string
  roomName: string
  hasPassword: boolean
  count: number
  countLabel: string
  status: string
  createdAt: number
  type: 'gomoku' | 'editor'
}

const gomokuStatusConfig: Record<string, { label: string; lightClass: string; darkClass: string }> = {
  WAITING: { label: '等待中', lightClass: 'bg-yellow-100 text-yellow-700', darkClass: 'bg-yellow-900/30 text-yellow-400' },
  PLAYING: { label: '对战中', lightClass: 'bg-green-100 text-green-700', darkClass: 'bg-green-900/30 text-green-400' },
  FINISHED: { label: '已结束', lightClass: 'bg-gray-100 text-gray-600', darkClass: 'bg-gray-700/50 text-gray-400' }
}

const editorStatusConfig = {
  label: '协作中',
  lightClass: 'bg-blue-100 text-blue-700',
  darkClass: 'bg-blue-900/30 text-blue-400'
}

const allRooms = computed<UnifiedRoom[]>(() => {
  const gomoku: UnifiedRoom[] = gomokuRooms.value.map((r: GomokuRoom) => ({
    id: r.roomId,
    roomName: r.roomName,
    hasPassword: r.hasPassword,
    count: r.playerCount,
    countLabel: `${r.playerCount}/2`,
    status: r.status,
    createdAt: 0,
    type: 'gomoku' as const
  }))

  const editor: UnifiedRoom[] = editorRooms.value.map((r: EditorRoom) => ({
    id: r.docId,
    roomName: r.roomName,
    hasPassword: r.hasPassword,
    count: r.participantCount,
    countLabel: `${r.participantCount} 人`,
    status: r.participantCount > 0 ? 'ACTIVE' : 'IDLE',
    createdAt: r.createdAt || 0,
    type: 'editor' as const
  }))

  return [...gomoku, ...editor].sort((a, b) => b.createdAt - a.createdAt)
})

function getGomokuStatusBadge(status: string) {
  const config = gomokuStatusConfig[status]
  if (!config) return { label: status, class: '' }
  return {
    label: config.label,
    class: isDark.value ? config.darkClass : config.lightClass
  }
}

function getEditorStatusBadge() {
  return {
    label: editorStatusConfig.label,
    class: isDark.value ? editorStatusConfig.darkClass : editorStatusConfig.lightClass
  }
}

function handleRoomClick(room: UnifiedRoom) {
  selectedRoomType.value = room.type

  if (room.type === 'editor') {
    if (room.hasPassword) {
      selectedRoomId.value = room.id
      inputPassword.value = ''
      isPasswordDialogOpen.value = true
      return
    }
    editorJoinRoom(room.id)
    return
  }

  // Gomoku
  if (room.count >= 2) {
    spectateRoom(room.id)
    return
  }
  if (room.hasPassword) {
    selectedRoomId.value = room.id
    inputPassword.value = ''
    isPasswordDialogOpen.value = true
    return
  }
  gomokuJoinRoom(room.id)
}

function handlePasswordSubmit() {
  if (!inputPassword.value.trim()) return
  if (selectedRoomType.value === 'editor') {
    editorJoinRoom(selectedRoomId.value, inputPassword.value.trim())
  } else {
    gomokuJoinRoom(selectedRoomId.value, inputPassword.value.trim())
  }
  isPasswordDialogOpen.value = false
  inputPassword.value = ''
}

function handleCreateRoom() {
  if (newRoomType.value === 'editor') {
    if (!newRoomName.value.trim()) return
    editorCreateRoom(newRoomName.value.trim(), newRoomPassword.value.trim() || undefined)
    newRoomName.value = ''
    newRoomPassword.value = ''
    isCreateDialogOpen.value = false
    return
  }

  if (!newRoomName.value.trim()) return
  gomokuCreateRoom(newRoomName.value.trim(), newRoomPassword.value.trim() || undefined)
  newRoomName.value = ''
  newRoomPassword.value = ''
  isCreateDialogOpen.value = false
}

function closeCreateDialog() {
  isCreateDialogOpen.value = false
  newRoomName.value = ''
  newRoomPassword.value = ''
  newRoomType.value = 'gomoku'
}

function closePasswordDialog() {
  isPasswordDialogOpen.value = false
  inputPassword.value = ''
}

// Navigate when gomoku room is created/joined
watch(gameState, (state) => {
  if (state && currentRoomId.value) {
    router.push(`/gomoku/${currentRoomId.value}`)
  }
})

// Navigate when editor room is created/joined
editorSetOnRoomJoined((docId: string) => {
  editorJoinedDocId.value = docId
})

watch(editorJoinedDocId, (docId) => {
  if (docId) {
    router.push(`/editor/${docId}`)
  }
})

onMounted(() => {
  const stored = localStorage.getItem('user')
  if (!stored) {
    router.push('/login')
    return
  }
  try {
    const user = JSON.parse(stored)
    if (!user?.userId || !user?.username) {
      router.push('/login')
      return
    }
    gomokuConnect(user.userId, user.username)
    editorConnectToLobby(user.userId, user.username)
  } catch {
    router.push('/login')
  }
})

onUnmounted(() => {
  gomokuDisconnect()
  editorDisconnect()
})
</script>

<template>
  <div class="min-h-screen transition-colors" :class="isDark ? 'bg-[#18181B] text-gray-100' : 'bg-white text-gray-800'">
    <!-- Header -->
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
          <h1 class="text-lg font-semibold">应用大厅</h1>
        </div>
        <div class="flex items-center gap-3">
          <!-- Connection status -->
          <span class="flex items-center gap-1.5 text-xs" :class="isDark ? 'text-gray-400' : 'text-gray-500'">
            <span class="w-2 h-2 rounded-full" :class="(gomokuConnected && editorConnected) ? 'bg-green-500' : 'bg-red-400'"></span>
            {{ (gomokuConnected && editorConnected) ? '已连接' : '连接中...' }}
          </span>
          <!-- Room count -->
          <span
            class="text-xs px-2.5 py-1 rounded-full"
            :class="isDark ? 'bg-gray-800 text-gray-400' : 'bg-gray-100 text-gray-500'"
          >
            {{ allRooms.length }} 个房间
          </span>
          <!-- Create button -->
          <button
            @click="isCreateDialogOpen = true"
            class="px-4 py-2 text-xs font-medium rounded-xl bg-[#18181B] text-white hover:bg-[#27272A] transition-colors"
          >
            创建房间
          </button>
        </div>
      </div>
    </header>

    <!-- Main -->
    <main class="max-w-7xl mx-auto px-6 py-8">
      <!-- Empty state -->
      <div v-if="allRooms.length === 0" class="flex flex-col items-center justify-center py-24">
        <div class="w-12 h-12 rounded-full flex items-center justify-center mb-4" :class="isDark ? 'bg-gray-800' : 'bg-gray-100'">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" :class="isDark ? 'text-gray-500' : 'text-gray-400'">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
          </svg>
        </div>
        <p class="text-sm" :class="isDark ? 'text-gray-500' : 'text-gray-400'">暂无房间，点击右上角创建一个吧</p>
      </div>

      <!-- Room grid -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="room in allRooms"
          :key="room.type + '-' + room.id"
          class="rounded-2xl border p-5 transition-all duration-200 hover:shadow-lg hover:-translate-y-0.5"
          :class="isDark ? 'border-gray-700 bg-[#27272A] hover:border-gray-600' : 'border-gray-200 bg-white hover:border-gray-300'"
        >
          <!-- Type badge + name + status -->
          <div class="flex items-center gap-2 mb-3">
            <span class="text-[10px] px-1.5 py-0.5 rounded font-medium tracking-wide uppercase"
              :class="room.type === 'gomoku'
                ? (isDark ? 'bg-gray-700 text-gray-400' : 'bg-gray-100 text-gray-500')
                : (isDark ? 'bg-blue-900/30 text-blue-400' : 'bg-blue-50 text-blue-600')"
            >{{ room.type === 'gomoku' ? '五子棋' : '编辑器' }}</span>
            <h3 class="font-medium text-sm truncate flex-1">{{ room.roomName }}</h3>
            <span
              :class="['inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium flex-shrink-0',
                room.type === 'gomoku' ? getGomokuStatusBadge(room.status).class : getEditorStatusBadge().class]"
            >
              {{ room.type === 'gomoku' ? getGomokuStatusBadge(room.status).label : getEditorStatusBadge().label }}
            </span>
          </div>

          <!-- Info -->
          <div class="flex items-center gap-4 text-xs mb-4" :class="isDark ? 'text-gray-400' : 'text-gray-500'">
            <span class="flex items-center gap-1">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
              {{ room.countLabel }}
            </span>
            <span v-if="room.hasPassword" class="flex items-center gap-1" title="需要密码">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </span>
          </div>

          <!-- Action button -->
          <button
            @click="handleRoomClick(room)"
            class="w-full px-3 py-2 text-xs font-medium rounded-xl transition-colors"
            :class="room.type === 'editor'
              ? 'bg-[#18181B] text-white hover:bg-[#27272A]'
              : (room.count >= 2
                ? (isDark ? 'border border-gray-600 text-gray-300 hover:bg-gray-700' : 'border border-gray-300 text-gray-700 hover:bg-gray-50')
                : 'bg-[#18181B] text-white hover:bg-[#27272A]')"
          >
            {{ room.type === 'editor' ? '加入编辑' : (room.count >= 2 ? '观战' : '加入对战') }}
          </button>
        </div>
      </div>
    </main>

    <!-- Create room dialog -->
    <Teleport to="body">
      <div v-if="isCreateDialogOpen" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/50" @click="closeCreateDialog"></div>
        <div
          class="relative w-full max-w-md mx-4 rounded-2xl border p-6 shadow-xl"
          :class="isDark ? 'bg-[#27272A] border-gray-700' : 'bg-white border-gray-200'"
        >
          <h2 class="text-base font-semibold mb-5">创建房间</h2>
          <div class="space-y-4">
            <!-- Room type selector -->
            <div>
              <label class="block text-xs font-medium mb-2" :class="isDark ? 'text-gray-300' : 'text-gray-700'">房间类型</label>
              <div class="grid grid-cols-2 gap-2">
                <button
                  @click="newRoomType = 'gomoku'"
                  class="px-3 py-3 rounded-xl border text-xs font-medium transition-all"
                  :class="newRoomType === 'gomoku'
                    ? 'bg-[#18181B] text-white border-[#18181B]'
                    : (isDark ? 'border-gray-600 text-gray-300 hover:border-gray-500' : 'border-gray-200 text-gray-600 hover:border-gray-300')"
                >
                  <div class="text-sm mb-0.5">五子棋</div>
                  <div class="text-[10px] opacity-60">实时对战</div>
                </button>
                <button
                  @click="newRoomType = 'editor'"
                  class="px-3 py-3 rounded-xl border text-xs font-medium transition-all"
                  :class="newRoomType === 'editor'
                    ? 'bg-[#18181B] text-white border-[#18181B]'
                    : (isDark ? 'border-gray-600 text-gray-300 hover:border-gray-500' : 'border-gray-200 text-gray-600 hover:border-gray-300')"
                >
                  <div class="text-sm mb-0.5">协作编辑</div>
                  <div class="text-[10px] opacity-60">多人编码</div>
                </button>
              </div>
            </div>

            <!-- Shared fields: name + password -->
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

    <!-- Password dialog -->
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

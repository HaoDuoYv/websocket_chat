<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useGomokuWebSocket } from '@/composables/useGomokuWebSocket'
import GomokuBoard from '@/components/GomokuBoard.vue'
import GomokuChat from '@/components/GomokuChat.vue'

const router = useRouter()
const route = useRoute()

const EMPTY_BOARD = Array.from({ length: 15 }, () => Array(15).fill(0))

const isDarkTheme = ref(localStorage.getItem('theme') === 'dark')

const {
  isConnected, gameState, chatMessages,
  myUserId, opponentDisconnected, opponentLeft, moveRejected, joinFailed,
  restartRequest, restartRejected,
  turnStartTime, turnTimeLimit, timeoutMessage,
  connect, makeMove,
  requestRestart, respondRestart, leaveRoom, sendChat,
  surrender, joinAsPlayer,
  rejoinRoom
} = useGomokuWebSocket()

// 用户认证
const user = ref<{ userId: string; username: string } | null>(null)

// 确认对话框状态
const confirmDialog = ref<{
  isOpen: boolean
  title: string
  message: string
  onConfirm: () => void
}>({
  isOpen: false,
  title: '',
  message: '',
  onConfirm: () => {}
})

// 提示消息
const toastMessage = ref('')
let toastTimer: ReturnType<typeof setTimeout> | null = null

// 移动端聊天面板展开
const isChatExpanded = ref(false)

// 当前回合文本
const currentTurnText = computed(() => {
  if (!gameState.value) return ''
  if (gameState.value.status === 'FINISHED') return ''
  if (gameState.value.status === 'WAITING') return '等待对手加入...'
  return gameState.value.currentTurn === 1 ? '黑方落子' : '白方落子'
})

// 胜负结果文本
const resultText = computed(() => {
  if (!gameState.value || gameState.value.status !== 'FINISHED') return ''
  if (gameState.value.winner === 0) return '平局！'
  return gameState.value.winner === 1 ? '黑方获胜！' : '白方获胜！'
})

// 是否为玩家（非观战者）
const isPlayer = computed(() => {
  return gameState.value?.myRole === 'black' || gameState.value?.myRole === 'white'
})

// 是否轮到我
const showToast = (message: string, duration = 3000) => {
  toastMessage.value = message
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastMessage.value = ''
  }, duration)
}

const showConfirmDialog = (title: string, message: string, onConfirm: () => void) => {
  confirmDialog.value = { isOpen: true, title, message, onConfirm }
}

const closeConfirmDialog = () => {
  confirmDialog.value.isOpen = false
}

const handleConfirm = () => {
  confirmDialog.value.onConfirm()
  closeConfirmDialog()
}

// 落子
const handleMove = (row: number, col: number) => {
  makeMove(row, col)
}

// 认输
const handleSurrender = () => {
  showConfirmDialog('认输', '确定要认输吗？此操作不可撤销。', () => {
    surrender()
  })
}

// 再来一局
const handleRequestRestart = () => {
  requestRestart()
}

// 离开房间
const handleLeaveRoom = () => {
  leaveRoom()
  router.push('/gomoku')
}

// 监听重新开始请求
watch(restartRequest, (request) => {
  if (!request) return
  if (request.fromUserId === myUserId.value) return
  showConfirmDialog(
    '重新开始',
    `${request.fromUsername} 请求再来一局，是否同意？`,
    () => respondRestart(true)
  )
  confirmDialog.value = {
    ...confirmDialog.value,
    onConfirm: () => {
      respondRestart(true)
      closeConfirmDialog()
    }
  }
})

// 监听重新开始被拒绝
watch(restartRejected, (rejected) => {
  if (rejected) {
    showToast('对方拒绝重新开始')
  }
})

// 监听落子被拒绝
watch(moveRejected, (reason) => {
  if (reason) {
    showToast(reason)
  }
})

// 聊天发送
const handleSendChat = (content: string) => {
  sendChat(content)
}

// 倒计时
const countdown = ref(0)
let countdownInterval: ReturnType<typeof setInterval> | null = null

const startCountdown = () => {
  if (countdownInterval) clearInterval(countdownInterval)
  countdownInterval = setInterval(() => {
    if (!gameState.value || gameState.value.status !== 'PLAYING' || turnStartTime.value === 0) {
      countdown.value = 0
      return
    }
    const elapsed = Math.floor((Date.now() - turnStartTime.value) / 1000)
    countdown.value = Math.max(0, Math.floor(turnTimeLimit / 1000) - elapsed)
  }, 1000)
}

const stopCountdown = () => {
  if (countdownInterval) {
    clearInterval(countdownInterval)
    countdownInterval = null
  }
  countdown.value = 0
}

// 监听超时提示
watch(timeoutMessage, (msg) => {
  if (msg) {
    showToast(msg)
    timeoutMessage.value = ''
  }
})

// 监听游戏状态变化启动/停止倒计时
watch(() => gameState.value?.status, (status) => {
  if (status === 'PLAYING') {
    startCountdown()
  } else {
    stopCountdown()
  }
})

watch(turnStartTime, () => {
  if (gameState.value?.status === 'PLAYING') {
    startCountdown()
  }
})

const canJoinAsPlayer = computed(() => {
  if (!gameState.value || gameState.value.myRole !== 'spectator') return false
  return !gameState.value.blackPlayer || !gameState.value.whitePlayer
})

const canStartGame = computed(() => {
  if (!gameState.value || !isPlayer.value) return false
  const hasBothPlayers = !!gameState.value.blackPlayer && !!gameState.value.whitePlayer
  return hasBothPlayers && (gameState.value.status === 'FINISHED' || gameState.value.status === 'WAITING')
})

const handleJoinAsPlayer = () => {
  joinAsPlayer()
}

watch(joinFailed, (val) => {
  if (val) router.push('/gomoku')
})

onMounted(() => {
  const userData = localStorage.getItem('user')
  if (!userData) {
    router.push('/')
    return
  }

  user.value = JSON.parse(userData)
  if (user.value) {
    connect(user.value.userId, user.value.username)
  }
})

// 连接后自动重新加入房间
watch(isConnected, (connected) => {
  if (connected && !gameState.value) {
    const roomId = route.params.roomId as string
    if (roomId) {
      rejoinRoom(roomId)
    }
  }
})

onUnmounted(() => {
  if (toastTimer) clearTimeout(toastTimer)
  stopCountdown()
})
</script>

<template>
  <div class="flex h-screen" :class="isDarkTheme ? 'bg-[#18181B]' : 'bg-white'">
    <div class="flex-1 flex flex-col">
      <!-- 顶部栏 -->
      <header class="px-4 py-3 border-b flex items-center justify-between" :class="isDarkTheme ? 'border-gray-800' : 'border-gray-100'">
        <div class="flex items-center gap-3">
          <button
            @click="handleLeaveRoom"
            class="w-8 h-8 flex items-center justify-center transition-colors"
            :class="isDarkTheme ? 'text-white hover:text-gray-200' : 'text-gray-400 hover:text-gray-600'"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
          </button>
          <div>
            <h1 class="text-sm font-medium" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">
              五子棋对战 - {{ gameState?.roomName || '加载中...' }}
            </h1>
            <p class="text-xs flex items-center gap-1" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">
              <span
                class="w-1.5 h-1.5 rounded-full"
                :class="isConnected ? 'bg-green-500' : 'bg-red-400'"
              ></span>
              {{ isConnected ? '在线' : '离线' }}
            </p>
          </div>
        </div>
      </header>

      <!-- 对手断线横幅 -->
      <div v-if="opponentDisconnected && !opponentLeft" class="px-4 py-2 text-center text-xs" :class="isDarkTheme ? 'bg-yellow-900/30 text-yellow-400' : 'bg-yellow-50 text-yellow-600'">
        对手已断线，等待重连...
      </div>
      <!-- 对手离开横幅 -->
      <div v-if="opponentLeft" class="px-4 py-2 text-center text-xs" :class="isDarkTheme ? 'bg-red-900/30 text-red-400' : 'bg-red-50 text-red-600'">
        对手已离开
      </div>

      <!-- 胜负结果横幅 -->
      <div v-if="gameState?.status === 'FINISHED'" class="px-4 py-2 text-center text-xs font-medium" :class="isDarkTheme ? 'bg-[#27272A] text-gray-200' : 'bg-gray-100 text-gray-800'">
        {{ resultText }}
      </div>

      <!-- 主内容区域 -->
      <div class="flex-1 flex flex-col lg:flex-row overflow-hidden">
        <!-- 棋盘区域 -->
        <div class="flex-1 flex items-center justify-center p-4">
          <div class="w-full max-w-[560px]">
            <!-- 玩家信息 - 桌面端在右侧，移动端在棋盘上方 -->
            <div class="lg:hidden mb-3 flex items-center justify-between text-xs" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">
              <div class="flex items-center gap-2">
                <span class="w-3 h-3 rounded-full bg-[#18181B] inline-block"></span>
                <span>{{ gameState?.blackPlayer?.username || '等待加入' }}</span>
                <span
                  v-if="gameState?.status === 'PLAYING' && gameState?.currentTurn === 1 && countdown > 0"
                  class="text-xs font-mono font-medium"
                  :class="countdown <= 10 ? 'text-red-500' : (isDarkTheme ? 'text-gray-400' : 'text-gray-500')"
                >{{ countdown }}s</span>
              </div>
              <div v-if="currentTurnText" class="font-medium" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-700'">
                {{ currentTurnText }}
              </div>
              <div class="flex items-center gap-2">
                <span>{{ gameState?.whitePlayer?.username || '等待加入' }}</span>
                <span
                  v-if="gameState?.status === 'PLAYING' && gameState?.currentTurn === 2 && countdown > 0"
                  class="text-xs font-mono font-medium"
                  :class="countdown <= 10 ? 'text-red-500' : (isDarkTheme ? 'text-gray-400' : 'text-gray-500')"
                >{{ countdown }}s</span>
                <span class="w-3 h-3 rounded-full border-2 inline-block" :class="isDarkTheme ? 'border-gray-400' : 'border-gray-500'"></span>
              </div>
              <div v-if="gameState?.spectators && gameState.spectators.length > 0" class="flex items-center gap-1" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">
                <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>
                <span>{{ gameState.spectators.length }}</span>
              </div>
            </div>

            <!-- 倒计时预警横幅 -->
            <div
              v-if="gameState?.status === 'PLAYING' && countdown > 0 && countdown <= 10"
              class="mb-2 flex items-center justify-center gap-2 rounded-xl px-4 py-2 animate-pulse bg-gradient-to-r from-red-600 to-red-500 text-white shadow-lg"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              <span class="text-base font-bold font-mono">{{ countdown }}s</span>
            </div>

            <GomokuBoard
              :board="gameState?.board || EMPTY_BOARD"
              :current-turn="gameState?.currentTurn || 0"
              :my-role="gameState?.myRole || null"
              :status="gameState?.status || 'WAITING'"
              :win-line="gameState?.winLine || null"
              :last-move="gameState?.lastMove || null"
              :is-dark="isDarkTheme"
              @move="handleMove"
            />
          </div>
        </div>

        <!-- 右侧面板 - 桌面端 -->
        <div class="hidden lg:flex w-72 border-l flex-col" :class="isDarkTheme ? 'border-gray-800 bg-[#27272A]' : 'border-gray-100 bg-gray-50'">
          <!-- 玩家信息 -->
          <div class="px-4 py-3 border-b" :class="isDarkTheme ? 'border-gray-700' : 'border-gray-200'">
            <div class="flex items-center gap-2 mb-2">
              <span class="w-4 h-4 rounded-full bg-[#18181B] inline-block"></span>
              <span class="text-xs font-medium" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">
                黑方: {{ gameState?.blackPlayer?.username || '等待加入' }}
              </span>
              <span
                v-if="gameState?.status === 'PLAYING' && gameState?.currentTurn === 1 && countdown > 0"
                class="text-xs font-mono font-medium"
                :class="countdown <= 10 ? 'text-red-500' : (isDarkTheme ? 'text-gray-400' : 'text-gray-500')"
              >{{ countdown }}s</span>
            </div>
            <div class="flex items-center gap-2 mb-2">
              <span class="w-4 h-4 rounded-full border-2 inline-block" :class="isDarkTheme ? 'border-gray-400' : 'border-gray-500'"></span>
              <span class="text-xs font-medium" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">
                白方: {{ gameState?.whitePlayer?.username || '等待加入' }}
              </span>
              <span
                v-if="gameState?.status === 'PLAYING' && gameState?.currentTurn === 2 && countdown > 0"
                class="text-xs font-mono font-medium"
                :class="countdown <= 10 ? 'text-red-500' : (isDarkTheme ? 'text-gray-400' : 'text-gray-500')"
              >{{ countdown }}s</span>
            </div>
            <div v-if="currentTurnText" class="text-xs mt-2" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">
              当前: {{ currentTurnText }}
            </div>
          </div>

          <!-- 观战者列表 -->
          <div v-if="gameState?.spectators && gameState.spectators.length > 0" class="px-4 py-3 border-b" :class="isDarkTheme ? 'border-gray-700' : 'border-gray-200'">
            <div class="text-xs font-medium mb-2" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">
              观战者 ({{ gameState.spectators.length }})
            </div>
            <div class="flex flex-wrap gap-1">
              <span
                v-for="spectator in gameState.spectators"
                :key="spectator.userId"
                class="inline-flex items-center px-2 py-0.5 text-xs"
                :class="isDarkTheme ? 'bg-gray-800 text-gray-300' : 'bg-gray-100 text-gray-600'"
              >
                {{ spectator.username }}
              </span>
            </div>
          </div>

          <!-- 聊天区域 -->
          <div class="flex-1 overflow-hidden">
            <GomokuChat
              :messages="chatMessages"
              :is-dark="isDarkTheme"
              @send="handleSendChat"
            />
          </div>
        </div>
      </div>

      <!-- 底部操作栏 -->
      <div class="px-4 py-3 border-t flex items-center gap-2" :class="isDarkTheme ? 'border-gray-800' : 'border-gray-100'">
        <button
          v-if="isPlayer && gameState?.status === 'PLAYING'"
          @click="handleSurrender"
          class="px-3 py-1.5 text-xs font-medium border transition-colors"
          :class="isDarkTheme
            ? 'border-gray-700 text-red-400 hover:bg-red-900/20'
            : 'border-gray-200 text-red-500 hover:bg-red-50'"
        >
          认输
        </button>
        <button
          v-if="canStartGame"
          @click="handleRequestRestart"
          class="px-3 py-1.5 text-xs font-medium border transition-colors"
          :class="isDarkTheme
            ? 'border-gray-700 text-gray-300 hover:bg-gray-800'
            : 'border-gray-200 text-gray-600 hover:bg-gray-50'"
        >
          开始游戏
        </button>
        <button
          v-if="canJoinAsPlayer"
          @click="handleJoinAsPlayer"
          class="px-3 py-1.5 text-xs font-medium border transition-colors"
          :class="isDarkTheme
            ? 'border-green-700 text-green-400 hover:bg-green-900/20'
            : 'border-green-200 text-green-600 hover:bg-green-50'"
        >
          加入对局
        </button>
        <div class="flex-1"></div>
        <!-- 移动端聊天展开按钮 -->
        <button
          class="lg:hidden px-3 py-1.5 text-xs font-medium border transition-colors"
          :class="isDarkTheme
            ? 'border-gray-700 text-gray-300 hover:bg-gray-800'
            : 'border-gray-200 text-gray-600 hover:bg-gray-50'"
          @click="isChatExpanded = !isChatExpanded"
        >
          {{ isChatExpanded ? '收起聊天' : '聊天' }}
        </button>
        <button
          @click="handleLeaveRoom"
          class="px-3 py-1.5 text-xs font-medium border transition-colors"
          :class="isDarkTheme
            ? 'border-gray-700 text-gray-300 hover:bg-gray-800'
            : 'border-gray-200 text-gray-600 hover:bg-gray-50'"
        >
          离开
        </button>
      </div>

      <!-- 移动端聊天面板 -->
      <div
        v-if="isChatExpanded"
        class="lg:hidden border-t h-64"
        :class="isDarkTheme ? 'border-gray-800 bg-[#27272A]' : 'border-gray-100 bg-gray-50'"
      >
        <GomokuChat
          :messages="chatMessages"
          :is-dark="isDarkTheme"
          @send="handleSendChat"
        />
      </div>
    </div>

    <!-- Toast 提示 -->
    <div
      v-if="toastMessage"
      class="fixed top-6 left-1/2 -translate-x-1/2 z-50 px-4 py-2 text-xs font-medium shadow-lg"
      :class="isDarkTheme
        ? 'bg-gray-800 text-gray-200 border border-gray-700'
        : 'bg-white text-gray-800 border border-gray-200'"
    >
      {{ toastMessage }}
    </div>

    <!-- 确认对话框 -->
    <div v-if="confirmDialog.isOpen" class="fixed inset-0 bg-black/50 flex items-center justify-center z-[60] p-4">
      <div class="w-full max-w-sm overflow-hidden" :class="isDarkTheme ? 'bg-[#27272A]' : 'bg-white'">
        <div class="px-5 py-4 border-b" :class="isDarkTheme ? 'border-gray-700' : 'border-gray-100'">
          <h3 class="text-sm font-medium" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">
            {{ confirmDialog.title }}
          </h3>
        </div>
        <div class="px-5 py-4">
          <p class="text-xs" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-600'">
            {{ confirmDialog.message }}
          </p>
        </div>
        <div class="px-5 py-3 flex gap-2" :class="isDarkTheme ? 'bg-gray-900/40' : 'bg-gray-50'">
          <button
            @click="() => {
              if (restartRequest) respondRestart(false)
              closeConfirmDialog()
            }"
            class="flex-1 px-3 py-2 text-xs font-medium border transition-colors"
            :class="isDarkTheme
              ? 'border-gray-700 text-gray-300 hover:bg-gray-800'
              : 'border-gray-200 text-gray-600 hover:bg-white'"
          >
            拒绝
          </button>
          <button
            @click="handleConfirm"
            class="flex-1 px-3 py-2 text-xs font-medium bg-[#18181B] text-white hover:bg-[#27272A] transition-colors"
          >
            同意
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import { useWebSocket } from '@/composables/useWebSocket'
import MentionNotification from '@/components/MentionNotification.vue'
import MessageList from '@/components/MessageList.vue'
import MessageInput from '@/components/MessageInput.vue'
import ChatSidebar from '@/components/ChatSidebar.vue'
import FilePreviewModal from '@/components/FilePreviewModal.vue'
import { uploadFile } from '@/api/file'

const PROJECT_NOTICE_STORAGE_KEY = 'project-notice-dismissed'
const projectNotice = {
  title: '关于本项目',
  summary: '这是一个基于 WebSocket 的即时聊天系统演示项目，适合学习实时通信、前后端分离与聊天场景设计。',
  highlights: [
    '支持私聊、群聊、文件传输、表情消息与管理员后台。',
    '前端使用 Vue 3 + TypeScript，后端基于 Spring Boot 与 WebSocket。',
    '更适合用于学习、演示和局域网环境体验，不建议直接作为生产方案使用。'
  ],
  links: [{ label: 'GitHub 项目地址', href: 'https://github.com/HaoDuoYv/websocket_chat' }]
}

interface PendingAttachment {
  id: string
  file: File
  previewUrl: string
  isImage: boolean
}

interface User {
  userId: string
  username: string
}

const route = useRoute()
const router = useRouter()
const currentRoomId = ref(route.params.chatId as string)
const roomId = computed(() => currentRoomId.value)
const user = ref<any>(null)
const messagesContainer = ref<InstanceType<typeof MessageList> | null>(null)
const messageInputRef = ref<InstanceType<typeof MessageInput> | null>(null)
const uploadError = ref('')
const isDraggingFile = ref(false)
const isSendingFiles = ref(false)
const uploadProgress = ref(0)
const uploadingFileName = ref('')
let dragDepth = 0

const { connect, sendMessage, sendFileMessage, messages, onlineUsers, setCurrentRoom, rooms, loadMessageHistory, sendMentionMessage, markMentionsAsRead, loadUnreadMentions, unreadMentionCount, latestMention, loadRoomMembers, roomMembers } = useWebSocket()

const isDarkTheme = ref(localStorage.getItem('theme') === 'dark')
const isProjectNoticeOpen = ref(false)
const previewingFile = ref<{ fileName: string; fileSize: number; fileUrl: string; fileType: string } | null>(null)

const toggleTheme = () => {
  isDarkTheme.value = !isDarkTheme.value
  localStorage.setItem('theme', isDarkTheme.value ? 'dark' : 'light')
}

const roomMessages = computed(() =>
  messages.value
    .filter(msg => String(msg.roomId) === currentRoomId.value)
    .sort((a, b) => a.seq - b.seq)
)

const currentRoom = computed(() => rooms.value.find(r => r.id === roomId.value))
const roomTitle = computed(() => currentRoom.value?.name || '聊天')
const onlineMemberCount = computed(() => onlineUsers.value.length)

const showUploadError = (error: string) => {
  uploadError.value = error
  window.setTimeout(() => { if (uploadError.value === error) uploadError.value = '' }, 3000)
}

const initChat = () => {
  const userData = localStorage.getItem('user')
  if (!userData) { router.push('/login'); return }
  user.value = JSON.parse(userData)
  connect(user.value, user.value.token)
  messagesContainer.value?.scrollToBottom()
}

onMounted(() => {
  initChat()
  setCurrentRoom(currentRoomId.value)
  loadMessageHistory(currentRoomId.value)
  loadUnreadMentions(currentRoomId.value)
  loadRoomMembers(currentRoomId.value)
})

onBeforeRouteUpdate((to, from, next) => {
  if (to.params.chatId !== from.params.chatId) {
    messageInputRef.value?.clearPendingAttachments()
    currentRoomId.value = to.params.chatId as string
    setCurrentRoom(currentRoomId.value)
    loadMessageHistory(currentRoomId.value)
    loadUnreadMentions(currentRoomId.value)
    loadRoomMembers(currentRoomId.value)
  }
  next()
})

const handleSendText = (content: string, mentions: User[] = [], mentionAll: boolean = false) => {
  if (!user.value || !roomId.value) return
  if (mentions.length > 0 || mentionAll) {
    sendMentionMessage(roomId.value, content, mentions, mentionAll)
  } else {
    sendMessage(roomId.value, content, user.value.userId)
  }
}

const handleMarkMentionsAsRead = () => {
  if (!roomId.value) return
  markMentionsAsRead(roomId.value)
}

const scrollToLatestMention = () => {
  const mention = latestMention.value[roomId.value]
  if (mention?.messageId) {
    messagesContainer.value?.scrollToMessage(String(mention.messageId))
  }
  handleMarkMentionsAsRead()
}

const handleSendFiles = async (attachments: PendingAttachment[]) => {
  if (!user.value || !roomId.value) return
  const failedIds = new Set<string>()
  isSendingFiles.value = true
  uploadProgress.value = 0
  try {
    for (const attachment of attachments) {
      uploadingFileName.value = attachment.file.name
      uploadProgress.value = 0
      try {
        const response = await uploadFile(attachment.file, roomId.value, user.value.userId, p => { uploadProgress.value = p })
        if (!response.success) {
          failedIds.add(attachment.id)
          showUploadError(response.message || `文件 ${attachment.file.name} 上传失败`)
          continue
        }
        sendFileMessage(roomId.value, user.value.userId, {
          fileId: response.fileId,
          fileName: response.fileName,
          fileUrl: response.fileUrl,
          fileSize: response.fileSize,
          fileType: response.fileType
        })
      } catch (error: any) {
        failedIds.add(attachment.id)
        showUploadError(error.message || `文件 ${attachment.file.name} 上传失败`)
      }
    }
  } finally {
    messageInputRef.value?.removeSentAttachments(new Set(attachments.filter(a => !failedIds.has(a.id)).map(a => a.id)))
    isSendingFiles.value = false
    uploadProgress.value = 0
    uploadingFileName.value = ''
  }
}

const hasDraggedFiles = (event: DragEvent) => event.dataTransfer?.types ? Array.from(event.dataTransfer.types).includes('Files') : false

const handleDragEnter = (event: DragEvent) => {
  if (!hasDraggedFiles(event)) return
  event.preventDefault()
  dragDepth += 1
  isDraggingFile.value = true
}

const handleDragOver = (event: DragEvent) => {
  if (!hasDraggedFiles(event)) return
  event.preventDefault()
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'copy'
  isDraggingFile.value = true
}

const handleDragLeave = (event: DragEvent) => {
  if (!hasDraggedFiles(event)) return
  event.preventDefault()
  dragDepth = Math.max(0, dragDepth - 1)
  if (dragDepth === 0) isDraggingFile.value = false
}

const handleDropUpload = async (event: DragEvent) => {
  if (!hasDraggedFiles(event)) return
  event.preventDefault()
  dragDepth = 0
  isDraggingFile.value = false
  const files = event.dataTransfer?.files
  if (files && files.length > 0) await messageInputRef.value?.uploadFiles(files)
}

const dismissProjectNotice = () => {
  localStorage.setItem(PROJECT_NOTICE_STORAGE_KEY, 'true')
  isProjectNoticeOpen.value = false
}

const openFilePreview = (file: { fileName: string; fileSize: number; fileUrl: string; fileType: string }) => {
  previewingFile.value = file
}
</script>

<template>
  <div class="flex h-screen" :class="isDarkTheme ? 'bg-[#18181B]' : 'bg-white'">
    <div
      class="relative flex-1 flex flex-col"
      @dragenter="handleDragEnter"
      @dragover="handleDragOver"
      @dragleave="handleDragLeave"
      @drop="handleDropUpload"
    >
      <header class="px-6 py-4 border-b flex items-center justify-between" :class="isDarkTheme ? 'border-gray-800' : 'border-gray-100'">
        <div class="flex items-center gap-4">
          <button
            @click="router.push('/login')"
            class="w-9 h-9 flex items-center justify-center transition-colors"
            :class="isDarkTheme ? 'text-white hover:text-gray-200' : 'text-gray-400 hover:text-gray-600'"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
          </button>
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 flex items-center justify-center text-white text-sm font-medium bg-[#18181B]">
              {{ roomTitle.charAt(0).toUpperCase() }}
            </div>
            <div>
              <h1 class="font-medium text-sm" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">{{ roomTitle }}</h1>
              <p class="text-xs text-[#737373] flex items-center gap-1">
                <span class="w-1.5 h-1.5 bg-[#737373] rounded-full"></span>
                {{ onlineMemberCount }} 人在线
              </p>
            </div>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <button
            @click="isProjectNoticeOpen = true"
            class="w-9 h-9 flex items-center justify-center transition-colors"
            :class="isDarkTheme ? 'text-white hover:text-gray-200' : 'text-[#18181B] hover:text-[#27272A]'"
            title="关于本项目"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>
            </svg>
          </button>
          <button
            @click="toggleTheme"
            class="w-9 h-9 flex items-center justify-center transition-colors"
            :class="isDarkTheme ? 'text-yellow-400 hover:text-yellow-300' : 'text-gray-400 hover:text-gray-600'"
            :title="isDarkTheme ? '切换到浅色模式' : '切换到深色模式'"
          >
            <svg v-if="isDarkTheme" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
            </svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
            </svg>
          </button>
        </div>
      </header>

      <MentionNotification
        v-if="unreadMentionCount[roomId] > 0"
        :count="unreadMentionCount[roomId]"
        :latest-mention="latestMention[roomId]"
        :is-dark="isDarkTheme"
        @click="scrollToLatestMention"
        @mark-read="handleMarkMentionsAsRead"
      />

      <MessageList
        ref="messagesContainer"
        :messages="roomMessages"
        :current-user-id="user?.userId || ''"
        :is-dark="isDarkTheme"
        @file-preview="openFilePreview"
      />

      <div v-if="uploadError" class="px-6 py-2" :class="isDarkTheme ? 'bg-red-900/20' : 'bg-red-50'">
        <p class="text-xs text-red-500">{{ uploadError }}</p>
      </div>

      <MessageInput
        ref="messageInputRef"
        :is-dark="isDarkTheme"
        :disabled="isSendingFiles"
        :online-users="roomMembers"
        :current-user-id="user?.userId"
        @send="handleSendText"
        @send-files="handleSendFiles"
        @error="showUploadError"
      />

      <div v-if="isSendingFiles" class="absolute inset-0 z-40 flex items-center justify-center bg-black/25 backdrop-blur-sm">
        <div class="w-80 rounded-2xl border px-5 py-4" :class="isDarkTheme ? 'border-gray-700 bg-gray-900 text-gray-100' : 'border-gray-200 bg-white text-gray-800'">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-sm font-medium">正在发送附件</p>
              <p class="mt-1 truncate text-xs opacity-70">{{ uploadingFileName || '准备中...' }}</p>
            </div>
            <span class="text-sm font-medium">{{ uploadProgress }}%</span>
          </div>
          <div class="mt-3 h-2 overflow-hidden rounded-full" :class="isDarkTheme ? 'bg-gray-800' : 'bg-gray-200'">
            <div class="h-full rounded-full bg-[#18181B] transition-all duration-200" :style="{ width: `${uploadProgress}%` }"></div>
          </div>
        </div>
      </div>

      <div v-if="isDraggingFile" class="absolute inset-0 z-40 flex items-center justify-center bg-[#18181B]/10 backdrop-blur-sm">
        <div class="rounded-2xl border-2 border-dashed px-8 py-10 text-center" :class="isDarkTheme ? 'border-[#525252] bg-gray-900/80 text-gray-100' : 'border-[#18181B] bg-white/95 text-gray-700'">
          <p class="text-base font-medium">松开发送图片或文件</p>
          <p class="mt-2 text-sm opacity-70">文件会先进入输入框，点击发送后再上传</p>
        </div>
      </div>
    </div>

    <ChatSidebar :users="onlineUsers" :is-dark="isDarkTheme" />
  </div>

  <Teleport to="body">
    <div v-if="isProjectNoticeOpen" class="fixed inset-0 z-[80] flex items-center justify-center bg-black/50 p-4" @click.self="isProjectNoticeOpen = false">
      <div class="w-full max-w-2xl overflow-hidden rounded-3xl border shadow-2xl" :class="isDarkTheme ? 'border-gray-700 bg-gray-900 text-gray-100' : 'border-gray-200 bg-white text-gray-800'">
        <div class="border-b px-6 py-5" :class="isDarkTheme ? 'border-gray-800' : 'border-gray-100'">
          <p class="text-xs font-semibold uppercase tracking-[0.25em] text-[#18181B]">关于本项目</p>
          <h2 class="mt-2 text-2xl font-semibold">{{ projectNotice.title }}</h2>
          <p class="mt-3 text-sm leading-6" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-600'">{{ projectNotice.summary }}</p>
        </div>
        <div class="px-6 py-5">
          <div class="space-y-3">
            <div v-for="(item, index) in projectNotice.highlights" :key="item" class="flex items-start gap-3 rounded-2xl px-4 py-3" :class="isDarkTheme ? 'bg-gray-800/70' : 'bg-gray-50'">
              <span class="mt-0.5 flex h-6 w-6 items-center justify-center rounded-full bg-[#18181B] text-xs font-semibold text-white">{{ index + 1 }}</span>
              <p class="text-sm leading-6">{{ item }}</p>
            </div>
          </div>
          <div class="mt-5 rounded-2xl border px-4 py-4" :class="isDarkTheme ? 'border-gray-700 bg-gray-950/40' : 'border-gray-200 bg-white'">
            <p class="text-sm font-medium">相关链接</p>
            <div class="mt-3 flex flex-col gap-2">
              <a v-for="link in projectNotice.links" :key="link.href" :href="link.href" target="_blank" rel="noopener noreferrer" class="inline-flex items-center gap-2 text-sm text-[#525252] hover:text-[#18181B] hover:underline break-all">
                <span>{{ link.label }}</span>
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 17 17 7"/><path d="M7 7h10v10"/></svg>
              </a>
            </div>
          </div>
        </div>
        <div class="flex items-center justify-end gap-3 border-t px-6 py-4" :class="isDarkTheme ? 'border-gray-800 bg-gray-950/40' : 'border-gray-100 bg-gray-50'">
          <button type="button" class="rounded-xl px-4 py-2 text-sm transition-colors" :class="isDarkTheme ? 'text-gray-300 hover:bg-gray-800' : 'text-gray-600 hover:bg-white'" @click="isProjectNoticeOpen = false">关闭</button>
          <button type="button" class="rounded-xl bg-[#18181B] px-4 py-2 text-sm text-white transition-colors hover:bg-[#27272A]" @click="dismissProjectNotice">今日不再提示</button>
        </div>
      </div>
    </div>
  </Teleport>

  <FilePreviewModal
    :file="previewingFile"
    :is-dark="isDarkTheme"
    @close="previewingFile = null"
  />
</template>

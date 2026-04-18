<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import { useWebSocket } from '@/composables/useWebSocket'
import FileMessage from '@/components/FileMessage.vue'
import FileUploadButton from '@/components/FileUploadButton.vue'
import { formatFileSize, getFileIcon, isImageFile, uploadFile } from '@/api/file'

const PROJECT_NOTICE_STORAGE_KEY = 'project-notice-dismissed'
const projectNotice = {
  title: '项目公告',
  summary: '这是一个基于 WebSocket 的即时聊天系统演示项目，适合学习实时通信、前后端分离与聊天场景设计。',
  highlights: [
    '支持私聊、群聊、文件传输、表情消息与管理员后台。',
    '前端使用 Vue 3 + TypeScript，后端基于 Spring Boot 与 WebSocket。',
    '更适合用于学习、演示和局域网环境体验，不建议直接作为生产方案使用。'
  ],
  links: [
    {
      label: 'GitHub 项目地址',
      href: 'https://github.com/HaoDuoYv/websocket_chat'
    }
  ]
}

interface PendingAttachment {
  id: string
  file: File
  previewUrl: string
  isImage: boolean
}

const route = useRoute()
const router = useRouter()
const currentRoomId = ref(route.params.chatId as string)
const roomId = computed(() => currentRoomId.value)
const user = ref<any>(null)
const newMessage = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const uploadError = ref('')
const showEmojiPicker = ref(false)
const isDraggingFile = ref(false)
const fileUploadButtonRef = ref<{ queueFiles: (files: File[] | FileList) => Promise<void> } | null>(null)
const pendingAttachments = ref<PendingAttachment[]>([])
const previewingAttachment = ref<PendingAttachment | null>(null)
const isSendingFiles = ref(false)
const uploadProgress = ref(0)
const uploadingFileName = ref('')
let dragDepth = 0

const { connect, sendMessage, sendFileMessage, messages, onlineUsers, setCurrentRoom, rooms, loadMessageHistory } = useWebSocket()

const isDarkTheme = ref(localStorage.getItem('theme') === 'dark')
const isProjectNoticeOpen = ref(false)
const hasShownProjectNotice = ref(false)

const toggleTheme = () => {
  isDarkTheme.value = !isDarkTheme.value
  localStorage.setItem('theme', isDarkTheme.value ? 'dark' : 'light')
}

const roomMessages = computed(() => {
  return messages.value
    .filter(msg => String(msg.roomId) === currentRoomId.value)
    .sort((a, b) => a.seq - b.seq)
})

const currentRoom = computed(() => rooms.value.find(r => r.id === roomId.value))
const roomTitle = computed(() => currentRoom.value?.name || '聊天')
const onlineMemberCount = computed(() => onlineUsers.value.length)
const canSend = computed(() => {
  return !!user.value && !!roomId.value && !isSendingFiles.value && (
    pendingAttachments.value.length > 0 || !!newMessage.value.trim()
  )
})

const getAvatarColor = (userId: string) => {
  const colors = ['#0891B2', '#0EA5E9', '#10B981', '#6366F1', '#64748B']
  let hash = 0
  for (let i = 0; i < userId.length; i++) {
    hash = userId.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

const getAvatarText = (name: string) => {
  return name ? name.charAt(0).toUpperCase() : '?'
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const showUploadError = (error: string) => {
  uploadError.value = error
  window.setTimeout(() => {
    if (uploadError.value === error) {
      uploadError.value = ''
    }
  }, 3000)
}

const revokeAttachment = (attachment: PendingAttachment) => {
  if (attachment.previewUrl) {
    URL.revokeObjectURL(attachment.previewUrl)
  }
}

const clearPendingAttachments = () => {
  pendingAttachments.value.forEach(revokeAttachment)
  pendingAttachments.value = []
  previewingAttachment.value = null
}

const buildAttachmentId = (file: File) => `${file.name}-${file.size}-${file.lastModified}`

const queuePendingFiles = (files: File[]) => {
  const knownIds = new Set(pendingAttachments.value.map(attachment => attachment.id))

  for (const file of files) {
    const id = buildAttachmentId(file)
    if (knownIds.has(id)) continue

    pendingAttachments.value.push({
      id,
      file,
      previewUrl: isImageFile(file.type) ? URL.createObjectURL(file) : '',
      isImage: isImageFile(file.type)
    })
    knownIds.add(id)
  }
}

const removePendingAttachment = (attachmentId: string) => {
  const index = pendingAttachments.value.findIndex(attachment => attachment.id === attachmentId)
  if (index === -1) return

  const [attachment] = pendingAttachments.value.splice(index, 1)
  if (previewingAttachment.value?.id === attachment.id) {
    previewingAttachment.value = null
  }
  revokeAttachment(attachment)
}

const openAttachmentPreview = (attachment: PendingAttachment) => {
  if (!attachment.isImage) return
  previewingAttachment.value = attachment
}

const closeAttachmentPreview = () => {
  previewingAttachment.value = null
}

const handleAttachmentPreviewKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && previewingAttachment.value) {
    closeAttachmentPreview()
  }
}

watch(previewingAttachment, attachment => {
  if (typeof document === 'undefined') return

  if (attachment) {
    document.body.style.overflow = 'hidden'
    return
  }

  document.body.style.overflow = ''
})

watch(roomMessages, () => {
  scrollToBottom()
}, { deep: true })

const showProjectNotice = () => {
  hasShownProjectNotice.value = true
  isProjectNoticeOpen.value = true
}

const closeProjectNotice = () => {
  isProjectNoticeOpen.value = false
}

const dismissProjectNotice = () => {
  localStorage.setItem(PROJECT_NOTICE_STORAGE_KEY, 'true')
  closeProjectNotice()
}

const initChat = () => {
  const userData = localStorage.getItem('user')
  if (!userData) {
    router.push('/')
    return
  }

  user.value = JSON.parse(userData)
  connect(user.value)
  scrollToBottom()
}

onMounted(() => {
  initChat()
  setCurrentRoom(currentRoomId.value)
  loadMessageHistory(currentRoomId.value)
  window.addEventListener('keydown', handleAttachmentPreviewKeydown)
})

onUnmounted(() => {
  clearPendingAttachments()
  document.body.style.overflow = ''
  window.removeEventListener('keydown', handleAttachmentPreviewKeydown)
})

onBeforeRouteUpdate((to, from, next) => {
  if (to.params.chatId !== from.params.chatId) {
    clearPendingAttachments()
    currentRoomId.value = to.params.chatId as string
    setCurrentRoom(currentRoomId.value)
    loadMessageHistory(currentRoomId.value)
  }
  next()
})

const sendPendingAttachments = async () => {
  if (!user.value || !roomId.value || pendingAttachments.value.length === 0) return

  const queuedAttachments = [...pendingAttachments.value]
  const failedIds = new Set<string>()

  isSendingFiles.value = true
  uploadProgress.value = 0

  try {
    for (const attachment of queuedAttachments) {
      uploadingFileName.value = attachment.file.name
      uploadProgress.value = 0

      try {
        const response = await uploadFile(
          attachment.file,
          roomId.value,
          user.value.userId,
          progress => {
            uploadProgress.value = progress
          }
        )

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
    const attachmentsToKeep: PendingAttachment[] = []
    for (const attachment of pendingAttachments.value) {
      if (failedIds.has(attachment.id)) {
        attachmentsToKeep.push(attachment)
      } else {
        revokeAttachment(attachment)
      }
    }

    pendingAttachments.value = attachmentsToKeep
    if (previewingAttachment.value && !failedIds.has(previewingAttachment.value.id)) {
      previewingAttachment.value = null
    }

    isSendingFiles.value = false
    uploadProgress.value = 0
    uploadingFileName.value = ''
  }
}

const handleSendMessage = async () => {
  if (!canSend.value || !user.value) return

  const content = newMessage.value.trim()
  if (content) {
    sendMessage(roomId.value, content, user.value.userId)
    newMessage.value = ''
  }

  showEmojiPicker.value = false

  if (pendingAttachments.value.length > 0) {
    await sendPendingAttachments()
  }
}

const handleBack = () => {
  router.push('/')
}

const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const formatDate = (timestamp: number) => {
  const date = new Date(timestamp)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)

  if (date.toDateString() === today.toDateString()) {
    return '今天'
  }
  if (date.toDateString() === yesterday.toDateString()) {
    return '昨天'
  }
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const shouldShowDate = (index: number) => {
  if (index === 0) return true
  const current = roomMessages.value[index]
  const prev = roomMessages.value[index - 1]
  return new Date(current.timestamp).toDateString() !== new Date(prev.timestamp).toDateString()
}

const shouldShowTime = (index: number) => {
  if (index === 0) return true
  const current = roomMessages.value[index]
  const prev = roomMessages.value[index - 1]
  return current.timestamp - prev.timestamp > 5 * 60 * 1000
}

const emojis = ['😀', '😂', '🥰', '😎', '🤔', '👍', '❤️', '🎉', '🔥', '👏', '🙏', '✅', '❌', '💡']

const insertEmoji = (emoji: string) => {
  newMessage.value += emoji
}

const uploadFiles = async (files: File[] | FileList) => {
  if (!user.value || !roomId.value) return
  await fileUploadButtonRef.value?.queueFiles(files)
}

const hasDraggedFiles = (event: DragEvent) => {
  const types = event.dataTransfer?.types
  return types ? Array.from(types).includes('Files') : false
}

const handlePasteUpload = async (event: ClipboardEvent) => {
  const files = Array.from(event.clipboardData?.files || []).filter(file => file.type.startsWith('image/'))
  if (files.length === 0) return

  event.preventDefault()
  await uploadFiles(files)
}

const handleDragEnter = (event: DragEvent) => {
  if (!hasDraggedFiles(event)) return

  event.preventDefault()
  dragDepth += 1
  isDraggingFile.value = true
}

const handleDragOver = (event: DragEvent) => {
  if (!hasDraggedFiles(event)) return

  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy'
  }
  isDraggingFile.value = true
}

const handleDragLeave = (event: DragEvent) => {
  if (!hasDraggedFiles(event)) return

  event.preventDefault()
  dragDepth = Math.max(0, dragDepth - 1)
  if (dragDepth === 0) {
    isDraggingFile.value = false
  }
}

const handleDropUpload = async (event: DragEvent) => {
  if (!hasDraggedFiles(event)) return

  event.preventDefault()
  dragDepth = 0
  isDraggingFile.value = false

  const files = event.dataTransfer?.files
  if (files && files.length > 0) {
    await uploadFiles(files)
  }
}

const isImageMessage = (message: { type?: string; fileType?: string }) => {
  return message.type === 'file' && isImageFile(message.fileType || '')
}
</script>

<template>
  <div class="flex h-screen" :class="isDarkTheme ? 'bg-[#0F172A]' : 'bg-white'">
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
            @click="handleBack"
            class="w-9 h-9 flex items-center justify-center transition-colors"
            :class="isDarkTheme ? 'text-gray-500 hover:text-gray-300' : 'text-gray-400 hover:text-gray-600'"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
          </button>

          <div class="flex items-center gap-3">
            <div
              class="w-10 h-10 flex items-center justify-center text-white text-sm font-medium"
              :style="{ backgroundColor: getAvatarColor(roomId) }"
            >
              {{ getAvatarText(roomTitle) }}
            </div>
            <div>
              <h1 class="font-medium text-sm" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">{{ roomTitle }}</h1>
              <p class="text-xs text-[#22C55E] flex items-center gap-1">
                <span class="w-1.5 h-1.5 bg-[#22C55E] rounded-full"></span>
                {{ onlineMemberCount }} 人在线
              </p>
            </div>
          </div>
        </div>

        <div class="flex items-center gap-2">
          <button
            @click="showProjectNotice"
            class="w-9 h-9 flex items-center justify-center transition-colors"
            :class="isDarkTheme ? 'text-cyan-400 hover:text-cyan-300' : 'text-[#0891B2] hover:text-[#0e7490]'"
            title="项目公告"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 11.5a2.5 2.5 0 0 1 2.5-2.5H9l4.5-3v12L9 15H6.5A2.5 2.5 0 0 1 4 12.5z"/>
              <path d="M14 9a4 4 0 0 1 0 6"/>
              <path d="M16 7a7 7 0 0 1 0 10"/>
            </svg>
          </button>

          <button
            @click="toggleTheme"
            class="w-9 h-9 flex items-center justify-center transition-colors"
            :class="isDarkTheme ? 'text-yellow-400 hover:text-yellow-300' : 'text-gray-400 hover:text-gray-600'"
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

          <button class="w-9 h-9 flex items-center justify-center transition-colors" :class="isDarkTheme ? 'text-gray-500 hover:text-gray-300' : 'text-gray-400 hover:text-gray-600'">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="1"/>
              <circle cx="19" cy="12" r="1"/>
              <circle cx="5" cy="12" r="1"/>
            </svg>
          </button>
        </div>
      </header>

      <div ref="messagesContainer" class="flex-1 overflow-y-auto px-6 py-6">
        <div v-if="roomMessages.length === 0" class="flex flex-col items-center justify-center h-full" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-300'">
          <div class="w-14 h-14 bg-[#0891B2] flex items-center justify-center mb-4">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
          </div>
          <p class="text-sm" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-400'">还没有消息</p>
          <p class="text-xs mt-1" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-300'">发送第一条消息开始聊天吧</p>
        </div>

        <template v-else>
          <template v-for="(message, index) in roomMessages" :key="message.id">
            <div v-if="shouldShowDate(index)" class="flex justify-center my-6">
              <span class="text-xs" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-300'">{{ formatDate(message.timestamp) }}</span>
            </div>

            <div v-else-if="shouldShowTime(index)" class="flex justify-center my-3">
              <span class="text-xs" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-300'">{{ formatTime(message.timestamp) }}</span>
            </div>

            <div
              :class="[
                'flex gap-3 mb-4',
                String(message.senderId) === user?.userId ? 'flex-row-reverse' : 'flex-row'
              ]"
            >
              <div
                class="w-8 h-8 flex-shrink-0 flex items-center justify-center text-white text-xs font-medium"
                :style="{ backgroundColor: getAvatarColor(String(message.senderId)) }"
              >
                {{ getAvatarText(message.senderName) }}
              </div>

              <div :class="['flex flex-col max-w-[65%]', String(message.senderId) === user?.userId ? 'items-end' : 'items-start']">
                <div v-if="String(message.senderId) !== user?.userId" class="text-xs mb-1" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">
                  {{ message.senderName }}
                </div>

                <div
                  :class="[
                    isImageMessage(message) ? 'px-1 py-1' : 'px-4 py-2 text-sm',
                    String(message.senderId) === user?.userId
                      ? 'bg-[#0891B2] text-white'
                      : (isDarkTheme ? 'bg-gray-800 text-gray-200' : 'bg-gray-100 text-gray-700')
                  ]"
                >
                  <div v-if="message.type === 'file' && message.fileId" class="min-w-[200px]">
                    <FileMessage
                      :file-name="message.fileName || '未命名文件'"
                      :file-size="message.fileSize || 0"
                      :file-url="message.fileUrl || ''"
                      :file-type="message.fileType || ''"
                      :is-dark="isDarkTheme"
                    />
                  </div>

                  <div v-else>{{ message.content }}</div>
                </div>

                <div class="text-xs mt-1" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-300'">{{ formatTime(message.timestamp) }}</div>
              </div>
            </div>
          </template>
        </template>
      </div>

      <div v-if="uploadError" class="px-6 py-2" :class="isDarkTheme ? 'bg-red-900/20' : 'bg-red-50'">
        <p class="text-xs text-red-500">{{ uploadError }}</p>
      </div>

      <div class="px-6 py-4 border-t" :class="isDarkTheme ? 'border-gray-800' : 'border-gray-100'">
        <div v-if="showEmojiPicker" class="mb-3">
          <div class="flex flex-wrap gap-1">
            <button
              v-for="emoji in emojis"
              :key="emoji"
              @click="insertEmoji(emoji)"
              class="w-8 h-8 text-base transition-colors"
              :class="isDarkTheme ? 'hover:bg-gray-800' : 'hover:bg-gray-100'"
            >
              {{ emoji }}
            </button>
          </div>
        </div>

        <div v-if="pendingAttachments.length > 0" class="mb-3 flex gap-3 overflow-x-auto pb-1">
          <div
            v-for="attachment in pendingAttachments"
            :key="attachment.id"
            class="group relative shrink-0 overflow-hidden rounded-2xl border"
            :class="isDarkTheme ? 'border-gray-700 bg-gray-800/80' : 'border-gray-200 bg-gray-50'"
          >
            <button
              v-if="attachment.isImage"
              type="button"
              class="block h-24 w-24 cursor-zoom-in overflow-hidden transition-transform duration-200 hover:scale-[1.02]"
              @click="openAttachmentPreview(attachment)"
            >
              <img :src="attachment.previewUrl" :alt="attachment.file.name" class="h-full w-full object-cover" />
            </button>
            <div v-else class="flex h-24 w-56 items-center gap-3 px-4">
              <div class="text-3xl">{{ getFileIcon(attachment.file.name) }}</div>
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-medium" :class="isDarkTheme ? 'text-gray-100' : 'text-gray-800'">{{ attachment.file.name }}</p>
                <p class="mt-1 text-xs" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">{{ formatFileSize(attachment.file.size) }}</p>
              </div>
            </div>

            <button
              type="button"
              class="absolute right-2 top-2 flex h-6 w-6 items-center justify-center rounded-full bg-black/60 text-xs text-white opacity-0 transition group-hover:opacity-100"
              @click="removePendingAttachment(attachment.id)"
            >
              ×
            </button>

            <div v-if="attachment.isImage" class="absolute inset-x-0 bottom-0 bg-black/50 px-2 py-1 text-white">
              <p class="truncate text-xs">{{ attachment.file.name }}</p>
              <p class="text-[11px] opacity-80">{{ formatFileSize(attachment.file.size) }} · 点击预览</p>
            </div>
          </div>
        </div>

        <div class="flex items-center gap-2">
          <button
            @click="showEmojiPicker = !showEmojiPicker"
            class="w-8 h-8 flex items-center justify-center transition-colors"
            :class="isDarkTheme ? 'text-gray-500 hover:text-gray-300' : 'text-gray-400 hover:text-gray-600'"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <path d="M8 14s1.5 2 4 2 4-2 4-2"/>
              <line x1="9" y1="9" x2="9.01" y2="9"/>
              <line x1="15" y1="9" x2="15.01" y2="9"/>
            </svg>
          </button>

          <FileUploadButton
            ref="fileUploadButtonRef"
            :is-dark="isDarkTheme"
            :disabled="isSendingFiles"
            @files-selected="queuePendingFiles"
            @upload-error="showUploadError"
          />

          <div class="flex-1 relative">
            <input
              v-model="newMessage"
              @paste="handlePasteUpload"
              @keyup.enter="handleSendMessage"
              type="text"
              placeholder="输入消息"
              class="w-full px-0 py-2 bg-transparent border-0 border-b text-sm focus:outline-none focus:border-[#0891B2] transition-colors"
              :class="isDarkTheme ? 'border-gray-700 text-gray-200 placeholder-gray-500' : 'border-gray-200 text-gray-700 placeholder-gray-400'"
            />
          </div>

          <button
            @click="handleSendMessage"
            :disabled="!canSend"
            class="w-8 h-8 bg-[#0891B2] hover:bg-[#0E7490] text-white flex items-center justify-center transition-colors"
            :class="isDarkTheme ? 'disabled:bg-gray-800' : 'disabled:bg-gray-200'"
          >
            <svg v-if="!isSendingFiles" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="22" y1="2" x2="11" y2="13"/>
              <polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
            <svg v-else class="animate-spin" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
            </svg>
          </button>
        </div>
      </div>

      <div
        v-if="isSendingFiles"
        class="absolute inset-0 z-40 flex items-center justify-center bg-black/25 backdrop-blur-sm"
      >
        <div class="w-80 rounded-2xl border px-5 py-4" :class="isDarkTheme ? 'border-gray-700 bg-slate-900 text-slate-100' : 'border-gray-200 bg-white text-slate-800'">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-sm font-medium">正在发送附件</p>
              <p class="mt-1 truncate text-xs opacity-70">{{ uploadingFileName || '准备中...' }}</p>
            </div>
            <span class="text-sm font-medium">{{ uploadProgress }}%</span>
          </div>
          <div class="mt-3 h-2 overflow-hidden rounded-full" :class="isDarkTheme ? 'bg-slate-800' : 'bg-slate-200'">
            <div class="h-full rounded-full bg-[#0891B2] transition-all duration-200" :style="{ width: `${uploadProgress}%` }"></div>
          </div>
        </div>
      </div>

      <div
        v-if="isDraggingFile"
        class="absolute inset-0 z-40 flex items-center justify-center bg-[#0891B2]/10 backdrop-blur-sm"
      >
        <div
          class="rounded-2xl border-2 border-dashed px-8 py-10 text-center"
          :class="isDarkTheme ? 'border-[#38BDF8] bg-slate-900/80 text-slate-100' : 'border-[#0891B2] bg-white/95 text-slate-700'"
        >
          <p class="text-base font-medium">松开发送图片或文件</p>
          <p class="mt-2 text-sm opacity-70">文件会先进入输入框，点击发送后再上传</p>
        </div>
      </div>
    </div>

    <aside class="w-60 border-l hidden lg:flex flex-col" :class="isDarkTheme ? 'border-gray-800 bg-[#1E293B]' : 'border-gray-100 bg-white'">
      <div class="px-5 py-4 border-b" :class="isDarkTheme ? 'border-gray-800' : 'border-gray-100'">
        <h3 class="text-sm font-medium" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">在线成员</h3>
        <p class="text-xs mt-1" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">{{ onlineUsers.length }} 人在线</p>
      </div>

      <div class="flex-1 overflow-y-auto py-2">
        <div
          v-for="onlineUser in onlineUsers"
          :key="onlineUser.userId"
          class="flex items-center gap-3 px-5 py-2.5 transition-colors"
          :class="isDarkTheme ? 'hover:bg-gray-800' : 'hover:bg-gray-50'"
        >
          <div class="relative">
            <div
              class="w-8 h-8 flex items-center justify-center text-white text-xs font-medium"
              :style="{ backgroundColor: getAvatarColor(onlineUser.userId) }"
            >
              {{ getAvatarText(onlineUser.username) }}
            </div>
            <div class="absolute bottom-0 right-0 w-2 h-2 bg-[#22C55E] border-2 rounded-full" :class="isDarkTheme ? 'border-gray-800' : 'border-white'"></div>
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm truncate" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">{{ onlineUser.username }}</p>
            <p class="text-xs text-[#22C55E]">在线</p>
          </div>
        </div>
      </div>
    </aside>
  </div>

  <Teleport to="body">
    <div
      v-if="isProjectNoticeOpen"
      class="fixed inset-0 z-[80] flex items-center justify-center bg-black/50 p-4"
      @click.self="closeProjectNotice"
    >
      <div class="w-full max-w-2xl overflow-hidden rounded-3xl border shadow-2xl" :class="isDarkTheme ? 'border-slate-700 bg-slate-900 text-slate-100' : 'border-slate-200 bg-white text-slate-800'">
        <div class="border-b px-6 py-5" :class="isDarkTheme ? 'border-slate-800' : 'border-slate-100'">
          <p class="text-xs font-semibold uppercase tracking-[0.25em] text-[#0891B2]">项目公告</p>
          <h2 class="mt-2 text-2xl font-semibold">{{ projectNotice.title }}</h2>
          <p class="mt-3 text-sm leading-6" :class="isDarkTheme ? 'text-slate-300' : 'text-slate-600'">{{ projectNotice.summary }}</p>
        </div>
        <div class="px-6 py-5">
          <div class="space-y-3">
            <div
              v-for="(item, index) in projectNotice.highlights"
              :key="item"
              class="flex items-start gap-3 rounded-2xl px-4 py-3"
              :class="isDarkTheme ? 'bg-slate-800/70' : 'bg-slate-50'"
            >
              <span class="mt-0.5 flex h-6 w-6 items-center justify-center rounded-full bg-[#0891B2] text-xs font-semibold text-white">{{ index + 1 }}</span>
              <p class="text-sm leading-6">{{ item }}</p>
            </div>
          </div>

          <div class="mt-5 rounded-2xl border px-4 py-4" :class="isDarkTheme ? 'border-slate-700 bg-slate-950/40' : 'border-slate-200 bg-white'">
            <p class="text-sm font-medium">相关链接</p>
            <div class="mt-3 flex flex-col gap-2">
              <a
                v-for="link in projectNotice.links"
                :key="link.href"
                :href="link.href"
                target="_blank"
                rel="noopener noreferrer"
                class="inline-flex items-center gap-2 text-sm text-[#0891B2] hover:underline break-all"
              >
                <span>{{ link.label }}</span>
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M7 17 17 7"/>
                  <path d="M7 7h10v10"/>
                </svg>
              </a>
            </div>
          </div>
        </div>
        <div class="flex items-center justify-end gap-3 border-t px-6 py-4" :class="isDarkTheme ? 'border-slate-800 bg-slate-950/40' : 'border-slate-100 bg-slate-50'">
          <button
            type="button"
            class="rounded-xl px-4 py-2 text-sm transition-colors"
            :class="isDarkTheme ? 'text-slate-300 hover:bg-slate-800' : 'text-slate-600 hover:bg-white'"
            @click="closeProjectNotice"
          >
            关闭
          </button>
          <button
            type="button"
            class="rounded-xl bg-[#0891B2] px-4 py-2 text-sm text-white transition-colors hover:bg-[#0e7490]"
            @click="dismissProjectNotice"
          >
            今日不再提示
          </button>
        </div>
      </div>
    </div>
  </Teleport>

  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="previewingAttachment"
        class="fixed inset-0 z-[70] flex items-center justify-center bg-black/90 p-4"
        @click.self="closeAttachmentPreview"
      >
        <div class="absolute inset-x-0 top-0 flex items-center justify-between gap-4 bg-gradient-to-b from-black/60 to-transparent px-4 py-4 text-white">
          <p class="min-w-0 truncate text-sm font-medium" :title="previewingAttachment.file.name">
            {{ previewingAttachment.file.name }}
          </p>
          <button
            type="button"
            class="shrink-0 rounded px-3 py-2 text-sm transition-colors hover:bg-white/10"
            @click="closeAttachmentPreview"
          >
            关闭
          </button>
        </div>
        <img
          :src="previewingAttachment.previewUrl"
          :alt="previewingAttachment.file.name"
          class="max-h-full max-w-full rounded object-contain transition duration-200 ease-out"
        />
      </div>
    </Transition>
  </Teleport>
</template>

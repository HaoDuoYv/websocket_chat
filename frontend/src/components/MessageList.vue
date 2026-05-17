<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import FileMessage from '@/components/FileMessage.vue'
import { isImageFile } from '@/api/file'

interface Message {
  id: string
  roomId: string
  content: string
  senderId: string
  senderName: string
  timestamp: number
  seq: number
  type?: 'text' | 'file' | 'system'
  fileId?: string
  fileName?: string
  fileUrl?: string
  fileSize?: number
  fileType?: string
}

const props = defineProps<{
  messages: Message[]
  currentUserId: string
  isDark: boolean
}>()

const emit = defineEmits<{
  filePreview: [file: { fileName: string; fileSize: number; fileUrl: string; fileType: string }]
}>()

const container = ref<HTMLElement | null>(null)

const getAvatarColor = (userId: string) => {
  const colors = ['#18181B', '#3F3F46', '#52525B', '#71717A', '#A1A1AA', '#27272A', '#525252', '#737373']
  let hash = 0
  for (let i = 0; i < userId.length; i++) {
    hash = userId.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

const getAvatarText = (name: string) => name ? name.charAt(0).toUpperCase() : '?'

const formatTime = (timestamp: number) =>
  new Date(timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

const formatDate = (timestamp: number) => {
  const date = new Date(timestamp)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date.toDateString() === today.toDateString()) return '今天'
  if (date.toDateString() === yesterday.toDateString()) return '昨天'
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const shouldShowDate = (index: number) => {
  if (index === 0) return true
  return new Date(props.messages[index].timestamp).toDateString() !==
    new Date(props.messages[index - 1].timestamp).toDateString()
}

const shouldShowTime = (index: number) => {
  if (index === 0) return true
  return props.messages[index].timestamp - props.messages[index - 1].timestamp > 5 * 60 * 1000
}

const isImageMessage = (msg: Message) =>
  msg.type === 'file' && isImageFile(msg.fileType || '', msg.fileName)

const scrollToBottom = () => {
  nextTick(() => {
    if (container.value) {
      container.value.scrollTop = container.value.scrollHeight
    }
  })
}

watch(() => props.messages.length, () => scrollToBottom())

defineExpose({ scrollToBottom })
</script>

<template>
  <div ref="container" class="flex-1 overflow-y-auto px-6 py-6">
    <div v-if="messages.length === 0" class="flex flex-col items-center justify-center h-full" :class="isDark ? 'text-gray-500' : 'text-gray-300'">
      <div class="w-14 h-14 bg-[#18181B] flex items-center justify-center mb-4">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
        </svg>
      </div>
      <p class="text-sm" :class="isDark ? 'text-gray-400' : 'text-gray-400'">还没有消息</p>
      <p class="text-xs mt-1" :class="isDark ? 'text-gray-500' : 'text-gray-300'">发送第一条消息开始聊天吧</p>
    </div>

    <template v-else>
      <template v-for="(message, index) in messages" :key="message.id">
        <div v-if="shouldShowDate(index)" class="flex justify-center my-6">
          <span class="text-xs" :class="isDark ? 'text-gray-500' : 'text-gray-300'">{{ formatDate(message.timestamp) }}</span>
        </div>

        <div v-else-if="shouldShowTime(index)" class="flex justify-center my-3">
          <span class="text-xs" :class="isDark ? 'text-gray-500' : 'text-gray-300'">{{ formatTime(message.timestamp) }}</span>
        </div>

        <div
          :class="[
            'flex gap-3 mb-4 bubble-pop',
            String(message.senderId) === currentUserId ? 'flex-row-reverse' : 'flex-row'
          ]"
        >
          <div
            class="w-8 h-8 flex-shrink-0 flex items-center justify-center text-white text-xs font-medium rounded-full shadow-sm"
            :style="{ backgroundColor: getAvatarColor(String(message.senderId)) }"
          >
            {{ getAvatarText(message.senderName) }}
          </div>

          <div :class="['flex flex-col max-w-[65%]', String(message.senderId) === currentUserId ? 'items-end' : 'items-start']">
            <div v-if="String(message.senderId) !== currentUserId" class="text-xs mb-1 ml-1" :class="isDark ? 'text-gray-400' : 'text-gray-500'">
              {{ message.senderName }}
            </div>

            <div
              :class="[
                'transition-all duration-200',
                isImageMessage(message) ? 'px-1 py-1 rounded-2xl' : 'px-4 py-3 text-sm rounded-2xl',
                String(message.senderId) === currentUserId
                  ? 'bg-[#18181B] text-white shadow-md rounded-br-md'
                  : (isDark ? 'bg-gray-800 text-gray-100 shadow-sm rounded-bl-md' : 'bg-white text-gray-800 shadow-sm border border-gray-100 rounded-bl-md')
              ]"
            >
              <div v-if="message.type === 'file' && message.fileId" class="min-w-[200px]">
                <FileMessage
                  :file-name="message.fileName || '未命名文件'"
                  :file-size="message.fileSize || 0"
                  :file-url="message.fileUrl || ''"
                  :file-type="message.fileType || ''"
                  :is-dark="isDark"
                  @preview="emit('filePreview', $event)"
                />
              </div>
              <div v-else class="leading-relaxed">{{ message.content }}</div>
            </div>

            <div class="flex items-center gap-1 mt-1 text-xs" :class="isDark ? 'text-gray-500' : 'text-gray-400'">
              <span>{{ formatTime(message.timestamp) }}</span>
              <span v-if="String(message.senderId) === currentUserId" class="flex items-center">
                <svg class="w-3 h-3 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                </svg>
              </span>
            </div>
          </div>
        </div>
      </template>
    </template>
  </div>
</template>

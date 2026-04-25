<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import type { GomokuChatMessage } from '@/composables/useGomokuWebSocket'

interface Props {
  messages: GomokuChatMessage[]
  isDark: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  send: [content: string]
}>()

const inputContent = ref('')
const messagesContainer = ref<HTMLElement | null>(null)

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const handleSend = () => {
  const content = inputContent.value.trim()
  if (!content) return
  emit('send', content)
  inputContent.value = ''
}

const roleLabel = (role: string) => {
  if (role === 'black') return '黑方'
  if (role === 'white') return '白方'
  return '观战'
}

const roleTagClass = (role: string) => {
  if (role === 'black') {
    return props.isDark
      ? 'bg-gray-700 text-gray-200'
      : 'bg-[#18181B] text-white'
  }
  if (role === 'white') {
    return props.isDark
      ? 'bg-gray-600 text-gray-200'
      : 'bg-gray-200 text-gray-800'
  }
  return props.isDark
    ? 'bg-gray-800 text-gray-400'
    : 'bg-gray-100 text-gray-500'
}

const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

watch(() => props.messages, () => {
  scrollToBottom()
}, { deep: true })
</script>

<template>
  <div class="flex flex-col h-full">
    <!-- 消息列表 -->
    <div ref="messagesContainer" class="flex-1 overflow-y-auto px-4 py-3">
      <div v-if="messages.length === 0" class="flex items-center justify-center h-full">
        <p class="text-xs" :class="isDark ? 'text-gray-500' : 'text-gray-400'">暂无消息</p>
      </div>

      <div v-for="(msg, index) in messages" :key="index" class="mb-3">
        <div class="flex items-start gap-2">
          <span
            class="inline-flex items-center px-1.5 py-0.5 text-[10px] font-medium shrink-0"
            :class="roleTagClass(msg.role)"
          >
            {{ roleLabel(msg.role) }}
          </span>
          <div class="min-w-0 flex-1">
            <div class="flex items-baseline gap-2">
              <span class="text-xs font-medium" :class="isDark ? 'text-gray-300' : 'text-gray-700'">
                {{ msg.senderName }}
              </span>
              <span class="text-[10px]" :class="isDark ? 'text-gray-600' : 'text-gray-400'">
                {{ formatTime(msg.timestamp) }}
              </span>
            </div>
            <p class="text-xs mt-0.5 break-words" :class="isDark ? 'text-gray-400' : 'text-gray-600'">
              {{ msg.content }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="px-3 py-2 border-t" :class="isDark ? 'border-gray-700' : 'border-gray-100'">
      <div class="flex items-center gap-2">
        <input
          v-model="inputContent"
          @keyup.enter="handleSend"
          type="text"
          placeholder="输入消息..."
          class="flex-1 px-3 py-1.5 text-xs bg-transparent border-0 border-b focus:outline-none transition-colors"
          :class="isDark
            ? 'border-gray-700 text-gray-200 placeholder-gray-500 focus:border-gray-500'
            : 'border-gray-200 text-gray-700 placeholder-gray-400 focus:border-[#18181B]'"
        />
        <button
          @click="handleSend"
          :disabled="!inputContent.trim()"
          class="w-7 h-7 bg-[#18181B] hover:bg-[#27272A] text-white flex items-center justify-center transition-colors disabled:opacity-40"
          :class="isDark ? 'disabled:bg-gray-700' : 'disabled:bg-gray-200'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="22" y1="2" x2="11" y2="13"/>
            <polygon points="22 2 15 22 11 13 2 9 22 2"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

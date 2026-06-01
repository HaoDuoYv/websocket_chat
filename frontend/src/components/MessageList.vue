<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import FileMessage from '@/components/FileMessage.vue'
import CodeBlock from '@/components/CodeBlock.vue'
import { isImageFile } from '@/api/file'
import AiAttachmentPreview from '@/components/AiAttachmentPreview.vue'
import AssistantBadge from '@/components/AssistantBadge.vue'
import { useChatStore } from '@/stores/chat'

const chatStore = useChatStore()

interface Message {
  id: string
  roomId?: string
  content: string
  senderId?: string
  senderName?: string
  senderAvatarUrl?: string
  timestamp?: number
  createdAt?: number
  seq?: number
  type?: 'text' | 'file' | 'system'
  senderType?: 'user' | 'assistant'
  fileId?: string
  fileName?: string
  fileUrl?: string
  fileSize?: number
  fileType?: string
  role?: 'user' | 'assistant' | 'system'
  conversationId?: string
}

interface AiAssistant {
  id: string
  name: string
  avatarColor?: string
  avatarUrl?: string
}

interface ContentSegment {
  type: 'text' | 'code'
  content: string
  language?: string
}

interface ToolCallState {
  callId: string
  toolName: string
  args: string
  result?: string
  status: 'running' | 'done'
}

const props = defineProps<{
  messages: Message[]
  currentUserId: string
  isDark: boolean
  isAiMode?: boolean
  currentAssistant?: AiAssistant | null
  currentUserAvatar?: string
  isThinking?: boolean
  isStreaming?: boolean
  streamContent?: string
  toolCalls?: ToolCallState[]
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
  const current = props.messages[index]
  const prev = props.messages[index - 1]
  const currentTime = current.timestamp || current.createdAt || 0
  const prevTime = prev.timestamp || prev.createdAt || 0
  return new Date(currentTime).toDateString() !== new Date(prevTime).toDateString()
}

const shouldShowTime = (index: number) => {
  if (index === 0) return true
  const current = props.messages[index]
  const prev = props.messages[index - 1]
  const currentTime = current.timestamp || current.createdAt || 0
  const prevTime = prev.timestamp || prev.createdAt || 0
  return currentTime - prevTime > 5 * 60 * 1000
}

const isImageMessage = (msg: Message) =>
  msg.type === 'file' && isImageFile(msg.fileType || '', msg.fileName)

const isAiMessage = (msg: Message) => {
  if (props.isAiMode) {
    return msg.role === 'assistant'
  }
  return false
}

// 解析内容为代码块和文本段落
const parseContent = (content: string): ContentSegment[] => {
  const segments: ContentSegment[] = []
  const regex = /```(\w*)\n?([\s\S]*?)```/g
  let lastIndex = 0
  let match

  while ((match = regex.exec(content)) !== null) {
    // 添加代码块前的文本
    if (match.index > lastIndex) {
      const textBefore = content.slice(lastIndex, match.index)
      if (textBefore.trim()) {
        segments.push({ type: 'text', content: textBefore })
      }
    }
    
    // 添加代码块
    const language = match[1] || ''
    const code = match[2] || ''
    if (code.trim()) {
      segments.push({ type: 'code', content: code.trim(), language: language || undefined })
    }
    
    lastIndex = match.index + match[0].length
  }

  // 添加剩余的文本
  if (lastIndex < content.length) {
    const remaining = content.slice(lastIndex)
    if (remaining.trim()) {
      segments.push({ type: 'text', content: remaining })
    }
  }

  // 如果没有匹配到任何代码块，返回整个内容作为文本
  if (segments.length === 0) {
    segments.push({ type: 'text', content })
  }

  return segments
}

// 渲染纯文本（处理行内代码、粗体、斜体等）
const renderInlineMarkdown = (text: string): string => {
  return text
    .replace(/`([^`]+)`/g, '<code class="bg-gray-200 dark:bg-gray-700 px-1.5 py-0.5 rounded text-xs font-mono">$1</code>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/\n/g, '<br>')
}

// 获取解析后的内容段落
const getMessageSegments = (content: string): ContentSegment[] => {
  return parseContent(content)
}

const copyMessage = (content: string) => {
  navigator.clipboard.writeText(content)
}

interface Mention {
  userId: string
  username: string
}

const parseMentionContent = (content: string, mentions: Mention[]) => {
  const parts: Array<{type: 'text' | 'mention', content: string, userId?: string}> = []
  
  const mentionPattern = /@(\S+?)(?=\s|$|[，。！？,.!?])/g
  let match
  let lastIndex = 0
  
  while ((match = mentionPattern.exec(content)) !== null) {
    if (match.index > lastIndex) {
      parts.push({type: 'text', content: content.substring(lastIndex, match.index)})
    }
    
    const username = match[1]
    const mention = mentions.find(m => m.username === username)
    
    parts.push({
      type: 'mention',
      content: `@${username}`,
      userId: mention?.userId
    })
    
    lastIndex = match.index + match[0].length
  }
  
  if (lastIndex < content.length) {
    parts.push({type: 'text', content: content.substring(lastIndex)})
  }
  
  return parts
}

const scrollToBottom = () => {
  nextTick(() => {
    if (container.value) {
      container.value.scrollTop = container.value.scrollHeight
    }
  })
}

const scrollToMessage = (messageId: string) => {
  nextTick(() => {
    if (!container.value) return
    const el = container.value.querySelector(`[data-message-id="${messageId}"]`) as HTMLElement
    if (!el) return
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    el.classList.add('mention-highlight-flash')
    setTimeout(() => el.classList.remove('mention-highlight-flash'), 2000)
  })
}

watch(() => props.messages.length, () => scrollToBottom())
watch(() => props.streamContent, () => scrollToBottom())

defineExpose({ scrollToBottom, scrollToMessage })
</script>

<template>
  <div ref="container" class="flex-1 overflow-y-auto px-6 py-6">
    <div v-if="messages.length === 0 && !isStreaming && !isThinking" class="flex flex-col items-center justify-center h-full" :class="isDark ? 'text-gray-500' : 'text-gray-300'">
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
          <span class="text-xs" :class="isDark ? 'text-gray-500' : 'text-gray-300'">{{ formatDate(message.timestamp || message.createdAt || 0) }}</span>
        </div>

        <div v-else-if="shouldShowTime(index)" class="flex justify-center my-3">
          <span class="text-xs" :class="isDark ? 'text-gray-500' : 'text-gray-300'">{{ formatTime(message.timestamp || message.createdAt || 0) }}</span>
        </div>

        <!-- AI消息样式 -->
        <div
          v-if="isAiMessage(message)"
          class="flex gap-3 mb-4 group"
          :data-message-id="message.id"
        >
          <img
            v-if="currentAssistant?.avatarUrl"
            :src="currentAssistant.avatarUrl"
            class="w-8 h-8 flex-shrink-0 rounded-full object-cover shadow-sm"
          />
          <div
            v-else
            class="w-8 h-8 flex-shrink-0 flex items-center justify-center text-white text-xs font-medium rounded-full shadow-sm"
            :style="{ background: currentAssistant?.avatarColor || 'linear-gradient(135deg, #2563EB, #3B82F6)' }"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z"/>
              <circle cx="12" cy="8" r="2"/>
            </svg>
          </div>

          <div class="flex flex-col max-w-[85%] min-w-0">
            <div class="text-xs mb-1 ml-1" :class="isDark ? 'text-gray-400' : 'text-gray-500'">
              {{ currentAssistant?.name || 'AI助手' }}
            </div>

            <!-- 消息内容：支持代码块和普通文本混合 -->
            <div class="space-y-2">
              <template v-for="(segment, segIndex) in getMessageSegments(message.content)" :key="segIndex">
                <CodeBlock 
                  v-if="segment.type === 'code'" 
                  :code="segment.content" 
                  :language="segment.language" 
                />
                <div
                  v-else
                  class="px-4 py-3 text-sm rounded-2xl rounded-bl-md"
                  :class="isDark ? 'bg-[#262626] text-gray-100 shadow-sm' : 'bg-[#F4F4F5] text-gray-800 shadow-sm'"
                  v-html="renderInlineMarkdown(segment.content)"
                />
              </template>
            </div>

            <div class="flex items-center gap-2 mt-1">
              <span class="text-xs" :class="isDark ? 'text-gray-500' : 'text-gray-400'">
                {{ formatTime(message.timestamp || message.createdAt || 0) }}
              </span>
              <button 
                @click="copyMessage(message.content)"
                class="text-xs opacity-0 group-hover:opacity-100 transition-opacity duration-200 cursor-pointer"
                :class="isDark ? 'text-gray-500 hover:text-gray-400' : 'text-gray-400 hover:text-gray-600'"
              >
                复制
              </button>
            </div>
          </div>
        </div>

        <!-- 普通消息样式 -->
        <div
          v-else
          :data-message-id="message.id"
          :class="[
            'flex gap-3 mb-4 bubble-pop',
            isAiMode || String(message.senderId || '') === currentUserId ? 'flex-row-reverse' : 'flex-row'
          ]"
        >
          <img
            v-if="String(message.senderId || '') === currentUserId && currentUserAvatar"
            :src="currentUserAvatar"
            class="w-8 h-8 flex-shrink-0 rounded-full object-cover shadow-sm"
          />
          <img
            v-else-if="message.senderAvatarUrl"
            :src="message.senderAvatarUrl"
            class="w-8 h-8 flex-shrink-0 rounded-full object-cover shadow-sm"
          />
          <div
            v-else
            class="w-8 h-8 flex-shrink-0 flex items-center justify-center text-white text-xs font-medium rounded-full shadow-sm"
            :style="{ backgroundColor: getAvatarColor(String(message.senderId || '')) }"
          >
            {{ getAvatarText(message.senderName || '') }}
          </div>

          <div :class="['flex flex-col max-w-[65%]', isAiMode || String(message.senderId || '') === currentUserId ? 'items-end' : 'items-start']">
            <div v-if="!isAiMode && String(message.senderId || '') !== currentUserId" class="text-xs mb-1 ml-1 flex items-center gap-1" :class="isDark ? 'text-gray-400' : 'text-gray-500'">
              {{ message.senderName || '' }}
              <AssistantBadge v-if="message.senderType === 'assistant'" :is-dark="isDark" />
            </div>

            <AiAttachmentPreview
              v-if="(message as any).attachments && (message as any).attachments.length > 0"
              :attachments="(message as any).attachments"
              :is-dark="isDark"
            />

            <div
              :class="[
                'transition-all duration-200',
                isImageMessage(message) ? 'px-1 py-1 rounded-2xl' : 'px-4 py-3 text-sm rounded-2xl',
                message.senderType === 'assistant' && typeof message.content === 'string' && message.content.startsWith('[智能体回复失败:')
                  ? (isDark ? 'bg-orange-900/30 text-orange-200' : 'bg-orange-100 text-orange-800')
                  : (isAiMode || String(message.senderId || '') === currentUserId
                    ? 'bg-[#18181B] text-white shadow-md rounded-br-md'
                    : (isDark ? 'bg-gray-800 text-gray-100 shadow-sm rounded-bl-md' : 'bg-white text-gray-800 shadow-sm border border-gray-100 rounded-bl-md'))
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
              <div v-else class="leading-relaxed">
                <template v-for="(part, index) in parseMentionContent(message.content, [])" :key="index">
                  <span v-if="part.type === 'text'">{{ part.content }}</span>
                  <span
                    v-else
                    :class="[
                      part.userId === currentUserId ? 'mention-self' : 'mention-highlight'
                    ]"
                  >{{ part.content }}</span>
                </template>
                <span
                  v-if="message.senderType === 'assistant' && chatStore.streamingMessageIds.has(String(message.id))"
                  class="inline-block w-[2px] h-[1em] align-middle ml-0.5 animate-pulse"
                  :class="isDark ? 'bg-gray-300' : 'bg-gray-700'"
                ></span>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- AI思考中 -->
      <div v-if="isAiMode && isThinking && !isStreaming" class="flex gap-3 mb-4">
        <img
          v-if="currentAssistant?.avatarUrl"
          :src="currentAssistant.avatarUrl"
          class="w-8 h-8 flex-shrink-0 rounded-full object-cover shadow-sm"
        />
        <div
          v-else
          class="w-8 h-8 flex-shrink-0 flex items-center justify-center text-white text-xs font-medium rounded-full shadow-sm"
          :style="{ background: currentAssistant?.avatarColor || 'linear-gradient(135deg, #2563EB, #3B82F6)' }"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z"/>
            <circle cx="12" cy="8" r="2"/>
          </svg>
        </div>
        <div class="flex flex-col max-w-[85%] min-w-0">
          <div class="text-xs mb-1 ml-1" :class="isDark ? 'text-gray-400' : 'text-gray-500'">
            {{ currentAssistant?.name || 'AI助手' }}
          </div>
          <div class="px-4 py-3 rounded-2xl rounded-bl-md" :class="isDark ? 'bg-[#262626]' : 'bg-[#F4F4F5]'">
            <div class="flex items-center gap-1.5">
              <div class="flex gap-1">
                <span class="w-2 h-2 rounded-full animate-bounce" :class="isDark ? 'bg-blue-400' : 'bg-blue-500'" style="animation-delay: 0ms"></span>
                <span class="w-2 h-2 rounded-full animate-bounce" :class="isDark ? 'bg-blue-400' : 'bg-blue-500'" style="animation-delay: 150ms"></span>
                <span class="w-2 h-2 rounded-full animate-bounce" :class="isDark ? 'bg-blue-400' : 'bg-blue-500'" style="animation-delay: 300ms"></span>
              </div>
              <span class="text-xs ml-1" :class="isDark ? 'text-blue-400' : 'text-blue-500'">思考中</span>
            </div>
          </div>
        </div>
      </div>

      <!-- AI流式输出 -->
      <div v-if="isAiMode && isStreaming && streamContent" class="flex gap-3 mb-4">
        <img
          v-if="currentAssistant?.avatarUrl"
          :src="currentAssistant.avatarUrl"
          class="w-8 h-8 flex-shrink-0 rounded-full object-cover shadow-sm"
        />
        <div
          v-else
          class="w-8 h-8 flex-shrink-0 flex items-center justify-center text-white text-xs font-medium rounded-full shadow-sm"
          :style="{ background: currentAssistant?.avatarColor || 'linear-gradient(135deg, #2563EB, #3B82F6)' }"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z"/>
            <circle cx="12" cy="8" r="2"/>
          </svg>
        </div>
        <div class="flex flex-col max-w-[85%] min-w-0">
          <div class="text-xs mb-1 ml-1" :class="isDark ? 'text-gray-400' : 'text-gray-500'">
            {{ currentAssistant?.name || 'AI助手' }}
          </div>
          <div class="space-y-2">
            <template v-for="(segment, segIndex) in getMessageSegments(streamContent)" :key="segIndex">
              <CodeBlock 
                v-if="segment.type === 'code'" 
                :code="segment.content" 
                :language="segment.language" 
              />
              <div
                v-else
                class="px-4 py-3 text-sm rounded-2xl rounded-bl-md"
                :class="isDark ? 'bg-[#262626] text-gray-100 shadow-sm' : 'bg-[#F4F4F5] text-gray-800 shadow-sm'"
                v-html="renderInlineMarkdown(segment.content)"
              />
            </template>
          </div>
          <div class="flex items-center gap-2 mt-1">
            <span class="text-xs animate-pulse" :class="isDark ? 'text-blue-400' : 'text-blue-500'">输入中...</span>
          </div>
        </div>
      </div>

      <!-- AI加载状态 -->
      <div v-if="isAiMode && isStreaming && !streamContent" class="flex gap-3 mb-4">
        <img
          v-if="currentAssistant?.avatarUrl"
          :src="currentAssistant.avatarUrl"
          class="w-8 h-8 flex-shrink-0 rounded-full object-cover shadow-sm"
        />
        <div
          v-else
          class="w-8 h-8 flex-shrink-0 flex items-center justify-center text-white text-xs font-medium rounded-full shadow-sm"
          :style="{ background: currentAssistant?.avatarColor || 'linear-gradient(135deg, #2563EB, #3B82F6)' }"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z"/>
            <circle cx="12" cy="8" r="2"/>
          </svg>
        </div>
        <div class="px-4 py-3 rounded-2xl rounded-bl-md" :class="isDark ? 'bg-[#262626]' : 'bg-[#F4F4F5] shadow-sm'">
          <div class="flex gap-1.5">
            <div class="w-2 h-2 rounded-full animate-bounce" :class="isDark ? 'bg-gray-500' : 'bg-gray-400'" style="animation-delay: 0ms"></div>
            <div class="w-2 h-2 rounded-full animate-bounce" :class="isDark ? 'bg-gray-500' : 'bg-gray-400'" style="animation-delay: 150ms"></div>
            <div class="w-2 h-2 rounded-full animate-bounce" :class="isDark ? 'bg-gray-500' : 'bg-gray-400'" style="animation-delay: 300ms"></div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.mention-highlight {
  color: #1890ff;
  background-color: rgba(24, 144, 255, 0.1);
  padding: 0 2px;
  border-radius: 3px;
  cursor: pointer;
}

.mention-highlight:hover {
  background-color: rgba(24, 144, 255, 0.2);
}

.mention-self {
  color: #ff4d4f;
  background-color: rgba(255, 77, 79, 0.1);
  font-weight: bold;
}

.mention-highlight-flash {
  animation: mention-flash 2s ease-out;
}

@keyframes mention-flash {
  0% { background-color: rgba(59, 130, 246, 0.2); border-radius: 8px; }
  100% { background-color: transparent; }
}
</style>
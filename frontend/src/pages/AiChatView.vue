<template>
  <div class="flex h-screen" :class="isDarkTheme ? 'bg-[#18181B]' : 'bg-white'">
    <!-- 左侧边栏 - 会话列表 + 上下文控制 -->
    <aside class="w-60 border-r hidden lg:flex flex-col" :class="isDarkTheme ? 'border-gray-800 bg-[#27272A]' : 'border-gray-100 bg-white'">
      <div class="px-4 py-3 border-b" :class="isDarkTheme ? 'border-gray-800' : 'border-gray-100'">
        <button 
          @click="handleCreateConversation"
          class="w-full flex items-center justify-center gap-2 py-2 px-3 rounded-lg text-sm font-medium transition-all duration-200 cursor-pointer"
          :class="isDarkTheme 
            ? 'bg-white/10 text-white hover:bg-white/15' 
            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          新建对话
        </button>
      </div>

      <div class="flex-1 overflow-y-auto py-2">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          @click="handleSelectConversation(conv)"
          class="flex items-center gap-3 px-4 py-2.5 transition-colors cursor-pointer group"
          :class="[
            currentConversation?.id === conv.id 
              ? (isDarkTheme ? 'bg-white/10' : 'bg-gray-100') 
              : (isDarkTheme ? 'hover:bg-gray-800' : 'hover:bg-gray-50')
          ]"
        >
          <div class="flex-1 min-w-0">
            <p class="text-sm truncate" :class="[
              currentConversation?.id === conv.id 
                ? (isDarkTheme ? 'text-white' : 'text-gray-900') 
                : (isDarkTheme ? 'text-gray-300' : 'text-gray-700')
            ]">{{ conv.title }}</p>
            <p class="text-xs mt-0.5" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">{{ conv.messageCount }}条消息</p>
          </div>
          <button 
            @click.stop="handleDeleteConversation(conv.id)"
            class="opacity-0 group-hover:opacity-100 p-1 rounded transition-all duration-200 cursor-pointer"
            :class="isDarkTheme ? 'hover:bg-white/10 text-gray-500 hover:text-gray-300' : 'hover:bg-gray-200 text-gray-400 hover:text-gray-600'"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
            </svg>
          </button>
        </div>

        <div v-if="conversations.length === 0" class="px-4 py-8 text-center">
          <p class="text-xs" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">暂无对话</p>
        </div>
      </div>

      <div class="px-4 py-3 border-t" :class="isDarkTheme ? 'border-gray-800' : 'border-gray-100'">
        <p class="text-xs font-medium mb-2" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">上下文消息数</p>
        <div class="flex gap-1">
          <button 
            v-for="size in [5, 10, 20]"
            :key="size"
            @click="contextSize = size"
            class="flex-1 py-1.5 rounded text-xs font-medium transition-colors duration-200 cursor-pointer"
            :class="contextSize === size 
              ? (isDarkTheme ? 'bg-white/10 text-white' : 'bg-gray-200 text-gray-800') 
              : (isDarkTheme ? 'text-gray-500 hover:text-gray-400' : 'text-gray-400 hover:text-gray-600')"
          >
            {{ size }}
          </button>
        </div>
      </div>
    </aside>

    <!-- 右侧聊天区域 -->
    <div class="relative flex-1 flex flex-col">
      <!-- 顶部栏 -->
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
          <div class="flex items-center gap-3" :class="currentAssistant?.isSystem ? '' : 'cursor-pointer'" @click="!currentAssistant?.isSystem && (isAvatarDialogOpen = true)">
            <img 
              v-if="currentAssistant?.avatarUrl" 
              :src="currentAssistant.avatarUrl" 
              class="w-10 h-10 rounded-full object-cover"
            />
            <div 
              v-else
              class="w-10 h-10 flex items-center justify-center text-white text-sm font-medium rounded-full"
              :style="{ background: currentAssistant?.avatarColor || 'linear-gradient(135deg, #2563EB, #3B82F6)' }"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z"/>
                <circle cx="12" cy="8" r="2"/>
              </svg>
            </div>
            <div>
              <h1 class="font-medium text-sm" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">
                {{ currentAssistant?.name || 'AI助手' }}
              </h1>
              <p class="text-xs flex items-center gap-1" :class="isThinking ? 'text-blue-500' : isStreaming ? 'text-amber-500' : 'text-[#737373]'">
                <span class="w-1.5 h-1.5 rounded-full" :class="isThinking ? 'bg-blue-500 animate-pulse' : isStreaming ? 'bg-amber-500 animate-pulse' : 'bg-emerald-500'"></span>
                {{ isThinking ? '思考中...' : isStreaming ? '正在输入中...' : (currentAssistant?.model || '在线') }}
              </p>
            </div>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <button
            v-if="currentConversation"
            @click="loadAiHistory(currentConversation.id)"
            class="w-9 h-9 flex items-center justify-center transition-colors"
            :class="isDarkTheme ? 'text-white hover:text-gray-200' : 'text-gray-400 hover:text-gray-600'"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6"/><path d="M1 20v-6h6"/>
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
            </svg>
          </button>
          <button
            @click="toggleTheme"
            class="w-9 h-9 flex items-center justify-center transition-colors"
            :class="isDarkTheme ? 'text-yellow-400 hover:text-yellow-300' : 'text-gray-400 hover:text-gray-600'"
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

      <!-- 消息列表 -->
      <MessageList
        ref="messagesContainer"
        :messages="messages"
        :current-user-id="''"
        :is-dark="isDarkTheme"
        :is-ai-mode="true"
        :current-assistant="currentAssistant"
        :current-user-avatar="currentUserAvatar"
        :is-thinking="isThinking"
        :is-streaming="isStreaming"
        :stream-content="streamContent"
        :tool-calls="activeToolCalls"
      />

      <!-- 错误提示 -->
      <div v-if="error" class="px-6 py-2" :class="isDarkTheme ? 'bg-red-900/20' : 'bg-red-50'">
        <p class="text-xs" :class="isDarkTheme ? 'text-red-400' : 'text-red-500'">{{ error }}</p>
      </div>

      <!-- 输入区域 -->
      <div
        class="px-6 py-4 border-t relative"
        :class="[
          isDarkTheme ? 'border-gray-800' : 'border-gray-100',
          isDragging ? (isDarkTheme ? 'bg-blue-500/10' : 'bg-blue-50') : ''
        ]"
        @dragover="handleDragOver"
        @dragleave="handleDragLeave"
        @drop="handleDrop"
      >
        <div
          v-if="isDragging"
          class="absolute inset-0 border-2 border-dashed rounded-lg pointer-events-none"
          :class="isDarkTheme ? 'border-blue-500' : 'border-blue-400'"
        ></div>

        <div v-if="pendingAttachments.length > 0" class="flex flex-wrap gap-2 mb-2">
          <AiAttachmentChip
            v-for="a in pendingAttachments"
            :key="a.id"
            :attachment="a"
            :is-dark="isDarkTheme"
            removable
            @remove="removeAttachment"
          />
        </div>

        <div class="flex items-center gap-3">
          <div class="flex-1">
            <textarea
              v-model="inputMessage"
              @keydown.enter.exact.prevent="handleSend"
              @input="autoResize"
              @paste="handlePaste"
              ref="inputRef"
              placeholder="输入消息..."
              rows="1"
              class="w-full px-4 py-2.5 resize-none rounded-xl text-sm focus:outline-none transition-all duration-200"
              :class="isDarkTheme
                ? 'bg-[#1e1e22] text-gray-200 placeholder-gray-600 border border-gray-800'
                : 'bg-gray-50 text-gray-700 placeholder-gray-400 border border-gray-200'"
              style="max-height: 120px; min-height: 40px;"
            />
            <div class="flex items-center gap-3 mt-2">
              <button
                @click="fileInputRef?.click()"
                type="button"
                class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium transition-all duration-200 cursor-pointer"
                :class="isDarkTheme
                  ? 'text-gray-500 hover:text-gray-300 hover:bg-white/5'
                  : 'text-gray-400 hover:text-gray-600 hover:bg-gray-100'"
                title="附加文件"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21.44 11.05l-9.19 9.19a6 6 0 01-8.49-8.49l9.19-9.19a4 4 0 015.66 5.66l-9.2 9.19a2 2 0 01-2.83-2.83l8.49-8.48"/>
                </svg>
                附件
              </button>
              <input
                ref="fileInputRef"
                type="file"
                multiple
                :accept="FILE_INPUT_ACCEPT"
                class="hidden"
                @change="handleFileInputChange"
              />
              <button
                @click="webSearchEnabled = !webSearchEnabled"
                class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium transition-all duration-200 cursor-pointer"
                :class="webSearchEnabled
                  ? (isDarkTheme ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30' : 'bg-blue-50 text-blue-600 border border-blue-200')
                  : (isDarkTheme ? 'text-gray-500 hover:text-gray-400 border border-transparent' : 'text-gray-400 hover:text-gray-600 border border-transparent')"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
                联网搜索
                <span v-if="webSearchEnabled" class="w-1.5 h-1.5 rounded-full bg-blue-500 animate-pulse"></span>
              </button>
            </div>
          </div>
          <button
            @click="handleSend"
            :disabled="(!inputMessage.trim() && pendingAttachments.length === 0) || isStreaming || isThinking"
            class="w-10 h-10 flex items-center justify-center rounded-xl transition-all duration-200 cursor-pointer disabled:cursor-not-allowed disabled:opacity-40"
            :class="isDarkTheme
              ? 'bg-white/10 text-white hover:bg-white/15'
              : 'bg-[#18181B] text-white hover:bg-[#27272A]'"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- 头像上传弹窗 -->
  <Teleport to="body">
    <Transition name="fade">
      <div 
        v-if="isAvatarDialogOpen"
        class="fixed inset-0 z-[80] flex items-center justify-center bg-black/50"
        @click.self="isAvatarDialogOpen = false"
      >
        <div 
          class="w-full max-w-sm mx-4 rounded-2xl shadow-xl overflow-hidden"
          :class="isDarkTheme ? 'bg-[#27272A]' : 'bg-white'"
        >
          <div class="px-6 py-4 border-b" :class="isDarkTheme ? 'border-gray-700' : 'border-gray-100'">
            <div class="flex items-center justify-between">
              <h3 class="text-base font-medium" :class="isDarkTheme ? 'text-white' : 'text-gray-900'">更换头像</h3>
              <button
                @click="isAvatarDialogOpen = false"
                class="p-1 rounded-lg transition-colors"
                :class="isDarkTheme ? 'hover:bg-gray-700 text-gray-400' : 'hover:bg-gray-100 text-gray-500'"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>
          </div>
          
          <div class="px-6 py-6">
            <div class="flex flex-col items-center gap-4">
              <AvatarUpload
                ref="avatarUploadRef"
                :model-value="currentAssistant?.avatarUrl"
                :default-icon="currentAssistant?.avatarIcon || '✨'"
                :default-color="currentAssistant?.avatarColor"
                size="xl"
                hint="点击上传新头像（JPG/PNG/GIF/WebP，最大5MB）"
                @upload="handleAvatarUpload"
              />
              
              <div class="text-center">
                <p class="text-lg font-medium" :class="isDarkTheme ? 'text-white' : 'text-gray-900'">
                  {{ currentAssistant?.name }}
                </p>
                <p class="text-sm mt-1" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">
                  {{ currentAssistant?.model }}
                </p>
              </div>
            </div>
          </div>
          
          <div class="px-6 py-4 border-t" :class="isDarkTheme ? 'border-gray-700' : 'border-gray-100'">
            <button
              @click="isAvatarDialogOpen = false"
              class="w-full py-2.5 rounded-xl text-sm font-medium transition-colors"
              :class="isDarkTheme ? 'bg-gray-700 text-white hover:bg-gray-600' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
            >
              完成
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAiStore } from '@/stores/ai'
import { useWebSocket } from '@/composables/useWebSocket'
import type { AiConversation } from '@/stores/ai'
import MessageList from '@/components/MessageList.vue'
import AiAttachmentChip from '@/components/AiAttachmentChip.vue'
import {
  buildPendingAttachment,
  FILE_INPUT_ACCEPT,
  MAX_ATTACHMENTS,
  type PendingAttachment
} from '@/utils/aiAttachment'
import AvatarUpload from '@/components/AvatarUpload.vue'
import { uploadAiAvatar } from '@/api/avatar'
import { useToast } from '@/composables/useToast'

const toast = useToast()

const route = useRoute()
const router = useRouter()
const aiStore = useAiStore()
const { connect, sendAiChat, loadAiConversations, createAiConversation, deleteAiConversation, loadAiHistory } = useWebSocket()

const isDarkTheme = ref(localStorage.getItem('theme') === 'dark')
const currentAssistant = computed(() => aiStore.currentAssistant)
const currentConversation = computed(() => aiStore.currentConversation)
const conversations = computed(() => aiStore.conversations)
const messages = computed(() => aiStore.messages)
const isStreaming = computed(() => aiStore.isStreaming)
const isThinking = computed(() => aiStore.isThinking)
const streamContent = computed(() => aiStore.streamContent)
const activeToolCalls = computed(() => aiStore.activeToolCalls)
const error = computed(() => aiStore.error)
const currentUserAvatar = computed(() => {
  try {
    const userData = localStorage.getItem('user')
    if (userData) {
      const user = JSON.parse(userData)
      return user.avatarUrl || ''
    }
  } catch {}
  return ''
})

const inputMessage = ref('')
const contextSize = ref(10)
const webSearchEnabled = ref(false)
const messagesContainer = ref<InstanceType<typeof MessageList> | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
const pendingAttachments = ref<PendingAttachment[]>([])
const fileInputRef = ref<HTMLInputElement | null>(null)
const isDragging = ref(false)
const isAvatarDialogOpen = ref(false)
const avatarUploadRef = ref<InstanceType<typeof AvatarUpload> | null>(null)
const isUploadingAvatar = ref(false)

const assistantId = computed(() => route.params.assistantId as string)

const toggleTheme = () => {
  isDarkTheme.value = !isDarkTheme.value
  localStorage.setItem('theme', isDarkTheme.value ? 'dark' : 'light')
}

function autoResize() {
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
    inputRef.value.style.height = Math.min(inputRef.value.scrollHeight, 120) + 'px'
  }
}

onMounted(async () => {
  const userData = localStorage.getItem('user')
  if (!userData) {
    router.push('/login')
    return
  }
  
  const user = JSON.parse(userData)
  connect(user, user.token)
  
  if (assistantId.value) {
    // 清除之前的聊天状态
    aiStore.clearChatState()
    loadAiConversations(assistantId.value)
    // 加载助手详情（如果当前store中没有或ID不匹配）
    if (!currentAssistant.value || currentAssistant.value.id !== assistantId.value) {
      try {
        const resp = await fetch(`/api/ai/assistants/${assistantId.value}`)
        if (resp.ok) {
          const assistant = await resp.json()
          aiStore.setCurrentAssistant(assistant)
        }
      } catch (e) {
        console.error('加载AI助手详情失败:', e)
      }
    }
  }
})

watch(() => assistantId.value, async (id) => {
  if (id) {
    // 切换助手时清除聊天状态
    aiStore.clearChatState()
    loadAiConversations(id)
    if (!currentAssistant.value || currentAssistant.value.id !== id) {
      try {
        const resp = await fetch(`/api/ai/assistants/${id}`)
        if (resp.ok) {
          const assistant = await resp.json()
          aiStore.setCurrentAssistant(assistant)
        }
      } catch (e) {
        console.error('加载AI助手详情失败:', e)
      }
    }
  }
})

function handleCreateConversation() {
  if (assistantId.value) {
    createAiConversation(assistantId.value)
  }
}

function handleSelectConversation(conv: AiConversation) {
  aiStore.setCurrentConversation(conv)
  loadAiHistory(conv.id)
}

function handleDeleteConversation(conversationId: string) {
  deleteAiConversation(conversationId)
  aiStore.removeConversation(conversationId)
}

async function handleAvatarUpload(file: File) {
  if (!currentAssistant.value) return
  
  isUploadingAvatar.value = true
  avatarUploadRef.value?.setUploading(true)
  
  try {
    const response = await uploadAiAvatar(
      currentAssistant.value.id,
      file,
      (progress) => {
        avatarUploadRef.value?.setProgress(progress)
      }
    )
    
    if (response.success) {
      // Update assistant in store
      aiStore.updateAssistantAvatar(currentAssistant.value.id, response.url || '')
      toast.success('头像更新成功')
      isAvatarDialogOpen.value = false
    } else {
      toast.error(response.message || '头像上传失败')
    }
  } catch (error: any) {
    toast.error(error?.message || '头像上传失败')
  } finally {
    isUploadingAvatar.value = false
    avatarUploadRef.value?.setUploading(false)
    avatarUploadRef.value?.setProgress(0)
  }
}

async function addFiles(files: FileList | File[]) {
  const arr = Array.from(files)
  for (const file of arr) {
    if (pendingAttachments.value.length >= MAX_ATTACHMENTS) {
      toast.error(`最多同时附加 ${MAX_ATTACHMENTS} 个文件`)
      return
    }
    const result = await buildPendingAttachment(file)
    if ('error' in result) {
      toast.error(result.error)
      continue
    }
    pendingAttachments.value.push(result)
  }
}

function removeAttachment(id: string) {
  pendingAttachments.value = pendingAttachments.value.filter(a => a.id !== id)
}

function handleFileInputChange(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    addFiles(target.files)
    target.value = ''
  }
}

function handlePaste(e: ClipboardEvent) {
  if (!e.clipboardData) return
  const files: File[] = []
  for (const item of Array.from(e.clipboardData.items)) {
    if (item.kind === 'file') {
      const f = item.getAsFile()
      if (f) files.push(f)
    }
  }
  if (files.length > 0) {
    e.preventDefault()
    addFiles(files)
  }
}

function handleDragOver(e: DragEvent) {
  e.preventDefault()
  isDragging.value = true
}

function handleDragLeave(e: DragEvent) {
  e.preventDefault()
  isDragging.value = false
}

function handleDrop(e: DragEvent) {
  e.preventDefault()
  isDragging.value = false
  if (e.dataTransfer?.files && e.dataTransfer.files.length > 0) {
    addFiles(e.dataTransfer.files)
  }
}

function handleSend() {
  const content = inputMessage.value.trim()
  const hasAttachments = pendingAttachments.value.length > 0
  if ((!content && !hasAttachments) || isStreaming.value || isThinking.value) return

  aiStore.clearError()
  aiStore.clearToolCalls()
  aiStore.setThinking(true)

  const attachmentsForWs = pendingAttachments.value.map(a => ({
    kind: a.kind,
    name: a.name,
    mimeType: a.mimeType,
    data: a.data
  }))
  const attachmentsForLocal = pendingAttachments.value.map(a => ({
    kind: a.kind,
    name: a.name,
    mimeType: a.mimeType,
    size: a.size,
    data: a.kind === 'image' ? (a.previewUrl || '') : a.data
  }))

  const userMessage = {
    id: Date.now().toString(),
    conversationId: currentConversation.value?.id || '',
    role: 'user' as const,
    senderId: 'user',
    senderName: '我',
    content,
    createdAt: Date.now(),
    attachments: attachmentsForLocal
  }
  aiStore.addMessage(userMessage as any)

  sendAiChat(
    assistantId.value,
    content,
    currentConversation.value?.id,
    webSearchEnabled.value,
    attachmentsForWs
  )

  inputMessage.value = ''
  pendingAttachments.value = []

  nextTick(() => {
    if (inputRef.value) {
      inputRef.value.style.height = 'auto'
    }
  })
}
</script>
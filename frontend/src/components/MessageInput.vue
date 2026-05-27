<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import FileUploadButton from '@/components/FileUploadButton.vue'
import MentionInput from './MentionInput.vue'
import { formatFileSize, getFileIcon, isImageFile } from '@/api/file'
import { emojiCategories } from '@/config/emojis'

interface User {
  userId: string
  username: string
}

interface PendingAttachment {
  id: string
  file: File
  previewUrl: string
  isImage: boolean
}

const props = defineProps<{
  isDark: boolean
  disabled: boolean
  onlineUsers: User[]
  currentUserId?: string
}>()

const emit = defineEmits<{
  send: [content: string, mentions: User[], mentionAll: boolean]
  sendFiles: [attachments: PendingAttachment[]]
  error: [message: string]
}>()

const mentionInputRef = ref<InstanceType<typeof MentionInput> | null>(null)

const newMessage = ref('')
const showEmojiPicker = ref(false)
const activeEmojiCategory = ref('frequent')
const fileUploadButtonRef = ref<{ queueFiles: (files: File[] | FileList) => Promise<void> } | null>(null)
const pendingAttachments = ref<PendingAttachment[]>([])
const previewingAttachment = ref<PendingAttachment | null>(null)

const canSend = computed(() =>
  !props.disabled && (pendingAttachments.value.length > 0 || !!newMessage.value.trim())
)

const buildAttachmentId = (file: File) => `${file.name}-${file.size}-${file.lastModified}`

const queuePendingFiles = (files: File[]) => {
  const knownIds = new Set(pendingAttachments.value.map(a => a.id))
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

const removePendingAttachment = (id: string) => {
  const index = pendingAttachments.value.findIndex(a => a.id === id)
  if (index === -1) return
  const [attachment] = pendingAttachments.value.splice(index, 1)
  if (previewingAttachment.value?.id === attachment.id) {
    previewingAttachment.value = null
  }
  if (attachment.previewUrl) URL.revokeObjectURL(attachment.previewUrl)
}

const clearPendingAttachments = () => {
  pendingAttachments.value.forEach(a => { if (a.previewUrl) URL.revokeObjectURL(a.previewUrl) })
  pendingAttachments.value = []
  previewingAttachment.value = null
}

const openAttachmentPreview = (a: PendingAttachment) => {
  if (a.isImage) previewingAttachment.value = a
}

const closeAttachmentPreview = () => { previewingAttachment.value = null }

const insertEmoji = (emoji: string) => { newMessage.value += emoji }

const handleMentionSend = (content: string, mentions: User[], mentionAll: boolean) => {
  emit('send', content, mentions, mentionAll)
  showEmojiPicker.value = false
  if (pendingAttachments.value.length > 0) {
    emit('sendFiles', [...pendingAttachments.value])
  }
}

const handleSendButtonClick = () => {
  // 触发 MentionInput 内部的发送逻辑，确保 mentions 状态正确传递
  mentionInputRef.value?.triggerSend()
  if (pendingAttachments.value.length > 0) {
    emit('sendFiles', [...pendingAttachments.value])
  }
}

const uploadFiles = async (files: File[] | FileList) => {
  await fileUploadButtonRef.value?.queueFiles(files)
}

watch(previewingAttachment, a => {
  if (typeof document === 'undefined') return
  document.body.style.overflow = a ? 'hidden' : ''
})

defineExpose({ clearPendingAttachments, uploadFiles, removeSentAttachments: (ids: Set<string>) => {
  pendingAttachments.value = pendingAttachments.value.filter(a => !ids.has(a.id))
}})
</script>

<template>
  <div class="px-6 py-4 border-t" :class="isDark ? 'border-gray-800' : 'border-gray-100'">
    <!-- 表情选择器 -->
    <div v-if="showEmojiPicker" class="mb-3">
      <div class="flex gap-1 mb-2 overflow-x-auto pb-1">
        <button
          v-for="cat in emojiCategories"
          :key="cat.key"
          @click="activeEmojiCategory = cat.key"
          class="shrink-0 px-2 py-1 text-xs rounded-md transition-colors"
          :class="activeEmojiCategory === cat.key
            ? (isDark ? 'bg-white/10 text-white' : 'bg-gray-200 text-gray-800')
            : (isDark ? 'text-gray-400 hover:text-gray-200' : 'text-gray-500 hover:text-gray-700')"
        >
          {{ cat.icon }} {{ cat.label }}
        </button>
      </div>
      <div class="grid grid-cols-8 gap-0.5 max-h-48 overflow-y-auto">
        <button
          v-for="(emoji, idx) in emojiCategories.find(c => c.key === activeEmojiCategory)?.emojis"
          :key="idx"
          @click="insertEmoji(emoji)"
          class="w-8 h-8 text-lg flex items-center justify-center rounded transition-colors"
          :class="isDark ? 'hover:bg-gray-800' : 'hover:bg-gray-100'"
        >
          {{ emoji }}
        </button>
      </div>
    </div>

    <!-- 待发送附件 -->
    <div v-if="pendingAttachments.length > 0" class="mb-3 flex gap-3 overflow-x-auto pb-1">
      <div
        v-for="attachment in pendingAttachments"
        :key="attachment.id"
        class="group relative shrink-0 overflow-hidden rounded-2xl border"
        :class="isDark ? 'border-gray-700 bg-gray-800/80' : 'border-gray-200 bg-gray-50'"
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
            <p class="truncate text-sm font-medium" :class="isDark ? 'text-gray-100' : 'text-gray-800'">{{ attachment.file.name }}</p>
            <p class="mt-1 text-xs" :class="isDark ? 'text-gray-400' : 'text-gray-500'">{{ formatFileSize(attachment.file.size) }}</p>
          </div>
        </div>
        <button
          type="button"
          class="absolute right-2 top-2 flex h-6 w-6 items-center justify-center rounded-full bg-black/60 text-xs text-white opacity-0 transition group-hover:opacity-100"
          @click="removePendingAttachment(attachment.id)"
        >×</button>
        <div v-if="attachment.isImage" class="absolute inset-x-0 bottom-0 bg-black/50 px-2 py-1 text-white">
          <p class="truncate text-xs">{{ attachment.file.name }}</p>
          <p class="text-[11px] opacity-80">{{ formatFileSize(attachment.file.size) }} · 点击预览</p>
        </div>
      </div>
    </div>

    <!-- 输入栏 -->
    <div class="flex items-center gap-2">
      <button
        @click="showEmojiPicker = !showEmojiPicker"
        class="w-8 h-8 flex items-center justify-center transition-colors"
        :class="isDark ? 'text-white hover:text-gray-200' : 'text-gray-400 hover:text-gray-600'"
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
        :is-dark="isDark"
        :disabled="disabled"
        @files-selected="queuePendingFiles"
        @upload-error="emit('error', $event)"
      />

      <div class="flex-1 relative">
        <MentionInput
          ref="mentionInputRef"
          v-model="newMessage"
          :users="onlineUsers"
          :is-dark="isDark"
          :disabled="disabled"
          :current-user-id="currentUserId"
          @send="handleMentionSend"
        />
      </div>

      <button
        @click="handleSendButtonClick"
        :disabled="!canSend"
        class="w-10 h-10 bg-[#18181B] hover:bg-[#27272A] text-white flex items-center justify-center transition-all duration-200 rounded-xl btn-press disabled:opacity-40"
        :class="isDark ? 'disabled:bg-gray-800' : 'disabled:bg-gray-200'"
      >
        <svg v-if="!disabled" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13"/>
          <polygon points="22 2 15 22 11 13 2 9 22 2"/>
        </svg>
        <svg v-else class="animate-spin" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
        </svg>
      </button>
    </div>
  </div>

  <!-- 附件预览模态框 -->
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
          <button type="button" class="shrink-0 rounded px-3 py-2 text-sm transition-colors hover:bg-white/10" @click="closeAttachmentPreview">
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

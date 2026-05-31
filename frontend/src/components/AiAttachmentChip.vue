<template>
  <div
    class="relative group inline-flex items-center gap-2 rounded-lg border px-2 py-1.5 text-xs"
    :class="isDark ? 'bg-[#1e1e22] border-gray-700' : 'bg-gray-50 border-gray-200'"
  >
    <img
      v-if="attachment.kind === 'image' && attachment.previewUrl"
      :src="attachment.previewUrl"
      class="w-12 h-12 object-cover rounded"
      alt=""
    />
    <div v-else class="w-12 h-12 flex items-center justify-center rounded"
         :class="isDark ? 'bg-gray-800 text-gray-400' : 'bg-gray-200 text-gray-500'">
      <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
        <polyline points="14 2 14 8 20 8"/>
      </svg>
    </div>
    <div class="flex flex-col min-w-0 max-w-[140px]">
      <span class="truncate" :class="isDark ? 'text-gray-200' : 'text-gray-700'">{{ attachment.name }}</span>
      <span :class="isDark ? 'text-gray-500' : 'text-gray-400'">{{ formatSize(attachment.size) }}</span>
    </div>
    <button
      v-if="removable"
      @click="$emit('remove', attachment.id)"
      class="ml-1 w-5 h-5 flex items-center justify-center rounded-full opacity-0 group-hover:opacity-100 transition-opacity"
      :class="isDark ? 'bg-gray-700 hover:bg-gray-600 text-gray-300' : 'bg-gray-300 hover:bg-gray-400 text-white'"
      type="button"
      title="移除"
    >
      <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <line x1="18" y1="6" x2="6" y2="18"/>
        <line x1="6" y1="6" x2="18" y2="18"/>
      </svg>
    </button>
  </div>
</template>

<script setup lang="ts">
import type { PendingAttachment } from '@/utils/aiAttachment'
import { formatSize } from '@/utils/aiAttachment'

defineProps<{
  attachment: PendingAttachment
  isDark: boolean
  removable?: boolean
}>()
defineEmits<{ (e: 'remove', id: string): void }>()
</script>

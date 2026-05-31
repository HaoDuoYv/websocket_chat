<template>
  <div class="flex flex-col gap-2 mb-2">
    <template v-for="(att, idx) in attachments" :key="idx">
      <div
        v-if="att.kind === 'image'"
        class="rounded-lg overflow-hidden cursor-pointer max-w-[260px]"
        @click="openImage(att)"
      >
        <img :src="imageSrc(att)" :alt="att.name" class="w-full h-auto block" />
      </div>
      <div
        v-else
        class="rounded-lg border px-3 py-2 max-w-[320px]"
        :class="isDark ? 'bg-[#1e1e22] border-gray-700' : 'bg-gray-50 border-gray-200'"
      >
        <button
          type="button"
          class="w-full flex items-center gap-2 text-left text-xs"
          @click="toggle(idx)"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
               :class="isDark ? 'text-gray-400' : 'text-gray-500'">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
          </svg>
          <span class="flex-1 truncate" :class="isDark ? 'text-gray-200' : 'text-gray-700'">{{ att.name }}</span>
          <span :class="isDark ? 'text-gray-500' : 'text-gray-400'">{{ expanded[idx] ? '收起' : '展开' }}</span>
        </button>
        <pre
          v-if="expanded[idx]"
          class="mt-2 text-xs overflow-x-auto whitespace-pre-wrap break-all max-h-60 overflow-y-auto"
          :class="isDark ? 'text-gray-300' : 'text-gray-600'"
        >{{ previewText(att) }}</pre>
      </div>
    </template>

    <Teleport to="body">
      <div
        v-if="lightbox"
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 cursor-zoom-out"
        @click="lightbox = null"
      >
        <img :src="lightbox" alt="" class="max-w-[90vw] max-h-[90vh] object-contain" />
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { MessageAttachment } from '@/stores/ai'

defineProps<{
  attachments: MessageAttachment[]
  isDark: boolean
}>()

const expanded = ref<Record<number, boolean>>({})
const lightbox = ref<string | null>(null)

function imageSrc(att: MessageAttachment): string {
  if (att.data.startsWith('data:')) return att.data
  return `data:${att.mimeType};base64,${att.data}`
}

function openImage(att: MessageAttachment) {
  lightbox.value = imageSrc(att)
}

function toggle(idx: number) {
  expanded.value = { ...expanded.value, [idx]: !expanded.value[idx] }
}

function previewText(att: MessageAttachment): string {
  const lines = att.data.split('\n')
  if (lines.length <= 20) return att.data
  return lines.slice(0, 20).join('\n') + `\n...（共 ${lines.length} 行）`
}
</script>

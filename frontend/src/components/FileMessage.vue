<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { formatFileSize, getFileIcon, isImageFile } from '@/api/file'

interface Props {
  fileName: string
  fileSize: number
  fileUrl: string
  fileType: string
  isDark?: boolean
}

const props = defineProps<Props>()
const isPreviewOpen = ref(false)
const imageMessage = computed(() => isImageFile(props.fileType))
const originalBodyOverflow = ref('')

const handleDownload = () => {
  const link = document.createElement('a')
  link.href = props.fileUrl
  link.download = props.fileName
  link.rel = 'noopener'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const handlePreview = () => {
  if (!imageMessage.value) {
    window.open(props.fileUrl, '_blank', 'noopener')
    return
  }

  isPreviewOpen.value = true
}

const closePreview = () => {
  isPreviewOpen.value = false
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && isPreviewOpen.value) {
    closePreview()
  }
}

watch(isPreviewOpen, open => {
  if (typeof document === 'undefined') return

  if (open) {
    originalBodyOverflow.value = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', handleKeydown)
    return
  }

  document.body.style.overflow = originalBodyOverflow.value
  window.removeEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (typeof document !== 'undefined') {
    document.body.style.overflow = originalBodyOverflow.value
  }
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="space-y-2">
    <div v-if="imageMessage" class="inline-flex max-w-full flex-col items-start gap-2">
      <button
        type="button"
        class="group relative overflow-hidden rounded-[24px] border shadow-sm ring-1 ring-inset transition-transform duration-200 hover:scale-[1.01]"
        :class="isDark ? 'border-slate-700/80 bg-slate-900/70 ring-white/10' : 'border-slate-200 bg-white ring-slate-200/70'"
        @click="handlePreview"
      >
        <img
          :src="props.fileUrl"
          :alt="fileName"
          class="block max-h-[360px] max-w-[220px] cursor-zoom-in object-cover sm:max-w-[260px] md:max-w-[320px]"
          loading="lazy"
        />

        <div
          class="pointer-events-none absolute inset-x-0 bottom-0 bg-gradient-to-t px-4 pb-3 pt-8"
          :class="isDark ? 'from-slate-950/85 to-transparent' : 'from-black/60 to-transparent'"
        >
          <p class="truncate text-sm font-medium text-white drop-shadow-sm" :title="fileName">
            {{ fileName }}
          </p>
          <p class="mt-1 text-[11px] text-white/80">
            {{ formatFileSize(fileSize) }}
          </p>
        </div>

        <button
          type="button"
          class="absolute right-3 top-3 rounded-full px-2.5 py-1 text-[11px] opacity-0 shadow-sm transition-all group-hover:opacity-100"
          :class="isDark ? 'bg-slate-900/80 text-white hover:bg-slate-900' : 'bg-white/90 text-slate-700 hover:bg-white'"
          @click.stop="handleDownload"
        >
          下载
        </button>
      </button>
    </div>

    <div v-else class="flex items-start gap-3 rounded p-3" :class="isDark ? 'bg-white/10' : 'bg-black/5'">
      <div class="flex-shrink-0">
        <div class="flex h-16 w-16 items-center justify-center rounded text-3xl" :class="isDark ? 'bg-gray-700' : 'bg-gray-200'">
          {{ getFileIcon(fileName) }}
        </div>
      </div>

      <div class="min-w-0 flex-1">
        <p class="truncate text-sm font-medium" :title="fileName">
          {{ fileName }}
        </p>
        <p class="mt-1 text-xs opacity-70">
          {{ formatFileSize(fileSize) }}
        </p>

        <div class="mt-2 flex gap-2">
          <button
            @click="handleDownload"
            class="rounded px-2 py-1 text-xs transition-colors"
            :class="isDark ? 'bg-white/20 hover:bg-white/30' : 'bg-black/10 hover:bg-black/20'"
          >
            下载
          </button>
          <button
            @click="handlePreview"
            class="rounded px-2 py-1 text-xs transition-colors"
            :class="isDark ? 'bg-white/20 hover:bg-white/30' : 'bg-black/10 hover:bg-black/20'"
          >
            打开
          </button>
        </div>
      </div>
    </div>
  </div>

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
        v-if="isPreviewOpen"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/88 p-4"
        @click.self="closePreview"
      >
        <div class="absolute inset-x-0 top-0 flex items-center justify-between gap-4 bg-gradient-to-b from-black/60 to-transparent px-4 py-4 text-white">
          <p class="min-w-0 truncate text-sm font-medium" :title="fileName">
            {{ fileName }}
          </p>
          <button
            type="button"
            class="shrink-0 rounded px-3 py-2 text-sm transition-colors hover:bg-white/10"
            @click="closePreview"
          >
            关闭
          </button>
        </div>

        <img
          :src="props.fileUrl"
          :alt="fileName"
          class="max-h-full max-w-full rounded object-contain transition duration-200 ease-out"
        />
      </div>
    </Transition>
  </Teleport>
</template>

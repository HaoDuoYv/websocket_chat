<script setup lang="ts">
import { computed, ref } from 'vue'
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
</script>

<template>
  <div class="space-y-2">
    <div v-if="imageMessage" class="inline-flex max-w-full flex-col items-start gap-2">
      <div class="group relative overflow-hidden rounded-2xl" :class="isDark ? 'bg-white/10' : 'bg-black/5'">
        <img
          :src="fileUrl"
          :alt="fileName"
          class="block max-h-[360px] max-w-[220px] cursor-zoom-in object-cover sm:max-w-[260px] md:max-w-[320px]"
          loading="lazy"
          @dblclick.stop="handlePreview"
        />

        <button
          type="button"
          class="absolute right-2 top-2 rounded-full px-2.5 py-1 text-[11px] opacity-0 transition-all group-hover:opacity-100"
          :class="isDark ? 'bg-slate-900/70 text-white hover:bg-slate-900/90' : 'bg-white/85 text-slate-700 hover:bg-white'"
          @click.stop="handleDownload"
        >
          下载
        </button>
      </div>

      <div class="min-w-0 px-1">
        <p class="text-[11px] opacity-50">
          双击预览
        </p>
      </div>
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
    <div
      v-if="isPreviewOpen"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/88 p-4"
      @click.self="closePreview"
    >
      <button
        type="button"
        class="absolute right-4 top-4 rounded px-3 py-2 text-sm text-white transition-colors hover:bg-white/10"
        @click="closePreview"
      >
        关闭
      </button>

      <img
        :src="fileUrl"
        :alt="fileName"
        class="max-h-full max-w-full rounded object-contain"
      />
    </div>
  </Teleport>
</template>

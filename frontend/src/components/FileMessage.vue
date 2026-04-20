<script setup lang="ts">
import { computed } from 'vue'
import { getFileIcon, isImageFile } from '@/api/file'

interface Props {
  fileName: string
  fileSize: number
  fileUrl: string
  fileType: string
  isDark?: boolean
}

const props = defineProps<Props>()
const imageMessage = computed(() => isImageFile(props.fileType))

const handleDownload = async () => {
  try {
    // 对于 HTML 和 TXT 文件，使用 Fetch API 强制下载
    if (props.fileType.includes('text/html') || props.fileType.includes('text/plain')) {
      const response = await fetch(props.fileUrl)
      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = props.fileName
      link.rel = 'noopener'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    } else {
      // 其他文件类型使用常规下载
      const link = document.createElement('a')
      link.href = props.fileUrl
      link.download = props.fileName
      link.rel = 'noopener'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    }
  } catch (error) {
    console.error('下载失败:', error)
    // 失败时使用备用方法
    const link = document.createElement('a')
    link.href = props.fileUrl
    link.download = props.fileName
    link.rel = 'noopener'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }
}

const handlePreview = () => {
  // 所有文件类型都在新窗口中预览
  window.open(props.fileUrl, '_blank', 'noopener')
}
</script>

<template>
  <div class="space-y-2">
    <div v-if="imageMessage" class="inline-flex max-w-full flex-col items-start gap-2">
      <div class="group relative overflow-hidden rounded-[24px] border shadow-sm ring-1 ring-inset transition-transform duration-200 hover:scale-[1.01]"
        :class="isDark ? 'border-slate-700/80 bg-slate-900/70 ring-white/10' : 'border-slate-200 bg-white ring-slate-200/70'">
        <img
          :src="props.fileUrl"
          :alt="fileName"
          class="block max-h-[360px] max-w-[220px] cursor-zoom-in object-cover sm:max-w-[260px] md:max-w-[320px]"
          loading="lazy"
        />

        <div class="flex absolute right-3 top-3 gap-2">
          <button
            type="button"
            class="rounded-full px-2.5 py-1 text-[11px] opacity-0 shadow-sm transition-all group-hover:opacity-100"
            :class="isDark ? 'bg-slate-900/80 text-white hover:bg-slate-900' : 'bg-white/90 text-slate-700 hover:bg-white'"
            @click="handleDownload"
          >
            下载
          </button>
          <button
            type="button"
            class="rounded-full px-2.5 py-1 text-[11px] opacity-0 shadow-sm transition-all group-hover:opacity-100"
            :class="isDark ? 'bg-slate-900/80 text-white hover:bg-slate-900' : 'bg-white/90 text-slate-700 hover:bg-white'"
            @click="handlePreview"
          >
            预览
          </button>
        </div>
      </div>
    </div>

    <div v-else class="flex items-start gap-3 rounded p-3" :class="isDark ? 'bg-white/10' : 'bg-black/5'">
      <div class="flex-shrink-0">
        <div class="flex h-16 w-16 items-center justify-center rounded text-3xl" :class="isDark ? 'bg-gray-700' : 'bg-gray-200'">
          {{ getFileIcon(fileName) }}
        </div>
      </div>

      <div class="min-w-0 flex-1">

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
            预览
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

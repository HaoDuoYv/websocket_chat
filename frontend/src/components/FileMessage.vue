<script setup lang="ts">
import { computed } from 'vue'
import { formatFileSize, getFileIcon, isImageFile, getFileTypeDescription, isVideoFile } from '@/api/file'

interface Props {
  fileName: string
  fileSize: number
  fileUrl: string
  fileType: string
  isDark?: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  preview: [{ fileName: string; fileSize: number; fileUrl: string; fileType: string }]
}>()
const imageMessage = computed(() => isImageFile(props.fileType))
const videoMessage = computed(() => isVideoFile(props.fileType))

const handleDownload = async () => {
  try {
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
  emit('preview', {
    fileName: props.fileName,
    fileSize: props.fileSize,
    fileUrl: props.fileUrl,
    fileType: props.fileType
  })
}
</script>

<template>
  <div class="space-y-2">
    <!-- 图片文件 -->
    <div v-if="imageMessage" class="inline-flex max-w-full flex-col items-start gap-2">
      <div class="group relative overflow-hidden rounded-[24px] border shadow-sm ring-1 ring-inset transition-all duration-300 hover:scale-[1.02] hover:shadow-md"
        :class="isDark ? 'border-gray-700/80 bg-gray-900/70 ring-white/10' : 'border-gray-200 bg-white ring-gray-200/70'">
        <img
          :src="props.fileUrl"
          :alt="fileName"
          class="block max-h-[360px] max-w-[220px] cursor-zoom-in object-cover sm:max-w-[260px] md:max-w-[320px] transition-transform duration-500 hover:scale-105"
          loading="lazy"
          @click="handlePreview"
        />

        <!-- 图片信息叠加层 -->
        <div class="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/70 to-transparent p-3 transform translate-y-full group-hover:translate-y-0 transition-transform duration-300">
          <div class="flex items-center justify-between">
            <span class="text-white text-xs font-medium truncate">{{ fileName }}</span>
            <span class="text-white/80 text-xs">{{ formatFileSize(fileSize) }}</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="flex absolute right-3 top-3 gap-2">
          <button
            type="button"
            class="rounded-full px-2.5 py-1 text-[11px] opacity-0 shadow-sm transition-all group-hover:opacity-100"
            :class="isDark ? 'bg-gray-900/80 text-white hover:bg-gray-900' : 'bg-white/90 text-gray-700 hover:bg-white'"
            @click="handleDownload"
            title="下载"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
          </button>
          <button
            type="button"
            class="rounded-full px-2.5 py-1 text-[11px] opacity-0 shadow-sm transition-all group-hover:opacity-100"
            :class="isDark ? 'bg-gray-900/80 text-white hover:bg-gray-900' : 'bg-white/90 text-gray-700 hover:bg-white'"
            @click="handlePreview"
            title="预览"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 视频文件 -->
    <div v-else-if="videoMessage" class="inline-flex max-w-full flex-col items-start gap-2">
      <div class="relative overflow-hidden rounded-[16px] border shadow-sm ring-1 ring-inset"
        :class="isDark ? 'border-gray-700/80 bg-gray-900/70 ring-white/10' : 'border-gray-200 bg-white ring-gray-200/70'">
        <video
          :src="props.fileUrl"
          class="block max-h-[300px] max-w-[260px] sm:max-w-[320px] object-cover"
          controls
          preload="metadata"
          playsinline
        >
          您的浏览器不支持视频播放
        </video>
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs" :class="isDark ? 'text-gray-400' : 'text-gray-500'">{{ fileName }}</span>
        <span class="text-xs opacity-60" :class="isDark ? 'text-gray-500' : 'text-gray-400'">{{ formatFileSize(fileSize) }}</span>
      </div>
    </div>

    <!-- 普通文件 -->
    <div v-else class="flex items-start gap-3 rounded-lg p-4 transition-all duration-300 hover:shadow-sm"
      :class="isDark ? 'bg-white/10 hover:bg-white/15' : 'bg-black/5 hover:bg-black/10'">
      <div class="flex-shrink-0">
        <div class="flex h-14 w-14 items-center justify-center rounded-lg text-2xl transition-transform duration-300 hover:scale-110"
          :class="isDark ? 'bg-gray-700' : 'bg-gray-200'">
          {{ getFileIcon(fileName) }}
        </div>
      </div>

      <div class="min-w-0 flex-1">
        <p class="text-sm font-medium truncate" :class="isDark ? 'text-gray-200' : 'text-gray-800'">
          {{ fileName }}
        </p>
        <p class="mt-1 text-xs" :class="isDark ? 'text-gray-400' : 'text-gray-500'">
          {{ formatFileSize(fileSize) }} · {{ getFileTypeDescription(fileType) }}
        </p>

        <div class="mt-2.5 flex gap-2">
          <button
            @click="handleDownload"
            class="flex items-center gap-1 rounded px-2.5 py-1 text-xs font-medium transition-colors"
            :class="isDark ? 'bg-white/20 hover:bg-white/30' : 'bg-black/10 hover:bg-black/20'"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            下载
          </button>
          <button
            @click="handlePreview"
            class="flex items-center gap-1 rounded px-2.5 py-1 text-xs font-medium transition-colors"
            :class="isDark ? 'bg-white/20 hover:bg-white/30' : 'bg-black/10 hover:bg-black/20'"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
            预览
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

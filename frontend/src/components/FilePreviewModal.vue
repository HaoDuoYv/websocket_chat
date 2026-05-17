<script setup lang="ts">
import { ref, watch, computed, onUnmounted } from 'vue'
import { marked } from 'marked'
import mammoth from 'mammoth'
import { formatFileSize, isImageFile, isVideoFile, isPdfFile, isTextFile, isCodeFile } from '@/api/file'

interface PreviewFile {
  fileName: string
  fileSize: number
  fileUrl: string
  fileType: string
}

const props = defineProps<{
  file: PreviewFile | null
  isDark: boolean
}>()

const emit = defineEmits<{ close: [] }>()

const loading = ref(true)
const error = ref('')
const htmlContent = ref('')
const textContent = ref('')

const fileExtension = computed(() => {
  if (!props.file) return ''
  return props.file.fileName.split('.').pop()?.toLowerCase() || ''
})

const isMarkdown = computed(() => ['md', 'markdown'].includes(fileExtension.value))
const isDocx = computed(() => fileExtension.value === 'docx')
const isDoc = computed(() => fileExtension.value === 'doc')
const isPdf = computed(() => props.file ? isPdfFile(props.file.fileType) : false)
const isText = computed(() => props.file ? isTextFile(props.file.fileType) || isCodeFile(props.file.fileName) : false)
const isImage = computed(() => props.file ? isImageFile(props.file.fileType, props.file.fileName) : false)
const isVideo = computed(() => props.file ? isVideoFile(props.file.fileType, props.file.fileName) : false)

const previewMode = computed(() => {
  if (isImage.value) return 'image'
  if (isVideo.value) return 'video'
  if (isPdf.value) return 'pdf'
  if (isDocx.value) return 'word'
  if (isDoc.value) return 'doc-old'
  if (isMarkdown.value) return 'markdown'
  if (isText.value) return 'text'
  return 'unsupported'
})

const loadContent = async () => {
  if (!props.file) return
  loading.value = true
  error.value = ''
  htmlContent.value = ''
  textContent.value = ''

  try {
    if (previewMode.value === 'markdown' || previewMode.value === 'text') {
      const resp = await fetch(props.file.fileUrl)
      if (!resp.ok) throw new Error('加载失败')
      const text = await resp.text()
      if (previewMode.value === 'markdown') {
        htmlContent.value = marked.parse(text) as string
      } else {
        textContent.value = text
      }
    } else if (previewMode.value === 'word') {
      const resp = await fetch(props.file.fileUrl)
      if (!resp.ok) throw new Error('加载失败')
      const arrayBuffer = await resp.arrayBuffer()
      const result = await mammoth.convertToHtml({ arrayBuffer })
      htmlContent.value = result.value
      if (result.messages.length > 0) {
        console.warn('Word conversion warnings:', result.messages)
      }
    }
  } catch (e: any) {
    error.value = e.message || '加载文件内容失败'
  } finally {
    loading.value = false
  }
}

watch(() => props.file, (f) => {
  if (f) loadContent()
  document.body.style.overflow = f ? 'hidden' : ''
}, { immediate: true })

onUnmounted(() => { document.body.style.overflow = '' })

const close = () => emit('close')

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') close()
}

watch(() => props.file, (f) => {
  if (f) {
    window.addEventListener('keydown', handleKeydown)
  } else {
    window.removeEventListener('keydown', handleKeydown)
  }
}, { immediate: true })

onUnmounted(() => window.removeEventListener('keydown', handleKeydown))

const download = () => {
  if (!props.file) return
  const a = document.createElement('a')
  a.href = props.file.fileUrl
  a.download = props.file.fileName
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}
</script>

<template>
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
        v-if="file"
        class="fixed inset-0 z-[90] flex flex-col bg-black/80 backdrop-blur-sm"
        @click.self="close"
      >
        <!-- 顶栏 -->
        <div class="flex items-center justify-between gap-4 px-6 py-3 bg-gradient-to-b from-black/60 to-transparent text-white shrink-0">
          <div class="flex items-center gap-3 min-w-0">
            <span class="text-lg">📄</span>
            <div class="min-w-0">
              <p class="truncate text-sm font-medium">{{ file.fileName }}</p>
              <p class="text-xs opacity-70">{{ formatFileSize(file.fileSize) }}</p>
            </div>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <button
              @click="download"
              class="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm transition-colors hover:bg-white/10"
              title="下载"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                <polyline points="7 10 12 15 17 10"/>
                <line x1="12" y1="15" x2="12" y2="3"/>
              </svg>
              下载
            </button>
            <button
              @click="close"
              class="rounded-lg px-3 py-1.5 text-sm transition-colors hover:bg-white/10"
            >
              关闭
            </button>
          </div>
        </div>

        <!-- 内容区 -->
        <div class="flex-1 overflow-auto p-4">
          <!-- 加载中 -->
          <div v-if="loading" class="flex h-full items-center justify-center">
            <div class="flex flex-col items-center gap-3 text-white">
              <svg class="animate-spin" xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
              </svg>
              <p class="text-sm opacity-70">加载中...</p>
            </div>
          </div>

          <!-- 错误 -->
          <div v-else-if="error" class="flex h-full items-center justify-center">
            <div class="flex flex-col items-center gap-3 text-white">
              <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="15" y1="9" x2="9" y2="15"/>
                <line x1="9" y1="9" x2="15" y2="15"/>
              </svg>
              <p class="text-sm">{{ error }}</p>
              <button @click="loadContent" class="mt-2 rounded-lg bg-white/10 px-4 py-2 text-sm hover:bg-white/20">重试</button>
            </div>
          </div>

          <!-- PDF 预览 -->
          <div v-else-if="previewMode === 'pdf'" class="flex h-full items-center justify-center">
            <iframe
              :src="file.fileUrl"
              class="h-full w-full max-w-5xl rounded-lg bg-white shadow-2xl"
              style="border: none;"
            ></iframe>
          </div>

          <!-- 图片预览 -->
          <div v-else-if="previewMode === 'image'" class="flex h-full items-center justify-center">
            <img
              :src="file.fileUrl"
              :alt="file.fileName"
              class="max-h-full max-w-full rounded-lg object-contain shadow-2xl"
            />
          </div>

          <!-- 视频预览 -->
          <div v-else-if="previewMode === 'video'" class="flex h-full items-center justify-center">
            <video
              :src="file.fileUrl"
              class="max-h-full max-w-full rounded-lg shadow-2xl"
              controls
              autoplay
            ></video>
          </div>

          <!-- Word / Markdown 预览 (HTML 渲染) -->
          <div
            v-else-if="previewMode === 'word' || previewMode === 'markdown'"
            class="mx-auto max-w-4xl rounded-xl bg-white p-8 shadow-2xl prose prose-sm prose-slate max-w-none"
            v-html="htmlContent"
          ></div>

          <!-- 旧版 .doc 格式 -->
          <div v-else-if="previewMode === 'doc-old'" class="flex h-full items-center justify-center">
            <div class="flex flex-col items-center gap-4 text-white">
              <div class="text-6xl">📝</div>
              <p class="text-lg font-medium">{{ file.fileName }}</p>
              <p class="text-sm opacity-80">旧版 .doc 格式暂不支持在线预览</p>
              <p class="text-xs opacity-60">仅支持 .docx 格式的在线预览，当前文件为旧版 .doc 格式</p>
              <button
                @click="download"
                class="mt-2 flex items-center gap-2 rounded-lg bg-white/10 px-6 py-3 text-sm transition-colors hover:bg-white/20"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="7 10 12 15 17 10"/>
                  <line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
                下载文件
              </button>
            </div>
          </div>

          <!-- 文本/代码预览 -->
          <div v-else-if="previewMode === 'text'" class="mx-auto max-w-4xl">
            <pre class="rounded-xl bg-gray-900 p-6 text-sm text-gray-100 overflow-auto shadow-2xl font-mono whitespace-pre-wrap break-words">{{ textContent }}</pre>
          </div>

          <!-- 不支持的文件类型 -->
          <div v-else class="flex h-full items-center justify-center">
            <div class="flex flex-col items-center gap-4 text-white">
              <div class="text-6xl">📁</div>
              <p class="text-lg font-medium">{{ file.fileName }}</p>
              <p class="text-sm opacity-70">该文件类型暂不支持在线预览</p>
              <button
                @click="download"
                class="mt-2 flex items-center gap-2 rounded-lg bg-white/10 px-6 py-3 text-sm transition-colors hover:bg-white/20"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="7 10 12 15 17 10"/>
                  <line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
                下载文件
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.prose :deep(pre) {
  background-color: transparent;
  padding: 0;
}
.prose :deep(img) {
  max-width: 100%;
  border-radius: 0.5rem;
}
.prose :deep(table) {
  width: 100%;
  border-collapse: collapse;
}
.prose :deep(th),
.prose :deep(td) {
  border: 1px solid #e2e8f0;
  padding: 0.5rem 0.75rem;
}
.prose :deep(th) {
  background-color: #f8fafc;
}
.prose :deep(blockquote) {
  border-left-color: #18181b;
}
</style>

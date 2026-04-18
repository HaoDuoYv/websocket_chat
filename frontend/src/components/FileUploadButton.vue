<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  disabled?: boolean
  isDark?: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  filesSelected: [files: File[]]
  uploadError: [error: string]
}>()

const fileInput = ref<HTMLInputElement | null>(null)

const MAX_FILE_SIZE = 524288000

const openPicker = () => {
  if (props.disabled) return
  fileInput.value?.click()
}

const hasDirectory = (items?: DataTransferItemList | null) => {
  if (!items) return false

  for (let i = 0; i < items.length; i++) {
    const entry = items[i].webkitGetAsEntry?.()
    if (entry && entry.isDirectory) {
      return true
    }
  }

  return false
}

const validateFiles = (inputFiles: File[] | FileList): File[] => {
  const files = Array.from(inputFiles).filter(file => file.size > 0)
  const validFiles: File[] = []

  for (const file of files) {
    if (file.size > MAX_FILE_SIZE) {
      emit('uploadError', `文件 ${file.name} 超过 500MB 限制`)
      continue
    }

    validFiles.push(file)
  }

  return validFiles
}

const queueFiles = async (inputFiles: File[] | FileList) => {
  if (props.disabled) return

  const validFiles = validateFiles(inputFiles)
  if (validFiles.length === 0) return

  emit('filesSelected', validFiles)
}

const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (files) {
    await queueFiles(files)
  }

  target.value = ''
}

const handleDrop = async (event: DragEvent) => {
  event.preventDefault()

  if (hasDirectory(event.dataTransfer?.items)) {
    emit('uploadError', '不支持上传文件夹')
    return
  }

  const files = event.dataTransfer?.files
  if (files && files.length > 0) {
    await queueFiles(files)
  }
}

const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
}

defineExpose({
  openPicker,
  queueFiles
})
</script>

<template>
  <div class="relative" @drop="handleDrop" @dragover="handleDragOver">
    <input
      ref="fileInput"
      type="file"
      multiple
      class="hidden"
      @change="handleFileChange"
    />

    <button
      @click="openPicker"
      :disabled="disabled"
      class="p-2 transition-colors disabled:cursor-not-allowed disabled:opacity-50"
      :class="isDark ? 'text-gray-400 hover:text-white' : 'text-gray-500 hover:text-gray-800'"
      title="选择图片或文件"
    >
      <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/>
      </svg>
    </button>
  </div>
</template>

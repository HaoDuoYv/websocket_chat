<template>
  <div class="avatar-upload-container">
    <div 
      class="avatar-wrapper"
      :class="{ 'cursor-pointer': !disabled }"
      @click="triggerUpload"
    >
      <img 
        v-if="previewUrl || modelValue" 
        :src="previewUrl || modelValue" 
        class="avatar-image"
        :class="sizeClass"
      />
      <div 
        v-else 
        class="avatar-placeholder"
        :class="sizeClass"
        :style="{ background: defaultColor || 'linear-gradient(135deg, #667eea, #764ba2)' }"
      >
        <span class="avatar-icon">{{ defaultIcon || '👤' }}</span>
      </div>
      
      <div v-if="!disabled" class="avatar-overlay" :class="sizeClass">
        <svg xmlns="http://www.w3.org/2000/svg" class="upload-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
          <polyline points="17 8 12 3 7 8"></polyline>
          <line x1="12" y1="3" x2="12" y2="15"></line>
        </svg>
      </div>
      
      <div v-if="uploading" class="upload-progress" :class="sizeClass">
        <div class="progress-bar" :style="{ width: progress + '%' }"></div>
      </div>
    </div>
    
    <input 
      ref="fileInput"
      type="file"
      accept="image/jpeg,image/png,image/gif,image/webp"
      class="hidden"
      @change="handleFileChange"
    />
    
    <p v-if="hint && !disabled" class="text-xs text-gray-500 mt-2 text-center">{{ hint }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  modelValue?: string
  defaultIcon?: string
  defaultColor?: string
  size?: 'sm' | 'md' | 'lg' | 'xl'
  disabled?: boolean
  hint?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'upload', file: File): void
}>()

const fileInput = ref<HTMLInputElement | null>(null)
const previewUrl = ref<string>('')
const uploading = ref(false)
const progress = ref(0)

const sizeClass = computed(() => {
  switch (props.size) {
    case 'sm': return 'w-10 h-10'
    case 'md': return 'w-16 h-16'
    case 'lg': return 'w-24 h-24'
    case 'xl': return 'w-32 h-32'
    default: return 'w-16 h-16'
  }
})

const triggerUpload = () => {
  if (!props.disabled && fileInput.value) {
    fileInput.value.click()
  }
}

const handleFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  if (input.files && input.files[0]) {
    const file = input.files[0]
    
    // Validate file type
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
    if (!allowedTypes.includes(file.type)) {
      alert('只支持 JPG、PNG、GIF、WebP 格式的图片')
      return
    }
    
    // Validate file size (5MB)
    if (file.size > 5 * 1024 * 1024) {
      alert('头像大小不能超过5MB')
      return
    }
    
    // Create preview
    const reader = new FileReader()
    reader.onload = (e) => {
      previewUrl.value = e.target?.result as string
    }
    reader.readAsDataURL(file)
    
    emit('upload', file)
  }
  
  // Reset input
  input.value = ''
}

const setUploading = (value: boolean) => {
  uploading.value = value
}

const setProgress = (value: number) => {
  progress.value = value
}

defineExpose({
  setUploading,
  setProgress
})
</script>

<style scoped>
.avatar-upload-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-wrapper {
  position: relative;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.avatar-icon {
  font-size: 1.2em;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.upload-icon {
  width: 24px;
  height: 24px;
  color: white;
}

.upload-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: rgba(0, 0, 0, 0.3);
}

.progress-bar {
  height: 100%;
  background: #10b981;
  transition: width 0.2s;
}
</style>

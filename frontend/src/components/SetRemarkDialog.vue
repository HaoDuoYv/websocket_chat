<template>
  <div v-if="isOpen" class="fixed inset-0 bg-black/30 flex items-center justify-center z-50 p-4" @click.self="handleClose">
    <div class="w-full max-w-sm" :class="isDark ? 'bg-[#1E293B]' : 'bg-white'">
      <div class="px-5 py-4 border-b flex items-center justify-between" :class="isDark ? 'border-gray-800' : 'border-gray-100'">
        <h3 class="text-sm font-medium" :class="isDark ? 'text-gray-200' : 'text-gray-800'">设置备注</h3>
        <button @click="handleClose" :class="isDark ? 'text-gray-500 hover:text-gray-300' : 'text-gray-400 hover:text-gray-600'">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>

      <form class="px-5 py-4" @submit.prevent="handleSubmit">
        <p class="mb-3 text-xs" :class="isDark ? 'text-gray-400' : 'text-gray-500'">给 {{ username }} 设置仅自己可见的备注名</p>
        <input
          v-model.trim="remarkName"
          type="text"
          maxlength="100"
          placeholder="请输入备注名"
          class="w-full px-3 py-2 text-sm border-0 focus:outline-none focus:ring-0"
          :class="isDark ? 'bg-gray-800 text-gray-200 placeholder-gray-500' : 'bg-gray-50 text-gray-700 placeholder-gray-400'"
        />
        <div class="mt-4 flex gap-3">
          <button
            type="button"
            class="flex-1 px-4 py-2 text-sm"
            :class="isDark ? 'bg-gray-800 text-gray-300 hover:bg-gray-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
            @click="handleClose"
          >
            取消
          </button>
          <button
            type="submit"
            class="flex-1 px-4 py-2 text-sm bg-[#0891B2] text-white hover:bg-[#0E7490] disabled:bg-gray-300"
            :disabled="!remarkName"
          >
            保存
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  isOpen: boolean
  isDark: boolean
  username: string
  initialRemark?: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'save', remarkName: string): void
}>()

const remarkName = ref('')

watch(() => props.isOpen, isOpen => {
  if (isOpen) {
    remarkName.value = props.initialRemark || ''
  }
})

const handleClose = () => {
  emit('close')
}

const handleSubmit = () => {
  if (!remarkName.value) return
  emit('save', remarkName.value)
}
</script>

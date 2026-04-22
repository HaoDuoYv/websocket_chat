<template>
  <div v-if="isOpen" class="fixed inset-0 bg-black/50 flex items-center justify-center z-[60] p-4">
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden">
      <!-- Header -->
      <div class="px-6 py-4 border-b border-gray-100">
        <h3 class="text-lg font-semibold text-gray-800">{{ title }}</h3>
      </div>
      
      <!-- Content -->
      <div class="px-6 py-4">
        <p class="text-gray-600">{{ message }}</p>
      </div>
      
      <!-- Actions -->
      <div class="px-6 py-4 bg-gray-50 flex gap-3">
        <button
          @click="handleCancel"
          class="flex-1 px-4 py-2.5 border border-gray-300 text-gray-700 text-sm font-medium rounded-xl hover:bg-white transition-colors"
        >
          {{ cancelText }}
        </button>
        <button
          @click="handleConfirm"
          class="flex-1 px-4 py-2.5 bg-[#18181B] text-white text-sm font-medium rounded-xl hover:bg-[#27272A] transition-colors"
        >
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  isOpen: boolean
  title: string
  message: string
  confirmText?: string
  cancelText?: string
}

withDefaults(defineProps<Props>(), {
  confirmText: '确认',
  cancelText: '取消'
})

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()

const handleConfirm = () => {
  emit('confirm')
}

const handleCancel = () => {
  emit('cancel')
}
</script>

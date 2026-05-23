<template>
  <Teleport to="body">
    <Transition name="toast">
      <div 
        v-if="visible"
        class="fixed top-6 left-1/2 -translate-x-1/2 z-[200] flex items-center gap-3 px-5 py-3 rounded-xl shadow-2xl backdrop-blur-sm max-w-sm"
        :class="typeClasses[type || 'info']"
      >
        <span class="text-lg">{{ icon }}</span>
        <p class="text-sm font-medium">{{ message }}</p>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  message: string
  type?: 'success' | 'error' | 'warning' | 'info'
  duration?: number
}>()

const visible = ref(false)

const typeClasses: Record<string, string> = {
  success: 'bg-emerald-600/95 text-white',
  error: 'bg-red-600/95 text-white',
  warning: 'bg-amber-500/95 text-white',
  info: 'bg-blue-600/95 text-white'
}

const icon = computed(() => {
  switch (props.type) {
    case 'success': return '✓'
    case 'error': return '✕'
    case 'warning': return '!'
    case 'info': return 'i'
    default: return '✓'
  }
})

const show = () => {
  visible.value = true
  setTimeout(() => {
    visible.value = false
  }, props.duration || 2500)
}

defineExpose({ show })
</script>

<style scoped>
.toast-enter-active {
  transition: all 0.3s ease-out;
}
.toast-leave-active {
  transition: all 0.3s ease-in;
}
.toast-enter-from {
  opacity: 0;
  transform: translate(-50%, -20px);
}
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -20px);
}
</style>

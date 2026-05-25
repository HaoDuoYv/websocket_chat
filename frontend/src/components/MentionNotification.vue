<script setup lang="ts">
defineProps<{
  count: number
  latestMention?: {
    senderName?: string
    content?: string
  }
  isDark: boolean
}>()

const emit = defineEmits<{
  click: []
  markRead: []
}>()

const handleClick = () => {
  emit('click')
}

const handleMarkRead = () => {
  emit('markRead')
}
</script>

<template>
  <div 
    class="px-4 py-2 cursor-pointer transition-colors"
    :class="isDark ? 'bg-blue-900/20 hover:bg-blue-900/30' : 'bg-blue-50 hover:bg-blue-100'"
    @click="handleClick"
  >
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-2">
        <span class="text-blue-500">🔔</span>
        <span class="text-sm font-medium" :class="isDark ? 'text-blue-300' : 'text-blue-600'">
          有人@我 ({{ count }})
        </span>
      </div>
      <button 
        class="text-xs px-2 py-1 rounded"
        :class="isDark ? 'text-gray-400 hover:text-gray-200' : 'text-gray-500 hover:text-gray-700'"
        @click.stop="handleMarkRead"
      >
        标记已读
      </button>
    </div>
    <div v-if="latestMention" class="mt-1 text-xs truncate" :class="isDark ? 'text-gray-400' : 'text-gray-500'">
      {{ latestMention.senderName }}: {{ latestMention.content?.substring(0, 50) }}{{ (latestMention.content?.length || 0) > 50 ? '...' : '' }}
    </div>
  </div>
</template>

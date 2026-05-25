<script setup lang="ts">
defineProps<{
  count: number
  latestMention?: { senderName?: string; content?: string } | null
  isDark: boolean
}>()

const emit = defineEmits<{
  (e: 'click'): void
  (e: 'mark-read'): void
}>()
</script>

<template>
  <div
    class="absolute top-[72px] left-1/2 -translate-x-1/2 z-30 cursor-pointer select-none"
    @click="emit('click')"
  >
    <div
      class="flex items-center gap-2 rounded-full px-4 py-2 shadow-lg backdrop-blur-sm transition-all hover:scale-105"
      :class="isDark ? 'bg-blue-600/90 text-white' : 'bg-blue-500/90 text-white'"
    >
      <span class="text-xs font-medium">
        {{ count }} 条未读 @消息
      </span>
      <span v-if="latestMention?.senderName" class="text-xs opacity-80 truncate max-w-[160px]">
        {{ latestMention.senderName }}: {{ latestMention.content?.slice(0, 20) }}
      </span>
      <button
        class="ml-1 text-xs opacity-70 hover:opacity-100 transition-opacity"
        @click.stop="emit('mark-read')"
      >
        标为已读
      </button>
    </div>
  </div>
</template>

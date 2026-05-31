<template>
  <span
    class="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold"
    :class="isDark ? 'bg-purple-900/40 text-purple-300' : 'bg-purple-100 text-purple-700'"
  >AI</span>
</template>

<script setup lang="ts">
defineProps<{ isDark: boolean }>()
</script>

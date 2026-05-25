<script setup lang="ts">
import { ref, computed, watch } from 'vue'

interface User {
  userId: string
  username: string
}

const props = defineProps<{
  users: User[]
  isDark: boolean
  position: { top: number; left: number }
}>()

const emit = defineEmits<{
  select: [user: User | null]
  close: []
}>()

const searchQuery = ref('')
const selectedIndex = ref(0)

const filteredUsers = computed(() => {
  if (!searchQuery.value) return props.users
  return props.users.filter(user => 
    user.username.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const handleSelect = (user: User | null) => {
  emit('select', user)
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    selectedIndex.value = Math.min(selectedIndex.value + 1, filteredUsers.value.length)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    selectedIndex.value = Math.max(selectedIndex.value - 1, 0)
  } else if (e.key === 'Enter') {
    e.preventDefault()
    e.stopPropagation()
    if (selectedIndex.value === 0) {
      handleSelect(null) // @所有人
    } else {
      handleSelect(filteredUsers.value[selectedIndex.value - 1])
    }
  } else if (e.key === 'Escape') {
    emit('close')
  }
}

watch(searchQuery, () => {
  selectedIndex.value = 0
})
</script>

<template>
  <div
    class="mention-list border"
    :class="isDark ? 'bg-gray-800 border-gray-700' : 'bg-white border-gray-200'"
    :style="{ top: position.top + 'px', left: position.left + 'px' }"
    @keydown="handleKeydown"
  >
    <div class="p-2">
      <input
        v-model="searchQuery"
        placeholder="搜索成员..."
        class="w-full px-2 py-1 text-sm border rounded"
        :class="isDark ? 'bg-gray-700 border-gray-600 text-white' : 'bg-gray-50 border-gray-300'"
        autofocus
      />
    </div>
    
    <div class="max-h-48 overflow-y-auto">
      <div
        class="px-3 py-2 cursor-pointer flex items-center gap-2"
        :class="[
          isDark ? 'hover:bg-gray-700' : 'hover:bg-gray-100',
          selectedIndex === 0 ? (isDark ? 'bg-gray-700' : 'bg-blue-50') : ''
        ]"
        @click="handleSelect(null)"
      >
        <span class="text-lg">👥</span>
        <span class="text-sm" :class="isDark ? 'text-white' : 'text-gray-800'">@所有人</span>
      </div>
      
      <div
        v-for="(user, index) in filteredUsers"
        :key="user.userId"
        class="px-3 py-2 cursor-pointer flex items-center gap-2"
        :class="[
          isDark ? 'hover:bg-gray-700' : 'hover:bg-gray-100',
          selectedIndex === index + 1 ? (isDark ? 'bg-gray-700' : 'bg-blue-50') : ''
        ]"
        @click="handleSelect(user)"
      >
        <span class="text-lg">👤</span>
        <span class="text-sm" :class="isDark ? 'text-white' : 'text-gray-800'">{{ user.username }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mention-list {
  position: fixed;
  z-index: 9999;
  min-width: 200px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
</style>

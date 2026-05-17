<script setup lang="ts">
interface User {
  userId: string
  username: string
  isOnline?: boolean
}

defineProps<{
  users: User[]
  isDark: boolean
}>()

const getAvatarColor = (userId: string) => {
  const colors = ['#18181B', '#3F3F46', '#52525B', '#71717A', '#A1A1AA', '#27272A', '#525252', '#737373']
  let hash = 0
  for (let i = 0; i < userId.length; i++) {
    hash = userId.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

const getAvatarText = (name: string) => name ? name.charAt(0).toUpperCase() : '?'
</script>

<template>
  <aside class="w-60 border-l hidden lg:flex flex-col" :class="isDark ? 'border-gray-800 bg-[#27272A]' : 'border-gray-100 bg-white'">
    <div class="px-5 py-4 border-b" :class="isDark ? 'border-gray-800' : 'border-gray-100'">
      <h3 class="text-sm font-medium" :class="isDark ? 'text-gray-200' : 'text-gray-800'">在线成员</h3>
      <p class="text-xs mt-1" :class="isDark ? 'text-gray-500' : 'text-gray-400'">{{ users.length }} 人在线</p>
    </div>

    <div class="flex-1 overflow-y-auto py-2">
      <div
        v-for="u in users"
        :key="u.userId"
        class="flex items-center gap-3 px-5 py-2.5 transition-colors"
        :class="isDark ? 'hover:bg-gray-800' : 'hover:bg-gray-50'"
      >
        <div class="relative">
          <div
            class="w-8 h-8 flex items-center justify-center text-white text-xs font-medium"
            :style="{ backgroundColor: getAvatarColor(u.userId) }"
          >
            {{ getAvatarText(u.username) }}
          </div>
          <div class="absolute bottom-0 right-0 w-2 h-2 bg-[#737373] border-2 rounded-full" :class="isDark ? 'border-gray-800' : 'border-white'"></div>
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-sm truncate" :class="isDark ? 'text-gray-200' : 'text-gray-800'">{{ u.username }}</p>
          <p class="text-xs text-[#737373]">在线</p>
        </div>
      </div>
    </div>
  </aside>
</template>

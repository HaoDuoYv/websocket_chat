<template>
  <div v-if="isOpen" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
    <div class="bg-white w-full max-w-md">
      <!-- Header -->
      <div class="px-6 py-4 border-b border-neutral-200 flex justify-between items-center">
        <h2 class="text-lg font-semibold text-neutral-900">邀请用户</h2>
        <button 
          @click="close" 
          class="text-neutral-400 hover:text-neutral-900 transition-colors"
        >
          ✕
        </button>
      </div>
      
      <!-- Content -->
      <div class="p-6 space-y-4">
        <!-- Selected Count -->
        <div v-if="selectedUsers.length > 0" class="text-sm text-neutral-600">
          已选择 {{ selectedUsers.length }} 位用户
        </div>
        
        <!-- User List -->
        <div>
          <label class="block text-sm font-medium text-neutral-700 mb-1.5">
            可邀请的用户
            <span v-if="availableUsers.length > 0" class="text-neutral-500 font-normal">
              ({{ availableUsers.length }})
            </span>
          </label>
          
          <!-- Empty State -->
          <div v-if="availableUsers.length === 0" class="p-4 bg-neutral-50 text-center">
            <p class="text-sm text-neutral-500">暂无可邀请的用户</p>
            <p class="text-xs text-neutral-400 mt-1">所有在线用户都已加入群聊</p>
          </div>
          
          <!-- User List -->
          <div v-else class="border border-neutral-200 max-h-48 overflow-y-auto">
            <div
              v-for="user in availableUsers"
              :key="user.userId"
              @click="toggleUser(user.userId)"
              class="flex items-center gap-3 px-3 py-2 cursor-pointer hover:bg-neutral-50 border-b border-neutral-100 last:border-b-0 transition-colors"
            >
              <div 
                :class="[
                  'w-4 h-4 border flex items-center justify-center transition-colors',
                  selectedUsers.includes(user.userId) 
                    ? 'bg-neutral-900 border-neutral-900 text-white' 
                    : 'border-neutral-300'
                ]"
              >
                <span v-if="selectedUsers.includes(user.userId)">✓</span>
              </div>
              <span class="text-sm text-neutral-700">{{ user.username }}</span>
            </div>
          </div>
        </div>
        
        <!-- Actions -->
        <div class="flex gap-3 pt-2">
          <button
            type="button"
            @click="close"
            class="flex-1 px-4 py-2 border border-neutral-300 text-neutral-700 text-sm font-medium hover:bg-neutral-50 transition-colors"
          >
            取消
          </button>
          <button
            @click="handleInvite"
            :disabled="selectedUsers.length === 0"
            class="flex-1 px-4 py-2 bg-neutral-900 text-white text-sm font-medium hover:bg-neutral-800 disabled:bg-neutral-300 disabled:cursor-not-allowed transition-colors"
          >
            邀请
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

interface User {
  userId: string
  username: string
  online?: boolean
}

const props = defineProps<{
  isOpen: boolean
  onlineUsers: User[]
  currentUserId: string
  chatParticipants: string[]
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'invite', userIds: string[]): void
}>()

const selectedUsers = ref<string[]>([])

const availableUsers = computed(() => {
  return props.onlineUsers.filter(user => 
    user.userId !== props.currentUserId && 
    !props.chatParticipants.includes(user.userId)
  )
})

const toggleUser = (userId: string) => {
  const index = selectedUsers.value.indexOf(userId)
  if (index > -1) {
    selectedUsers.value.splice(index, 1)
  } else {
    selectedUsers.value.push(userId)
  }
}

const handleInvite = () => {
  if (selectedUsers.value.length > 0) {
    emit('invite', selectedUsers.value)
    close()
  }
}

const close = () => {
  selectedUsers.value = []
  emit('close')
}
</script>

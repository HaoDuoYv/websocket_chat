<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import MentionList from './MentionList.vue'

interface User {
  userId: string
  username: string
}

const props = defineProps<{
  modelValue: string
  users: User[]
  isDark: boolean
  disabled: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  send: [content: string, mentions: User[], mentionAll: boolean]
}>()

const inputRef = ref<HTMLInputElement | null>(null)
const showMentionList = ref(false)
const mentionListPosition = ref({ top: 0, left: 0 })
const mentionStartPos = ref(-1)
const mentionedUsers = ref<User[]>([])
const mentionAll = ref(false)

const localValue = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const handleInput = (e: Event) => {
  const value = (e.target as HTMLInputElement).value
  const cursorPos = (e.target as HTMLInputElement).selectionStart || 0
  
  // 查找光标前最近的@符号
  const textBeforeCursor = value.substring(0, cursorPos)
  const lastAtIndex = textBeforeCursor.lastIndexOf('@')
  
  if (lastAtIndex !== -1) {
    // 检查@前面是否是空格、换行或行首
    const charBeforeAt = lastAtIndex > 0 ? value[lastAtIndex - 1] : ''
    if (lastAtIndex === 0 || charBeforeAt === ' ' || charBeforeAt === '\n') {
      mentionStartPos.value = lastAtIndex
      showMentionList.value = true
      updateMentionListPosition()
      return
    }
  }
  showMentionList.value = false
}

const updateMentionListPosition = () => {
  if (inputRef.value) {
    const rect = inputRef.value.getBoundingClientRect()
    mentionListPosition.value = {
      top: rect.bottom + 4,
      left: rect.left
    }
  }
}

const handleSelectMention = (user: User | null) => {
  if (user) {
    const beforeAt = localValue.value.substring(0, mentionStartPos.value)
    const afterCursor = localValue.value.substring(inputRef.value?.selectionStart || 0)
    localValue.value = `${beforeAt}@${user.username} ${afterCursor}`
    mentionedUsers.value.push(user)
  } else {
    const beforeAt = localValue.value.substring(0, mentionStartPos.value)
    const afterCursor = localValue.value.substring(inputRef.value?.selectionStart || 0)
    localValue.value = `${beforeAt}@所有人 ${afterCursor}`
    mentionAll.value = true
  }
  showMentionList.value = false
  nextTick(() => {
    inputRef.value?.focus()
  })
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !showMentionList.value) {
    e.preventDefault()
    handleSend()
  }
}

const handleSend = () => {
  const content = localValue.value.trim()
  if (content) {
    emit('send', content, mentionedUsers.value, mentionAll.value)
    localValue.value = ''
    mentionedUsers.value = []
    mentionAll.value = false
  }
}

const clearMentions = () => {
  mentionedUsers.value = []
  mentionAll.value = false
}

defineExpose({ clearMentions })
</script>

<template>
  <div class="relative">
    <input
      ref="inputRef"
      v-model="localValue"
      @input="handleInput"
      @keydown="handleKeydown"
      :disabled="disabled"
      placeholder="输入消息，@提及成员"
      class="w-full px-3 py-2.5 bg-transparent border border-[#E5E5E5] rounded-xl text-sm focus:outline-none focus:border-[#18181B] focus:ring-2 focus:ring-[#18181B]/10 transition-all duration-200"
      :class="[
        isDark ? 'border-gray-700 text-gray-200 placeholder-gray-500 bg-gray-800/50' : 'border-gray-200 text-gray-700 placeholder-gray-400',
        disabled ? 'opacity-50 cursor-not-allowed' : ''
      ]"
    />
    
    <MentionList
      v-if="showMentionList"
      :users="users"
      :is-dark="isDark"
      :position="mentionListPosition"
      @select="handleSelectMention"
      @close="showMentionList = false"
    />
  </div>
</template>

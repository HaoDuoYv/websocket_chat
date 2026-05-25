<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'

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
const showDropdown = ref(false)
const dropdownPos = ref({ top: 0, left: 0, width: 0 })
const mentionStartPos = ref(-1)
const mentionQuery = ref('')
const selectedIndex = ref(0)
const mentionedUsers = ref<User[]>([])
const mentionAll = ref(false)

const localValue = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const filteredUsers = computed(() => {
  const q = mentionQuery.value.toLowerCase()
  if (!q) return props.users
  return props.users.filter(u => u.username.toLowerCase().includes(q))
})

// 所有选项：@所有人 + 过滤后的用户
const allOptions = computed(() => [null, ...filteredUsers.value])

const updateDropdownPos = () => {
  if (!inputRef.value) return
  const rect = inputRef.value.getBoundingClientRect()
  dropdownPos.value = {
    top: rect.top - 4,
    left: rect.left,
    width: rect.width
  }
}

const viewportHeight = computed(() => window.innerHeight)

const openDropdown = () => {
  selectedIndex.value = 0
  showDropdown.value = true
  updateDropdownPos()
}

const closeDropdown = () => {
  showDropdown.value = false
  mentionStartPos.value = -1
  mentionQuery.value = ''
}

const handleInput = (e: Event) => {
  const input = e.target as HTMLInputElement
  const val = input.value
  const cursor = input.selectionStart ?? val.length

  const textBefore = val.substring(0, cursor)
  const atIdx = textBefore.lastIndexOf('@')

  if (atIdx !== -1) {
    const afterAt = textBefore.substring(atIdx + 1)
    // @后面没有空格才触发
    if (!afterAt.includes(' ')) {
      mentionStartPos.value = atIdx
      mentionQuery.value = afterAt
      selectedIndex.value = 0
      openDropdown()
      return
    }
  }

  closeDropdown()
}

const selectOption = (option: User | null) => {
  const before = localValue.value.substring(0, mentionStartPos.value)
  const after = localValue.value.substring(inputRef.value?.selectionStart ?? localValue.value.length)

  if (option === null) {
    localValue.value = `${before}@所有人 ${after}`
    mentionAll.value = true
  } else {
    localValue.value = `${before}@${option.username} ${after}`
    if (!mentionedUsers.value.find(u => u.userId === option.userId)) {
      mentionedUsers.value.push(option)
    }
  }

  closeDropdown()
  nextTick(() => {
    inputRef.value?.focus()
    // 把光标移到插入内容之后
    const pos = before.length + (option === null ? '@所有人 '.length : `@${option.username} `.length)
    inputRef.value?.setSelectionRange(pos, pos)
  })
}

const handleKeydown = (e: KeyboardEvent) => {
  if (showDropdown.value) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      selectedIndex.value = Math.min(selectedIndex.value + 1, allOptions.value.length - 1)
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      selectedIndex.value = Math.max(selectedIndex.value - 1, 0)
    } else if (e.key === 'Enter') {
      e.preventDefault()
      e.stopPropagation()
      selectOption(allOptions.value[selectedIndex.value] ?? null)
    } else if (e.key === 'Escape') {
      e.preventDefault()
      closeDropdown()
    }
    return
  }

  if (e.key === 'Enter') {
    e.preventDefault()
    handleSend()
  }
}

const handleSend = () => {
  const content = localValue.value.trim()
  if (!content) return
  emit('send', content, mentionedUsers.value, mentionAll.value)
  localValue.value = ''
  mentionedUsers.value = []
  mentionAll.value = false
}

// 点击外部关闭
const handleClickOutside = (_e: MouseEvent) => {
  if (showDropdown.value) closeDropdown()
}

onMounted(() => document.addEventListener('mousedown', handleClickOutside))
onUnmounted(() => document.removeEventListener('mousedown', handleClickOutside))

const clearMentions = () => {
  mentionedUsers.value = []
  mentionAll.value = false
}

defineExpose({ clearMentions, triggerSend: handleSend })
</script>

<template>
  <div class="relative w-full">
    <input
      ref="inputRef"
      v-model="localValue"
      @input="handleInput"
      @keydown="handleKeydown"
      :disabled="disabled"
      placeholder="输入消息，@提及成员"
      class="w-full px-3 py-2.5 bg-transparent border rounded-xl text-sm focus:outline-none focus:ring-2 transition-all duration-200"
      :class="[
        isDark
          ? 'border-gray-700 text-gray-200 placeholder-gray-500 bg-gray-800/50 focus:border-gray-500 focus:ring-gray-700/30'
          : 'border-gray-200 text-gray-700 placeholder-gray-400 focus:border-[#18181B] focus:ring-[#18181B]/10',
        disabled ? 'opacity-50 cursor-not-allowed' : ''
      ]"
    />

    <!-- 下拉菜单：Teleport 到 body，避免父容器定位干扰 -->
    <Teleport to="body">
      <div
        v-if="showDropdown && allOptions.length > 0"
        class="mention-dropdown"
        :class="isDark ? 'bg-gray-800 border-gray-700 text-gray-100' : 'bg-white border-gray-200 text-gray-800'"
        :style="{
          position: 'fixed',
          bottom: (viewportHeight - dropdownPos.top) + 'px',
          left: dropdownPos.left + 'px',
          minWidth: dropdownPos.width + 'px',
          zIndex: 9999
        }"
        @mousedown.prevent
      >
        <div
          v-for="(option, idx) in allOptions"
          :key="option ? option.userId : '__all__'"
          class="flex items-center gap-2 px-3 py-2 cursor-pointer text-sm"
          :class="[
            idx === selectedIndex
              ? (isDark ? 'bg-gray-700' : 'bg-blue-50')
              : (isDark ? 'hover:bg-gray-700' : 'hover:bg-gray-50')
          ]"
          @mousedown.prevent="selectOption(option)"
        >
          <span class="text-base">{{ option === null ? '👥' : '👤' }}</span>
          <span>{{ option === null ? '@所有人' : option.username }}</span>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.mention-dropdown {
  border-width: 1px;
  border-style: solid;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  max-height: 220px;
  overflow-y: auto;
}
</style>

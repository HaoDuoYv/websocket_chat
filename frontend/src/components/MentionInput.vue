<script setup lang="ts">
import { ref, computed, nextTick, onBeforeUnmount } from 'vue'

interface User {
  userId: string
  username: string
}

interface AssistantOption {
  userId: string         // 复用 userId 字段，存 assistantId
  username: string       // 智能体名
  isAssistant: true
  avatarIcon?: string
  avatarColor?: string
}

const props = defineProps<{
  modelValue: string
  users: User[]
  isDark: boolean
  disabled: boolean
  currentUserId?: string
  assistants?: AssistantOption[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  send: [content: string, mentions: User[], mentionAll: boolean, assistants: AssistantOption[]]
}>()

const inputRef = ref<HTMLInputElement | null>(null)
const dropdownRef = ref<HTMLDivElement | null>(null)
const showDropdown = ref(false)
const mentionQuery = ref('')
const selectedIndex = ref(0)
const mentionedUsers = ref<User[]>([])
const mentionedAssistants = ref<AssistantOption[]>([])
const mentionAll = ref(false)
const mentionStart = ref(-1)

// 输入框的值，双向绑定
const inputValue = computed({
  get: () => props.modelValue,
  set: (v: string) => emit('update:modelValue', v)
})

// 过滤后的用户列表（排除自己）
const filtered = computed(() => {
  const q = mentionQuery.value.toLowerCase()
  let list = props.users.filter(u => u.userId !== props.currentUserId)
  if (q) list = list.filter(u => u.username.toLowerCase().includes(q))
  return list
})

// 所有可选项：@所有人 放在最前面，其次是智能体，最后是用户
const options = computed(() => {
  const q = mentionQuery.value.toLowerCase()
  const assistantList = (props.assistants ?? []).filter(a =>
    !q || a.username.toLowerCase().includes(q)
  )
  return [
    { userId: '__all__', username: '所有人' } as User,
    ...assistantList,
    ...filtered.value
  ] as Array<User | AssistantOption>
})

// 更新下拉位置（相对于输入框上方）
const updatePos = () => {
  if (!inputRef.value || !dropdownRef.value) return
  const rect = inputRef.value.getBoundingClientRect()
  const dd = dropdownRef.value
  dd.style.position = 'fixed'
  dd.style.bottom = (window.innerHeight - rect.top + 4) + 'px'
  dd.style.left = rect.left + 'px'
  dd.style.width = rect.width + 'px'
}

// 检测输入框中的 @ 触发
const onInput = () => {
  const el = inputRef.value
  if (!el) return
  const val = el.value
  const cur = el.selectionStart ?? val.length
  const before = val.slice(0, cur)
  const at = before.lastIndexOf('@')

  if (at >= 0 && !before.slice(at + 1).includes(' ')) {
    mentionStart.value = at
    mentionQuery.value = before.slice(at + 1)
    selectedIndex.value = 0
    showDropdown.value = true
    nextTick(updatePos)
  } else {
    close()
  }
}

const close = () => {
  showDropdown.value = false
  mentionStart.value = -1
  mentionQuery.value = ''
}

// 选择一个选项
const pick = (opt: User | AssistantOption) => {
  const el = inputRef.value
  if (!el) return
  const val = el.value
  const before = val.slice(0, mentionStart.value)
  const after = val.slice(el.selectionStart ?? val.length)
  const isAll = opt.userId === '__all__'
  const isAssistant = (opt as AssistantOption).isAssistant === true
  const name = isAll ? '所有人' : opt.username
  const insert = `@${name} `

  inputValue.value = before + insert + after
  if (isAll) {
    mentionAll.value = true
  } else if (isAssistant) {
    if (!mentionedAssistants.value.find(a => a.userId === opt.userId)) {
      mentionedAssistants.value.push(opt as AssistantOption)
    }
  } else {
    if (!mentionedUsers.value.find(u => u.userId === opt.userId)) {
      mentionedUsers.value.push(opt as User)
    }
  }
  close()
  nextTick(() => {
    el.focus()
    const pos = before.length + insert.length
    el.setSelectionRange(pos, pos)
  })
}

// 键盘事件
const onKeydown = (e: KeyboardEvent) => {
  if (showDropdown.value) {
    const len = options.value.length
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      selectedIndex.value = (selectedIndex.value + 1) % len
      scrollToSelected()
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      selectedIndex.value = (selectedIndex.value - 1 + len) % len
      scrollToSelected()
    } else if (e.key === 'Enter') {
      e.preventDefault()
      e.stopPropagation()
      pick(options.value[selectedIndex.value])
    } else if (e.key === 'Escape') {
      e.preventDefault()
      close()
    }
    return
  }
  if (e.key === 'Enter' && !e.isComposing) {
    e.preventDefault()
    doSend()
  }
}

const scrollToSelected = () => {
  nextTick(() => {
    const dd = dropdownRef.value
    if (!dd) return
    const item = dd.querySelector('[data-selected="true"]') as HTMLElement
    if (item) item.scrollIntoView({ block: 'nearest' })
  })
}

// 发送
const doSend = () => {
  const content = inputValue.value.trim()
  if (!content) return
  emit('send', content, mentionedUsers.value, mentionAll.value, mentionedAssistants.value)
  inputValue.value = ''
  mentionedUsers.value = []
  mentionedAssistants.value = []
  mentionAll.value = false
}

// 全局 mousedown 关闭下拉（排除下拉菜单自身和输入框）
const onDocMouseDown = (e: MouseEvent) => {
  if (!showDropdown.value) return
  const t = e.target as Node
  if (inputRef.value?.contains(t)) return
  if (dropdownRef.value?.contains(t)) return
  close()
}

document.addEventListener('mousedown', onDocMouseDown)
onBeforeUnmount(() => document.removeEventListener('mousedown', onDocMouseDown))

const clearMentions = () => {
  mentionedUsers.value = []
  mentionedAssistants.value = []
  mentionAll.value = false
}

defineExpose({ clearMentions, triggerSend: doSend })
</script>

<template>
  <div class="relative w-full">
    <input
      ref="inputRef"
      :value="inputValue"
      @input="(e: Event) => { inputValue = (e.target as HTMLInputElement).value; onInput() }"
      @keydown="onKeydown"
      :disabled="disabled"
      placeholder="输入消息，@提及成员"
      class="mention-input"
      :class="[
        isDark
          ? 'border-gray-700 text-gray-200 placeholder-gray-500 bg-gray-800/50 focus:border-gray-500 focus:ring-gray-700/30'
          : 'border-gray-200 text-gray-700 placeholder-gray-400 focus:border-[#18181B] focus:ring-[#18181B]/10',
        disabled ? 'opacity-50 cursor-not-allowed' : ''
      ]"
    />
    <!-- 下拉菜单：Teleport 到 body，用 inline style 确保样式生效 -->
    <Teleport to="body">
      <div
        ref="dropdownRef"
        v-show="showDropdown && options.length > 0"
        class="mention-dd"
        :class="isDark ? 'bg-gray-800 border-gray-700 text-gray-100' : 'bg-white border-gray-200 text-gray-800'"
        @mousedown.prevent
      >
        <div
          v-for="(opt, idx) in options"
          :key="opt.userId"
          :data-selected="idx === selectedIndex"
          class="mention-item"
          :class="[
            idx === selectedIndex
              ? (isDark ? 'bg-gray-700' : 'bg-blue-50')
              : (isDark ? 'hover:bg-gray-700' : 'hover:bg-gray-50')
          ]"
          @mousedown.prevent="pick(opt)"
        >
          <span v-if="opt.userId === '__all__'" class="mention-icon">👥</span>
          <span v-else-if="(opt as AssistantOption).isAssistant" class="mention-icon" :style="(opt as AssistantOption).avatarIcon ? { background: (opt as AssistantOption).avatarColor, borderRadius: '50%', width: '1.25rem', height: '1.25rem', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.75rem', color: '#fff', flexShrink: 0 } : {}">
            {{ (opt as AssistantOption).avatarIcon || '🤖' }}
          </span>
          <span v-else class="mention-icon">👤</span>
          <span>{{ opt.userId === '__all__' ? '@所有人' : `@${opt.username}` }}</span>
          <span v-if="(opt as AssistantOption).isAssistant" class="ml-auto text-[10px] px-1.5 py-0.5 rounded bg-purple-100 text-purple-700 dark:bg-purple-900/40 dark:text-purple-300 flex-shrink-0">AI</span>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.mention-input {
  width: 100%;
  padding: 0.625rem 0.75rem;
  background: transparent;
  border-width: 1px;
  border-style: solid;
  border-radius: 0.75rem;
  font-size: 0.875rem;
  outline: none;
  transition: all 0.2s;
}
.mention-input:focus {
  --tw-ring-offset-shadow: var(--tw-ring-inset) 0 0 0 var(--tw-ring-offset-width) var(--tw-ring-offset-color);
  --tw-ring-shadow: var(--tw-ring-inset) 0 0 0 calc(2px + var(--tw-ring-offset-width)) var(--tw-ring-color);
  box-shadow: var(--tw-ring-offset-shadow), var(--tw-ring-shadow), var(--tw-shadow, 0 0 #0000);
}
</style>

<style>
/* 全局样式：Teleport 到 body 的下拉菜单 */
.mention-dd {
  border-width: 1px;
  border-style: solid;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  max-height: 220px;
  overflow-y: auto;
  z-index: 99999;
}
.mention-dd .mention-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  cursor: pointer;
  font-size: 0.875rem;
  user-select: none;
}
.mention-dd .mention-icon {
  font-size: 1rem;
  flex-shrink: 0;
}
</style>

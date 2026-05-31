<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-[80] flex items-center justify-center bg-black/40"
      @click.self="$emit('close')"
    >
      <div
        class="w-[480px] max-h-[70vh] rounded-xl overflow-hidden flex flex-col"
        :class="isDark ? 'bg-gray-900 text-gray-100' : 'bg-white text-gray-800'"
      >
        <div class="px-4 py-3 border-b" :class="isDark ? 'border-gray-800' : 'border-gray-200'">
          <div class="text-base font-semibold">添加智能体到群</div>
        </div>
        <div class="flex border-b" :class="isDark ? 'border-gray-800' : 'border-gray-200'">
          <button
            v-for="t in tabs"
            :key="t.key"
            class="flex-1 py-2 text-sm"
            :class="[
              activeTab === t.key
                ? (isDark ? 'border-b-2 border-purple-500 text-purple-400' : 'border-b-2 border-purple-600 text-purple-700')
                : (isDark ? 'text-gray-400' : 'text-gray-500')
            ]"
            @click="activeTab = t.key"
          >{{ t.label }}</button>
        </div>
        <div class="flex-1 overflow-y-auto p-3">
          <div v-if="visibleList.length === 0" class="text-sm text-center py-8" :class="isDark ? 'text-gray-500' : 'text-gray-400'">
            暂无可添加的智能体
          </div>
          <div
            v-for="a in visibleList"
            :key="a.id"
            class="flex items-center gap-3 px-3 py-2 rounded-lg cursor-pointer"
            :class="isDark ? 'hover:bg-gray-800' : 'hover:bg-gray-100'"
            @click="$emit('select', a)"
          >
            <div
              class="w-9 h-9 rounded-full flex items-center justify-center text-white text-sm"
              :style="{ backgroundColor: a.avatarColor || '#7c3aed' }"
            >{{ (a.avatarIcon || a.name || 'A').slice(0, 1) }}</div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-1">
                <span class="text-sm font-medium truncate">{{ a.name }}</span>
                <AssistantBadge :is-dark="isDark" />
              </div>
              <div class="text-xs truncate" :class="isDark ? 'text-gray-500' : 'text-gray-400'">{{ a.model || '' }}</div>
            </div>
          </div>
        </div>
        <div class="px-4 py-2 border-t flex justify-end" :class="isDark ? 'border-gray-800' : 'border-gray-200'">
          <button
            class="px-4 py-1.5 rounded-md text-sm"
            :class="isDark ? 'bg-gray-800 hover:bg-gray-700 text-gray-200' : 'bg-gray-100 hover:bg-gray-200 text-gray-700'"
            @click="$emit('close')"
          >取消</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import AssistantBadge from './AssistantBadge.vue'

const props = defineProps<{
  open: boolean
  available: any[]
  alreadyInRoomIds: string[]
  isDark: boolean
  currentUserId: string
}>()

defineEmits<{
  close: []
  select: [assistant: any]
}>()

const tabs: Array<{ key: 'mine' | 'public'; label: string }> = [
  { key: 'mine', label: '我的' },
  { key: 'public', label: '公开' }
]
const activeTab = ref<'mine' | 'public'>('mine')

const visibleList = computed(() => {
  const inRoom = new Set(props.alreadyInRoomIds.map(String))
  const list = props.available.filter(a => !inRoom.has(String(a.id)))
  if (activeTab.value === 'mine') {
    return list.filter(a => String(a.ownerId || '') === String(props.currentUserId))
  }
  return list.filter(a => a.isPublic === true || a.isSystem === true)
})
</script>

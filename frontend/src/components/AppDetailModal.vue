<template>
  <div
    v-if="isOpen"
    class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
    @click.self="handleClose"
  >
    <div
      :class="[
        'w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden transition-colors',
        isDark ? 'bg-[#27272A] text-gray-100' : 'bg-white text-gray-800'
      ]"
    >
      <!-- Header -->
      <div
        :class="[
          'px-6 py-5 flex items-start gap-4 border-b',
          isDark ? 'border-gray-700' : 'border-gray-100'
        ]"
      >
        <img
          v-if="app"
          :src="app.img"
          :alt="app.title"
          class="w-14 h-14 rounded-xl object-cover flex-shrink-0"
        />
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2">
            <h3 class="text-lg font-semibold truncate">{{ app?.title }}</h3>
            <span
              v-if="app?.status"
              :class="[
                'inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium flex-shrink-0',
                statusClass
              ]"
            >
              {{ statusLabel }}
            </span>
          </div>
          <p
            :class="[
              'mt-1 text-sm',
              isDark ? 'text-gray-400' : 'text-gray-500'
            ]"
          >
            {{ app?.subtitle }}
          </p>
        </div>
        <button
          @click="handleClose"
          :class="[
            'flex-shrink-0 transition-colors',
            isDark ? 'text-gray-500 hover:text-gray-300' : 'text-gray-400 hover:text-gray-600'
          ]"
        >
          ✕
        </button>
      </div>

      <!-- Body -->
      <div class="px-6 py-5 space-y-5 max-h-[60vh] overflow-y-auto">
        <!-- Description -->
        <div v-if="app?.description">
          <h4
            :class="[
              'text-sm font-medium mb-1.5',
              isDark ? 'text-gray-300' : 'text-gray-700'
            ]"
          >
            功能描述
          </h4>
          <p
            :class="[
              'text-sm leading-relaxed',
              isDark ? 'text-gray-400' : 'text-gray-600'
            ]"
          >
            {{ app.description }}
          </p>
        </div>

        <!-- Tech Stack -->
        <div v-if="app?.techStack?.length">
          <h4
            :class="[
              'text-sm font-medium mb-2',
              isDark ? 'text-gray-300' : 'text-gray-700'
            ]"
          >
            技术栈
          </h4>
          <div class="flex flex-wrap gap-2">
            <span
              v-for="tech in app.techStack"
              :key="tech"
              :class="[
                'px-2.5 py-1 rounded-lg text-xs font-medium',
                isDark
                  ? 'bg-gray-800 text-gray-300 border border-gray-700'
                  : 'bg-gray-100 text-gray-600 border border-gray-200'
              ]"
            >
              {{ tech }}
            </span>
          </div>
        </div>

        <!-- Usage Scenario -->
        <div v-if="app?.usageScenario">
          <h4
            :class="[
              'text-sm font-medium mb-1.5',
              isDark ? 'text-gray-300' : 'text-gray-700'
            ]"
          >
            使用场景
          </h4>
          <p
            :class="[
              'text-sm leading-relaxed',
              isDark ? 'text-gray-400' : 'text-gray-600'
            ]"
          >
            {{ app.usageScenario }}
          </p>
        </div>
      </div>

      <!-- Actions -->
      <div
        :class="[
          'px-6 py-4 flex gap-3 border-t',
          isDark ? 'border-gray-700 bg-gray-800/50' : 'border-gray-100 bg-gray-50'
        ]"
      >
        <button
          @click="handleClose"
          :class="[
            'flex-1 px-4 py-2.5 text-sm font-medium rounded-xl border transition-colors',
            isDark
              ? 'border-gray-600 text-gray-300 hover:bg-gray-700'
              : 'border-gray-300 text-gray-700 hover:bg-white'
          ]"
        >
          关闭
        </button>
        <a
          v-if="app?.url"
          :href="app.url"
          target="_blank"
          rel="noopener noreferrer"
          class="flex-1 px-4 py-2.5 bg-[#18181B] text-white text-sm font-medium rounded-xl hover:bg-[#27272A] transition-colors text-center"
        >
          {{ app.go || '前往' }}
        </a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch, onUnmounted } from 'vue'
import type { AppConfig } from '@/config/apps'

interface Props {
  isOpen: boolean
  app: AppConfig | null
  isDark: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  close: []
}>()

const statusMap: Record<string, { label: string; class: string; darkClass: string }> = {
  active: {
    label: '已完成',
    class: 'bg-green-100 text-green-700',
    darkClass: 'bg-green-900/30 text-green-400'
  },
  developing: {
    label: '开发中',
    class: 'bg-yellow-100 text-yellow-700',
    darkClass: 'bg-yellow-900/30 text-yellow-400'
  },
  planned: {
    label: '规划中',
    class: 'bg-gray-100 text-gray-600',
    darkClass: 'bg-gray-700/50 text-gray-400'
  }
}

const statusLabel = computed(() => {
  const status = props.app?.status
  return status ? statusMap[status]?.label ?? '' : ''
})

const statusClass = computed(() => {
  const status = props.app?.status
  if (!status) return ''
  const entry = statusMap[status]
  return props.isDark ? entry?.darkClass ?? '' : entry?.class ?? ''
})

function handleClose() {
  emit('close')
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.isOpen) {
    handleClose()
  }
}

watch(
  () => props.isOpen,
  (open) => {
    if (open) {
      document.addEventListener('keydown', handleKeydown)
      document.body.style.overflow = 'hidden'
    } else {
      document.removeEventListener('keydown', handleKeydown)
      document.body.style.overflow = ''
    }
  }
)

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
})
</script>

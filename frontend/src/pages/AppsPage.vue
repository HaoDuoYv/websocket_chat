<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { AppConfig } from '@/config/apps'
import { appsConfig } from '@/config/apps'
import AppDetailModal from '@/components/AppDetailModal.vue'

const router = useRouter()

const isLoading = ref(true)
const apps = ref<AppConfig[]>([])
const error = ref('')
const selectedApp = ref<AppConfig | null>(null)
const isDetailOpen = ref(false)
const isDark = ref(localStorage.getItem('theme') === 'dark')

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

function getStatusClass(status?: string): string {
  if (!status) return ''
  const entry = statusMap[status]
  return isDark.value ? entry?.darkClass ?? '' : entry?.class ?? ''
}

function getStatusLabel(status?: string): string {
  if (!status) return ''
  return statusMap[status]?.label ?? ''
}

async function fetchApps() {
  isLoading.value = true
  error.value = ''

  try {
    const response = await fetch('/api/apps')
    if (!response.ok) throw new Error(`请求失败: ${response.status}`)
    const data = await response.json()
    apps.value = Array.isArray(data) ? data : data.apps ?? data.data ?? []
  } catch (err: any) {
    console.warn('接口请求失败，使用本地兜底数据:', err?.message)
    apps.value = appsConfig
    error.value = ''
  } finally {
    isLoading.value = false
  }
}

function openDetail(app: AppConfig) {
  selectedApp.value = app
  isDetailOpen.value = true
}

function closeDetail() {
  isDetailOpen.value = false
  selectedApp.value = null
}

function openUrl(url: string) {
  if (!url) return
  window.open(url, '_blank', 'noopener,noreferrer')
}

function goBack() {
  router.back()
}

onMounted(fetchApps)
</script>

<template>
  <div class="min-h-screen transition-colors" :class="isDark ? 'bg-[#18181B] text-gray-100' : 'bg-white text-gray-800'">
    <!-- 顶部导航 -->
    <header class="border-b px-6 py-5" :class="isDark ? 'border-gray-800' : 'border-gray-100'">
      <div class="max-w-7xl mx-auto flex items-center gap-4">
        <button
          @click="goBack"
          class="w-9 h-9 flex items-center justify-center rounded-xl transition-colors"
          :class="isDark ? 'hover:bg-gray-800 text-gray-400 hover:text-gray-200' : 'hover:bg-gray-100 text-gray-500 hover:text-gray-700'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="19" y1="12" x2="5" y2="12"/>
            <polyline points="12 19 5 12 12 5"/>
          </svg>
        </button>
        <div class="flex-1">
          <h1 class="text-lg font-semibold">应用中心</h1>
        </div>
        <span
          v-if="!isLoading"
          class="text-xs px-2.5 py-1 rounded-full"
          :class="isDark ? 'bg-gray-800 text-gray-400' : 'bg-gray-100 text-gray-500'"
        >
          {{ apps.length }} 个应用
        </span>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="max-w-7xl mx-auto px-6 py-8">
      <!-- 加载骨架屏 -->
      <div v-if="isLoading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        <div
          v-for="i in 4"
          :key="i"
          class="rounded-2xl border p-5 animate-pulse"
          :class="isDark ? 'border-gray-700 bg-[#27272A]' : 'border-gray-200 bg-gray-50'"
        >
          <div class="flex items-center gap-3 mb-4">
            <div class="w-12 h-12 rounded-xl" :class="isDark ? 'bg-gray-700' : 'bg-gray-200'"></div>
            <div class="flex-1">
              <div class="h-4 w-24 rounded mb-2" :class="isDark ? 'bg-gray-700' : 'bg-gray-200'"></div>
              <div class="h-3 w-16 rounded" :class="isDark ? 'bg-gray-700' : 'bg-gray-200'"></div>
            </div>
          </div>
          <div class="h-3 w-full rounded mb-2" :class="isDark ? 'bg-gray-700' : 'bg-gray-200'"></div>
          <div class="h-3 w-2/3 rounded mb-4" :class="isDark ? 'bg-gray-700' : 'bg-gray-200'"></div>
          <div class="flex gap-2">
            <div class="h-8 flex-1 rounded-xl" :class="isDark ? 'bg-gray-700' : 'bg-gray-200'"></div>
            <div class="h-8 flex-1 rounded-xl" :class="isDark ? 'bg-gray-700' : 'bg-gray-200'"></div>
          </div>
        </div>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="error" class="flex flex-col items-center justify-center py-24">
        <div class="w-12 h-12 rounded-full flex items-center justify-center mb-4" :class="isDark ? 'bg-red-900/30' : 'bg-red-50'">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="text-red-500">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        </div>
        <p class="text-sm mb-4" :class="isDark ? 'text-gray-400' : 'text-gray-500'">{{ error }}</p>
        <button
          @click="fetchApps"
          class="px-4 py-2 bg-[#18181B] text-white text-sm rounded-xl hover:bg-[#27272A] transition-colors"
        >
          重试
        </button>
      </div>

      <!-- 空状态 -->
      <div v-else-if="apps.length === 0" class="flex flex-col items-center justify-center py-24">
        <div class="w-12 h-12 rounded-full flex items-center justify-center mb-4" :class="isDark ? 'bg-gray-800' : 'bg-gray-100'">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" :class="isDark ? 'text-gray-500' : 'text-gray-400'">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
          </svg>
        </div>
        <p class="text-sm" :class="isDark ? 'text-gray-500' : 'text-gray-400'">暂无应用</p>
      </div>

      <!-- 应用卡片网格 -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        <div
          v-for="app in apps"
          :key="app.id"
          class="rounded-2xl border p-5 transition-all duration-200 hover:shadow-lg hover:-translate-y-0.5"
          :class="isDark ? 'border-gray-700 bg-[#27272A] hover:border-gray-600' : 'border-gray-200 bg-white hover:border-gray-300'"
        >
          <!-- 卡片头部：图标 + 标题 + 状态 -->
          <div class="flex items-start gap-3 mb-3">
            <img
              :src="app.img"
              :alt="app.title"
              class="w-12 h-12 rounded-xl object-cover flex-shrink-0"
            />
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <h3 class="font-medium text-sm truncate">{{ app.title }}</h3>
                <span
                  v-if="app.status"
                  :class="['inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium flex-shrink-0', getStatusClass(app.status)]"
                >
                  {{ getStatusLabel(app.status) }}
                </span>
              </div>
              <p class="text-xs mt-0.5 truncate" :class="isDark ? 'text-gray-500' : 'text-gray-400'">
                {{ app.subtitle }}
              </p>
            </div>
          </div>

          <!-- 描述 -->
          <p class="text-xs leading-relaxed line-clamp-2 mb-4" :class="isDark ? 'text-gray-400' : 'text-gray-500'">
            {{ app.text }}
          </p>

          <!-- 操作按钮 -->
          <div class="flex gap-2">
            <button
              @click="openDetail(app)"
              class="flex-1 px-3 py-2 text-xs font-medium rounded-xl border transition-colors"
              :class="isDark
                ? 'border-gray-600 text-gray-300 hover:bg-gray-700'
                : 'border-gray-300 text-gray-700 hover:bg-gray-50'"
            >
              项目说明
            </button>
            <button
              v-if="app.url"
              @click="openUrl(app.url)"
              class="flex-1 px-3 py-2 text-xs font-medium rounded-xl bg-[#18181B] text-white hover:bg-[#27272A] transition-colors"
            >
              尝试一下
            </button>
            <button
              v-else
              disabled
              class="flex-1 px-3 py-2 text-xs font-medium rounded-xl transition-colors cursor-not-allowed"
              :class="isDark
                ? 'bg-gray-700 text-gray-500'
                : 'bg-gray-100 text-gray-400'"
            >
              暂未开放
            </button>
          </div>
        </div>
      </div>
    </main>

    <!-- 详情模态窗口 -->
    <AppDetailModal
      :is-open="isDetailOpen"
      :app="selectedApp"
      :is-dark="isDark"
      @close="closeDetail"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useEditorWebSocket } from '@/composables/useEditorWebSocket'
import CodeEditor from '@/components/CodeEditor.vue'
import LivePreview from '@/components/LivePreview.vue'

const route = useRoute()
const router = useRouter()

const {
  isConnected,
  connectionError,
  participants,
  currentRoomName,
  connect,
  disconnect,
  joinDoc,
  leaveDoc,
  sendYjsUpdate,
  setOnYjsUpdate,
  setOnSyncRequest
} = useEditorWebSocket()

const user = ref<{ userId: string; username: string } | null>(null)
const isDarkTheme = ref(false)
const activeLanguage = ref<'html' | 'css' | 'js'>('html')
const docId = computed(() => route.params.docId as string)

// Preview content
const htmlContent = ref('')
const cssContent = ref('')
const jsContent = ref('')

const previewKey = ref(0)

const languages = [
  { key: 'html' as const, label: 'HTML', icon: 'H' },
  { key: 'css' as const, label: 'CSS', icon: 'C' },
  { key: 'js' as const, label: 'JS', icon: 'J' }
]

function handleTextChange(language: string, text: string) {
  if (language === 'html') htmlContent.value = text
  else if (language === 'css') cssContent.value = text
  else if (language === 'js') jsContent.value = text
}

function goBack() {
  leaveDoc(docId.value)
  router.push('/gomoku')
}

function refreshPreview() {
  previewKey.value++
}

onMounted(() => {
  if (!route.params.docId) {
    router.replace('/gomoku')
    return
  }

  const userData = localStorage.getItem('user')
  if (!userData) {
    router.push('/')
    return
  }
  user.value = JSON.parse(userData)

  const savedTheme = localStorage.getItem('theme')
  isDarkTheme.value = savedTheme === 'dark'

  connect(user.value!.userId, user.value!.username)
})

// Auto-join doc when connected
watch(isConnected, (connected) => {
  if (connected && docId.value) {
    joinDoc(docId.value)
  }
})

function retryConnection() {
  if (!user.value) return
  connect(user.value.userId, user.value.username)
}

onUnmounted(() => {
  if (docId.value) {
    leaveDoc(docId.value)
  }
  disconnect()
})
</script>

<template>
  <div class="h-screen flex flex-col" :class="isDarkTheme ? 'bg-[#18181B] text-gray-200' : 'bg-white text-gray-800'">
    <!-- Header -->
    <header
      class="flex items-center justify-between px-4 h-11 border-b flex-shrink-0"
      :class="isDarkTheme ? 'border-gray-800 bg-[#1c1c1f]' : 'border-gray-200 bg-gray-50/50'"
    >
      <div class="flex items-center gap-2">
        <button
          @click="goBack"
          class="flex items-center justify-center w-7 h-7 rounded-md transition-colors"
          :class="isDarkTheme ? 'hover:bg-gray-700/60 text-gray-400 hover:text-gray-200' : 'hover:bg-gray-200 text-gray-500 hover:text-gray-700'"
          title="返回大厅"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <div class="w-px h-4" :class="isDarkTheme ? 'bg-gray-700' : 'bg-gray-300'"></div>
        <span class="text-xs font-medium" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-600'">
          {{ currentRoomName || '协作编辑器' }}
        </span>
        <span class="text-[10px] px-1.5 py-0.5 rounded font-medium" :class="isDarkTheme ? 'bg-blue-900/30 text-blue-400' : 'bg-blue-50 text-blue-600'">
          {{ docId }}
        </span>
      </div>

      <div class="flex items-center gap-2">
        <!-- Connection status -->
        <div class="flex items-center gap-1.5 mr-1">
          <div
            class="w-1.5 h-1.5 rounded-full"
            :class="isConnected ? 'bg-green-500' : connectionError ? 'bg-red-400' : 'bg-yellow-400 animate-pulse'"
          ></div>
          <button
            v-if="connectionError"
            @click="retryConnection"
            class="text-[11px] px-1.5 py-0.5 rounded transition-colors"
            :class="isDarkTheme ? 'text-red-400 hover:bg-gray-700' : 'text-red-500 hover:bg-gray-100'"
          >
            重连
          </button>
        </div>

        <!-- Theme toggle -->
        <button
          @click="isDarkTheme = !isDarkTheme"
          class="w-7 h-7 flex items-center justify-center rounded-md transition-colors"
          :class="isDarkTheme ? 'text-gray-400 hover:text-gray-200 hover:bg-gray-700/60' : 'text-gray-400 hover:text-gray-600 hover:bg-gray-200'"
        >
          <svg v-if="isDarkTheme" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
          </svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          </svg>
        </button>
      </div>
    </header>

    <!-- Main content -->
    <div class="flex flex-1 min-h-0">
      <!-- Editor panel -->
      <div class="flex-1 flex flex-col min-w-0">
        <!-- Language tabs -->
        <div
          class="flex h-8 border-b flex-shrink-0"
          :class="isDarkTheme ? 'border-gray-800 bg-[#1c1c1f]' : 'border-gray-200 bg-gray-50/50'"
        >
          <button
            v-for="lang in languages"
            :key="lang.key"
            @click="activeLanguage = lang.key"
            class="px-3 text-[11px] font-medium transition-colors cursor-pointer relative flex items-center gap-1.5"
            :class="[
              activeLanguage === lang.key
                ? (isDarkTheme ? 'text-gray-100 bg-[#1e1e22]' : 'text-gray-800 bg-white')
                : (isDarkTheme ? 'text-gray-500 hover:text-gray-300 hover:bg-gray-800/50' : 'text-gray-400 hover:text-gray-600 hover:bg-gray-100')
            ]"
          >
            <span
              class="inline-flex items-center justify-center w-4 h-4 rounded text-[9px] font-bold"
              :class="activeLanguage === lang.key
                ? (isDarkTheme ? 'bg-gray-700 text-gray-200' : 'bg-gray-200 text-gray-700')
                : (isDarkTheme ? 'bg-gray-800 text-gray-500' : 'bg-gray-100 text-gray-400')"
            >{{ lang.icon }}</span>
            {{ lang.label }}
            <span
              v-if="activeLanguage === lang.key"
              class="absolute bottom-0 left-1 right-1 h-[2px] rounded-t"
              :class="isDarkTheme ? 'bg-blue-500' : 'bg-blue-500'"
            ></span>
          </button>
        </div>

        <!-- Editor area -->
        <div class="flex-1 min-h-0" :class="isDarkTheme ? 'bg-[#1e1e22]' : 'bg-white'">
          <CodeEditor
            v-if="docId"
            :language="activeLanguage"
            :dark="isDarkTheme"
            :doc-id="docId"
            :send-update="sendYjsUpdate"
            :set-on-yjs-update="setOnYjsUpdate"
            :set-on-sync-request="setOnSyncRequest"
            @text-change="handleTextChange"
            class="h-full"
          />
        </div>
      </div>

      <!-- Preview panel -->
      <div
        class="w-5/12 flex flex-col border-l flex-shrink-0"
        :class="isDarkTheme ? 'border-gray-800' : 'border-gray-200'"
      >
        <!-- Preview header -->
        <div
          class="flex items-center justify-between px-3 h-8 border-b flex-shrink-0"
          :class="isDarkTheme ? 'border-gray-800 bg-[#1c1c1f]' : 'border-gray-200 bg-gray-50/50'"
        >
          <span class="text-[11px] font-medium tracking-wide uppercase" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">
            Preview
          </span>
          <button
            @click="refreshPreview"
            class="w-5 h-5 flex items-center justify-center rounded transition-colors"
            :class="isDarkTheme ? 'text-gray-500 hover:text-gray-300 hover:bg-gray-700/60' : 'text-gray-400 hover:text-gray-600 hover:bg-gray-200'"
            title="刷新预览"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
            </svg>
          </button>
        </div>
        <div class="flex-1 min-h-0" :class="isDarkTheme ? 'bg-[#1e1e22]' : 'bg-white'">
          <LivePreview
            :key="previewKey"
            :html-content="htmlContent"
            :css-content="cssContent"
            :js-content="jsContent"
          />
        </div>
      </div>
    </div>

    <!-- Bottom status bar -->
    <div
      class="flex items-center justify-between px-3 h-6 border-t flex-shrink-0 text-[10px]"
      :class="isDarkTheme ? 'border-gray-800 bg-[#1c1c1f] text-gray-500' : 'border-gray-200 bg-gray-50/50 text-gray-400'"
    >
      <div class="flex items-center gap-3">
        <span class="flex items-center gap-1">
          <span class="w-1.5 h-1.5 rounded-full" :class="isConnected ? 'bg-green-500' : 'bg-gray-400'"></span>
          {{ isConnected ? 'Connected' : 'Disconnected' }}
        </span>
        <span>{{ activeLanguage.toUpperCase() }}</span>
      </div>
      <div class="flex items-center gap-3">
        <span v-if="participants.length > 0" class="flex items-center gap-1">
          <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          {{ participants.length }}
        </span>
        <span>{{ user?.username || '' }}</span>
      </div>
    </div>
  </div>
</template>

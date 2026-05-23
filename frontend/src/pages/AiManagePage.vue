<template>
  <div class="min-h-screen" :class="isDarkTheme ? 'bg-[#0f0f11]' : 'bg-[#fafafa]'">
    <!-- 顶部导航 -->
    <header class="sticky top-0 z-10 backdrop-blur-xl border-b" :class="isDarkTheme ? 'bg-[#18181b]/80 border-gray-800/50' : 'bg-white/80 border-gray-200/60'">
      <div class="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
        <div class="flex items-center gap-4">
          <button 
            @click="router.push('/login')" 
            class="p-2 rounded-xl transition-colors"
            :class="isDarkTheme ? 'hover:bg-white/5 text-gray-400' : 'hover:bg-gray-100 text-gray-500'"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
          </button>
          <div>
            <h1 class="text-lg font-semibold" :class="isDarkTheme ? 'text-gray-100' : 'text-gray-900'">AI助手</h1>
            <p class="text-xs" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">创建和管理你的专属AI智能体</p>
          </div>
        </div>
        <button 
          @click="showCreateDialog = true"
          class="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all hover:scale-[1.02] active:scale-[0.98]"
          :class="isDarkTheme 
            ? 'bg-gradient-to-r from-blue-600 to-blue-500 hover:from-blue-500 hover:to-blue-400 text-white shadow-lg shadow-blue-500/20' 
            : 'bg-gradient-to-r from-blue-500 to-blue-400 hover:from-blue-400 hover:to-blue-300 text-white shadow-lg shadow-blue-500/20'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          创建新助手
        </button>
      </div>
    </header>

    <div class="max-w-5xl mx-auto p-6 space-y-8">
      <!-- 系统AI -->
      <section>
        <div class="flex items-center gap-2 mb-4">
          <div class="w-1.5 h-1.5 rounded-full bg-blue-500"></div>
          <h2 class="text-sm font-semibold uppercase tracking-wider" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">系统助手</h2>
        </div>
        
        <div 
          @click="systemAssistant && goToChat(systemAssistant)"
          class="group relative rounded-2xl p-5 transition-all hover:scale-[1.01] cursor-pointer overflow-hidden"
          :class="isDarkTheme ? 'bg-[#1e1e22] hover:bg-[#252529]' : 'bg-white hover:shadow-xl shadow-sm'"
        >
          <div class="absolute inset-0 opacity-5" style="background: linear-gradient(135deg, #667eea, #764ba2)"></div>
          <div class="relative flex items-center gap-4">
            <div class="relative">
              <img 
                v-if="(systemAssistant as any)?.avatarUrl" 
                :src="(systemAssistant as any).avatarUrl" 
                class="w-14 h-14 rounded-2xl object-cover shadow-xl"
              />
              <div 
                v-else
                class="w-14 h-14 rounded-2xl flex items-center justify-center text-2xl shadow-xl"
                style="background: linear-gradient(135deg, #667eea, #764ba2)"
              >
                🤖
              </div>
              <div class="absolute -bottom-1 -right-1 w-5 h-5 rounded-full flex items-center justify-center" :class="isDarkTheme ? 'bg-[#1e1e22]' : 'bg-white'">
                <div class="w-3 h-3 rounded-full" :class="systemAssistant ? 'bg-emerald-500' : 'bg-gray-400'"></div>
              </div>
            </div>
            <div class="flex-1">
              <h3 class="font-semibold text-base" :class="isDarkTheme ? 'text-gray-100' : 'text-gray-900'">
                {{ systemAssistant?.name || '系统助手' }}
              </h3>
              <p class="text-sm mt-0.5" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">
                {{ systemAssistant ? '点击开始对话' : '管理员未配置AI服务' }}
              </p>
              <div v-if="systemAssistant" class="flex items-center gap-2 mt-2">
                <span class="px-2 py-0.5 rounded-md text-xs font-medium" :class="isDarkTheme ? 'bg-blue-500/10 text-blue-400' : 'bg-blue-50 text-blue-600'">
                  {{ systemAssistant.model }}
                </span>
              </div>
            </div>
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="transition-transform group-hover:translate-x-1" :class="isDarkTheme ? 'text-gray-600' : 'text-gray-300'">
              <path d="M9 18l6-6-6-6"/>
            </svg>
          </div>
        </div>
      </section>

      <!-- 用户自建AI -->
      <section>
        <div class="flex items-center gap-2 mb-4">
          <div class="w-1.5 h-1.5 rounded-full bg-emerald-500"></div>
          <h2 class="text-sm font-semibold uppercase tracking-wider" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">
            我创建的助手
            <span class="ml-2 px-2 py-0.5 rounded-full text-xs" :class="isDarkTheme ? 'bg-white/10 text-gray-400' : 'bg-gray-100 text-gray-500'">
              {{ userAssistants.length }}
            </span>
          </h2>
        </div>
        
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div 
            v-for="assistant in userAssistants"
            :key="assistant.id"
            @click="goToChat(assistant)"
            class="group rounded-2xl p-5 transition-all hover:scale-[1.01] cursor-pointer"
            :class="isDarkTheme ? 'bg-[#1e1e22] hover:bg-[#252529]' : 'bg-white hover:shadow-xl shadow-sm'"
          >
            <div class="flex items-start gap-4">
              <div class="relative">
                <img 
                  v-if="(assistant as any).avatarUrl" 
                  :src="(assistant as any).avatarUrl" 
                  class="w-12 h-12 rounded-xl object-cover shadow-lg"
                />
                <div 
                  v-else
                  class="w-12 h-12 rounded-xl flex items-center justify-center text-xl shadow-lg"
                  :style="{ background: assistant.avatarColor || 'linear-gradient(135deg, #f093fb, #f5576c)' }"
                >
                  {{ assistant.avatarIcon || '✨' }}
                </div>
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between">
                  <h3 class="font-semibold truncate" :class="isDarkTheme ? 'text-gray-100' : 'text-gray-900'">{{ assistant.name }}</h3>
                  <div class="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button 
                      @click.stop="handleEdit(assistant)"
                      class="p-1.5 rounded-lg transition-colors"
                      :class="isDarkTheme ? 'hover:bg-white/10 text-gray-500' : 'hover:bg-gray-100 text-gray-400'"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                        <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
                      </svg>
                    </button>
                    <button 
                      @click.stop="handleShare(assistant)"
                      class="p-1.5 rounded-lg transition-colors"
                      :class="isDarkTheme ? 'hover:bg-white/10 text-gray-500' : 'hover:bg-gray-100 text-gray-400'"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/>
                        <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
                      </svg>
                    </button>
                    <button 
                      @click.stop="handleDelete(assistant.id)"
                      class="p-1.5 rounded-lg transition-colors"
                      :class="isDarkTheme ? 'hover:bg-red-500/10 hover:text-red-400 text-gray-500' : 'hover:bg-red-50 hover:text-red-500 text-gray-400'"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
                      </svg>
                    </button>
                  </div>
                </div>
                <p class="text-sm mt-1 truncate" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">
                  {{ assistant.systemPrompt?.substring(0, 60) || '自定义AI助手' }}{{ assistant.systemPrompt && assistant.systemPrompt.length > 60 ? '...' : '' }}
                </p>
                <div class="flex items-center gap-2 mt-3">
                  <span class="px-2 py-0.5 rounded-md text-xs font-medium" :class="isDarkTheme ? 'bg-white/5 text-gray-400' : 'bg-gray-100 text-gray-500'">
                    {{ assistant.model }}
                  </span>
                  <span v-if="assistant.shareCode" class="px-2 py-0.5 rounded-md text-xs font-medium" :class="isDarkTheme ? 'bg-emerald-500/10 text-emerald-400' : 'bg-emerald-50 text-emerald-600'">
                    已分享
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 新建卡片 -->
          <button 
            @click="showCreateDialog = true"
            class="rounded-2xl border-2 border-dashed p-5 flex flex-col items-center justify-center min-h-[140px] transition-all hover:scale-[1.01]"
            :class="isDarkTheme 
              ? 'border-gray-800 hover:border-gray-700 text-gray-500 hover:text-gray-400' 
              : 'border-gray-200 hover:border-gray-300 text-gray-400 hover:text-gray-500'"
          >
            <div class="w-12 h-12 rounded-xl flex items-center justify-center mb-3" :class="isDarkTheme ? 'bg-white/5' : 'bg-gray-100'">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
            </div>
            <span class="text-sm font-medium">创建新助手</span>
          </button>
        </div>
      </section>

      <!-- 通过分享码添加 -->
      <section>
        <div class="flex items-center gap-2 mb-4">
          <div class="w-1.5 h-1.5 rounded-full bg-purple-500"></div>
          <h2 class="text-sm font-semibold uppercase tracking-wider" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">通过分享码添加</h2>
        </div>
        
        <div class="rounded-2xl p-5" :class="isDarkTheme ? 'bg-[#1e1e22]' : 'bg-white shadow-sm'">
          <div class="flex gap-3">
            <input 
              v-model="shareCode"
              placeholder="输入分享码..."
              class="flex-1 px-4 py-2.5 rounded-xl text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-all"
              :class="isDarkTheme ? 'bg-[#0f0f11] border-gray-800 text-gray-200 placeholder-gray-600' : 'bg-gray-50 border-gray-200 placeholder-gray-400'"
            />
            <button 
              @click="handleAddByShareCode"
              :disabled="!shareCode.trim()"
              class="px-5 py-2.5 rounded-xl text-sm font-medium text-white transition-all disabled:opacity-50 hover:scale-[1.02] active:scale-[0.98]"
              :class="isDarkTheme 
                ? 'bg-gradient-to-r from-emerald-600 to-emerald-500 hover:from-emerald-500 hover:to-emerald-400' 
                : 'bg-gradient-to-r from-emerald-500 to-emerald-400 hover:from-emerald-400 hover:to-emerald-300'"
            >
              添加
            </button>
          </div>
        </div>
      </section>
    </div>

    <!-- 创建/编辑弹窗 -->
    <CreateAiDialog 
      v-if="showCreateDialog"
      :editing="editingAssistant"
      @close="handleCloseDialog"
      @created="handleCreated"
    />

    <!-- 分享弹窗 -->
    <Teleport to="body">
      <div v-if="sharingAssistant" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50" @click.self="sharingAssistant = null">
        <div class="rounded-2xl p-6 max-w-sm w-full mx-4" :class="isDarkTheme ? 'bg-[#1e1e22]' : 'bg-white'">
          <div class="flex items-center gap-3 mb-4">
            <div 
              class="w-10 h-10 rounded-xl flex items-center justify-center text-lg"
              :style="{ background: sharingAssistant.avatarColor || 'linear-gradient(135deg, #f093fb, #f5576c)' }"
            >
              {{ sharingAssistant.avatarIcon || '✨' }}
            </div>
            <div>
              <h3 class="font-semibold" :class="isDarkTheme ? 'text-gray-100' : 'text-gray-900'">分享助手</h3>
              <p class="text-xs" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">{{ sharingAssistant.name }}</p>
            </div>
          </div>
          
          <p class="text-sm mb-4" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">分享以下链接给好友，即可添加此助手：</p>
          
          <div class="flex gap-2">
            <input 
              :value="sharingAssistant.shareCode"
              readonly
              class="flex-1 px-3 py-2 rounded-xl text-sm font-mono"
              :class="isDarkTheme ? 'bg-[#0f0f11] text-gray-300 border border-gray-800' : 'bg-gray-50 text-gray-700 border border-gray-200'"
            />
            <button 
              @click="copyShareCode"
              class="px-4 py-2 rounded-xl text-sm font-medium text-white"
              :class="isDarkTheme ? 'bg-blue-600 hover:bg-blue-500' : 'bg-blue-500 hover:bg-blue-400'"
            >
              复制
            </button>
          </div>
          
          <button 
            @click="sharingAssistant = null"
            class="w-full mt-4 py-2.5 rounded-xl text-sm font-medium transition-colors"
            :class="isDarkTheme ? 'text-gray-400 hover:bg-white/5' : 'text-gray-500 hover:bg-gray-50'"
          >
            关闭
          </button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAiStore } from '@/stores/ai'
import type { AiAssistant } from '@/stores/ai'
import CreateAiDialog from '@/components/CreateAiDialog.vue'

const router = useRouter()
const aiStore = useAiStore()

const isDarkTheme = ref(localStorage.getItem('theme') === 'dark')
const systemAssistant = computed(() => aiStore.systemAssistant)
const userAssistants = computed(() => aiStore.userAssistants)

const showCreateDialog = ref(false)
const editingAssistant = ref<AiAssistant | null>(null)
const sharingAssistant = ref<AiAssistant | null>(null)
const shareCode = ref('')

const user = ref<any>(null)

onMounted(async () => {
  const userData = localStorage.getItem('user')
  if (userData) {
    user.value = JSON.parse(userData)
  }
  await loadAssistants()
})

async function loadAssistants() {
  try {
    const userId = user.value?.userId || ''
    const response = await fetch(`/api/ai/assistants?userId=${userId}`)
    const data = await response.json()
    aiStore.setSystemAssistant(data.system || null)
    aiStore.setUserAssistants(data.user || [])
  } catch (error) {
    console.error('加载AI助手失败:', error)
  }
}

function goToChat(assistant: AiAssistant) {
  aiStore.setCurrentAssistant(assistant)
  router.push(`/ai/${assistant.id}`)
}

function handleEdit(assistant: AiAssistant) {
  editingAssistant.value = assistant
  showCreateDialog.value = true
}

function handleShare(assistant: AiAssistant) {
  sharingAssistant.value = assistant
}

async function copyShareCode() {
  if (sharingAssistant.value?.shareCode) {
    await navigator.clipboard.writeText(sharingAssistant.value.shareCode)
    alert('已复制分享码')
  }
}

async function handleDelete(assistantId: string) {
  if (!confirm('确定要删除此助手吗？')) return
  
  try {
    const userId = user.value?.userId || ''
    await fetch(`/api/ai/assistants/${assistantId}?userId=${userId}`, { method: 'DELETE' })
    aiStore.removeUserAssistant(assistantId)
  } catch (error) {
    console.error('删除失败:', error)
  }
}

async function handleAddByShareCode() {
  const code = shareCode.value.trim()
  if (!code) return
  
  try {
    const userId = user.value?.userId || ''
    const response = await fetch('/api/ai/assistants/share', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId, shareCode: code })
    })
    
    if (response.ok) {
      const assistant = await response.json()
      aiStore.addUserAssistant(assistant)
      shareCode.value = ''
      alert('添加成功')
    } else {
      alert('分享码无效')
    }
  } catch (error) {
    console.error('添加失败:', error)
  }
}

function handleCloseDialog() {
  showCreateDialog.value = false
  editingAssistant.value = null
}

function handleCreated(assistant: AiAssistant) {
  aiStore.addUserAssistant(assistant)
  handleCloseDialog()
}
</script>

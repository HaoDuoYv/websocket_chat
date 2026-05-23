<template>
  <div class="min-h-screen" :class="isDarkTheme ? 'bg-[#18181B]' : 'bg-gray-50'">
    <header class="px-6 py-4 border-b" :class="isDarkTheme ? 'bg-[#1F1F23] border-gray-800' : 'bg-white border-gray-100'">
      <div class="flex items-center gap-4">
        <button @click="router.push('/admin')" class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
        </button>
        <div>
          <h1 class="text-lg font-medium" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">系统AI助手配置</h1>
          <p class="text-xs" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">配置系统级AI助手，所有用户可使用</p>
        </div>
      </div>
    </header>
    
    <div class="p-6">
      <div class="max-w-2xl">
        <div class="flex items-center justify-end mb-6">
          <div class="flex gap-2">
            <button 
              @click="testConnection"
              class="px-4 py-2 rounded-lg text-sm border hover:bg-gray-50 transition-colors"
              :class="isDarkTheme ? 'border-gray-600 text-gray-300 hover:bg-gray-800' : 'border-gray-200 text-gray-700'"
            >
              测试连接
            </button>
            <button 
              @click="saveConfig"
              class="px-4 py-2 rounded-lg text-sm bg-blue-500 hover:bg-blue-600 text-white transition-colors"
            >
              保存配置
            </button>
          </div>
        </div>

        <!-- 启用开关 -->
        <div class="rounded-xl p-5 mb-4" :class="isDarkTheme ? 'bg-[#27272A]' : 'bg-white shadow-sm'">
          <div class="flex items-center justify-between pb-4 border-b" :class="isDarkTheme ? 'border-gray-700' : 'border-gray-100'">
            <div>
              <div class="font-medium" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">启用系统AI助手</div>
              <div class="text-sm mt-0.5" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">开启后所有用户可在聊天列表中看到系统AI</div>
            </div>
            <button 
              @click="enabled = !enabled"
              class="w-12 h-6 rounded-full relative transition-colors"
              :class="enabled ? 'bg-blue-500' : 'bg-gray-200'"
            >
              <div 
                class="w-5 h-5 bg-white rounded-full absolute top-0.5 shadow-sm transition-transform"
                :class="enabled ? 'translate-x-6' : 'translate-x-0.5'"
              />
            </button>
          </div>

          <div v-if="enabled" class="pt-4 space-y-4">
            <!-- 基本信息 -->
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium mb-1" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-700'">助手名称</label>
                <input 
                  v-model="form.name"
                  class="w-full px-3 py-2 rounded-lg text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50"
                  :class="isDarkTheme ? 'bg-gray-700 border-gray-600 text-gray-200' : 'bg-white border-gray-200'"
                />
              </div>
              <div>
                <label class="block text-sm font-medium mb-1" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-700'">欢迎语</label>
                <input 
                  v-model="form.welcomeMessage"
                  class="w-full px-3 py-2 rounded-lg text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50"
                  :class="isDarkTheme ? 'bg-gray-700 border-gray-600 text-gray-200' : 'bg-white border-gray-200'"
                />
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium mb-1" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-700'">系统提示词</label>
              <textarea 
                v-model="form.systemPrompt"
                rows="4"
                class="w-full px-3 py-2 rounded-lg text-sm border resize-none focus:outline-none focus:ring-2 focus:ring-blue-500/50"
                :class="isDarkTheme ? 'bg-gray-700 border-gray-600 text-gray-200' : 'bg-white border-gray-200'"
              />
            </div>

            <!-- API配置 -->
            <div class="rounded-lg p-4" :class="isDarkTheme ? 'bg-gray-800' : 'bg-gray-50'">
              <h4 class="font-medium mb-3" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-700'">API配置</h4>
              
              <div class="mb-3">
                <label class="block text-xs mb-1" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">供应商</label>
                <select 
                  v-model="selectedProvider"
                  class="w-full px-3 py-2 rounded text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50 cursor-pointer"
                  :class="isDarkTheme ? 'bg-gray-700 border-gray-600 text-gray-200' : 'bg-white border-gray-200'"
                >
                  <option v-for="option in providerOptions" :key="option.value" :value="option.value">
                    {{ option.label }} {{ option.description ? `- ${option.description}` : '' }}
                  </option>
                </select>
              </div>
              
              <div class="grid grid-cols-3 gap-4 mb-3">
              <div class="col-span-2">
                <label class="block text-xs mb-1" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">Base URL</label>
                <input 
                  v-model="form.baseUrl"
                  :readonly="selectedProvider !== 'custom'"
                  class="w-full px-3 py-2 rounded text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50"
                  :class="[
                    isDarkTheme ? 'bg-gray-700 border-gray-600 text-gray-200' : 'bg-white border-gray-200',
                    selectedProvider !== 'custom' ? 'opacity-70' : ''
                  ]"
                />
              </div>
              <div>
                <label class="block text-xs mb-1" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">模型</label>
                <input 
                  v-model="form.model"
                  :readonly="selectedProvider !== 'custom'"
                  class="w-full px-3 py-2 rounded text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50"
                  :class="[
                    isDarkTheme ? 'bg-gray-700 border-gray-600 text-gray-200' : 'bg-white border-gray-200',
                    selectedProvider !== 'custom' ? 'opacity-70' : ''
                  ]"
                  placeholder="输入或选择模型"
                />
              </div>
              </div>

              <div class="mb-3">
                <label class="block text-xs mb-1" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">API Key</label>
                <div class="flex gap-2">
                  <input 
                    v-model="form.apiKey"
                    :type="showKey ? 'text' : 'password'"
                    class="flex-1 px-3 py-2 rounded text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50"
                    :class="isDarkTheme ? 'bg-gray-700 border-gray-600 text-gray-200' : 'bg-white border-gray-200'"
                  />
                  <button 
                    @click="showKey = !showKey"
                    class="px-3 py-2 rounded text-sm border"
                    :class="isDarkTheme ? 'border-gray-600 text-gray-300 hover:bg-gray-700' : 'border-gray-200 hover:bg-gray-100'"
                  >
                    {{ showKey ? '隐藏' : '显示' }}
                  </button>
                </div>
              </div>

              <div class="grid grid-cols-3 gap-4">
                <div>
                  <label class="block text-xs mb-1" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">温度 (0-2)</label>
                  <input 
                    v-model.number="form.temperature"
                    type="number"
                    min="0"
                    max="2"
                    step="0.1"
                    class="w-full px-3 py-2 rounded text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50"
                    :class="isDarkTheme ? 'bg-gray-700 border-gray-600 text-gray-200' : 'bg-white border-gray-200'"
                  />
                </div>
                <div>
                  <label class="block text-xs mb-1" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">最大上下文条数</label>
                  <input 
                    v-model.number="form.maxContext"
                    type="number"
                    class="w-full px-3 py-2 rounded text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50"
                    :class="isDarkTheme ? 'bg-gray-700 border-gray-600 text-gray-200' : 'bg-white border-gray-200'"
                  />
                </div>
                <div>
                  <label class="block text-xs mb-1" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">最大Token数</label>
                  <input 
                    v-model.number="form.maxTokens"
                    type="number"
                    class="w-full px-3 py-2 rounded text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50"
                    :class="isDarkTheme ? 'bg-gray-700 border-gray-600 text-gray-200' : 'bg-white border-gray-200'"
                  />
                </div>
              </div>
            </div>

            <!-- 高级设置 -->
            <div class="flex gap-4">
              <div class="flex-1 flex items-center justify-between p-3 rounded-lg" :class="isDarkTheme ? 'bg-gray-800' : 'bg-gray-50'">
                <div>
                  <div class="text-sm font-medium" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-700'">流式输出</div>
                  <div class="text-xs" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">实时显示AI回复</div>
                </div>
                <div class="w-9 h-5 bg-blue-500 rounded-full relative">
                  <div class="w-4 h-4 bg-white rounded-full absolute right-0.5 top-0.5 shadow-sm"/>
                </div>
              </div>
              <div class="flex-1 flex items-center justify-between p-3 rounded-lg" :class="isDarkTheme ? 'bg-gray-800' : 'bg-gray-50'">
                <div>
                  <div class="text-sm font-medium" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-700'">Markdown渲染</div>
                  <div class="text-xs" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">支持代码块等格式</div>
                </div>
                <div class="w-9 h-5 bg-blue-500 rounded-full relative">
                  <div class="w-4 h-4 bg-white rounded-full absolute right-0.5 top-0.5 shadow-sm"/>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 测试结果 -->
        <div v-if="testResult" class="rounded-xl p-4" :class="testResult.success ? 'bg-green-500/10 text-green-400' : 'bg-red-500/10 text-red-400'">
          {{ testResult.message }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getProviderOptions, getPresetByProvider } from '@/config/llmProviders'

const router = useRouter()
const isDarkTheme = ref(localStorage.getItem('theme') === 'dark')
const enabled = ref(false)
const showKey = ref(false)
const testResult = ref<{ success: boolean; message: string } | null>(null)
const selectedProvider = ref('openai')

const providerOptions = getProviderOptions()

const form = reactive({
  name: '系统助手',
  welcomeMessage: '你好！有什么可以帮你的？',
  systemPrompt: '你是一个友好的AI助手，请用简洁专业的语言回复用户的问题。',
  baseUrl: 'https://api.openai.com/v1',
  apiKey: '',
  model: 'gpt-4o',
  temperature: 0.7,
  maxContext: 20,
  maxTokens: 4096
})

watch(selectedProvider, (provider) => {
  const preset = getPresetByProvider(provider)
  if (preset && provider !== 'custom') {
    form.baseUrl = preset.baseUrl
    form.model = preset.model
  }
})

onMounted(async () => {
  await loadConfig()
})

async function loadConfig() {
  try {
    const response = await fetch('/api/admin/ai/config')
    const data = await response.json()
    enabled.value = data.enabled
    if (data.config) {
      Object.assign(form, data.config)
      // 检测供应商
      const detectedProvider = detectProvider(form.baseUrl)
      if (detectedProvider) {
        selectedProvider.value = detectedProvider
      }
    }
  } catch (error) {
    console.error('加载配置失败:', error)
  }
}

function detectProvider(baseUrl: string): string {
  const providers = ['openai', 'deepseek', 'glm', 'qwen', 'kimi', 'mimo']
  for (const provider of providers) {
    const preset = getPresetByProvider(provider)
    if (preset && baseUrl.includes(preset.baseUrl.split('/v1')[0])) {
      return provider
    }
  }
  return 'custom'
}

async function saveConfig() {
  try {
    const response = await fetch('/api/admin/ai/config', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...form, enabled: enabled.value })
    })
    
    if (response.ok) {
      alert('保存成功')
    } else {
      alert('保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    alert('保存失败')
  }
}

async function testConnection() {
  try {
    const response = await fetch('/api/admin/ai/test', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        baseUrl: form.baseUrl,
        apiKey: form.apiKey,
        model: form.model
      })
    })
    
    testResult.value = await response.json()
  } catch (error) {
    testResult.value = { success: false, message: '连接测试失败' }
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4" @click.self="$emit('close')">
      <div class="rounded-2xl max-w-lg w-full max-h-[90vh] overflow-hidden" :class="isDarkTheme ? 'bg-[#1e1e22]' : 'bg-white'">
        <!-- 头部 -->
        <div class="flex items-center justify-between p-5 border-b" :class="isDarkTheme ? 'border-gray-800/50' : 'border-gray-100'">
          <div class="flex items-center gap-3">
            <div 
              class="w-10 h-10 rounded-xl flex items-center justify-center text-lg"
              :style="{ background: form.avatarColor || 'linear-gradient(135deg, #f093fb, #f5576c)' }"
            >
              {{ form.avatarIcon || '✨' }}
            </div>
            <h2 class="text-lg font-semibold" :class="isDarkTheme ? 'text-gray-100' : 'text-gray-900'">
              {{ editing ? '编辑AI助手' : '创建AI助手' }}
            </h2>
          </div>
          <button 
            @click="$emit('close')" 
            class="p-2 rounded-xl transition-colors"
            :class="isDarkTheme ? 'hover:bg-white/5 text-gray-500' : 'hover:bg-gray-100 text-gray-400'"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>

        <!-- 表单 -->
        <div class="p-5 space-y-5 overflow-y-auto max-h-[calc(90vh-140px)]">
          <!-- 头像选择 -->
          <div class="flex items-center gap-4">
            <AvatarUpload
              ref="avatarUploadRef"
              :model-value="form.avatarUrl"
              :default-icon="form.avatarIcon"
              :default-color="form.avatarColor"
              size="lg"
              :hint="savedAssistantId ? '点击上传自定义头像' : '保存后可上传自定义头像'"
              @upload="handleAvatarUpload"
            />
            <div class="flex-1">
              <div class="text-sm font-medium" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-700'">AI头像</div>
              <div class="text-xs mt-1" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">上传图片或使用图标</div>
              <button 
                @click="cycleIcon"
                class="mt-2 text-xs px-3 py-1.5 rounded-lg transition-colors"
                :class="isDarkTheme ? 'bg-white/5 hover:bg-white/10 text-gray-400' : 'bg-gray-100 hover:bg-gray-200 text-gray-600'"
              >
                换个图标
              </button>
            </div>
          </div>

          <!-- 名称 -->
          <div>
            <label class="block text-sm font-medium mb-2" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-700'">
              AI名称 <span class="text-red-500">*</span>
            </label>
            <input 
              v-model="form.name"
              placeholder="例: 写作助手、代码专家"
              class="w-full px-4 py-2.5 rounded-xl text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-all"
              :class="isDarkTheme ? 'bg-[#0f0f11] border-gray-800 text-gray-200 placeholder-gray-600' : 'bg-gray-50 border-gray-200 placeholder-gray-400'"
            />
          </div>

          <!-- 角色设定 -->
          <div>
            <label class="block text-sm font-medium mb-2" :class="isDarkTheme ? 'text-gray-300' : 'text-gray-700'">
              角色设定 <span class="text-red-500">*</span>
            </label>
            <textarea 
              v-model="form.systemPrompt"
              placeholder="描述AI的角色和能力，例: 你是一位专业的写作助手，擅长文章润色、文案撰写和创意写作..."
              rows="4"
              class="w-full px-4 py-2.5 rounded-xl text-sm border resize-none focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-all"
              :class="isDarkTheme ? 'bg-[#0f0f11] border-gray-800 text-gray-200 placeholder-gray-600' : 'bg-gray-50 border-gray-200 placeholder-gray-400'"
            />
          </div>

          <!-- API配置 -->
          <div class="rounded-xl p-4" :class="isDarkTheme ? 'bg-[#0f0f11]' : 'bg-gray-50'">
            <h4 class="text-sm font-semibold mb-3" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">API配置</h4>
            
            <div class="space-y-3">
              <div>
                <label class="block text-xs font-medium mb-1.5" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">供应商</label>
                <select 
                  v-model="selectedProvider"
                  class="w-full px-3 py-2 rounded-lg text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-all cursor-pointer"
                  :class="isDarkTheme ? 'bg-[#18181b] border-gray-800 text-gray-200' : 'bg-white border-gray-200'"
                >
                  <option v-for="option in providerOptions" :key="option.value" :value="option.value">
                    {{ option.label }} {{ option.description ? `- ${option.description}` : '' }}
                  </option>
                </select>
              </div>

              <div>
                <label class="block text-xs font-medium mb-1.5" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">Base URL</label>
                <input 
                  v-model="form.baseUrl"
                  placeholder="https://api.openai.com/v1"
                  class="w-full px-3 py-2 rounded-lg text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-all"
                  :class="isDarkTheme ? 'bg-[#18181b] border-gray-800 text-gray-200 placeholder-gray-600' : 'bg-white border-gray-200 placeholder-gray-400'"
                />
              </div>

              <div>
                <label class="block text-xs font-medium mb-1.5" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">API Key <span class="text-red-500">*</span></label>
                <input 
                  v-model="form.apiKey"
                  type="password"
                  placeholder="sk-..."
                  class="w-full px-3 py-2 rounded-lg text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-all"
                  :class="isDarkTheme ? 'bg-[#18181b] border-gray-800 text-gray-200 placeholder-gray-600' : 'bg-white border-gray-200 placeholder-gray-400'"
                />
              </div>

              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label class="block text-xs font-medium mb-1.5" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">模型</label>
                  <input 
                    v-model="form.model"
                    placeholder="输入或选择模型"
                    class="w-full px-3 py-2 rounded-lg text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-all"
                    :class="isDarkTheme ? 'bg-[#18181b] border-gray-800 text-gray-200 placeholder-gray-600' : 'bg-white border-gray-200 placeholder-gray-400'"
                  />
                </div>
                <div>
                  <label class="block text-xs font-medium mb-1.5" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">温度 (0-2)</label>
                  <input 
                    v-model.number="form.temperature"
                    type="number"
                    min="0"
                    max="2"
                    step="0.1"
                    class="w-full px-3 py-2 rounded-lg text-sm border focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-all"
                    :class="isDarkTheme ? 'bg-[#18181b] border-gray-800 text-gray-200' : 'bg-white border-gray-200'"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部按钮 -->
        <div class="flex justify-end gap-3 p-5 border-t" :class="isDarkTheme ? 'border-gray-800/50' : 'border-gray-100'">
          <button 
            @click="$emit('close')"
            class="px-4 py-2.5 rounded-xl text-sm font-medium transition-colors"
            :class="isDarkTheme ? 'text-gray-400 hover:bg-white/5' : 'text-gray-500 hover:bg-gray-50'"
          >
            取消
          </button>
          <button 
            @click="handleSave"
            :disabled="!isValid"
            class="px-5 py-2.5 rounded-xl text-sm font-medium text-white transition-all disabled:opacity-50 hover:scale-[1.02] active:scale-[0.98]"
            :class="isDarkTheme 
              ? 'bg-gradient-to-r from-blue-600 to-blue-500 hover:from-blue-500 hover:to-blue-400 shadow-lg shadow-blue-500/20' 
              : 'bg-gradient-to-r from-blue-500 to-blue-400 hover:from-blue-400 hover:to-blue-300 shadow-lg shadow-blue-500/20'"
          >
            {{ editing ? '保存' : '创建' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, watch } from 'vue'
import type { AiAssistant } from '@/stores/ai'
import { getProviderOptions, getPresetByProvider } from '@/config/llmProviders'
import AvatarUpload from './AvatarUpload.vue'
import { uploadAiAvatar } from '@/api/avatar'
import { useToast } from '@/composables/useToast'

const toast = useToast()

const props = defineProps<{
  editing?: AiAssistant | null
}>()

const emit = defineEmits<{
  close: []
  created: [assistant: AiAssistant]
}>()

const isDarkTheme = ref(localStorage.getItem('theme') === 'dark')
const selectedProvider = ref('openai')

const providerOptions = getProviderOptions()

const icons = ['✨', '🤖', '💻', '✍️', '📝', '🎨', '🔬', '📚', '🎯', '💡', '🧠', '🎮', '🌐', '🎵', '📊']
const colors = [
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)',
  'linear-gradient(135deg, #a18cd1, #fbc2eb)',
  'linear-gradient(135deg, #fccb90, #d57eeb)',
  'linear-gradient(135deg, #667eea, #764ba2)',
  'linear-gradient(135deg, #f5576c, #ff6b6b)',
]

const form = reactive({
  name: '',
  systemPrompt: '',
  baseUrl: 'https://api.openai.com/v1',
  apiKey: '',
  model: '',
  temperature: 0.7,
  avatarIcon: '✨',
  avatarColor: colors[0],
  avatarUrl: ''
})

const avatarUploadRef = ref<InstanceType<typeof AvatarUpload> | null>(null)
const isUploadingAvatar = ref(false)
const savedAssistantId = ref<string | null>(null)

const isValid = computed(() => {
  return form.name.trim() && form.apiKey.trim()
})

watch(selectedProvider, (provider) => {
  const preset = getPresetByProvider(provider)
  if (preset && provider !== 'custom') {
    form.baseUrl = preset.baseUrl
  }
})

onMounted(() => {
  if (props.editing) {
    form.name = props.editing.name
    form.systemPrompt = props.editing.systemPrompt || ''
    form.baseUrl = props.editing.baseUrl || 'https://api.openai.com/v1'
    form.apiKey = props.editing.apiKey || ''
    form.model = props.editing.model || 'gpt-4o'
    form.temperature = props.editing.temperature || 0.7
    form.avatarIcon = props.editing.avatarIcon || '✨'
    form.avatarColor = props.editing.avatarColor || colors[0]
    form.avatarUrl = (props.editing as any).avatarUrl || ''
    savedAssistantId.value = props.editing.id
    
    // 检测供应商
    const detectedProvider = detectProvider(form.baseUrl)
    if (detectedProvider) {
      selectedProvider.value = detectedProvider
    }
  }
})

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

function cycleIcon() {
  const currentIndex = icons.indexOf(form.avatarIcon)
  form.avatarIcon = icons[(currentIndex + 1) % icons.length]
  form.avatarColor = colors[Math.floor(Math.random() * colors.length)]
}

async function handleAvatarUpload(file: File) {
  if (!savedAssistantId.value) {
    toast.warning('请先保存AI助手，然后再上传头像')
    return
  }
  
  isUploadingAvatar.value = true
  avatarUploadRef.value?.setUploading(true)
  
  try {
    const response = await uploadAiAvatar(
      savedAssistantId.value,
      file,
      (progress) => {
        avatarUploadRef.value?.setProgress(progress)
      }
    )
    
    if (response.success) {
      form.avatarUrl = response.url || ''
      toast.success('头像更新成功')
    } else {
      toast.error(response.message || '头像上传失败')
    }
  } catch (error: any) {
    toast.error(error?.message || '头像上传失败')
  } finally {
    isUploadingAvatar.value = false
    avatarUploadRef.value?.setUploading(false)
    avatarUploadRef.value?.setProgress(0)
  }
}

async function handleSave() {
  if (!isValid.value) return

  const url = props.editing 
    ? `/api/ai/assistants/${props.editing.id}`
    : '/api/ai/assistants'
  
  const method = props.editing ? 'PUT' : 'POST'
  
  const userData = localStorage.getItem('user')
  const userId = userData ? JSON.parse(userData).userId : null
  
  if (!userId) {
    toast.warning('请先登录')
    return
  }

  try {
    const response = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...form, userId })
    })

    if (response.ok) {
      const assistant = await response.json()
      savedAssistantId.value = assistant.id
      emit('created', assistant)
    } else {
      toast.error('保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    toast.error('保存失败')
  }
}
</script>

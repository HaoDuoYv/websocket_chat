<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { getProviderOptions, getPresetByProvider } from '@/config/llmProviders'
import AvatarUpload from '@/components/AvatarUpload.vue'
import { uploadAiAvatar } from '@/api/avatar'
import { ElMessage } from 'element-plus'

const enabled = ref(false)
const showKey = ref(false)
const testResult = ref<{ success: boolean; message: string } | null>(null)
const selectedProvider = ref('openai')
const avatarUploadRef = ref<InstanceType<typeof AvatarUpload> | null>(null)
const systemAssistantId = ref<string | null>(null)
const saving = ref(false)
const testing = ref(false)

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
  maxTokens: 4096,
  avatarUrl: ''
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
      systemAssistantId.value = data.config.id || null
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

async function handleAvatarUpload(file: File) {
  if (!systemAssistantId.value) {
    ElMessage.warning('请先保存配置后再上传头像')
    return
  }
  try {
    const response = await uploadAiAvatar(systemAssistantId.value, file, (progress) => {
      avatarUploadRef.value?.setProgress(progress)
    })
    if (response.success) {
      form.avatarUrl = response.url || ''
      ElMessage.success('头像上传成功')
    } else {
      ElMessage.error(response.message || '头像上传失败')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '头像上传失败')
  }
}

async function saveConfig() {
  saving.value = true
  try {
    const response = await fetch('/api/admin/ai/config', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...form, enabled: enabled.value })
    })
    if (response.ok) {
      ElMessage.success('保存成功')
    } else {
      ElMessage.error('保存失败')
    }
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function testConnection() {
  testing.value = true
  testResult.value = null
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
  } catch {
    testResult.value = { success: false, message: '连接测试失败' }
  } finally {
    testing.value = false
  }
}
</script>

<template>
  <div class="ai-config-page">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span class="page-title">AI 助手配置</span>
          <div class="page-actions">
            <el-button @click="testConnection" :loading="testing">测试连接</el-button>
            <el-button type="primary" @click="saveConfig" :loading="saving">保存配置</el-button>
          </div>
        </div>
      </template>

      <div class="config-section">
        <div class="section-row">
          <div>
            <div class="section-label">启用系统AI助手</div>
            <div class="section-desc">开启后所有用户可在聊天列表中看到系统AI</div>
          </div>
          <el-switch v-model="enabled" />
        </div>
      </div>

      <template v-if="enabled">
        <div class="avatar-section">
          <AvatarUpload
            ref="avatarUploadRef"
            :model-value="form.avatarUrl"
            default-icon="🤖"
            size="lg"
            hint="点击上传系统助手头像"
            @upload="handleAvatarUpload"
          />
        </div>

        <el-card shadow="never" class="inner-card">
          <template #header><span class="inner-title">基本信息</span></template>
          <el-form label-width="100px" label-position="left">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="助手名称">
                  <el-input v-model="form.name" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="欢迎语">
                  <el-input v-model="form.welcomeMessage" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="系统提示词">
              <el-input v-model="form.systemPrompt" type="textarea" :rows="4" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="inner-card">
          <template #header><span class="inner-title">API 配置</span></template>
          <el-form label-width="100px" label-position="left">
            <el-form-item label="供应商">
              <el-select v-model="selectedProvider" style="width: 100%;">
                <el-option
                  v-for="option in providerOptions"
                  :key="option.value"
                  :label="option.label + (option.description ? ` - ${option.description}` : '')"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
            <el-row :gutter="20">
              <el-col :span="16">
                <el-form-item label="Base URL">
                  <el-input v-model="form.baseUrl" :disabled="selectedProvider !== 'custom'" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="模型">
                  <el-input v-model="form.model" :disabled="selectedProvider !== 'custom'" placeholder="输入或选择模型" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="API Key">
              <el-input v-model="form.apiKey" :type="showKey ? 'text' : 'password'" show-password />
            </el-form-item>
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="温度">
                  <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" style="width: 100%;" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="上下文条数">
                  <el-input-number v-model="form.maxContext" :min="1" style="width: 100%;" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="最大Token">
                  <el-input-number v-model="form.maxTokens" :min="1" style="width: 100%;" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-card>

        <el-card shadow="never" class="inner-card">
          <template #header><span class="inner-title">高级设置</span></template>
          <div class="advanced-row">
            <div class="advanced-item">
              <div>
                <div class="section-label">流式输出</div>
                <div class="section-desc">实时显示AI回复</div>
              </div>
              <el-switch :model-value="true" disabled />
            </div>
            <div class="advanced-item">
              <div>
                <div class="section-label">Markdown渲染</div>
                <div class="section-desc">支持代码块等格式</div>
              </div>
              <el-switch :model-value="true" disabled />
            </div>
          </div>
        </el-card>
      </template>

      <el-alert
        v-if="testResult"
        :title="testResult.message"
        :type="testResult.success ? 'success' : 'error'"
        show-icon
        :closable="true"
        class="test-result"
        @close="testResult = null"
      />
    </el-card>
  </div>
</template>

<style scoped>
.ai-config-page {
  max-width: 900px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-weight: 600;
  font-size: 15px;
}

.page-actions {
  display: flex;
  gap: 8px;
}

.config-section {
  padding: 20px 0;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 20px;
}

:root.dark .config-section {
  border-bottom-color: #27272a;
}

.section-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-label {
  font-weight: 500;
  color: #0f0f0f;
}

:root.dark .section-label {
  color: #f5f5f5;
}

.section-desc {
  font-size: 13px;
  color: #6b7280;
  margin-top: 2px;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.inner-card {
  margin-bottom: 16px;
}

.inner-title {
  font-weight: 600;
  font-size: 14px;
}

.advanced-row {
  display: flex;
  gap: 16px;
}

.advanced-item {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

:root.dark .advanced-item {
  background: #27272a;
}

.test-result {
  margin-top: 16px;
}
</style>

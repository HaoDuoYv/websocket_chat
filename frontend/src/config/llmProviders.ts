export interface LlmProviderPreset {
  label: string
  baseUrl: string
  model: string
  description?: string
}

export const LLM_PROVIDER_PRESETS: Record<string, LlmProviderPreset> = {
  openai: {
    label: 'OpenAI',
    baseUrl: 'https://api.openai.com',
    model: 'gpt-4o-mini',
    description: 'GPT-4o Mini'
  },
  deepseek: {
    label: 'DeepSeek',
    baseUrl: 'https://api.deepseek.com',
    model: 'deepseek-chat',
    description: 'DeepSeek Chat'
  },
  glm: {
    label: '智谱 GLM',
    baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
    model: 'glm-4-flash',
    description: 'GLM-4 Flash'
  },
  qwen: {
    label: '通义千问',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode',
    model: 'qwen-turbo',
    description: 'Qwen Turbo'
  },
  kimi: {
    label: 'Kimi (月之暗面)',
    baseUrl: 'https://api.moonshot.cn',
    model: 'moonshot-v1-8k',
    description: 'Moonshot V1 8K'
  },
  mimo: {
    label: 'Mimo',
    baseUrl: 'https://api.mimo.com',
    model: 'mimo-default',
    description: 'Mimo Default'
  },
  custom: {
    label: '自定义',
    baseUrl: '',
    model: '',
    description: '自定义API端点'
  }
}

export const getProviderOptions = () => {
  return Object.entries(LLM_PROVIDER_PRESETS).map(([key, preset]) => ({
    value: key,
    label: preset.label,
    description: preset.description
  }))
}

export const getPresetByProvider = (provider: string): LlmProviderPreset | undefined => {
  return LLM_PROVIDER_PRESETS[provider]
}

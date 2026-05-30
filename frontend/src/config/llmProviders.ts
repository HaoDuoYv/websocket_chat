export interface LlmProviderPreset {
  label: string
  baseUrl: string
  description?: string
}

export const LLM_PROVIDER_PRESETS: Record<string, LlmProviderPreset> = {
  openai: {
    label: 'OpenAI',
    baseUrl: 'https://api.openai.com',
    description: 'OpenAI API'
  },
  deepseek: {
    label: 'DeepSeek',
    baseUrl: 'https://api.deepseek.com',
    description: 'DeepSeek API'
  },
  glm: {
    label: '智谱 GLM',
    baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
    description: '智谱AI API'
  },
  qwen: {
    label: '通义千问',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    description: '阿里云百炼 API'
  },
  kimi: {
    label: 'Kimi (月之暗面)',
    baseUrl: 'https://api.moonshot.cn',
    description: '月之暗面 API'
  },
  mimo: {
    label: '小米 MiMo',
    baseUrl: 'https://api.xiaomimimo.com/v1',
    description: '小米 MiMo API'
  },
  custom: {
    label: '自定义',
    baseUrl: '',
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

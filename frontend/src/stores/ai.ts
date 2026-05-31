import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface MessageAttachment {
  kind: 'image' | 'text'
  name: string
  mimeType: string
  size?: number
  data: string
}

export interface AiAssistant {
  id: string
  name: string
  avatarIcon?: string
  avatarColor?: string
  avatarUrl?: string
  systemPrompt?: string
  baseUrl?: string
  apiKey?: string
  model?: string
  temperature?: number
  maxContext?: number
  maxTokens?: number
  isSystem: boolean
  isPublic?: boolean
  shareCode?: string
  createdAt?: number
  updatedAt?: number
}

export interface AiConversation {
  id: string
  userId: string
  assistantId: string
  title: string
  summary?: string
  messageCount: number
  createdAt: number
  updatedAt: number
}

export interface AiMessage {
  id: string
  conversationId: string
  role: 'user' | 'assistant' | 'system'
  content: string
  tokenCount?: number
  createdAt: number
  attachments?: MessageAttachment[]
}

export interface ToolCallState {
  callId: string
  toolName: string
  args: string
  result?: string
  status: 'running' | 'done'
}

export const useAiStore = defineStore('ai', () => {
  const systemAssistant = ref<AiAssistant | null>(null)
  const userAssistants = ref<AiAssistant[]>([])
  const currentAssistant = ref<AiAssistant | null>(null)
  const currentConversation = ref<AiConversation | null>(null)
  const conversations = ref<AiConversation[]>([])
  const messages = ref<AiMessage[]>([])
  const isStreaming = ref(false)
  const isThinking = ref(false)
  const streamContent = ref('')
  const error = ref<string | null>(null)
  const isTyping = ref(false)
  const isStreamDone = ref(false)
  const activeToolCalls = ref<ToolCallState[]>([])

  const allAssistants = computed(() => {
    const list: AiAssistant[] = []
    if (systemAssistant.value) {
      list.push(systemAssistant.value)
    }
    list.push(...userAssistants.value)
    return list
  })

  function setSystemAssistant(assistant: AiAssistant | null) {
    systemAssistant.value = assistant
  }

  function setUserAssistants(assistants: AiAssistant[]) {
    userAssistants.value = assistants
  }

  function addUserAssistant(assistant: AiAssistant) {
    const index = userAssistants.value.findIndex(a => a.id === assistant.id)
    if (index >= 0) {
      userAssistants.value[index] = assistant
    } else {
      userAssistants.value.push(assistant)
    }
  }

  function removeUserAssistant(assistantId: string) {
    userAssistants.value = userAssistants.value.filter(a => a.id !== assistantId)
  }

  function updateAssistantAvatar(assistantId: string, avatarUrl: string) {
    // Update in userAssistants
    const index = userAssistants.value.findIndex(a => a.id === assistantId)
    if (index >= 0) {
      userAssistants.value[index] = { ...userAssistants.value[index], avatarUrl }
    }
    // Update currentAssistant if it's the same
    if (currentAssistant.value?.id === assistantId) {
      currentAssistant.value = { ...currentAssistant.value, avatarUrl }
    }
    // Update systemAssistant if it's the same
    if (systemAssistant.value?.id === assistantId) {
      systemAssistant.value = { ...systemAssistant.value, avatarUrl }
    }
  }

  function setCurrentAssistant(assistant: AiAssistant | null) {
    currentAssistant.value = assistant
  }

  function setCurrentConversation(conversation: AiConversation | null) {
    currentConversation.value = conversation
  }

  function setConversations(list: AiConversation[]) {
    conversations.value = list
  }

  function addConversation(conversation: AiConversation) {
    const index = conversations.value.findIndex(c => c.id === conversation.id)
    if (index >= 0) {
      conversations.value[index] = conversation
    } else {
      conversations.value.unshift(conversation)
    }
  }

  function removeConversation(conversationId: string) {
    conversations.value = conversations.value.filter(c => c.id !== conversationId)
    if (currentConversation.value?.id === conversationId) {
      currentConversation.value = null
    }
  }

  function setMessages(list: AiMessage[]) {
    messages.value = list
  }

  function addMessage(message: AiMessage) {
    messages.value.push(message)
  }

  function startStream() {
    isStreaming.value = true
    isThinking.value = false
    streamContent.value = ''
    isStreamDone.value = false
  }

  function setThinking(thinking: boolean) {
    isThinking.value = thinking
  }

  function appendStreamToken(token: string) {
    streamContent.value += token
  }

  function endStream() {
    isStreaming.value = false
    isThinking.value = false
    isTyping.value = false
    isStreamDone.value = true
    // 不立即清空streamContent，等complete事件后由addMessageFromStream处理
  }

  function addMessageFromStream(message: AiMessage) {
    // 先添加消息，再清空流式内容
    messages.value.push(message)
    streamContent.value = ''
    isStreamDone.value = false
    activeToolCalls.value = []
  }

  function setError(message: string | null) {
    error.value = message
  }

  function clearError() {
    error.value = null
  }

  function setTyping(typing: boolean) {
    isTyping.value = typing
  }

  function clearChatState() {
    currentConversation.value = null
    conversations.value = []
    messages.value = []
    isStreaming.value = false
    isThinking.value = false
    streamContent.value = ''
    error.value = null
    isTyping.value = false
    isStreamDone.value = false
    activeToolCalls.value = []
  }

  function addToolCall(callId: string, toolName: string, args: string) {
    activeToolCalls.value.push({ callId, toolName, args, status: 'running' })
  }

  function updateToolResult(callId: string, result: string) {
    const tc = activeToolCalls.value.find(t => t.callId === callId)
    if (tc) {
      tc.result = result
      tc.status = 'done'
    }
  }

  function clearToolCalls() {
    activeToolCalls.value = []
  }

  function reset() {
    systemAssistant.value = null
    userAssistants.value = []
    currentAssistant.value = null
    currentConversation.value = null
    conversations.value = []
    messages.value = []
    isStreaming.value = false
    isThinking.value = false
    streamContent.value = ''
    error.value = null
    isTyping.value = false
    isStreamDone.value = false
  }

  return {
    systemAssistant,
    userAssistants,
    currentAssistant,
    currentConversation,
    conversations,
    messages,
    isStreaming,
    isThinking,
    streamContent,
    error,
    isTyping,
    isStreamDone,
    activeToolCalls,
    allAssistants,
    setSystemAssistant,
    setUserAssistants,
    addUserAssistant,
    removeUserAssistant,
    updateAssistantAvatar,
    setCurrentAssistant,
    setCurrentConversation,
    setConversations,
    addConversation,
    removeConversation,
    setMessages,
    addMessage,
    addMessageFromStream,
    startStream,
    appendStreamToken,
    endStream,
    setError,
    clearError,
    setTyping,
    setThinking,
    clearChatState,
    reset,
    addToolCall,
    updateToolResult,
    clearToolCalls
  }
})

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import type { CSSProperties } from 'vue'
import LoginForm from '@/components/LoginForm.vue'
import CreateGroupDialog from '@/components/CreateGroupDialog.vue'
import FileMessage from '@/components/FileMessage.vue'
import FileUploadButton from '@/components/FileUploadButton.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import SetRemarkDialog from '@/components/SetRemarkDialog.vue'
import { useWebSocket } from '@/composables/useWebSocket'
import { formatFileSize, getFileIcon, getFileTypeDescription, isImageFile, uploadFile } from '@/api/file'
import { getUserRemarks, saveUserRemark } from '@/api/userRemark'

const PROJECT_NOTICE_STORAGE_KEY = 'project-notice-dismissed'
const ACTIVE_TAB_STORAGE_KEY = 'home-active-tab'
const projectNotice = {
  title: '项目公告',
  summary: '这是一个基于 WebSocket 的即时聊天系统演示项目，适合学习实时通信、前后端分离与聊天场景设计。',
  highlights: [
    '支持私聊、群聊、文件传输、表情消息与管理员后台。',
    '前端使用 Vue 3 + TypeScript，后端基于 Spring Boot 与 WebSocket。',
    '更适合用于学习、演示和局域网环境体验，不建议直接作为生产方案使用。'
  ],
  links: [
    {
      label: 'GitHub 项目地址',
      href: 'https://github.com/HaoDuoYv/websocket_chat'
    }
  ]
}

interface PendingAttachment {
  id: string
  file: File
  previewUrl: string
  isImage: boolean
}

interface FloatingNotice {
  id: string
  title: string
  body: string
  roomId: string
}

const user = ref<any>(null)
const isCreateDialogOpen = ref(false)
const isDarkTheme = ref(localStorage.getItem('theme') === 'dark')
const isProjectNoticeOpen = ref(false)
const hasShownProjectNotice = ref(false)
const {
  connect,
  disconnect,
  rooms,
  createRoom,
  onlineUsers,
  getUnreadCount,
  sendMessage,
  sendFileMessage,
  messages,
  startPrivateChat,
  setCurrentRoom,
  loadMessageHistory,
  socket,
  lastInviteResult,
  lastBannedResult,
  lastRoomMemberLeft,
  lastPrivateRoomCreated,
} = useWebSocket()

const selectedRoomId = ref<string | null>(null)
const newMessage = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const uploadError = ref('')
const showEmojiPicker = ref(false)
const isDraggingFile = ref(false)
const fileUploadButtonRef = ref<{ queueFiles: (files: File[] | FileList) => Promise<void> } | null>(null)
const pendingAttachments = ref<PendingAttachment[]>([])
const previewingAttachment = ref<PendingAttachment | null>(null)
const previewingSentImage = ref<{ fileName: string; fileSize: number; fileUrl: string; fileType: string } | null>(null)
const isSendingFiles = ref(false)
const uploadProgress = ref(0)
const uploadingFileName = ref('')
let dragDepth = 0

const activeTab = ref<'messages' | 'contacts'>((localStorage.getItem(ACTIVE_TAB_STORAGE_KEY) as 'messages' | 'contacts') || 'messages')
const searchQuery = ref('')
const confirmDialog = ref({
  isOpen: false,
  title: '',
  message: '',
  onConfirm: () => {}
})
const selectedContact = ref<any>(null)
const showChatMenu = ref(false)
const showMemberList = ref(false)
const roomMembers = ref<any[]>([])
const showInviteDialog = ref(false)
const isRemarkDialogOpen = ref(false)
const remarkTarget = ref<{ userId: string; username: string } | null>(null)
const userRemarks = ref<Record<string, string>>({})
const userRemarkRefreshTimer = ref<number | null>(null)
const browserNotificationEnabled = ref(false)
const notificationAudio = typeof Audio !== 'undefined' ? new Audio('/notification.mp3') : null
const floatingNotices = ref<FloatingNotice[]>([])
const unreadPageCount = computed(() => rooms.value.reduce((total, room) => total + getUnreadCount(room.id), 0))

const filteredRooms = computed(() => {
  if (!searchQuery.value.trim()) return rooms.value
  const query = searchQuery.value.toLowerCase()
  return rooms.value.filter(room =>
    getDisplayRoomName(room).toLowerCase().includes(query) ||
    getRoomPreview(room).toLowerCase().includes(query)
  )
})

const otherUsers = computed(() => {
  const selfId = user.value?.userId
  return onlineUsers.value.filter(onlineUser => onlineUser.userId !== selfId)
})

const contactSortName = (contact: { userId: string; username: string }) => {
  return getRemarkName(contact.userId, contact.username)
}

const allContacts = computed(() => {
  return [...otherUsers.value].sort((a, b) => contactSortName(a).localeCompare(contactSortName(b), 'zh-CN'))
})

const filteredContacts = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  if (!query) return allContacts.value
  return allContacts.value.filter(contact =>
    getRemarkName(contact.userId, contact.username).toLowerCase().includes(query)
  )
})

const inviteableUsers = computed(() => {
  const memberIds = new Set(roomMembers.value.map(m => m.userId))
  return filteredContacts.value.filter(contact => !memberIds.has(contact.userId))
})

const currentRoom = computed(() => {
  return rooms.value.find(r => r.id === selectedRoomId.value)
})

const roomMessages = computed(() => {
  if (!selectedRoomId.value) return []
  return messages.value
    .filter(msg => String(msg.roomId) === selectedRoomId.value)
    .sort((a, b) => a.seq - b.seq)
    .map(message => ({
      ...message,
      senderName: getMessageSenderName(String(message.senderId), message.senderName)
    }))
})

const isRoomOwner = computed(() => {
  if (!selectedRoomId.value || !currentRoom.value) return false
  return currentRoom.value.ownerId === user.value?.userId
})

const isGroupChat = computed(() => {
  return currentRoom.value?.type === 'public'
})

const onlineStatusText = computed(() => {
  if (!currentRoom.value) return ''
  if (isGroupChat.value) {
    return `${roomMembers.value.length || 0} 位成员`
  }
  return `${onlineUsers.value.filter(item => item.isOnline !== false).length} 人在线`
})

const canSend = computed(() => {
  return !!user.value && !!selectedRoomId.value && !isSendingFiles.value && (
    pendingAttachments.value.length > 0 || !!newMessage.value.trim()
  )
})

const getAvatarColor = (userId: string) => {
  const colors = ['#0891B2', '#0EA5E9', '#10B981', '#6366F1', '#8B5CF6', '#EC4899', '#F59E0B', '#64748B']
  let hash = 0
  for (let i = 0; i < userId.length; i++) {
    hash = userId.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

const getAvatarText = (name: string) => {
  return name ? name.charAt(0).toUpperCase() : '?'
}

const getRemarkName = (targetUserId: string, fallbackName: string) => {
  return userRemarks.value[targetUserId] || fallbackName
}

const getDisplayRoomName = (room: { id: string; name: string; type: string }) => {
  if (room.type !== 'private') return room.name

  // 私聊房间：room.name 由后端设置，已经是对方的用户名（或备注名）
  // 直接使用即可，无需额外匹配
  return room.name
}

const getMessageSenderName = (senderId: string, senderName: string) => {
  return getRemarkName(senderId, senderName)
}

const getRoomPreview = (room: { lastMessage?: { senderId: string; senderName?: string; content: string } }) => {
  if (!room.lastMessage) return ''
  const senderName = room.lastMessage.senderName
    ? getMessageSenderName(String(room.lastMessage.senderId), room.lastMessage.senderName)
    : ''
  return senderName ? `${senderName}: ${room.lastMessage.content}` : room.lastMessage.content
}

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()

  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const formatDate = (timestamp: number) => {
  const date = new Date(timestamp)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)

  if (date.toDateString() === today.toDateString()) return '今天'
  if (date.toDateString() === yesterday.toDateString()) return '昨天'
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const shouldShowDate = (index: number) => {
  if (index === 0) return true
  const current = roomMessages.value[index]
  const prev = roomMessages.value[index - 1]
  return new Date(current.timestamp).toDateString() !== new Date(prev.timestamp).toDateString()
}

const shouldShowTime = (index: number) => {
  if (index === 0) return true
  const current = roomMessages.value[index]
  const prev = roomMessages.value[index - 1]
  return current.timestamp - prev.timestamp > 5 * 60 * 1000
}

const truncateMessage = (content: string, maxLength: number = 30) => {
  if (!content) return ''
  return content.length > maxLength ? content.slice(0, maxLength) + '...' : content
}

const urlPattern = /(https?:\/\/[^\s<]+[^\s<.,;:!?，。；：！？）\]\}])/g

const escapeHtml = (content: string) => {
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const renderMessageContent = (content: string) => {
  if (!content) return ''

  return escapeHtml(content).replace(urlPattern, url => {
    return `<a href="${url}" target="_blank" rel="noopener noreferrer" class="underline break-all">${url}</a>`
  })
}

const messageContentClass = (isSelf: boolean) => {
  return isSelf ? 'text-white [&_a]:text-white' : (isDarkTheme.value ? 'text-gray-200 [&_a]:text-sky-300' : 'text-gray-700 [&_a]:text-sky-600')
}

const messageContentStyle = (): CSSProperties => ({
  overflowWrap: 'anywhere',
  wordBreak: 'break-word'
})

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const showUploadError = (error: string) => {
  uploadError.value = error
  window.setTimeout(() => {
    if (uploadError.value === error) {
      uploadError.value = ''
    }
  }, 3000)
}

const ensureNotificationPermission = async () => {
  if (typeof window === 'undefined' || !('Notification' in window)) {
    browserNotificationEnabled.value = false
    return
  }

  if (Notification.permission === 'granted') {
    browserNotificationEnabled.value = true
    return
  }

  if (Notification.permission === 'denied') {
    browserNotificationEnabled.value = false
    return
  }

  const permission = await Notification.requestPermission()
  browserNotificationEnabled.value = permission === 'granted'
}

const playNotificationSound = async () => {
  if (!notificationAudio) return

  try {
    notificationAudio.currentTime = 0
    await notificationAudio.play()
  } catch {
    notificationAudio.muted = true
  }
}

const showBrowserNotification = (title: string, body: string) => {
  if (!browserNotificationEnabled.value || document.visibilityState === 'visible') {
    return
  }

  new Notification(title, { body })
}

const removeFloatingNotice = (noticeId: string) => {
  floatingNotices.value = floatingNotices.value.filter(notice => notice.id !== noticeId)
}

const openNoticeRoom = (roomId: string, noticeId: string) => {
  handleRoomClick(roomId)
  removeFloatingNotice(noticeId)
}

const showFloatingNotice = (title: string, body: string, roomId: string) => {
  const notice: FloatingNotice = {
    id: `${roomId}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    title,
    body,
    roomId
  }

  floatingNotices.value = [...floatingNotices.value.slice(-2), notice]
  window.setTimeout(() => {
    removeFloatingNotice(notice.id)
  }, 5000)
}

const revokeAttachment = (attachment: PendingAttachment) => {
  if (attachment.previewUrl) {
    URL.revokeObjectURL(attachment.previewUrl)
  }
}

const clearPendingAttachments = () => {
  pendingAttachments.value.forEach(revokeAttachment)
  pendingAttachments.value = []
  previewingAttachment.value = null
}

const buildAttachmentId = (file: File) => `${file.name}-${file.size}-${file.lastModified}`

const queuePendingFiles = (files: File[]) => {
  const knownIds = new Set(pendingAttachments.value.map(attachment => attachment.id))

  for (const file of files) {
    const id = buildAttachmentId(file)
    if (knownIds.has(id)) continue

    pendingAttachments.value.push({
      id,
      file,
      previewUrl: isImageFile(file.type) ? URL.createObjectURL(file) : '',
      isImage: isImageFile(file.type)
    })
    knownIds.add(id)
  }
}

const removePendingAttachment = (attachmentId: string) => {
  const index = pendingAttachments.value.findIndex(attachment => attachment.id === attachmentId)
  if (index === -1) return

  const [attachment] = pendingAttachments.value.splice(index, 1)
  if (previewingAttachment.value?.id === attachment.id) {
    previewingAttachment.value = null
  }
  revokeAttachment(attachment)
}

const openAttachmentPreview = (attachment: PendingAttachment) => {
  if (!attachment.isImage) return
  previewingAttachment.value = attachment
}

const openSentImagePreview = (data: { fileName: string; fileSize: number; fileUrl: string; fileType: string }) => {
  if (!isImageFile(data.fileType)) return
  previewingSentImage.value = data
}

const closeAttachmentPreview = () => {
  previewingAttachment.value = null
  previewingSentImage.value = null
}

const downloadPreviewedAttachment = () => {
  const url = previewingAttachment.value?.previewUrl || previewingSentImage.value?.fileUrl
  const name = previewingAttachment.value?.file.name || previewingSentImage.value?.fileName
  if (!url || !name) return
  const link = document.createElement('a')
  link.href = url
  link.download = name
  link.rel = 'noopener'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const handleAttachmentPreviewKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && (previewingAttachment.value || previewingSentImage.value)) {
    closeAttachmentPreview()
  }
}

const anyPreviewOpen = computed(() => !!previewingAttachment.value || !!previewingSentImage.value)
watch(anyPreviewOpen, open => {
  if (typeof document === 'undefined') return
  document.body.style.overflow = open ? 'hidden' : ''
})

watch(activeTab, value => {
  localStorage.setItem(ACTIVE_TAB_STORAGE_KEY, value)
})

watch(roomMessages, () => {
  scrollToBottom()
}, { deep: true })

watch(selectedRoomId, (newId, oldId) => {
  if (newId) {
    setCurrentRoom(newId)
    loadMessageHistory(newId)
    scrollToBottom()
    if (currentRoom.value?.type === 'public') {
      loadRoomMembers(newId)
    } else {
      roomMembers.value = []
    }
  }
  if (oldId && newId !== oldId) {
    clearPendingAttachments()
  }
})

watch(lastInviteResult, async result => {
  if (!result) return

  if (result.success && selectedRoomId.value && showInviteDialog.value === false) {
    await loadRoomMembers(selectedRoomId.value)
  }

  alert(result.message)
  lastInviteResult.value = null
})

watch(() => document.visibilityState, () => {
  browserNotificationEnabled.value = typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'granted'
})

watch(unreadPageCount, count => {
  if (typeof document === 'undefined') return
  document.title = count > 0 ? `(${count}) WebSocket聊天` : 'WebSocket聊天'
}, { immediate: true })

watch(messages, (newMessages, oldMessages) => {
  if (!user.value || !oldMessages || newMessages.length <= oldMessages.length) return

  const incomingMessages = newMessages.slice(oldMessages.length)
  for (const message of incomingMessages) {
    if (String(message.senderId) === user.value.userId) continue
    if (String(message.roomId) === selectedRoomId.value && document.visibilityState === 'visible') continue

    const senderName = getMessageSenderName(String(message.senderId), message.senderName)
    const room = rooms.value.find(item => item.id === String(message.roomId))
    const roomName = room ? getDisplayRoomName(room) : '新消息'
    const noticeBody = roomName === senderName ? message.content : `${roomName} · ${message.content}`
    playNotificationSound()
    showBrowserNotification(senderName, noticeBody)
    showFloatingNotice(senderName, noticeBody, String(message.roomId))
  }
}, { deep: true })

watch(messages, async (newMessages, oldMessages) => {
  if (!selectedRoomId.value || !currentRoom.value || currentRoom.value.type !== 'public' || !oldMessages) return
  if (newMessages.length === oldMessages.length) return
  await loadRoomMembers(selectedRoomId.value)
}, { deep: true })

watch(lastBannedResult, result => {
  if (!result) return
  alert(result.message)
  handleLogout()
  lastBannedResult.value = null
})

watch(lastRoomMemberLeft, async event => {
  if (!event || !selectedRoomId.value || !currentRoom.value || currentRoom.value.type !== 'public') return
  if (event.roomId !== selectedRoomId.value) return

  roomMembers.value = roomMembers.value.filter(member => member.userId !== event.userId)
  await loadRoomMembers(event.roomId, showMemberList.value)
  lastRoomMemberLeft.value = null
})

// 当私聊房间创建后自动选中
watch(lastPrivateRoomCreated, room => {
  if (!room || !selectedContact.value) return
  // 仅在用户刚点击了联系人时才自动选中
  if (selectedRoomId.value !== room.id) {
    selectedRoomId.value = room.id
  }
  lastPrivateRoomCreated.value = null
})

const showProjectNotice = () => {
  hasShownProjectNotice.value = true
  isProjectNoticeOpen.value = true
}

const closeProjectNotice = () => {
  isProjectNoticeOpen.value = false
}

const dismissProjectNotice = () => {
  localStorage.setItem(PROJECT_NOTICE_STORAGE_KEY, 'true')
  closeProjectNotice()
}

onMounted(() => {
  const userData = localStorage.getItem('user')
  if (userData) {
    user.value = JSON.parse(userData)
    connect(user.value)
    loadUserRemarks(user.value.userId)
    startUserRemarkAutoRefresh()
  }
  ensureNotificationPermission()
  window.addEventListener('keydown', handleAttachmentPreviewKeydown)
})

onUnmounted(() => {
  clearPendingAttachments()
  stopUserRemarkAutoRefresh()
  document.body.style.overflow = ''
  window.removeEventListener('keydown', handleAttachmentPreviewKeydown)
})

const handleRoomClick = (roomId: string) => {
  if (!roomId) return
  selectedRoomId.value = roomId
}

const handleCreateGroup = () => {
  isCreateDialogOpen.value = true
}

const handleCreateGroupSubmit = (name: string, participants: string[]) => {
  createRoom(name, participants)
}

const showConfirmDialog = (title: string, message: string, onConfirm: () => void) => {
  confirmDialog.value = { isOpen: true, title, message, onConfirm }
}

const closeConfirmDialog = () => {
  confirmDialog.value.isOpen = false
}

const handleConfirm = () => {
  confirmDialog.value.onConfirm()
  closeConfirmDialog()
}

const handleContactClick = (targetUser: { userId: string; username: string }) => {
  selectedContact.value = targetUser

  // 如果私聊房间已存在，直接选中并切换
  const existingPrivateRoom = rooms.value.find(
    r => r.type === 'private' && r.name === targetUser.username
  )
  if (existingPrivateRoom) {
    selectedRoomId.value = existingPrivateRoom.id
    activeTab.value = 'messages'
  } else {
    startPrivateChat(targetUser.userId)
    activeTab.value = 'messages'
  }
}

const handleCreateDialogClose = () => {
  isCreateDialogOpen.value = false
}

const loadRoomMembers = async (roomId: string, shouldOpenDialog: boolean = false) => {
  if (!user.value) return

  try {
    const res = await fetch(`/api/rooms/${roomId}/members?userId=${user.value.userId}`)
    const data = await res.json()
    roomMembers.value = data.members || []
    if (shouldOpenDialog) {
      showMemberList.value = true
    }
  } catch (error) {
    console.error('获取成员列表失败:', error)
  }
}

const loadUserRemarks = async (userId: string): Promise<Record<string, string>> => {
  try {
    const data = await getUserRemarks(userId)
    const remarks = data.remarks || {}
    userRemarks.value = remarks
    return remarks
  } catch (error) {
    console.error('获取备注失败:', error)
    return userRemarks.value
  }
}

const syncRemarkViews = (targetUserId: string, remarkName: string) => {
  const displayName = remarkName.trim()

  if (selectedContact.value?.userId === targetUserId) {
    selectedContact.value = {
      ...selectedContact.value,
      username: displayName || selectedContact.value.username
    }
  }

  if (remarkTarget.value?.userId === targetUserId) {
    remarkTarget.value = {
      ...remarkTarget.value,
      username: displayName || remarkTarget.value.username
    }
  }

  roomMembers.value = roomMembers.value.map(member => {
    if (member.userId !== targetUserId) return member
    return {
      ...member,
      username: displayName || member.username
    }
  })
}

const startUserRemarkAutoRefresh = () => {
  if (userRemarkRefreshTimer.value !== null || !user.value) return

  userRemarkRefreshTimer.value = window.setInterval(() => {
    if (!user.value) return
    loadUserRemarks(user.value.userId)
  }, 5000)
}

const stopUserRemarkAutoRefresh = () => {
  if (userRemarkRefreshTimer.value === null) return
  window.clearInterval(userRemarkRefreshTimer.value)
  userRemarkRefreshTimer.value = null
}

const openRemarkDialog = (contact: { userId: string; username: string }) => {
  remarkTarget.value = contact
  isRemarkDialogOpen.value = true
}

const closeRemarkDialog = () => {
  isRemarkDialogOpen.value = false
  remarkTarget.value = null
}

const handleSaveRemark = async (remarkName: string) => {
  if (!user.value || !remarkTarget.value) return

  const targetUserId = remarkTarget.value.userId
  const normalizedRemarkName = remarkName.trim()

  try {
    await saveUserRemark({
      userId: user.value.userId,
      targetUserId,
      remarkName: normalizedRemarkName
    })
    userRemarks.value = {
      ...userRemarks.value,
      [targetUserId]: normalizedRemarkName
    }
    syncRemarkViews(targetUserId, normalizedRemarkName)
    await loadUserRemarks(user.value.userId)
    closeRemarkDialog()
    window.location.reload()
  } catch (error: any) {
    alert(error?.response?.data?.message || '保存备注失败')
  }
}

const handleLogout = () => {
  clearPendingAttachments()
  stopUserRemarkAutoRefresh()
  disconnect()
  localStorage.removeItem('user')
  user.value = null
  userRemarks.value = {}
  remarkTarget.value = null
  isRemarkDialogOpen.value = false
  rooms.value = []
  onlineUsers.value = []
  selectedRoomId.value = null
}

const handleLogin = (userData: any) => {
  user.value = userData
  connect(userData)
  loadUserRemarks(userData.userId)
  startUserRemarkAutoRefresh()
}

const toggleTheme = () => {
  isDarkTheme.value = !isDarkTheme.value
  localStorage.setItem('theme', isDarkTheme.value ? 'dark' : 'light')
}

const toggleChatMenu = (event: MouseEvent) => {
  event.stopPropagation()
  showChatMenu.value = !showChatMenu.value
}

const closeChatMenu = () => {
  showChatMenu.value = false
}

const handleShowMembers = async () => {
  closeChatMenu()
  if (!selectedRoomId.value) return

  await loadRoomMembers(selectedRoomId.value, true)
}

const handleShowInvite = async () => {
  closeChatMenu()
  if (!selectedRoomId.value) return

  await handleShowMembers()
  showInviteDialog.value = true
}

const handleKickMember = (memberId: string, memberName: string) => {
  showConfirmDialog(
    '移出成员',
    `确定要将 ${memberName} 移出群聊吗？`,
    async () => {
      try {
        const roomId = selectedRoomId.value
        const res = await fetch(`/api/rooms/${roomId}/kick`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            ownerId: user.value?.userId,
            targetUserId: memberId
          })
        })

        if (res.ok) {
          roomMembers.value = roomMembers.value.filter(m => m.userId !== memberId)
          if (roomId) {
            await loadRoomMembers(roomId, showMemberList.value)
          }
        } else {
          const data = await res.json()
          alert(data.message || '移出成员失败')
        }
      } catch (error) {
        console.error('移出成员失败:', error)
        alert('移出成员失败')
      }
    }
  )
}

const handleDissolveRoom = () => {
  closeChatMenu()
  showConfirmDialog(
    '解散群聊',
    `确定要解散群聊 "${currentRoom.value?.name}" 吗？此操作不可恢复。`,
    async () => {
      try {
        const res = await fetch(`/api/rooms/${selectedRoomId.value}/dissolve`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ ownerId: user.value?.userId })
        })

        if (res.ok) {
          const index = rooms.value.findIndex(r => r.id === selectedRoomId.value)
          if (index > -1) {
            rooms.value.splice(index, 1)
          }
          selectedRoomId.value = null
          showMemberList.value = false
          clearPendingAttachments()
        } else {
          const data = await res.json()
          alert(data.message || '解散群聊失败')
        }
      } catch (error) {
        console.error('解散群聊失败:', error)
        alert('解散群聊失败')
      }
    }
  )
}

const handleInviteMember = async (targetUserId: string) => {
  if (!selectedRoomId.value) return

  const roomId = selectedRoomId.value

  socket.value?.send(JSON.stringify({
    type: 'room:invite:member',
    data: {
      roomId,
      targetUserId
    }
  }))

  showInviteDialog.value = false
}

const sendPendingAttachments = async () => {
  if (!user.value || !selectedRoomId.value || pendingAttachments.value.length === 0) return

  const queuedAttachments = [...pendingAttachments.value]
  const failedIds = new Set<string>()

  isSendingFiles.value = true
  uploadProgress.value = 0

  try {
    for (const attachment of queuedAttachments) {
      uploadingFileName.value = attachment.file.name
      uploadProgress.value = 0

      try {
        const response = await uploadFile(
          attachment.file,
          selectedRoomId.value,
          user.value.userId,
          progress => {
            uploadProgress.value = progress
          }
        )

        if (!response.success) {
          failedIds.add(attachment.id)
          showUploadError(response.message || `文件 ${attachment.file.name} 上传失败`)
          continue
        }

        sendFileMessage(selectedRoomId.value, user.value.userId, {
          fileId: response.fileId,
          fileName: response.fileName,
          fileUrl: response.fileUrl,
          fileSize: response.fileSize,
          fileType: response.fileType
        })
      } catch (error: any) {
        failedIds.add(attachment.id)
        showUploadError(error.message || `文件 ${attachment.file.name} 上传失败`)
      }
    }
  } finally {
    const attachmentsToKeep: PendingAttachment[] = []
    for (const attachment of pendingAttachments.value) {
      if (failedIds.has(attachment.id)) {
        attachmentsToKeep.push(attachment)
      } else {
        revokeAttachment(attachment)
      }
    }

    pendingAttachments.value = attachmentsToKeep
    if (previewingAttachment.value && !failedIds.has(previewingAttachment.value.id)) {
      previewingAttachment.value = null
    }

    isSendingFiles.value = false
    uploadProgress.value = 0
    uploadingFileName.value = ''
  }
}

const handleSendMessage = async () => {
  if (!canSend.value || !user.value || !selectedRoomId.value) return

  const content = newMessage.value.trim()
  if (content) {
    sendMessage(selectedRoomId.value, content, user.value.userId)
    newMessage.value = ''
  }

  showEmojiPicker.value = false

  if (pendingAttachments.value.length > 0) {
    await sendPendingAttachments()
  }
}

const emojis = ['😀', '😂', '🥰', '😎', '🤔', '👍', '👎', '❤️', '🎉', '🔥', '👏', '🙏', '🤝', '✅', '❌', '📎', '📷', '🎵', '🎁', '💡']

const insertEmoji = (emoji: string) => {
  newMessage.value += emoji
}

const uploadFiles = async (files: File[] | FileList) => {
  if (!user.value || !selectedRoomId.value) return
  await fileUploadButtonRef.value?.queueFiles(files)
}

const hasDraggedFiles = (event: DragEvent) => {
  const types = event.dataTransfer?.types
  return types ? Array.from(types).includes('Files') : false
}

const handlePasteUpload = async (event: ClipboardEvent) => {
  const files = Array.from(event.clipboardData?.files || []).filter(file => file.type.startsWith('image/'))
  if (files.length === 0) return

  event.preventDefault()
  await uploadFiles(files)
}

const handleDragEnter = (event: DragEvent) => {
  if (!selectedRoomId.value || !hasDraggedFiles(event)) return

  event.preventDefault()
  dragDepth += 1
  isDraggingFile.value = true
}

const handleDragOver = (event: DragEvent) => {
  if (!selectedRoomId.value || !hasDraggedFiles(event)) return

  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy'
  }
  isDraggingFile.value = true
}

const handleDragLeave = (event: DragEvent) => {
  if (!hasDraggedFiles(event)) return

  event.preventDefault()
  dragDepth = Math.max(0, dragDepth - 1)
  if (dragDepth === 0) {
    isDraggingFile.value = false
  }
}

const handleDropUpload = async (event: DragEvent) => {
  if (!selectedRoomId.value || !hasDraggedFiles(event)) return

  event.preventDefault()
  dragDepth = 0
  isDraggingFile.value = false

  const files = event.dataTransfer?.files
  if (files && files.length > 0) {
    await uploadFiles(files)
  }
}

const isImageMessage = (message: { type?: string; fileType?: string }) => {
  return message.type === 'file' && isImageFile(message.fileType || '')
}
</script>

<template>
  <div v-if="!user" class="min-h-screen">
    <LoginForm @login="handleLogin" />
  </div>

  <!-- 简约风格 - 大量留白，干净简洁 -->
  <div v-else class="flex h-screen" :class="isDarkTheme ? 'bg-[#0F172A]' : 'bg-white'">
    <div class="fixed top-4 right-4 z-[70] flex w-[320px] max-w-[calc(100vw-2rem)] flex-col gap-3 pointer-events-none">
      <button
        v-for="notice in floatingNotices"
        :key="notice.id"
        type="button"
        class="pointer-events-auto overflow-hidden rounded-2xl border px-4 py-3 text-left shadow-lg backdrop-blur transition hover:scale-[1.01]"
        :class="isDarkTheme ? 'border-slate-700 bg-slate-900/95 text-slate-100' : 'border-slate-200 bg-white/95 text-slate-800'"
        @click="openNoticeRoom(notice.roomId, notice.id)"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <p class="text-xs font-semibold uppercase tracking-[0.2em] text-[#0891B2]">新消息</p>
            <p class="mt-1 truncate text-sm font-medium">{{ notice.title }}</p>
            <p class="mt-1 line-clamp-2 text-xs opacity-80">{{ notice.body }}</p>
          </div>
          <span class="mt-0.5 text-[11px] opacity-50">点击查看</span>
        </div>
      </button>
    </div>

    <!-- 左侧边栏 - 极简导航 -->
    <aside class="w-16 border-r flex flex-col items-center py-6" :class="isDarkTheme ? 'border-gray-800 bg-[#1E293B]' : 'border-gray-100 bg-white'">
      <!-- Logo - 简约几何 -->
      <div class="w-10 h-10 bg-[#0891B2] flex items-center justify-center mb-10">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
        </svg>
      </div>

      <!-- 导航按钮 - 极简线条 -->
      <nav class="flex-1 flex flex-col gap-2">
        <button
          @click="activeTab = 'messages'"
          class="w-10 h-10 flex items-center justify-center transition-all duration-200"
          :class="activeTab === 'messages' ? 'text-[#0891B2]' : (isDarkTheme ? 'text-gray-400 hover:text-gray-200' : 'text-gray-500 hover:text-gray-700')"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          <span v-if="rooms.some(r => getUnreadCount(r.id) > 0)" class="absolute top-4 left-8 w-2 h-2 bg-[#22C55E] rounded-full"></span>
        </button>

        <button
          @click="activeTab = 'contacts'"
          class="w-10 h-10 flex items-center justify-center transition-all duration-200"
          :class="activeTab === 'contacts' ? 'text-[#0891B2]' : (isDarkTheme ? 'text-gray-400 hover:text-gray-200' : 'text-gray-500 hover:text-gray-700')"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
        </button>
      </nav>

      <!-- 底部操作 -->
      <div class="flex flex-col gap-2">
        <!-- 主题切换 -->
        <button
          @click="showProjectNotice"
          class="w-10 h-10 flex items-center justify-center transition-colors"
          :class="isDarkTheme ? 'text-gray-400 hover:text-[#0891B2]' : 'text-gray-500 hover:text-[#0891B2]'"
          title="项目公告"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 11.5a2.5 2.5 0 0 1 2.5-2.5H9l4.5-3v12L9 15H6.5A2.5 2.5 0 0 1 4 12.5z"/>
            <path d="M14 9a4 4 0 0 1 0 6"/>
            <path d="M16 7a7 7 0 0 1 0 10"/>
          </svg>
        </button>

        <button
          @click="toggleTheme"
          class="w-10 h-10 flex items-center justify-center transition-colors"
          :class="isDarkTheme ? 'text-yellow-400 hover:text-yellow-300' : 'text-gray-500 hover:text-gray-700'"
          :title="isDarkTheme ? '切换到浅色模式' : '切换到深色模式'"
        >
          <svg v-if="isDarkTheme" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="5"/>
            <line x1="12" y1="1" x2="12" y2="3"/>
            <line x1="12" y1="21" x2="12" y2="23"/>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
            <line x1="1" y1="12" x2="3" y2="12"/>
            <line x1="21" y1="12" x2="23" y2="12"/>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
          </svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          </svg>
        </button>

        <button
          @click="handleLogout"
          class="w-10 h-10 flex items-center justify-center transition-colors"
          :class="isDarkTheme ? 'text-gray-400 hover:text-gray-200' : 'text-gray-500 hover:text-gray-700'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
        </button>
      </div>
    </aside>

    <!-- 中间栏 - 消息列表/联系人 -->
    <main class="w-72 border-r flex flex-col" :class="isDarkTheme ? 'border-gray-800 bg-[#1E293B]' : 'border-gray-100 bg-white'">
      <!-- 顶部栏 - 极简标题 -->
      <header class="px-5 py-5 border-b" :class="isDarkTheme ? 'border-gray-800' : 'border-gray-50'">
        <div class="flex items-center justify-between mb-4">
          <h1 class="text-sm font-medium tracking-wide uppercase" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-900'">
            {{ activeTab === 'messages' ? '消息' : '联系人' }}
          </h1>
          <button
            @click="handleCreateGroup"
            class="w-7 h-7 flex items-center justify-center transition-colors"
            :class="isDarkTheme ? 'text-gray-500 hover:text-[#0891B2]' : 'text-gray-400 hover:text-[#0891B2]'"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
          </button>
        </div>

        <!-- 搜索框 - 极简边框 -->
        <div class="relative">
          <input
            v-model="searchQuery"
            type="text"
            :placeholder="activeTab === 'messages' ? '搜索消息' : '搜索联系人'"
            class="w-full px-3 py-2 border-0 text-sm focus:outline-none focus:ring-0 transition-all"
            :class="isDarkTheme ? 'bg-gray-800 text-gray-200 placeholder-gray-500' : 'bg-gray-50 text-gray-700 placeholder-gray-400'"
          />
        </div>
      </header>

      <!-- 消息列表 -->
      <div v-if="activeTab === 'messages'" class="flex-1 overflow-y-auto">
        <div v-if="filteredRooms.length === 0" class="flex flex-col items-center justify-center h-48" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-300'">
          <p class="text-xs">暂无消息</p>
        </div>

        <div v-else>
          <div
            v-for="room in filteredRooms"
            :key="room.id"
            @click="handleRoomClick(room.id)"
            class="flex items-center gap-3 px-5 py-3 cursor-pointer transition-colors border-b"
            :class="[
              isDarkTheme ? 'border-gray-800' : 'border-gray-50',
              selectedRoomId === room.id
                ? (isDarkTheme ? 'bg-gray-800' : 'bg-gray-50')
                : (isDarkTheme ? 'hover:bg-gray-800/50' : 'hover:bg-gray-50/50')
            ]"
          >
            <!-- 头像 - 简约圆形 -->
            <div class="relative">
              <div
                class="w-10 h-10 rounded-full flex items-center justify-center text-white text-sm font-medium"
                :style="{ backgroundColor: getAvatarColor(room.id) }"
              >
                {{ getAvatarText(getDisplayRoomName(room)) }}
              </div>
              <div v-if="room.type === 'private'" class="absolute bottom-0 right-0 w-2.5 h-2.5 bg-[#22C55E] border-2 rounded-full" :class="isDarkTheme ? 'border-gray-800' : 'border-white'"></div>
            </div>

            <!-- 内容 - 极简信息 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between mb-0.5">
                <h3 class="font-medium text-sm truncate" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">{{ getDisplayRoomName(room) }}</h3>
                <span v-if="room.lastMessage" class="text-xs" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">
                  {{ formatTime(room.lastMessage.timestamp) }}
                </span>
              </div>
              <div class="flex items-center justify-between">
                <p class="text-xs truncate pr-2" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">
                  <span v-if="room.lastMessage">
                    <span v-if="room.lastMessage.senderId === user?.userId">我: </span>
                    {{ getRoomPreview(room) ? truncateMessage(getRoomPreview(room), 18) : '' }}
                  </span>
                  <span v-else class="italic">暂无消息</span>
                </p>
                <span
                  v-if="getUnreadCount(room.id) > 0"
                  class="min-w-[16px] h-4 px-1 bg-[#0891B2] text-white text-[10px] font-medium flex items-center justify-center rounded-full"
                >
                  {{ getUnreadCount(room.id) > 99 ? '99+' : getUnreadCount(room.id) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 联系人列表 -->
      <div v-else class="flex-1 overflow-y-auto">
        <div class="px-5 py-3 flex items-center justify-between gap-3">
          <h2 class="text-xs font-medium uppercase tracking-wider" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">联系人</h2>
          <p class="text-[11px]" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">
            {{ filteredContacts.length }} 人
          </p>
        </div>

        <div v-if="filteredContacts.length === 0" class="flex flex-col items-center justify-center h-48" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-300'">
          <p class="text-xs">暂无联系人</p>
        </div>

        <div v-else>
          <div
            v-for="contact in filteredContacts"
            :key="contact.userId"
            class="flex items-center gap-3 px-5 py-3 cursor-pointer transition-colors border-b"
            :class="isDarkTheme ? 'border-gray-800 hover:bg-gray-800/50' : 'border-gray-50 hover:bg-gray-50/50'"
          >
            <div>
              <div
                class="w-10 h-10 rounded-full flex items-center justify-center text-white text-sm font-medium"
                :style="{ backgroundColor: getAvatarColor(contact.userId) }"
              >
                {{ getAvatarText(getRemarkName(contact.userId, contact.username)) }}
              </div>
            </div>
            <div class="flex-1" @click="handleContactClick(contact)">
              <h3 class="font-medium text-sm" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">{{ getRemarkName(contact.userId, contact.username) }}</h3>
            </div>
            <button
              type="button"
              class="text-xs px-2 py-1"
              :class="isDarkTheme ? 'text-gray-400 hover:text-[#0891B2]' : 'text-gray-500 hover:text-[#0891B2]'"
              @click.stop="openRemarkDialog(contact)"
            >
              备注
            </button>
          </div>
        </div>
      </div>

      <!-- 底部用户信息 -->
      <div class="px-5 py-3 border-t" :class="isDarkTheme ? 'border-gray-800' : 'border-gray-50'">
        <div class="flex items-center gap-3">
          <div
            class="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-medium"
            :style="{ backgroundColor: getAvatarColor(user?.userId || '') }"
          >
            {{ getAvatarText(user?.username || '') }}
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-sm truncate" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">{{ user?.username }}</p>
            <p class="text-xs text-[#22C55E]">在线</p>
          </div>
        </div>
      </div>
    </main>

    <!-- 右侧 - 聊天窗口 -->
    <div
      class="relative flex-1 flex flex-col"
      :class="isDarkTheme ? 'bg-[#0F172A]' : 'bg-white'"
      @dragenter="handleDragEnter"
      @dragover="handleDragOver"
      @dragleave="handleDragLeave"
      @drop="handleDropUpload"
    >
      <!-- 未选择房间时的欢迎界面 -->
      <div v-if="!selectedRoomId" class="flex-1 flex items-center justify-center">
        <div class="text-center">
          <div class="w-16 h-16 bg-[#0891B2] flex items-center justify-center mx-auto mb-6">
            <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
          </div>
          <h2 class="text-lg font-light mb-2" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">WebSocket Chat</h2>
          <p class="text-sm" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">选择一个对话开始聊天</p>
        </div>
      </div>

      <!-- 聊天界面 -->
      <template v-else>
        <!-- 顶部栏 - 极简 -->
        <header class="px-6 py-4 border-b flex items-center justify-between" :class="isDarkTheme ? 'border-gray-800' : 'border-gray-50'">
          <div class="flex items-center gap-3">
            <div
              class="w-9 h-9 rounded-full flex items-center justify-center text-white text-sm font-medium"
              :style="{ backgroundColor: getAvatarColor(selectedRoomId) }"
            >
              {{ getAvatarText(currentRoom?.name || '') }}
            </div>
            <div>
              <h1 class="font-medium text-sm" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">{{ currentRoom ? getDisplayRoomName(currentRoom) : '' }}</h1>
              <p class="text-xs" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">{{ onlineStatusText }}</p>
            </div>
          </div>

          <div class="relative">
            <button
              @click="toggleChatMenu"
              class="w-8 h-8 flex items-center justify-center transition-colors"
              :class="isDarkTheme ? 'text-gray-500 hover:text-gray-300' : 'text-gray-400 hover:text-gray-600'"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="1"/>
                <circle cx="19" cy="12" r="1"/>
                <circle cx="5" cy="12" r="1"/>
              </svg>
            </button>

            <!-- 聊天菜单 - 极简 -->
            <div
              v-if="showChatMenu"
              class="absolute right-0 top-full mt-2 w-36 border py-1 z-50"
              :class="isDarkTheme ? 'bg-[#1E293B] border-gray-700' : 'bg-white border-gray-100'"
              v-click-outside="closeChatMenu"
            >
              <button
                @click="handleShowMembers"
                class="w-full px-4 py-2 text-left text-xs"
                :class="isDarkTheme ? 'text-gray-300 hover:bg-gray-800' : 'text-gray-600 hover:bg-gray-50'"
              >
                成员
              </button>
              <button
                v-if="isGroupChat"
                @click="handleShowInvite"
                class="w-full px-4 py-2 text-left text-xs"
                :class="isDarkTheme ? 'text-gray-300 hover:bg-gray-800' : 'text-gray-600 hover:bg-gray-50'"
              >
                邀请
              </button>
              <div v-if="isGroupChat && isRoomOwner" class="border-t my-1" :class="isDarkTheme ? 'border-gray-700' : 'border-gray-100'"></div>
              <button
                v-if="isGroupChat && isRoomOwner"
                @click="handleDissolveRoom"
                class="w-full px-4 py-2 text-left text-xs text-red-500"
                :class="isDarkTheme ? 'hover:bg-red-900/20' : 'hover:bg-red-50'"
              >
                解散
              </button>
            </div>
          </div>
        </header>

        <!-- 消息区域 - 大量留白 -->
        <div ref="messagesContainer" class="flex-1 overflow-y-auto px-6 py-6">
          <!-- 空状态 -->
          <div v-if="roomMessages.length === 0" class="flex flex-col items-center justify-center h-full" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-300'">
            <p class="text-xs">还没有消息</p>
          </div>
          
          <!-- 消息列表 -->
          <template v-else>
            <template v-for="(message, index) in roomMessages" :key="message.id">
              <!-- 日期分隔 -->
              <div v-if="shouldShowDate(index)" class="flex justify-center my-6">
                <span class="text-xs" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-300'">
                  {{ formatDate(message.timestamp) }}
                </span>
              </div>

              <!-- 时间戳 -->
              <div v-else-if="shouldShowTime(index)" class="flex justify-center my-3">
                <span class="text-xs" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-300'">
                  {{ formatTime(message.timestamp) }}
                </span>
              </div>

              <!-- 消息气泡 - 极简 -->
              <div
                :class="[
                  'flex gap-3 mb-4',
                  String(message.senderId) === user?.userId ? 'flex-row-reverse' : 'flex-row'
                ]"
              >
                <!-- 头像 -->
                <div
                  class="w-8 h-8 rounded-full flex-shrink-0 flex items-center justify-center text-white text-xs font-medium"
                  :style="{ backgroundColor: getAvatarColor(String(message.senderId)) }"
                >
                  {{ getAvatarText(getMessageSenderName(String(message.senderId), message.senderName)) }}
                </div>

                <!-- 消息内容 -->
                <div :class="['flex flex-col max-w-[65%]', String(message.senderId) === user?.userId ? 'items-end' : 'items-start']">
                  <!-- 用户名 -->
                  <div v-if="String(message.senderId) !== user?.userId" class="text-xs mb-1" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">
                    {{ getMessageSenderName(String(message.senderId), message.senderName) }}
                  </div>

                  <!-- 消息气泡 - 无圆角，简洁 -->
                  <div
                    :class="[
                      isImageMessage(message)
                        ? 'overflow-hidden rounded-[20px] px-0.5 py-0.5'
                        : 'rounded-2xl px-4 py-2 text-sm',
                      String(message.senderId) === user?.userId
                        ? 'bg-[#0891B2]'
                        : (isDarkTheme ? 'bg-gray-800' : 'bg-gray-100')
                    ]"
                  >
                    <!-- 文件消息 -->
                    <div v-if="message.type === 'file' && message.fileId" class="min-w-[200px]">
                      <FileMessage
                        :file-name="message.fileName || '未命名文件'"
                        :file-size="message.fileSize || 0"
                        :file-url="message.fileUrl || ''"
                        :file-type="message.fileType || ''"
                        :is-dark="isDarkTheme"
                        @preview="openSentImagePreview"
                      />
                    </div>

                    <!-- 普通文本消息 -->
                    <div
                      v-else
                      :class="messageContentClass(String(message.senderId) === user?.userId)"
                      :style="messageContentStyle()"
                      v-html="renderMessageContent(message.content)"
                    ></div>
                  </div>

                  <!-- 时间 -->
                  <div class="text-xs mt-1" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-300'">
                    {{ formatTime(message.timestamp) }}
                  </div>
                </div>
              </div>
            </template>
          </template>
        </div>

        <!-- 错误提示 -->
        <div v-if="uploadError" class="px-6 py-2" :class="isDarkTheme ? 'bg-red-900/20' : 'bg-red-50'">
          <p class="text-xs text-red-500">{{ uploadError }}</p>
        </div>

        <!-- 输入区域 - 极简 -->
        <div class="px-6 py-4 border-t" :class="isDarkTheme ? 'border-gray-800' : 'border-gray-50'">
          <div v-if="showEmojiPicker" class="mb-3">
            <div class="flex flex-wrap gap-1">
              <button
                v-for="emoji in emojis"
                :key="emoji"
                @click="insertEmoji(emoji)"
                class="w-7 h-7 text-base"
                :class="isDarkTheme ? 'hover:bg-gray-800' : 'hover:bg-gray-100'"
              >
                {{ emoji }}
              </button>
            </div>
          </div>

          <div v-if="pendingAttachments.length > 0" class="mb-3 flex gap-3 overflow-x-auto pb-1">
            <div
              v-for="attachment in pendingAttachments"
              :key="attachment.id"
              class="group relative shrink-0 overflow-hidden rounded-2xl border"
              :class="isDarkTheme ? 'border-gray-700 bg-gray-800/80' : 'border-gray-200 bg-gray-50'"
            >
              <button
                v-if="attachment.isImage"
                type="button"
                class="block h-24 w-24 cursor-zoom-in overflow-hidden transition-transform duration-200 hover:scale-[1.02]"
                @click="openAttachmentPreview(attachment)"
              >
                <img :src="attachment.previewUrl" :alt="attachment.file.name" class="h-full w-full object-cover" />
              </button>
              <div v-else class="flex h-24 w-56 items-center gap-3 px-4">
                <div class="text-3xl">{{ getFileIcon(attachment.file.name) }}</div>
                <div class="min-w-0 flex-1">
                  <p class="truncate text-sm font-medium" :class="isDarkTheme ? 'text-gray-100' : 'text-gray-800'">{{ attachment.file.name }}</p>
                  <p class="mt-1 text-xs" :class="isDarkTheme ? 'text-gray-400' : 'text-gray-500'">{{ formatFileSize(attachment.file.size) }}</p>
                </div>
              </div>

              <button
                type="button"
                class="absolute right-2 top-2 flex h-6 w-6 items-center justify-center rounded-full bg-black/60 text-xs text-white opacity-0 transition group-hover:opacity-100"
                @click="removePendingAttachment(attachment.id)"
              >
                ×
              </button>

              <div v-if="attachment.isImage" class="absolute inset-x-0 bottom-0 bg-black/50 px-2 py-1 text-white">
                <p class="truncate text-xs">{{ attachment.file.name }}</p>
                <p class="text-[11px] opacity-80">{{ formatFileSize(attachment.file.size) }} · 点击预览</p>
              </div>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <button
              @click="showEmojiPicker = !showEmojiPicker"
              class="w-8 h-8 flex items-center justify-center transition-colors"
              :class="isDarkTheme ? 'text-gray-500 hover:text-gray-300' : 'text-gray-400 hover:text-gray-600'"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <path d="M8 14s1.5 2 4 2 4-2 4-2"/>
                <line x1="9" y1="9" x2="9.01" y2="9"/>
                <line x1="15" y1="9" x2="15.01" y2="9"/>
              </svg>
            </button>

            <FileUploadButton
              ref="fileUploadButtonRef"
              :is-dark="isDarkTheme"
              :disabled="isSendingFiles"
              @files-selected="queuePendingFiles"
              @upload-error="showUploadError"
            />

            <div class="flex-1 relative">
              <input
                v-model="newMessage"
                @paste="handlePasteUpload"
                @keyup.enter="handleSendMessage"
                type="text"
                placeholder="输入消息"
                class="w-full px-0 py-2 bg-transparent border-0 border-b text-sm focus:outline-none focus:border-[#0891B2] transition-colors"
                :class="isDarkTheme ? 'border-gray-700 text-gray-200 placeholder-gray-500' : 'border-gray-200 text-gray-700 placeholder-gray-400'"
              />
            </div>

            <button
              @click="handleSendMessage"
              :disabled="!canSend"
              class="w-8 h-8 bg-[#0891B2] hover:bg-[#0E7490] text-white flex items-center justify-center transition-colors"
              :class="isDarkTheme ? 'disabled:bg-gray-800' : 'disabled:bg-gray-200'"
            >
              <svg v-if="!isSendingFiles" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="22" y1="2" x2="11" y2="13"/>
                <polygon points="22 2 15 22 11 13 2 9 22 2"/>
              </svg>
              <svg v-else class="animate-spin" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
              </svg>
            </button>
          </div>
        </div>

        <div
          v-if="isSendingFiles"
          class="absolute inset-0 z-40 flex items-center justify-center bg-black/25 backdrop-blur-sm"
        >
          <div class="w-96 rounded-2xl border px-6 py-5" :class="isDarkTheme ? 'border-gray-700 bg-slate-900 text-slate-100' : 'border-gray-200 bg-white text-slate-800'">
            <div class="flex items-center justify-between gap-3 mb-4">
              <div>
                <p class="text-sm font-medium">正在上传文件</p>
                <p class="mt-1 truncate text-xs opacity-70">{{ uploadingFileName || '准备中...' }}</p>
              </div>
              <span class="text-sm font-medium tabular-nums">{{ uploadProgress }}%</span>
            </div>
            <div class="mt-1 h-2.5 overflow-hidden rounded-full" :class="isDarkTheme ? 'bg-slate-800' : 'bg-slate-200'">
              <div class="h-full rounded-full bg-[#0891B2] transition-all duration-300 ease-out" :style="{ width: `${uploadProgress}%` }"></div>
            </div>
            <div v-if="uploadError" class="mt-4 text-xs text-red-500">
              {{ uploadError }}
            </div>
          </div>
        </div>

        <div
          v-if="isDraggingFile"
          class="absolute inset-0 z-40 flex items-center justify-center bg-[#0891B2]/15 backdrop-blur-sm transition-opacity duration-300"
        >
          <div
            class="rounded-2xl border-2 border-dashed px-10 py-12 text-center"
            :class="isDarkTheme ? 'border-[#38BDF8] bg-slate-900/80 text-slate-100' : 'border-[#0891B2] bg-white/95 text-slate-700'"
          >
            <div class="text-4xl mb-4">📁</div>
            <p class="text-lg font-medium mb-2">松开发送文件</p>
            <p class="text-sm opacity-70">支持图片、文档、音频、视频等多种文件类型</p>
            <p class="text-xs mt-4 opacity-60">单个文件最大 500MB</p>
          </div>
        </div>
      </template>
    </div>
  </div>
  
  <Teleport to="body">
    <div
      v-if="isProjectNoticeOpen"
      class="fixed inset-0 z-[80] flex items-center justify-center bg-black/50 p-4"
      @click.self="closeProjectNotice"
    >
      <div class="w-full max-w-2xl overflow-hidden rounded-3xl border shadow-2xl" :class="isDarkTheme ? 'border-slate-700 bg-slate-900 text-slate-100' : 'border-slate-200 bg-white text-slate-800'">
        <div class="border-b px-6 py-5" :class="isDarkTheme ? 'border-slate-800' : 'border-slate-100'">
          <p class="text-xs font-semibold uppercase tracking-[0.25em] text-[#0891B2]">项目公告</p>
          <h2 class="mt-2 text-2xl font-semibold">{{ projectNotice.title }}</h2>
          <p class="mt-3 text-sm leading-6" :class="isDarkTheme ? 'text-slate-300' : 'text-slate-600'">{{ projectNotice.summary }}</p>
        </div>
        <div class="px-6 py-5">
          <div class="space-y-3">
            <div
              v-for="(item, index) in projectNotice.highlights"
              :key="item"
              class="flex items-start gap-3 rounded-2xl px-4 py-3"
              :class="isDarkTheme ? 'bg-slate-800/70' : 'bg-slate-50'"
            >
              <span class="mt-0.5 flex h-6 w-6 items-center justify-center rounded-full bg-[#0891B2] text-xs font-semibold text-white">{{ index + 1 }}</span>
              <p class="text-sm leading-6">{{ item }}</p>
            </div>
          </div>

          <div class="mt-5 rounded-2xl border px-4 py-4" :class="isDarkTheme ? 'border-slate-700 bg-slate-950/40' : 'border-slate-200 bg-white'">
            <p class="text-sm font-medium">相关链接</p>
            <div class="mt-3 flex flex-col gap-2">
              <a
                v-for="link in projectNotice.links"
                :key="link.href"
                :href="link.href"
                target="_blank"
                rel="noopener noreferrer"
                class="inline-flex items-center gap-2 text-sm text-[#0891B2] hover:underline break-all"
              >
                <span>{{ link.label }}</span>
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M7 17 17 7"/>
                  <path d="M7 7h10v10"/>
                </svg>
              </a>
            </div>
          </div>
        </div>
        <div class="flex items-center justify-end gap-3 border-t px-6 py-4" :class="isDarkTheme ? 'border-slate-800 bg-slate-950/40' : 'border-slate-100 bg-slate-50'">
          <button
            type="button"
            class="rounded-xl px-4 py-2 text-sm transition-colors"
            :class="isDarkTheme ? 'text-slate-300 hover:bg-slate-800' : 'text-slate-600 hover:bg-white'"
            @click="closeProjectNotice"
          >
            关闭
          </button>
          <button
            type="button"
            class="rounded-xl bg-[#0891B2] px-4 py-2 text-sm text-white transition-colors hover:bg-[#0e7490]"
            @click="dismissProjectNotice"
          >
            今日不再提示
          </button>
        </div>
      </div>
    </div>
  </Teleport>

  <CreateGroupDialog
    :is-open="isCreateDialogOpen"
    :online-users="onlineUsers"
    :current-user-id="user?.userId"
    @close="handleCreateDialogClose"
    @create="handleCreateGroupSubmit"
  />
  
  <!-- 确认对话框 - 极简 -->
  <ConfirmDialog
    :is-open="confirmDialog.isOpen"
    :title="confirmDialog.title"
    :message="confirmDialog.message"
    @confirm="handleConfirm"
    @cancel="closeConfirmDialog"
  />

  <SetRemarkDialog
    :is-open="isRemarkDialogOpen"
    :is-dark="isDarkTheme"
    :username="remarkTarget?.username || ''"
    :initial-remark="remarkTarget ? (userRemarks[remarkTarget.userId] || '') : ''"
    @close="closeRemarkDialog"
    @save="handleSaveRemark"
  />
  
  <!-- 成员列表对话框 - 极简 -->
  <div v-if="showMemberList" class="fixed inset-0 bg-black/30 flex items-center justify-center z-50 p-4" @click.self="showMemberList = false">
    <div class="w-full max-w-xs" :class="isDarkTheme ? 'bg-[#1E293B]' : 'bg-white'">
      <div class="px-5 py-4 border-b flex justify-between items-center" :class="isDarkTheme ? 'border-gray-800' : 'border-gray-100'">
        <h3 class="text-sm font-medium" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">成员</h3>
        <button @click="showMemberList = false" :class="isDarkTheme ? 'text-gray-500 hover:text-gray-300' : 'text-gray-400 hover:text-gray-600'">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
      <div class="max-h-72 overflow-y-auto">
        <div
          v-for="member in roomMembers"
          :key="member.userId"
          class="flex items-center justify-between px-5 py-3 border-b"
          :class="isDarkTheme ? 'border-gray-800' : 'border-gray-50'"
        >
          <div class="flex items-center gap-3">
            <div
              class="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-medium"
              :style="{ backgroundColor: getAvatarColor(member.userId) }"
            >
              {{ getAvatarText(getRemarkName(member.userId, member.username)) }}
            </div>
            <div>
              <p class="text-sm" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">{{ getRemarkName(member.userId, member.username) }}</p>
              <p v-if="member.userId === currentRoom?.ownerId" class="text-xs text-[#0891B2]">群主</p>
            </div>
          </div>
          <button
            v-if="isRoomOwner && member.userId !== user?.userId"
            @click="handleKickMember(member.userId, getRemarkName(member.userId, member.username))"
            class="text-xs text-red-400 hover:text-red-600"
          >
            移除
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- 邀请成员对话框 - 极简 -->
  <div v-if="showInviteDialog" class="fixed inset-0 bg-black/30 flex items-center justify-center z-50 p-4" @click.self="showInviteDialog = false">
    <div class="w-full max-w-xs" :class="isDarkTheme ? 'bg-[#1E293B]' : 'bg-white'">
      <div class="px-5 py-4 border-b flex justify-between items-center" :class="isDarkTheme ? 'border-gray-800' : 'border-gray-100'">
        <h3 class="text-sm font-medium" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">邀请</h3>
        <button @click="showInviteDialog = false" :class="isDarkTheme ? 'text-gray-500 hover:text-gray-300' : 'text-gray-400 hover:text-gray-600'">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
      <div class="max-h-72 overflow-y-auto">
        <div v-if="inviteableUsers.length === 0" class="px-5 py-4 text-center text-xs" :class="isDarkTheme ? 'text-gray-500' : 'text-gray-400'">
          没有可邀请的用户
        </div>
        <div
          v-for="contact in inviteableUsers"
          :key="contact.userId"
          @click="handleInviteMember(contact.userId)"
          class="flex items-center gap-3 px-5 py-3 cursor-pointer border-b"
          :class="isDarkTheme ? 'border-gray-800 hover:bg-gray-800' : 'border-gray-50 hover:bg-gray-50'"
        >
          <div
            class="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-medium"
            :style="{ backgroundColor: getAvatarColor(contact.userId) }"
          >
            {{ getAvatarText(getRemarkName(contact.userId, contact.username)) }}
          </div>
          <div class="flex-1">
            <p class="text-sm" :class="isDarkTheme ? 'text-gray-200' : 'text-gray-800'">{{ getRemarkName(contact.userId, contact.username) }}</p>
            <p class="text-xs" :class="contact.isOnline ? 'text-[#22C55E]' : (isDarkTheme ? 'text-gray-500' : 'text-gray-400')">
              {{ contact.isOnline ? '在线' : '离线' }}
            </p>
          </div>
          <svg class="text-[#0891B2]" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
        </div>
      </div>
    </div>
  </div>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-300 ease-out"
      enter-from-class="opacity-0 scale-95"
      enter-to-class="opacity-100 scale-100"
      leave-active-class="transition duration-200 ease-in"
      leave-from-class="opacity-100 scale-100"
      leave-to-class="opacity-0 scale-95"
    >
      <div
        v-if="previewingAttachment || previewingSentImage"
        class="fixed inset-0 z-[70] flex items-center justify-center bg-black/90 p-4"
        @click.self="closeAttachmentPreview"
      >
        <!-- 顶部信息栏 -->
        <div class="absolute inset-x-0 top-0 flex items-center justify-between gap-4 bg-gradient-to-b from-black/60 to-transparent px-6 py-4 text-white">
          <div class="flex items-center gap-3 min-w-0">
            <button
              type="button"
              class="shrink-0 rounded-full p-2 transition-colors hover:bg-white/10"
              @click="closeAttachmentPreview"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
            <p class="min-w-0 truncate text-sm font-medium" :title="previewingAttachment?.file.name || previewingSentImage?.fileName">
              {{ previewingAttachment?.file.name || previewingSentImage?.fileName }}
            </p>
          </div>
          <div class="flex items-center gap-3 shrink-0">
            <span class="text-sm opacity-80">{{ formatFileSize(previewingAttachment?.file.size || previewingSentImage?.fileSize || 0) }}</span>
            <button
              type="button"
              class="shrink-0 rounded-full p-2 transition-colors hover:bg-white/10"
              @click="downloadPreviewedAttachment"
              title="下载"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                <polyline points="7 10 12 15 17 10"/>
                <line x1="12" y1="15" x2="12" y2="3"/>
              </svg>
            </button>
          </div>
        </div>
        <img
          :src="previewingAttachment?.previewUrl || previewingSentImage?.fileUrl"
          :alt="previewingAttachment?.file.name || previewingSentImage?.fileName"
          class="max-h-[80vh] max-w-[90vw] rounded-lg object-contain transition-transform duration-300 ease-out"
        />
        <!-- 底部操作提示 -->
        <div class="absolute inset-x-0 bottom-0 flex items-center justify-center bg-gradient-to-t from-black/60 to-transparent px-6 py-4">
          <p class="text-xs text-white/60">点击空白区域关闭预览</p>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

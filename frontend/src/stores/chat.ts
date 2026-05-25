import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface User {
  userId: string
  username: string
  avatarUrl?: string
  isOnline?: boolean
}

export interface FileInfo {
  fileId: string
  fileName: string
  fileUrl: string
  fileSize: number
  fileType: string
}

export interface Message {
  id: string
  roomId: string
  content: string
  senderId: string
  senderName: string
  senderAvatarUrl?: string
  timestamp: number
  seq: number
  type?: 'text' | 'file' | 'system'
  fileInfo?: FileInfo
  fileId?: string
  fileName?: string
  fileUrl?: string
  fileSize?: number
  fileType?: string
}

export interface Room {
  id: string
  name: string
  type: 'public' | 'private'
  ownerId?: string
  avatarUrl?: string
  createdAt: number
  lastMessage?: Message
}

export interface InviteResult {
  success: boolean
  message: string
  targetUserId?: string
}

export interface RoomMemberLeftEvent {
  roomId: string
  userId: string
}

const MAX_MESSAGES = 500

export const useChatStore = defineStore('chat', () => {
  const isConnected = ref(false)
  const reconnectAttempts = ref(0)
  const messages = ref<Message[]>([])
  const rooms = ref<Room[]>([])
  const onlineUsers = ref<User[]>([])
  const unreadCounts = ref<Record<string, number>>({})
  const currentRoomId = ref<string>('')
  const lastMessage = ref<Message | null>(null)
  const lastInviteResult = ref<InviteResult | null>(null)
  const lastBannedResult = ref<{ message: string } | null>(null)
  const lastRenamedResult = ref<{ userId: string; username: string } | null>(null)
  const lastRoomMemberLeft = ref<RoomMemberLeftEvent | null>(null)
  const lastPrivateRoomCreated = ref<{ id: string; name: string; type: string } | null>(null)
  const roomLastSeq = ref<Record<string, number>>({})
  const readReceipts = ref<Map<string, Set<string>>>(new Map())
  const unreadMentionCount = ref<Record<string, number>>({})
  const latestMention = ref<Record<string, any>>({})
  const roomMembers = ref<User[]>([])

  const roomMessages = computed(() => {
    return messages.value
      .filter(msg => String(msg.roomId) === currentRoomId.value)
      .sort((a, b) => a.seq - b.seq)
  })

  const currentRoom = computed(() => rooms.value.find(r => r.id === currentRoomId.value))

  const getUnreadCount = (roomId: string): number => {
    return unreadCounts.value[roomId] || 0
  }

  const getTotalUnreadCount = computed(() => {
    return Object.values(unreadCounts.value).reduce((sum, count) => sum + count, 0)
  })

  function trimMessages() {
    if (messages.value.length > MAX_MESSAGES * 2) {
      const keepIds = new Set<string>()
      const seenCounts = new Map<string, number>()
      for (let i = messages.value.length - 1; i >= 0; i--) {
        const msg = messages.value[i]
        const rid = String(msg.roomId)
        const count = seenCounts.get(rid) || 0
        if (count < MAX_MESSAGES) {
          keepIds.add(msg.id)
          seenCounts.set(rid, count + 1)
        }
      }
      messages.value = messages.value.filter(m => keepIds.has(m.id))
    }
  }

  function addMessage(msg: Message) {
    messages.value.push(msg)
    lastMessage.value = msg

    const roomIndex = rooms.value.findIndex(r => r.id === String(msg.roomId))
    if (roomIndex !== -1) {
      rooms.value[roomIndex].lastMessage = msg
    }

    if (currentRoomId.value !== String(msg.roomId)) {
      const roomId = String(msg.roomId)
      unreadCounts.value[roomId] = (unreadCounts.value[roomId] || 0) + 1
    }

    trimMessages()
  }

  function setCurrentRoom(roomId: string) {
    currentRoomId.value = roomId
    if (unreadCounts.value[roomId]) {
      delete unreadCounts.value[roomId]
    }
  }

  function clearState() {
    messages.value = []
    rooms.value = []
    onlineUsers.value = []
    unreadCounts.value = {}
    currentRoomId.value = ''
    lastMessage.value = null
    lastInviteResult.value = null
    lastBannedResult.value = null
    lastRenamedResult.value = null
    lastRoomMemberLeft.value = null
    lastPrivateRoomCreated.value = null
    roomLastSeq.value = new Map() as any
    readReceipts.value = new Map()
    unreadMentionCount.value = {}
    latestMention.value = {}
  }

  function incrementUnreadMentionCount(roomId: string) {
    unreadMentionCount.value[roomId] = (unreadMentionCount.value[roomId] || 0) + 1
  }

  function setUnreadMentionCount(roomId: string, count: number) {
    unreadMentionCount.value[roomId] = count
  }

  function clearUnreadMentionCount(roomId: string) {
    delete unreadMentionCount.value[roomId]
    delete latestMention.value[roomId]
  }

  function setLatestMention(roomId: string, mention: any) {
    latestMention.value[roomId] = mention
  }

  function setRoomMembers(members: User[]) {
    roomMembers.value = members
  }

  /** 用户改名后全局同步：消息列表、在线用户、群成员 */
  function handleUserRenamed(userId: string, newUsername: string) {
    // 1. 更新所有消息中的发送者名称
    for (const roomMsgs of Object.values(messages.value)) {
      for (const msg of roomMsgs) {
        if (msg.senderId === userId) {
          msg.senderName = newUsername
        }
      }
    }

    // 2. 更新在线用户列表
    const onlineUser = onlineUsers.value.find(u => u.userId === userId)
    if (onlineUser) {
      onlineUser.username = newUsername
    }

    // 3. 更新群成员列表
    const member = roomMembers.value.find(u => u.userId === userId)
    if (member) {
      member.username = newUsername
    }

    // 4. 更新私聊房间显示名（含对方名字的房间）
    for (const room of rooms.value) {
      if (room.type === 'private' && room.name && room.name.includes('(')) {
        // 私聊房间名格式可能是 "对方名字" 或含括号的格式
        // 不做处理，由 room:list:response 刷新时更新
      }
    }
  }

  return {
    isConnected,
    reconnectAttempts,
    messages,
    rooms,
    onlineUsers,
    unreadCounts,
    currentRoomId,
    lastMessage,
    lastInviteResult,
    lastBannedResult,
    lastRenamedResult,
    lastRoomMemberLeft,
    lastPrivateRoomCreated,
    roomLastSeq,
    readReceipts,
    unreadMentionCount,
    latestMention,
    roomMembers,
    roomMessages,
    currentRoom,
    getUnreadCount,
    getTotalUnreadCount,
    trimMessages,
    addMessage,
    setCurrentRoom,
    clearState,
    incrementUnreadMentionCount,
    setUnreadMentionCount,
    clearUnreadMentionCount,
    setLatestMention,
    setRoomMembers,
    handleUserRenamed
  }
})

import { storeToRefs } from 'pinia'
import { useChatStore } from '@/stores/chat'
import { useAiStore } from '@/stores/ai'
import type { Message, User, FileInfo, Room } from '@/stores/chat'
import type { AiConversation, AiMessage } from '@/stores/ai'

interface WebSocketEvent {
  type: string
  data: any
}

function requestNotificationPermission() {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission()
  }
}

function showMessageNotification(senderName: string, body: string) {
  if (document.hidden && 'Notification' in window && Notification.permission === 'granted') {
    new Notification(senderName, { body })
  }
}

// 模块级单例 — 所有组件共享同一个 socket
let sharedSocket: WebSocket | null = null
let currentUserId = ''
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let currentUser: User | null = null
let currentToken = ''
const MAX_RECONNECT_ATTEMPTS = 10
const BASE_RECONNECT_DELAY = 1000

export function useWebSocket() {
  const store = useChatStore()
  const {
    isConnected,
    reconnectAttempts,
    messages,
    rooms,
    onlineUsers,
    unreadCounts,
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
    roomMembers
  } = storeToRefs(store)

  const sendRoomAvatarUpdated = (roomId: string, avatarUrl: string) => {
    if (!sharedSocket || sharedSocket.readyState !== WebSocket.OPEN) return
    sharedSocket.send(JSON.stringify({
      type: 'room:avatar:updated',
      data: { roomId, avatarUrl }
    }))
  }

  const connect = (user: User, token?: string) => {
    if (sharedSocket && sharedSocket.readyState === WebSocket.OPEN && currentUserId === user.userId) {
      isConnected.value = true
      return
    }
    // 关闭旧连接
    if (sharedSocket && sharedSocket.readyState !== WebSocket.CLOSED) {
      sharedSocket.close()
    }
    currentUserId = user.userId
    currentUser = user
    if (token) currentToken = token
    store.reconnectAttempts = 0
    doConnect(user)
  }

  const doConnect = (user: User) => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }

    const wsUrl = currentToken ? `/ws/chat?token=${encodeURIComponent(currentToken)}` : '/ws/chat'
    sharedSocket = new WebSocket(wsUrl)

    sharedSocket.onopen = () => {
      console.log('WebSocket connected')
      isConnected.value = true
      reconnectAttempts.value = 0

      requestNotificationPermission()

      const joinEvent: WebSocketEvent = {
        type: 'user:join',
        data: {
          userId: user.userId,
          username: user.username
        }
      }
      sharedSocket?.send(JSON.stringify(joinEvent))

      const listEvent: WebSocketEvent = {
        type: 'room:list',
        data: {
          userId: user.userId
        }
      }
      sharedSocket?.send(JSON.stringify(listEvent))

      const userListEvent: WebSocketEvent = {
        type: 'user:list',
        data: {}
      }
      sharedSocket?.send(JSON.stringify(userListEvent))

      setTimeout(() => {
        syncRooms()
      }, 500)
    }

    sharedSocket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as WebSocketEvent
        handleEvent(data)
      } catch (error) {
        console.error('Error parsing WebSocket message:', error)
      }
    }

    sharedSocket.onclose = () => {
      console.log('WebSocket disconnected')
      isConnected.value = false

      if (currentUser && reconnectAttempts.value < MAX_RECONNECT_ATTEMPTS) {
        const delay = Math.min(BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts.value), 30000)
        reconnectAttempts.value++
        console.log(`Reconnecting in ${delay}ms (attempt ${reconnectAttempts.value}/${MAX_RECONNECT_ATTEMPTS})...`)
        reconnectTimer = setTimeout(() => {
          doConnect(currentUser!)
        }, delay)
      }
    }

    sharedSocket.onerror = (error) => {
      console.error('WebSocket error:', error)
    }
  }

  const handleEvent = (event: WebSocketEvent) => {
    switch (event.type) {
      case 'user:joined': {
        const newUser = event.data as User
        const existingIndex = onlineUsers.value.findIndex(u => u.userId === newUser.userId)
        if (existingIndex === -1) {
          onlineUsers.value.push(newUser)
        } else {
          onlineUsers.value[existingIndex] = {
            ...onlineUsers.value[existingIndex],
            ...newUser,
            isOnline: true
          }
        }
        break
      }

      case 'user:left': {
        const leftUserId = event.data.userId as string
        const leftIndex = onlineUsers.value.findIndex(u => u.userId === leftUserId)
        if (leftIndex !== -1) {
          onlineUsers.value[leftIndex] = {
            ...onlineUsers.value[leftIndex],
            ...(event.data as User),
            isOnline: false
          }
        }
        break
      }

      case 'message:new': {
        const receivedMsg = event.data as Message
        store.addMessage(receivedMsg)
        if (String(receivedMsg.senderId) === currentUserId) {
          readReceipts.value.delete(String(receivedMsg.roomId))
        }
        showMessageNotification(receivedMsg.senderName, receivedMsg.content.slice(0, 50))
        break
      }

      case 'message:new:file': {
        const fileMsgData = event.data
        const fileMessage: Message = {
          id: fileMsgData.id,
          roomId: String(fileMsgData.roomId),
          content: fileMsgData.content,
          senderId: String(fileMsgData.senderId),
          senderName: fileMsgData.senderName,
          senderAvatarUrl: fileMsgData.senderAvatarUrl,
          timestamp: fileMsgData.timestamp,
          seq: fileMsgData.seq,
          type: 'file',
          fileId: fileMsgData.fileId,
          fileName: fileMsgData.fileName,
          fileUrl: fileMsgData.fileUrl,
          fileSize: fileMsgData.fileSize,
          fileType: fileMsgData.fileType
        }
        store.addMessage(fileMessage)
        if (String(fileMsgData.senderId) === currentUserId) {
          readReceipts.value.delete(String(fileMsgData.roomId))
        }
        showMessageNotification(fileMsgData.senderName, '[文件]')
        break
      }

      case 'message:history:response': {
        const historyData = event.data as { roomId: string; messages: Message[] }
        const existingIds = new Set(messages.value.map(m => m.id))
        historyData.messages.forEach(msg => {
          if (!existingIds.has(msg.id)) {
            messages.value.push(msg)
          }
          // 缓存头像（历史消息可能包含离线用户的头像URL）
          if (msg.senderAvatarUrl && msg.senderId) {
            store.cacheUserAvatar(String(msg.senderId), msg.senderAvatarUrl)
          }
        })
        store.trimMessages()
        break
      }

      case 'room:created': {
        const newRoom = event.data as Room
        rooms.value.push(newRoom)
        break
      }

      case 'room:invite': {
        const invitedRoom = event.data as Room
        const existingInvitedRoom = rooms.value.find(r => r.id === invitedRoom.id)
        if (!existingInvitedRoom) {
          rooms.value.push(invitedRoom)
        }
        break
      }

      case 'room:invite:success':
        lastInviteResult.value = {
          success: true,
          message: '邀请成功',
          targetUserId: String(event.data.targetUserId)
        }
        break

      case 'room:invite:error':
        lastInviteResult.value = {
          success: false,
          message: event.data.message || '邀请失败'
        }
        break

      case 'user:banned':
        lastBannedResult.value = {
          message: event.data?.reason ? `账号已被封禁：${event.data.reason}` : '账号已被封禁'
        }
        disconnect()
        break

      case 'user:renamed': {
        const isSelf = event.data.self === true
        // 所有用户：更新消息列表、在线用户、群成员中的用户名
        store.handleUserRenamed(String(event.data.userId), event.data.username)
        // 仅被改名用户：更新本地 localStorage 和显示 toast
        if (isSelf) {
          lastRenamedResult.value = {
            userId: String(event.data.userId),
            username: event.data.username
          }
        }
        break
      }

      case 'kicked':
        localStorage.removeItem('user')
        currentUser = null
        currentToken = ''
        sharedSocket?.close()
        sharedSocket = null
        isConnected.value = false
        window.location.href = '/login'
        break

      case 'room:member:left':
        lastRoomMemberLeft.value = {
          roomId: String(event.data.roomId),
          userId: String(event.data.userId)
        }
        break

      case 'room:list:response':
        rooms.value = event.data.rooms || []
        break

      case 'room:private:created': {
        const privateRoom = event.data as Room
        lastPrivateRoomCreated.value = { id: String(privateRoom.id), name: privateRoom.name, type: privateRoom.type }
        const existingRoom = rooms.value.find(r => r.id === privateRoom.id)
        if (!existingRoom) {
          rooms.value.push(privateRoom)
        }
        break
      }

      case 'room:sync:response': {
        const syncData = event.data as { messages: Message[] }
        if (syncData.messages) {
          const existingIds = new Set(messages.value.map(m => m.id))
          syncData.messages.forEach(msg => {
            if (!existingIds.has(msg.id)) {
              messages.value.push(msg)
            }
            if (msg.senderAvatarUrl && msg.senderId) {
              store.cacheUserAvatar(String(msg.senderId), msg.senderAvatarUrl)
            }
            const roomId = String(msg.roomId)
            if (!roomLastSeq.value[roomId] || msg.seq > roomLastSeq.value[roomId]) {
              roomLastSeq.value[roomId] = msg.seq
            }
          })
          store.trimMessages()
        }
        break
      }

      case 'user:list:response':
        onlineUsers.value = event.data.users || []
        break

      case 'room:members:response':
        store.setRoomMembers(event.data.members || [])
        break

      case 'room:avatar:updated': {
        const { roomId, avatarUrl } = event.data
        const roomIndex = rooms.value.findIndex(r => r.id === String(roomId))
        if (roomIndex !== -1) {
          rooms.value[roomIndex] = { ...rooms.value[roomIndex], avatarUrl: avatarUrl || undefined }
        }
        break
      }

      case 'user:avatar:updated': {
        const { userId, avatarUrl } = event.data
        const userIndex = onlineUsers.value.findIndex(u => u.userId === String(userId))
        if (userIndex !== -1) {
          onlineUsers.value[userIndex] = { ...onlineUsers.value[userIndex], avatarUrl }
        }
        break
      }

      case 'message:read': {
        const readRoomId = String(event.data.roomId)
        const readUserId = String(event.data.userId)
        if (readUserId === currentUserId) break
        const existingSet = readReceipts.value.get(readRoomId)
        if (existingSet) {
          existingSet.add(readUserId)
        } else {
          readReceipts.value.set(readRoomId, new Set([readUserId]))
        }
        break
      }

      // AI相关事件
      case 'ai:chat:stream': {
        const aiStore = useAiStore()
        const { token, done } = event.data
        if (done) {
          aiStore.endStream()
        } else {
          if (!aiStore.isStreaming) {
            aiStore.startStream()
          }
          aiStore.appendStreamToken(token)
        }
        break
      }

      case 'ai:chat:complete': {
        const aiStore = useAiStore()
        const message = event.data as AiMessage
        aiStore.addMessageFromStream(message)
        aiStore.clearError()
        break
      }

      case 'ai:chat:error': {
        const aiStore = useAiStore()
        aiStore.endStream()
        aiStore.setError(event.data.message || 'AI调用失败')
        console.error('AI chat error:', event.data.message)
        break
      }

      case 'ai:chat:tool_call': {
        const aiStore = useAiStore()
        const { callId, toolName, args } = event.data
        aiStore.addToolCall(callId, toolName, args)
        break
      }

      case 'ai:chat:tool_result': {
        const aiStore = useAiStore()
        const { callId, result } = event.data
        aiStore.updateToolResult(callId, result)
        break
      }

      case 'ai:conversation:created': {
        const aiStore = useAiStore()
        const conversation = event.data.conversation as AiConversation
        aiStore.addConversation(conversation)
        aiStore.setCurrentConversation(conversation)
        break
      }

      case 'ai:conversation:list': {
        const aiStore = useAiStore()
        const conversations = event.data.conversations as AiConversation[]
        aiStore.setConversations(conversations)
        break
      }

      case 'ai:history': {
        const aiStore = useAiStore()
        const messages = event.data.messages as AiMessage[]
        aiStore.setMessages(messages)
        break
      }

      case 'mention:received': {
        const mentionData = event.data
        store.incrementUnreadMentionCount(String(mentionData.roomId))
        store.setLatestMention(String(mentionData.roomId), mentionData)
        break
      }

      case 'mention:unread:list:response': {
        const unreadData = event.data
        store.setUnreadMentionCount(String(unreadData.roomId), unreadData.count)
        if (unreadData.mentions && unreadData.mentions.length > 0) {
          store.setLatestMention(String(unreadData.roomId), unreadData.mentions[0])
        }
        break
      }
    }
  }

  const sendMessage = (roomId: string, content: string, _senderId: string) => {
    const messageEvent: WebSocketEvent = {
      type: 'message:send',
      data: { roomId, content }
    }
    sharedSocket?.send(JSON.stringify(messageEvent))
  }

  const sendFileMessage = (roomId: string, _senderId: string, fileInfo: FileInfo) => {
    const fileMessageEvent: WebSocketEvent = {
      type: 'message:send:file',
      data: {
        roomId,
        fileId: fileInfo.fileId,
        fileName: fileInfo.fileName,
        fileUrl: fileInfo.fileUrl,
        fileSize: fileInfo.fileSize,
        fileType: fileInfo.fileType
      }
    }
    sharedSocket?.send(JSON.stringify(fileMessageEvent))
  }

  const createRoom = (name: string, participants: string[]) => {
    const createEvent: WebSocketEvent = {
      type: 'room:create',
      data: { name, participants }
    }
    sharedSocket?.send(JSON.stringify(createEvent))
  }

  const joinRoom = (roomId: string) => {
    const joinEvent: WebSocketEvent = {
      type: 'room:join',
      data: { roomId }
    }
    sharedSocket?.send(JSON.stringify(joinEvent))
  }

  const sendReadReceipt = (roomId: string) => {
    if (!sharedSocket || sharedSocket.readyState !== WebSocket.OPEN) return
    const readEvent: WebSocketEvent = {
      type: 'message:read',
      data: { roomId, userId: currentUserId }
    }
    sharedSocket.send(JSON.stringify(readEvent))
  }

  const startPrivateChat = (targetUserId: string) => {
    const privateEvent: WebSocketEvent = {
      type: 'room:private:start',
      data: { targetUserId }
    }
    sharedSocket?.send(JSON.stringify(privateEvent))
  }

  const syncRooms = () => {
    if (!sharedSocket || sharedSocket.readyState !== WebSocket.OPEN) return

    const syncData = rooms.value.map(room => ({
      roomId: room.id,
      lastSeq: roomLastSeq.value[room.id] || 0
    }))

    const syncEvent: WebSocketEvent = {
      type: 'room:sync',
      data: { rooms: syncData }
    }
    sharedSocket.send(JSON.stringify(syncEvent))
  }

  const loadMessageHistory = (roomId: string) => {
    if (!sharedSocket || sharedSocket.readyState !== WebSocket.OPEN) {
      console.log('WebSocket not ready, retrying in 500ms...')
      setTimeout(() => loadMessageHistory(roomId), 500)
      return
    }
    const historyEvent: WebSocketEvent = {
      type: 'message:history',
      data: { roomId }
    }
    sharedSocket.send(JSON.stringify(historyEvent))
  }

  const loadRoomMembers = (roomId: string) => {
    if (!sharedSocket || sharedSocket.readyState !== WebSocket.OPEN) {
      console.log('WebSocket not ready for loadRoomMembers, retrying in 500ms...')
      setTimeout(() => loadRoomMembers(roomId), 500)
      return
    }
    const membersEvent: WebSocketEvent = {
      type: 'room:members',
      data: { roomId }
    }
    sharedSocket.send(JSON.stringify(membersEvent))
  }

  // AI相关函数
  const sendAiChat = (assistantId: string, content: string, conversationId?: string) => {
    const aiChatEvent: WebSocketEvent = {
      type: 'ai:chat',
      data: { assistantId, content, conversationId }
    }
    sharedSocket?.send(JSON.stringify(aiChatEvent))
  }

  const loadAiConversations = (assistantId: string) => {
    if (!sharedSocket || sharedSocket.readyState !== WebSocket.OPEN) {
      setTimeout(() => loadAiConversations(assistantId), 300)
      return
    }
    const event: WebSocketEvent = {
      type: 'ai:conversation:list',
      data: { assistantId }
    }
    sharedSocket.send(JSON.stringify(event))
  }

  const createAiConversation = (assistantId: string, title?: string) => {
    const event: WebSocketEvent = {
      type: 'ai:conversation:create',
      data: { assistantId, title }
    }
    sharedSocket?.send(JSON.stringify(event))
  }

  const deleteAiConversation = (conversationId: string) => {
    const event: WebSocketEvent = {
      type: 'ai:conversation:delete',
      data: { conversationId }
    }
    sharedSocket?.send(JSON.stringify(event))
  }

  const loadAiHistory = (conversationId: string) => {
    if (!sharedSocket || sharedSocket.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket not ready, cannot load AI history')
      return
    }
    const event: WebSocketEvent = {
      type: 'ai:history',
      data: { conversationId }
    }
    sharedSocket.send(JSON.stringify(event))
  }

  const disconnect = () => {
    currentUser = null
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    reconnectAttempts.value = MAX_RECONNECT_ATTEMPTS
    sharedSocket?.close()
    sharedSocket = null
  }

  const setCurrentRoom = (roomId: string) => {
    store.setCurrentRoom(roomId)
    sendReadReceipt(roomId)
  }

  const sendInviteMember = (roomId: string, targetUserId: string) => {
    if (!sharedSocket || sharedSocket.readyState !== WebSocket.OPEN) return
    sharedSocket.send(JSON.stringify({
      type: 'room:invite:member',
      data: { roomId, targetUserId }
    }))
  }

  const sendMentionMessage = (roomId: string, content: string, mentions: User[], mentionAll: boolean) => {
    if (!sharedSocket || sharedSocket.readyState !== WebSocket.OPEN) return
    
    const mentionEvent = {
      type: 'mention:send',
      data: {
        roomId,
        content,
        mentions: mentions.map(m => ({ userId: m.userId, username: m.username })),
        mentionAll
      }
    }
    sharedSocket.send(JSON.stringify(mentionEvent))
  }

  const markMentionsAsRead = (roomId: string) => {
    if (!sharedSocket || sharedSocket.readyState !== WebSocket.OPEN) return
    
    const readEvent = {
      type: 'mention:read',
      data: { roomId }
    }
    sharedSocket.send(JSON.stringify(readEvent))
    store.clearUnreadMentionCount(roomId)
  }

  const loadUnreadMentions = (roomId: string) => {
    if (!sharedSocket || sharedSocket.readyState !== WebSocket.OPEN) return

    const listEvent = {
      type: 'mention:unread:list',
      data: { roomId }
    }
    sharedSocket.send(JSON.stringify(listEvent))
  }

  const refreshRoomList = () => {
    if (!sharedSocket || sharedSocket.readyState !== WebSocket.OPEN || !currentUser) return
    sharedSocket.send(JSON.stringify({
      type: 'room:list',
      data: { userId: currentUser.userId }
    }))
  }

  return {
    isConnected,
    reconnectAttempts,
    messages,
    rooms,
    onlineUsers,
    unreadCounts,
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
    connect,
    sendMessage,
    sendFileMessage,
    createRoom,
    joinRoom,
    sendReadReceipt,
    startPrivateChat,
    disconnect,
    setCurrentRoom,
    getUnreadCount: store.getUnreadCount,
    getTotalUnreadCount: () => store.getTotalUnreadCount,
    loadMessageHistory,
    syncRooms,
    sendAiChat,
    loadAiConversations,
    createAiConversation,
    deleteAiConversation,
    loadAiHistory,
    sendInviteMember,
    sendMentionMessage,
    markMentionsAsRead,
    loadUnreadMentions,
    loadRoomMembers,
    sendRoomAvatarUpdated,
    refreshRoomList,
    userRenamedCounter: store.userRenamedCounter,
    userAvatarMap: store.userAvatarMap,
    cacheUserAvatar: store.cacheUserAvatar
  }
}
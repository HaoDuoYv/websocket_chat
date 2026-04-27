import { ref, onUnmounted } from 'vue'

interface User {
  userId: string
  username: string
  isOnline?: boolean
}

interface FileInfo {
  fileId: string
  fileName: string
  fileUrl: string
  fileSize: number
  fileType: string
}

interface Message {
  id: string
  roomId: string
  content: string
  senderId: string
  senderName: string
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

interface Room {
  id: string
  name: string
  type: 'public' | 'private'
  ownerId?: string
  createdAt: number
  lastMessage?: Message
}

interface WebSocketEvent {
  type: string
  data: any
}

interface InviteResult {
  success: boolean
  message: string
  targetUserId?: string
}

const normalizeMessageFileUrl = <T extends Message>(message: T): T => message

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

interface BannedResult {
  message: string
}

interface RoomMemberLeftEvent {
  roomId: string
  userId: string
}

export function useWebSocket() {
  const socket = ref<WebSocket | null>(null)
  const isConnected = ref(false)
  const messages = ref<Message[]>([])
  const rooms = ref<Room[]>([])
  const onlineUsers = ref<User[]>([])
  const unreadCounts = ref<Record<string, number>>({})
  const currentRoomId = ref<string>('')
  const lastMessage = ref<Message | null>(null)
  const lastInviteResult = ref<InviteResult | null>(null)
  const lastBannedResult = ref<BannedResult | null>(null)
  const lastRoomMemberLeft = ref<RoomMemberLeftEvent | null>(null)
  const lastPrivateRoomCreated = ref<{ id: string; name: string; type: string } | null>(null)

  // 每个房间的最后读取序列号（用于增量同步）
  const roomLastSeq = ref<Record<string, number>>({})

  // 已读回执：roomId -> 已读用户 ID 集合
  const readReceipts = ref<Map<string, Set<string>>>(new Map())

  let currentUserId = ''

  const connect = (user: User) => {
    currentUserId = user.userId
    socket.value = new WebSocket('/ws/chat')

    socket.value.onopen = () => {
      console.log('WebSocket connected')
      isConnected.value = true

      requestNotificationPermission()

      // 发送用户加入事件
      const joinEvent: WebSocketEvent = {
        type: 'user:join',
        data: {
          userId: user.userId,
          username: user.username
        }
      }
      socket.value?.send(JSON.stringify(joinEvent))

      // 请求房间列表
      const listEvent: WebSocketEvent = {
        type: 'room:list',
        data: {
          userId: user.userId
        }
      }
      socket.value?.send(JSON.stringify(listEvent))

      // 请求在线用户列表
      const userListEvent: WebSocketEvent = {
        type: 'user:list',
        data: {}
      }
      socket.value?.send(JSON.stringify(userListEvent))

      // 执行增量同步
      setTimeout(() => {
        syncRooms()
      }, 500)
    }

    socket.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as WebSocketEvent
        handleEvent(data)
      } catch (error) {
        console.error('Error parsing WebSocket message:', error)
      }
    }

    socket.value.onclose = () => {
      console.log('WebSocket disconnected')
      isConnected.value = false
    }

    socket.value.onerror = (error) => {
      console.error('WebSocket error:', error)
    }
  }

  const handleEvent = (event: WebSocketEvent) => {
    switch (event.type) {
      case 'user:joined':
        console.log('User joined:', event.data)
        const newUser = event.data as User
        const existingUserIndex = onlineUsers.value.findIndex(u => u.userId === newUser.userId)
        if (existingUserIndex === -1) {
          onlineUsers.value.push(newUser)
        } else {
          onlineUsers.value[existingUserIndex] = {
            ...onlineUsers.value[existingUserIndex],
            ...newUser,
            isOnline: true
          }
        }
        break

      case 'user:left':
        console.log('User left:', event.data)
        const leftUserId = event.data.userId as string
        const existingLeftUserIndex = onlineUsers.value.findIndex(u => u.userId === leftUserId)
        if (existingLeftUserIndex !== -1) {
          onlineUsers.value[existingLeftUserIndex] = {
            ...onlineUsers.value[existingLeftUserIndex],
            ...(event.data as User),
            isOnline: false
          }
        }
        break

      case 'message:new':
        const receivedMsg = event.data as Message
        console.log('Received message:', receivedMsg)
        messages.value.push(receivedMsg)
        lastMessage.value = receivedMsg

        // 更新房间的最后消息
        const roomIndex = rooms.value.findIndex(r => r.id === String(receivedMsg.roomId))
        if (roomIndex !== -1) {
          rooms.value[roomIndex].lastMessage = receivedMsg
        }

        // 如果不是当前打开的房间，增加未读数
        if (currentRoomId.value !== String(receivedMsg.roomId)) {
          const roomId = String(receivedMsg.roomId)
  unreadCounts.value[roomId] = (unreadCounts.value[roomId] || 0) + 1
        }

        showMessageNotification(receivedMsg.senderName, receivedMsg.content.slice(0, 50))
        break

      case 'message:new:file':
        const fileMsgData = event.data
        console.log('Received file message:', fileMsgData)

        const fileMessage = normalizeMessageFileUrl({
          id: fileMsgData.id,
          roomId: String(fileMsgData.roomId),
          content: fileMsgData.content,
          senderId: String(fileMsgData.senderId),
          senderName: fileMsgData.senderName,
          timestamp: fileMsgData.timestamp,
          seq: fileMsgData.seq,
          type: 'file',
          fileId: fileMsgData.fileId,
          fileName: fileMsgData.fileName,
          fileUrl: fileMsgData.fileUrl,
          fileSize: fileMsgData.fileSize,
          fileType: fileMsgData.fileType
        })

        messages.value.push(fileMessage)
        lastMessage.value = fileMessage

        // 更新房间的最后消息
        const fileRoomIndex = rooms.value.findIndex(r => r.id === String(fileMsgData.roomId))
        if (fileRoomIndex !== -1) {
          rooms.value[fileRoomIndex].lastMessage = fileMessage
        }

        // 如果不是当前打开的房间，增加未读数
        if (currentRoomId.value !== String(fileMsgData.roomId)) {
          const roomId = String(fileMsgData.roomId)
          unreadCounts.value[roomId] = (unreadCounts.value[roomId] || 0) + 1
        }

        showMessageNotification(fileMsgData.senderName, '[文件]')
        break

      case 'message:history:response':
        const historyData = event.data as { roomId: string; messages: Message[] }
        console.log('Received history:', historyData)
        // 将历史消息添加到 messages 数组（避免重复）
        const existingIds = new Set(messages.value.map(m => m.id))
        historyData.messages.forEach(msg => {
          if (!existingIds.has(msg.id)) {
            const normalizedMsg = normalizeMessageFileUrl({
              ...msg,
              type: 'file'
            })
            messages.value.push(normalizedMsg)
          }
        })
        break

      case 'room:created':
        const newRoom = event.data as Room
        rooms.value.push(newRoom)
        break

      case 'room:invite':
        // 被邀请加入群聊
        const invitedRoom = event.data as Room
        console.log('被邀请加入房间:', invitedRoom)
        // 检查是否已存在
        const existingInvitedRoom = rooms.value.find(r => r.id === invitedRoom.id)
        if (!existingInvitedRoom) {
          rooms.value.push(invitedRoom)
        }
        break

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

      case 'room:joined':
        console.log('Joined room:', event.data)
        break

      case 'room:member:joined':
        console.log('Room member joined:', event.data)
        break

      case 'room:member:left':
        console.log('Room member left:', event.data)
        lastRoomMemberLeft.value = {
          roomId: String(event.data.roomId),
          userId: String(event.data.userId)
        }
        break

      case 'room:list:response':
        rooms.value = event.data.rooms || []
        break

      case 'room:private:created':
        const privateRoom = event.data as Room
        lastPrivateRoomCreated.value = { id: String(privateRoom.id), name: privateRoom.name, type: privateRoom.type }
        // 检查是否已存在
        const existingRoom = rooms.value.find(r => r.id === privateRoom.id)
        if (!existingRoom) {
          rooms.value.push(privateRoom)
        }
        break

      case 'room:sync:response':
        const syncData = event.data as { messages: Message[] }
        console.log('Received sync messages:', syncData)
        if (syncData.messages) {
          const existingIds = new Set(messages.value.map(m => m.id))
          syncData.messages.forEach(msg => {
            if (!existingIds.has(msg.id)) {
                const normalizedMsg = normalizeMessageFileUrl({
                  ...msg,
                  type: 'file'
                })
                messages.value.push(normalizedMsg)
            }
            // 更新房间的最后序列号
            const roomId = String(msg.roomId)
            if (!roomLastSeq.value[roomId] || msg.seq > roomLastSeq.value[roomId]) {
              roomLastSeq.value[roomId] = msg.seq
            }
          })
        }
        break

      case 'user:list:response':
        onlineUsers.value = event.data.users || []
        break

      case 'message:read': {
        const readRoomId = String(event.data.roomId)
        const readUserId = String(event.data.userId)
        const existingSet = readReceipts.value.get(readRoomId)
        if (existingSet) {
          existingSet.add(readUserId)
        } else {
          readReceipts.value.set(readRoomId, new Set([readUserId]))
        }
        break
      }
    }
  }

  const sendMessage = (roomId: string, content: string, _senderId: string) => {
    const messageEvent: WebSocketEvent = {
      type: 'message:send',
      data: {
        roomId: roomId,
        content
      }
    }
    socket.value?.send(JSON.stringify(messageEvent))
  }

  const sendFileMessage = (roomId: string, _senderId: string, fileInfo: FileInfo) => {
    const fileMessageEvent: WebSocketEvent = {
      type: 'message:send:file',
      data: {
        roomId: roomId,
        fileId: fileInfo.fileId,
        fileName: fileInfo.fileName,
        fileUrl: fileInfo.fileUrl,
        fileSize: fileInfo.fileSize,
        fileType: fileInfo.fileType
      }
    }
    socket.value?.send(JSON.stringify(fileMessageEvent))
  }

  const createRoom = (name: string, participants: string[]) => {
    const createEvent: WebSocketEvent = {
      type: 'room:create',
      data: {
        name,
        participants
      }
    }
    socket.value?.send(JSON.stringify(createEvent))
  }

  const joinRoom = (roomId: string) => {
    const joinEvent: WebSocketEvent = {
      type: 'room:join',
      data: {
        roomId: roomId
      }
    }
    socket.value?.send(JSON.stringify(joinEvent))
  }

  const sendReadReceipt = (roomId: string) => {
    if (!socket.value || socket.value.readyState !== WebSocket.OPEN) return
    const readEvent: WebSocketEvent = {
      type: 'message:read',
      data: {
        roomId,
        userId: currentUserId
      }
    }
    socket.value.send(JSON.stringify(readEvent))
  }

  const startPrivateChat = (targetUserId: string) => {
    const privateEvent: WebSocketEvent = {
      type: 'room:private:start',
      data: {
        targetUserId: targetUserId
      }
    }
    socket.value?.send(JSON.stringify(privateEvent))
  }

  const syncRooms = () => {
    if (!socket.value || socket.value.readyState !== WebSocket.OPEN) {
      return
    }

    const syncData = rooms.value.map(room => ({
      roomId: room.id,
      lastSeq: roomLastSeq.value[room.id] || 0
    }))

    const syncEvent: WebSocketEvent = {
      type: 'room:sync',
      data: {
        rooms: syncData
      }
    }
    socket.value.send(JSON.stringify(syncEvent))
  }

  const loadMessageHistory = (roomId: string) => {
    if (!socket.value || socket.value.readyState !== WebSocket.OPEN) {
      console.log('WebSocket not ready, retrying in 500ms...')
      setTimeout(() => loadMessageHistory(roomId), 500)
      return
    }
    const historyEvent: WebSocketEvent = {
      type: 'message:history',
      data: {
        roomId: roomId
      }
    }
    socket.value.send(JSON.stringify(historyEvent))
  }

  const disconnect = () => {
    socket.value?.close()
  }

  const setCurrentRoom = (roomId: string) => {
    currentRoomId.value = roomId
    // 清除该房间的未读数
    if (unreadCounts.value[roomId]) {
      delete unreadCounts.value[roomId]
    }
    // 发送已读回执
    sendReadReceipt(roomId)
  }

  const getUnreadCount = (roomId: string): number => {
    return unreadCounts.value[roomId] || 0
  }

  const getTotalUnreadCount = (): number => {
    return Object.values(unreadCounts.value).reduce((sum, count) => sum + count, 0)
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    socket,
    isConnected,
    messages,
    rooms,
    onlineUsers,
    unreadCounts,
    lastMessage,
    lastInviteResult,
    lastBannedResult,
    lastRoomMemberLeft,
    lastPrivateRoomCreated,
    roomLastSeq,
    readReceipts,
    connect,
    sendMessage,
    sendFileMessage,
    createRoom,
    joinRoom,
    sendReadReceipt,
    startPrivateChat,
    disconnect,
    setCurrentRoom,
    getUnreadCount,
    getTotalUnreadCount,
    loadMessageHistory,
    syncRooms
  }
}

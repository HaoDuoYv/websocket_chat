import { ref, onUnmounted } from 'vue'
import { mergeUpdates } from 'yjs'

export interface EditorParticipant {
  userId: string
  username: string
}

export interface EditorRoom {
  docId: string
  roomName: string
  hasPassword: boolean
  participantCount: number
  creatorName: string
  createdAt?: number
}

interface EditorEvent {
  type: string
  data: any
}

function uint8ArrayToBase64(bytes: Uint8Array): string {
  let binary = ''
  const chunkSize = 8192
  for (let i = 0; i < bytes.length; i += chunkSize) {
    const chunk = bytes.subarray(i, i + chunkSize)
    binary += String.fromCharCode(...chunk)
  }
  return btoa(binary)
}

function base64ToUint8Array(base64: string): Uint8Array {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes
}

export function useEditorWebSocket() {
  const socket = ref<WebSocket | null>(null)
  const isConnected = ref(false)
  const connectionError = ref(false)
  const rooms = ref<EditorRoom[]>([])
  const participants = ref<EditorParticipant[]>([])
  const currentDocId = ref('')
  const currentRoomName = ref('')
  const joinFailed = ref(false)
  const joinFailedMessage = ref('')

  // Callbacks for Yjs
  let onYjsUpdate: ((update: Uint8Array, msgType: string) => void) | null = null
  let onSyncRequest: ((docId: string) => void) | null = null
  // Callback for room joined (used by lobby to navigate)
  let onRoomJoined: ((docId: string) => void) | null = null

  let reconnectAttempts = 0
  const maxReconnectAttempts = 5
  const reconnectDelay = 3000
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let connectTimeoutTimer: ReturnType<typeof setTimeout> | null = null
  let myUserId = ''
  let myUsername = ''
  let isLobbyMode = false

  const sendEvent = (type: string, data: Record<string, any> = {}) => {
    if (socket.value && socket.value.readyState === WebSocket.OPEN) {
      socket.value.send(JSON.stringify({ type, data }))
    }
  }

  const connect = (userId: string, username: string, lobby = false) => {
    myUserId = userId
    myUsername = username
    isLobbyMode = lobby
    connectionError.value = false
    joinFailed.value = false
    joinFailedMessage.value = ''

    if (connectTimeoutTimer) {
      clearTimeout(connectTimeoutTimer)
      connectTimeoutTimer = null
    }

    socket.value = new WebSocket('/ws/editor')

    socket.value.onopen = () => {
      isConnected.value = true
      connectionError.value = false
      reconnectAttempts = 0

      if (connectTimeoutTimer) {
        clearTimeout(connectTimeoutTimer)
        connectTimeoutTimer = null
      }

      if (isLobbyMode) {
        // Enter lobby mode: get room list
        sendEvent('editor:lobby:join', { userId, username })
      } else if (currentDocId.value) {
        // Rejoin a doc (reconnect scenario)
        sendEvent('editor:join', { docId: currentDocId.value, userId, username })
      }
    }

    connectTimeoutTimer = setTimeout(() => {
      if (!isConnected.value) {
        connectionError.value = true
      }
    }, 5000)

    socket.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as EditorEvent
        handleEvent(data)
      } catch (error) {
        console.error('Error parsing Editor WebSocket message:', error)
      }
    }

    socket.value.onclose = () => {
      isConnected.value = false
      attemptReconnect()
    }

    socket.value.onerror = (error) => {
      console.error('Editor WebSocket error:', error)
    }
  }

  const attemptReconnect = () => {
    if (reconnectAttempts >= maxReconnectAttempts) return
    reconnectAttempts++
    reconnectTimer = setTimeout(() => {
      connect(myUserId, myUsername, isLobbyMode)
    }, reconnectDelay)
  }

  const handleEvent = (event: EditorEvent) => {
    switch (event.type) {
      // Room list
      case 'editor:room:list':
        rooms.value = (event.data.rooms || []).map((r: any) => ({
          docId: r.docId,
          roomName: r.roomName,
          hasPassword: r.hasPassword,
          participantCount: r.participantCount ?? 0,
          creatorName: r.creatorName || '',
          createdAt: r.createdAt
        }))
        break

      // Room created
      case 'editor:room:created':
        currentDocId.value = event.data.docId
        currentRoomName.value = event.data.roomName || ''
        participants.value = (event.data.participants || []).map((p: any) => ({
          userId: String(p.userId),
          username: p.username || String(p.userId)
        }))
        joinFailed.value = false
        if (onRoomJoined) onRoomJoined(event.data.docId)
        break

      // Room joined (via room management)
      case 'editor:room:joined':
        currentDocId.value = event.data.docId
        currentRoomName.value = event.data.roomName || ''
        participants.value = (event.data.participants || []).map((p: any) => ({
          userId: String(p.userId),
          username: p.username || String(p.userId)
        }))
        joinFailed.value = false
        if (onRoomJoined) onRoomJoined(event.data.docId)
        break

      // Room join failed
      case 'editor:room:join:failed':
        joinFailed.value = true
        joinFailedMessage.value = event.data.message || '加入失败'
        break

      // Doc joined (direct join, e.g. on reconnect)
      case 'editor:joined':
        currentDocId.value = event.data.docId
        if (event.data.roomName) currentRoomName.value = event.data.roomName
        participants.value = (event.data.participants || []).map((p: any) => ({
          userId: String(p.userId),
          username: p.username || String(p.userId)
        }))
        break

      case 'editor:yjs-update': {
        const base64 = event.data.update
        if (base64 && onYjsUpdate) {
          try {
            const binary = base64ToUint8Array(base64)
            _recvCount++
            console.log(`[Editor Sync] RECV #${_recvCount} ${event.data.msgType || 'sync'} bytes=${binary.length} b64len=${base64.length}`)
            onYjsUpdate(binary, event.data.msgType || 'sync')
          } catch (e) {
            console.error('[Editor Sync] RECV FAILED', e, 'b64:', base64.substring(0, 50))
          }
        } else if (!onYjsUpdate) {
          console.warn('[Editor Sync] RECV but no onYjsUpdate callback set!')
        }
        break
      }

      case 'editor:participant:joined': {
        const exists = participants.value.some(p => p.userId === event.data.userId)
        if (!exists) {
          participants.value.push({
            userId: String(event.data.userId),
            username: event.data.username || String(event.data.userId)
          })
        }
        break
      }

      case 'editor:participant:left':
        participants.value = participants.value.filter(p => p.userId !== event.data.userId)
        break

      case 'editor:sync-request':
        console.log('[Editor Sync] SYNC-REQUEST received', event.data)
        if (onSyncRequest) {
          onSyncRequest(event.data.docId || currentDocId.value)
        }
        break

      case 'editor:error':
        console.error('Editor error:', event.data.message)
        break
    }
  }

  // ========== Lobby / Room Management ==========

  const connectToLobby = (userId: string, username: string) => {
    connect(userId, username, true)
  }

  const createRoom = (roomName: string, password?: string) => {
    sendEvent('editor:room:create', {
      roomName,
      password: password || undefined,
      userId: myUserId,
      username: myUsername
    })
  }

  const joinRoom = (docId: string, password?: string) => {
    joinFailed.value = false
    joinFailedMessage.value = ''
    sendEvent('editor:room:join', {
      docId,
      password: password || undefined,
      userId: myUserId,
      username: myUsername
    })
  }

  const leaveRoom = () => {
    if (currentDocId.value) {
      sendEvent('editor:room:leave', { docId: currentDocId.value, userId: myUserId })
      currentDocId.value = ''
      currentRoomName.value = ''
      participants.value = []
    }
  }

  // ========== Doc Operations ==========

  const joinDoc = (docId: string) => {
    sendEvent('editor:join', { docId, userId: myUserId, username: myUsername })
  }

  const leaveDoc = (docId: string) => {
    sendEvent('editor:leave', { docId })
    currentDocId.value = ''
    currentRoomName.value = ''
    participants.value = []
  }

  let _sendCount = 0
  let _recvCount = 0

  // Batch buffer for merging rapid Yjs updates before sending
  let pendingUpdates: Uint8Array[] = []
  let pendingDocId = ''
  let pendingMsgType: 'sync' | 'awareness' = 'sync'
  let flushScheduled = false

  function flushUpdates() {
    flushScheduled = false
    if (pendingUpdates.length === 0 || !pendingDocId) return
    const updates = pendingUpdates
    const docId = pendingDocId
    const msgType = pendingMsgType
    pendingUpdates = []
    pendingDocId = ''
    try {
      const merged = updates.length === 1 ? updates[0] : mergeUpdates(updates)
      const base64 = uint8ArrayToBase64(merged)
      _sendCount++
      sendEvent('editor:yjs-update', { docId, update: base64, msgType })
    } catch (e) {
      console.error('[Editor Sync] SEND FAILED', e)
    }
  }

  const sendYjsUpdate = (docId: string, update: Uint8Array, msgType: 'sync' | 'awareness' = 'sync') => {
    pendingUpdates.push(update)
    pendingDocId = docId
    pendingMsgType = msgType
    if (!flushScheduled) {
      flushScheduled = true
      requestAnimationFrame(flushUpdates)
    }
  }

  const setOnYjsUpdate = (cb: (update: Uint8Array, msgType: string) => void) => {
    onYjsUpdate = cb
  }

  const setOnSyncRequest = (cb: (docId: string) => void) => {
    onSyncRequest = cb
  }

  const setOnRoomJoined = (cb: (docId: string) => void) => {
    onRoomJoined = cb
  }

  const disconnect = () => {
    pendingUpdates = []
    flushScheduled = false
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (connectTimeoutTimer) {
      clearTimeout(connectTimeoutTimer)
      connectTimeoutTimer = null
    }
    reconnectAttempts = maxReconnectAttempts
    socket.value?.close()
    socket.value = null
    isConnected.value = false
    connectionError.value = false
    joinFailed.value = false
    joinFailedMessage.value = ''
    rooms.value = []
    participants.value = []
    currentDocId.value = ''
    currentRoomName.value = ''
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    socket,
    isConnected,
    connectionError,
    rooms,
    participants,
    currentDocId,
    currentRoomName,
    joinFailed,
    joinFailedMessage,
    connect,
    connectToLobby,
    disconnect,
    createRoom,
    joinRoom,
    leaveRoom,
    joinDoc,
    leaveDoc,
    sendYjsUpdate,
    setOnYjsUpdate,
    setOnSyncRequest,
    setOnRoomJoined
  }
}

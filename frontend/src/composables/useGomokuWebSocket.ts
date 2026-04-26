import { ref, onUnmounted } from 'vue'

export interface GomokuPlayer {
  userId: string
  username: string
}

export interface GomokuRoom {
  roomId: string
  roomName: string
  hasPassword: boolean
  playerCount: number
  status: 'WAITING' | 'PLAYING' | 'FINISHED'
  spectatorCount: number
  blackPlayer?: GomokuPlayer
  whitePlayer?: GomokuPlayer
}

export interface GomokuGameState {
  roomId: string
  roomName: string
  board: number[][]
  currentTurn: number
  status: 'WAITING' | 'PLAYING' | 'FINISHED'
  winner: number
  winLine: number[][] | null
  blackPlayer?: GomokuPlayer
  whitePlayer?: GomokuPlayer
  spectators: GomokuPlayer[]
  myRole: 'black' | 'white' | 'spectator' | null
  lastMove: [number, number] | null
}

export interface GomokuChatMessage {
  senderName: string
  role: string
  content: string
  timestamp: number
}

interface GomokuEvent {
  type: string
  data: any
}

/** 将后端 buildRoomState() 返回的字段映射到前端 GomokuGameState */
function mapRoomState(data: any, userId: string): GomokuGameState {
  const blackPlayer = data.player1Id
    ? { userId: String(data.player1Id), username: data.player1Name || '' }
    : undefined
  const whitePlayer = data.player2Id
    ? { userId: String(data.player2Id), username: data.player2Name || '' }
    : undefined

  let myRole: GomokuGameState['myRole'] = 'spectator'
  if (data.player1Id && String(data.player1Id) === String(userId)) {
    myRole = 'black'
  } else if (data.player2Id && String(data.player2Id) === String(userId)) {
    myRole = 'white'
  }

  const moveHistory: Array<{ row: number; col: number; player: number }> = data.moveHistory || []
  const lastMove: [number, number] | null = moveHistory.length > 0
    ? [moveHistory[moveHistory.length - 1].row, moveHistory[moveHistory.length - 1].col]
    : null

  return {
    roomId: data.roomId,
    roomName: data.roomName,
    board: data.board,
    currentTurn: data.currentTurn,
    status: data.state,
    winner: data.winner ?? 0,
    winLine: data.winLine || null,
    blackPlayer,
    whitePlayer,
    spectators: data.spectators || [],
    myRole,
    lastMove
  }
}

/** 将后端 buildRoomList() 返回的单个房间字段映射到前端 GomokuRoom */
function mapRoomListItem(data: any): GomokuRoom {
  const blackPlayer = data.player1Id
    ? { userId: String(data.player1Id), username: data.player1Name || '' }
    : undefined
  const whitePlayer = data.player2Id
    ? { userId: String(data.player2Id), username: data.player2Name || '' }
    : undefined
  const playerCount = (data.player1Id ? 1 : 0) + (data.player2Id ? 1 : 0)

  return {
    roomId: data.roomId,
    roomName: data.roomName,
    hasPassword: data.hasPassword,
    playerCount,
    status: data.state,
    spectatorCount: data.spectatorCount || 0,
    blackPlayer,
    whitePlayer
  }
}

export function useGomokuWebSocket() {
  const socket = ref<WebSocket | null>(null)
  const isConnected = ref(false)
  const rooms = ref<GomokuRoom[]>([])
  const gameState = ref<GomokuGameState | null>(null)
  const chatMessages = ref<GomokuChatMessage[]>([])
  const myUserId = ref<string>('')
  const myUsername = ref<string>('')
  const currentRoomId = ref<string>('')
  const restartRequest = ref<{ fromUserId: string; fromUsername: string } | null>(null)
  const opponentDisconnected = ref(false)
  const opponentLeft = ref(false)
  const moveRejected = ref<string>('')
  const joinFailed = ref(false)
  const restartRejected = ref(false)

  let reconnectAttempts = 0
  const maxReconnectAttempts = 5
  const reconnectDelay = 3000
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  const sendEvent = (type: string, data: Record<string, any> = {}, includeRoomId = false) => {
    if (socket.value && socket.value.readyState === WebSocket.OPEN) {
      const payload: Record<string, any> = {
        userId: myUserId.value,
        username: myUsername.value,
        ...data
      }
      if (includeRoomId && currentRoomId.value) {
        payload.roomId = currentRoomId.value
      }
      socket.value.send(JSON.stringify({ type, data: payload }))
    }
  }

  const connect = (userId: string, username: string) => {
    myUserId.value = userId
    myUsername.value = username

    socket.value = new WebSocket('/ws/gomoku')

    socket.value.onopen = () => {
      console.log('Gomoku WebSocket connected')
      isConnected.value = true
      reconnectAttempts = 0

      sendEvent('game:join', {
        userId,
        username
      })
    }

    socket.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as GomokuEvent
        handleEvent(data)
      } catch (error) {
        console.error('Error parsing Gomoku WebSocket message:', error)
      }
    }

    socket.value.onclose = () => {
      console.log('Gomoku WebSocket disconnected')
      isConnected.value = false
      attemptReconnect()
    }

    socket.value.onerror = (error) => {
      console.error('Gomoku WebSocket error:', error)
    }
  }

  const attemptReconnect = () => {
    if (reconnectAttempts >= maxReconnectAttempts) {
      console.log('Max reconnect attempts reached')
      return
    }

    reconnectAttempts++
    console.log(`Attempting to reconnect (${reconnectAttempts}/${maxReconnectAttempts})...`)

    reconnectTimer = setTimeout(() => {
      connect(myUserId.value, myUsername.value)
    }, reconnectDelay)
  }

  const disconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    reconnectAttempts = maxReconnectAttempts
    socket.value?.close()
    socket.value = null
    isConnected.value = false
    gameState.value = null
    currentRoomId.value = ''
    rooms.value = []
    chatMessages.value = []
    restartRequest.value = null
    opponentDisconnected.value = false
    opponentLeft.value = false
    moveRejected.value = ''
    joinFailed.value = false
    restartRejected.value = false
  }

  const handleEvent = (event: GomokuEvent) => {
    switch (event.type) {
      case 'game:room:list':
        rooms.value = (event.data.rooms || []).map(mapRoomListItem)
        break

      case 'game:room:created':
        gameState.value = mapRoomState(event.data, myUserId.value)
        currentRoomId.value = event.data.roomId
        break

      case 'game:room:joined':
        gameState.value = mapRoomState(event.data, myUserId.value)
        currentRoomId.value = event.data.roomId
        chatMessages.value = []
        restartRequest.value = null
        opponentDisconnected.value = false
        opponentLeft.value = false
        moveRejected.value = ''
        restartRejected.value = false
        break

      case 'game:move:made':
        if (gameState.value) {
          if (event.data.board) {
            gameState.value.board = event.data.board
          } else {
            const newBoard = gameState.value.board.map(r => [...r])
            newBoard[event.data.row][event.data.col] = event.data.player
            gameState.value.board = newBoard
          }
          gameState.value.currentTurn = event.data.currentTurn
          gameState.value.lastMove = [event.data.row, event.data.col]
        }
        break

      case 'game:room:spectating':
        gameState.value = mapRoomState(event.data, myUserId.value)
        currentRoomId.value = event.data.roomId
        chatMessages.value = []
        restartRequest.value = null
        opponentDisconnected.value = false
        opponentLeft.value = false
        moveRejected.value = ''
        restartRejected.value = false
        break

      case 'game:move:rejected':
        moveRejected.value = event.data.reason || ''
        break

      case 'game:over':
        if (gameState.value) {
          gameState.value.status = 'FINISHED'
          gameState.value.winner = event.data.winner
          gameState.value.winLine = event.data.winLine || null
        }
        break

      case 'game:draw':
        if (gameState.value) {
          gameState.value.status = 'FINISHED'
          gameState.value.winner = 0
        }
        break

      case 'game:restart:requested':
        restartRequest.value = {
          fromUserId: event.data.fromUserId,
          fromUsername: event.data.fromUsername || '对手'
        }
        break

      case 'game:restarted':
        if (gameState.value) {
          const restarted = mapRoomState(event.data, myUserId.value)
          gameState.value = { ...restarted }
        }
        restartRequest.value = null
        moveRejected.value = ''
        restartRejected.value = false
        opponentLeft.value = false
        opponentDisconnected.value = false
        break

      case 'game:restart:rejected':
        restartRejected.value = true
        restartRequest.value = null
        break

      case 'game:chat:message':
        chatMessages.value.push(event.data as GomokuChatMessage)
        break

      case 'game:player:joined':
        if (gameState.value) {
          if (event.data.role === 'black' && event.data.player) {
            gameState.value.blackPlayer = event.data.player
          } else if (event.data.role === 'white' && event.data.player) {
            gameState.value.whitePlayer = event.data.player
          }
          if (gameState.value.status === 'WAITING' && gameState.value.blackPlayer && gameState.value.whitePlayer) {
            gameState.value.status = 'PLAYING'
          }
          opponentLeft.value = false
        }
        break

      case 'game:spectator:joined':
        if (gameState.value && event.data.spectator) {
          const exists = gameState.value.spectators.some(s => s.userId === event.data.spectator.userId)
          if (!exists) {
            gameState.value.spectators.push(event.data.spectator)
          }
        }
        break

      case 'game:player:left':
        opponentLeft.value = true
        break

      case 'game:player:reconnected':
        opponentDisconnected.value = false
        opponentLeft.value = false
        break

      case 'game:room:join:rejected':
        if (event.data.reason && event.data.reason.includes('已在房间')) {
          const roomId = event.data.roomId || currentRoomId.value
          if (roomId) {
            rejoinRoom(roomId)
          }
        }
        break

      case 'game:player:disconnected':
        opponentDisconnected.value = true
        break

      case 'game:spectator:list:update':
        if (gameState.value && event.data.spectators) {
          gameState.value.spectators = event.data.spectators
        }
        break

      case 'game:room:state:update':
        if (gameState.value && event.data) {
          const updated = mapRoomState(event.data, myUserId.value)
          gameState.value = { ...updated }
          opponentLeft.value = false
          opponentDisconnected.value = false
        }
        break

      case 'game:rejoin:failed':
        joinFailed.value = true
        break

      case 'game:room:join:failed':
        console.error('加入房间失败:', event.data.reason)
        moveRejected.value = event.data.reason || '加入房间失败'
        break
    }
  }

  const createRoom = (roomName: string, password?: string) => {
    sendEvent('game:room:create', { roomName, password })
  }

  const joinRoom = (roomId: string, password?: string) => {
    sendEvent('game:room:join', { roomId, password })
  }

  const spectateRoom = (roomId: string) => {
    sendEvent('game:room:spectate', { roomId })
  }

  const makeMove = (row: number, col: number) => {
    sendEvent('game:move', { row, col }, true)
  }

  const requestRestart = () => {
    sendEvent('game:restart:request', {}, true)
  }

  const respondRestart = (accepted: boolean) => {
    sendEvent('game:restart:respond', { accepted }, true)
  }

  const sendChat = (content: string) => {
    sendEvent('game:chat:send', { content }, true)
  }

  const surrender = () => {
    sendEvent('game:surrender', {}, true)
  }

  const joinAsPlayer = () => {
    sendEvent('game:spectator:join:player', {}, true)
  }

  const leaveRoom = () => {
    if (currentRoomId.value) {
      sendEvent('game:room:leave', { roomId: currentRoomId.value })
    }
    gameState.value = null
    chatMessages.value = []
    restartRequest.value = null
    opponentDisconnected.value = false
    opponentLeft.value = false
    moveRejected.value = ''
    joinFailed.value = false
    restartRejected.value = false
    currentRoomId.value = ''
  }

  const rejoinRoom = (roomId: string) => {
    sendEvent('game:rejoin', { roomId })
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    socket,
    isConnected,
    rooms,
    gameState,
    chatMessages,
    myUserId,
    myUsername,
    currentRoomId,
    restartRequest,
    opponentDisconnected,
    opponentLeft,
    moveRejected,
    joinFailed,
    restartRejected,
    connect,
    disconnect,
    createRoom,
    joinRoom,
    spectateRoom,
    makeMove,
    requestRestart,
    respondRestart,
    sendChat,
    surrender,
    joinAsPlayer,
    leaveRoom,
    rejoinRoom
  }
}

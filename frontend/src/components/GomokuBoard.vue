<template>
  <div ref="containerRef" class="w-full max-w-[600px] aspect-square">
    <canvas
      ref="boardCanvas"
      :width="canvasSize"
      :height="canvasSize"
      class="w-full h-full cursor-pointer"
      @click="handleClick"
      @mousemove="handleMouseMove"
      @mouseleave="hoverPos = null"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'

interface Props {
  board: number[][]
  currentTurn: number
  myRole: 'black' | 'white' | 'spectator' | null
  status: 'WAITING' | 'PLAYING' | 'FINISHED'
  winLine: number[][] | null
  lastMove: [number, number] | null
  isDark: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  move: [row: number, col: number]
}>()

const BOARD_SIZE = 15
const STAR_POINTS = [[3, 3], [3, 11], [7, 7], [11, 3], [11, 11]]

const boardCanvas = ref<HTMLCanvasElement | null>(null)
const containerRef = ref<HTMLDivElement | null>(null)
const canvasSize = ref(600)
const hoverPos = ref<{ row: number; col: number } | null>(null)

const canPlace = computed(() => {
  if (props.status !== 'PLAYING') return false
  if (props.myRole === 'spectator' || props.myRole === null) return false
  const myTurn = props.myRole === 'black' ? 1 : 2
  return myTurn === props.currentTurn
})

const isValidBoard = computed(() =>
  Array.isArray(props.board) && props.board.length === BOARD_SIZE &&
  props.board.every(row => Array.isArray(row) && row.length === BOARD_SIZE)
)

let resizeObserver: ResizeObserver | null = null

function getCellSize(): number {
  return canvasSize.value / (BOARD_SIZE + 1)
}

function getPos(row: number, col: number): { x: number; y: number } {
  const cell = getCellSize()
  return { x: cell * (col + 1), y: cell * (row + 1) }
}

function drawBoard(ctx: CanvasRenderingContext2D) {
  const size = canvasSize.value
  const cell = getCellSize()

  // 棋盘底色
  ctx.fillStyle = props.isDark ? '#8B7355' : '#DEB887'
  ctx.fillRect(0, 0, size, size)

  // 网格线
  ctx.strokeStyle = props.isDark ? '#C4A87C' : '#5C4033'
  ctx.lineWidth = 1
  for (let i = 0; i < BOARD_SIZE; i++) {
    const pos = cell * (i + 1)
    ctx.beginPath()
    ctx.moveTo(pos, cell)
    ctx.lineTo(pos, cell * BOARD_SIZE)
    ctx.stroke()
    ctx.beginPath()
    ctx.moveTo(cell, pos)
    ctx.lineTo(cell * BOARD_SIZE, pos)
    ctx.stroke()
  }

  // 星位
  ctx.fillStyle = props.isDark ? '#C4A87C' : '#5C4033'
  for (const [r, c] of STAR_POINTS) {
    const { x, y } = getPos(r, c)
    ctx.beginPath()
    ctx.arc(x, y, Math.max(cell * 0.1, 2), 0, Math.PI * 2)
    ctx.fill()
  }
}

function drawStone(ctx: CanvasRenderingContext2D, row: number, col: number, color: number) {
  const cell = getCellSize()
  const { x, y } = getPos(row, col)
  const radius = cell * 0.42

  // 阴影
  ctx.save()
  ctx.shadowColor = 'rgba(0,0,0,0.4)'
  ctx.shadowBlur = cell * 0.08
  ctx.shadowOffsetX = cell * 0.04
  ctx.shadowOffsetY = cell * 0.04

  if (color === 1) {
    // 黑子
    const grad = ctx.createRadialGradient(x - radius * 0.3, y - radius * 0.3, radius * 0.1, x, y, radius)
    grad.addColorStop(0, '#555')
    grad.addColorStop(1, '#000')
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.arc(x, y, radius, 0, Math.PI * 2)
    ctx.fill()
  } else {
    // 白子
    const grad = ctx.createRadialGradient(x - radius * 0.3, y - radius * 0.3, radius * 0.1, x, y, radius)
    grad.addColorStop(0, '#fff')
    grad.addColorStop(1, '#ddd')
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.arc(x, y, radius, 0, Math.PI * 2)
    ctx.fill()
    ctx.restore()
    // 白子边框
    ctx.strokeStyle = '#bbb'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.arc(x, y, radius, 0, Math.PI * 2)
    ctx.stroke()
    return
  }
  ctx.restore()
}

function drawLastMove(ctx: CanvasRenderingContext2D) {
  if (!props.lastMove) return
  const cell = getCellSize()
  const [row, col] = props.lastMove
  const { x, y } = getPos(row, col)
  const markSize = cell * 0.12

  ctx.fillStyle = '#E53E3E'
  ctx.fillRect(x - markSize, y - markSize, markSize * 2, markSize * 2)
}

function drawWinLine(ctx: CanvasRenderingContext2D) {
  if (!props.winLine) return
  const cell = getCellSize()

  for (const [row, col] of props.winLine) {
    const { x, y } = getPos(row, col)
    const radius = cell * 0.42
    ctx.strokeStyle = '#FFD700'
    ctx.lineWidth = 3
    ctx.beginPath()
    ctx.arc(x, y, radius + 2, 0, Math.PI * 2)
    ctx.stroke()
  }
}

function drawHover(ctx: CanvasRenderingContext2D) {
  if (!isValidBoard.value || !hoverPos.value || !canPlace.value) return
  const { row, col } = hoverPos.value
  if (props.board[row][col] !== 0) return

  const cell = getCellSize()
  const { x, y } = getPos(row, col)
  const radius = cell * 0.42

  ctx.globalAlpha = 0.4
  if (props.currentTurn === 1) {
    ctx.fillStyle = '#000'
  } else {
    ctx.fillStyle = '#fff'
  }
  ctx.beginPath()
  ctx.arc(x, y, radius, 0, Math.PI * 2)
  ctx.fill()
  ctx.globalAlpha = 1
}

function draw() {
  const canvas = boardCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  // 高 DPI 适配
  const dpr = window.devicePixelRatio || 1
  const displaySize = canvasSize.value
  canvas.width = displaySize * dpr
  canvas.height = displaySize * dpr
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

  drawBoard(ctx)

  // 绘制棋子
  if (isValidBoard.value) {
    for (let r = 0; r < BOARD_SIZE; r++) {
      for (let c = 0; c < BOARD_SIZE; c++) {
        if (props.board[r][c] !== 0) {
          drawStone(ctx, r, c, props.board[r][c])
        }
      }
    }
  }

  drawLastMove(ctx)
  drawWinLine(ctx)
  drawHover(ctx)
}

function getGridPos(e: MouseEvent): { row: number; col: number } | null {
  const canvas = boardCanvas.value
  if (!canvas) return null
  const rect = canvas.getBoundingClientRect()
  const scaleX = canvasSize.value / rect.width
  const scaleY = canvasSize.value / rect.height
  const x = (e.clientX - rect.left) * scaleX
  const y = (e.clientY - rect.top) * scaleY
  const cell = getCellSize()
  const col = Math.round(x / cell) - 1
  const row = Math.round(y / cell) - 1
  if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) return null
  return { row, col }
}

function handleClick(e: MouseEvent) {
  if (!isValidBoard.value || !canPlace.value) return
  const pos = getGridPos(e)
  if (!pos) return
  if (props.board[pos.row][pos.col] !== 0) return
  emit('move', pos.row, pos.col)
}

function handleMouseMove(e: MouseEvent) {
  const pos = getGridPos(e)
  hoverPos.value = pos
  draw()
}

function updateSize() {
  if (!containerRef.value) return
  const width = containerRef.value.clientWidth
  canvasSize.value = Math.min(width, 600)
  draw()
}

watch(
  () => [props.board, props.winLine, props.lastMove, props.isDark, props.status, props.currentTurn],
  () => draw(),
  { deep: true }
)

onMounted(() => {
  updateSize()
  if (containerRef.value) {
    resizeObserver = new ResizeObserver(() => updateSize())
    resizeObserver.observe(containerRef.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
})
</script>

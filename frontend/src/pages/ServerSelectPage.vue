<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { setServerBaseUrl, isElectron } from '@/api/server-config'
import { getDiscoveredServers, startLocalServer, type DiscoveredServer } from '@/api/discovery'

const router = useRouter()
const servers = ref<DiscoveredServer[]>([])
const manualIp = ref('')
const manualPort = ref('8081')
const isScanning = ref(false)
const isConnecting = ref(false)
const isStarting = ref(false)
const errorMsg = ref('')
const lastServer = ref<{ ip: string; port: number; alias: string } | null>(null)

let scanTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  const saved = localStorage.getItem('lastServer')
  if (saved) {
    try {
      lastServer.value = JSON.parse(saved)
    } catch {}
  }

  if (isElectron()) {
    await scanServers()
    scanTimer = setInterval(scanServers, 5000)
  }
})

onUnmounted(() => {
  if (scanTimer) clearInterval(scanTimer)
})

async function scanServers() {
  isScanning.value = true
  try {
    servers.value = await getDiscoveredServers()
  } catch {
    // ignore
  }
  isScanning.value = false
}

async function connectToServer(ip: string, port: number, alias?: string) {
  isConnecting.value = true
  errorMsg.value = ''
  const url = `http://${ip}:${port}`

  try {
    const res = await fetch(`${url}/api/discovery/health`, { signal: AbortSignal.timeout(3000) })
    if (!res.ok) throw new Error('Server not healthy')

    setServerBaseUrl(url)
    localStorage.setItem('lastServer', JSON.stringify({ ip, port, alias: alias || `${ip}:${port}` }))
    router.push('/login')
  } catch {
    errorMsg.value = `无法连接到 ${ip}:${port}，请确认服务器已启动`
  }
  isConnecting.value = false
}

async function connectManual() {
  const port = parseInt(manualPort.value) || 8081
  await connectToServer(manualIp.value, port)
}

async function createLocalServer() {
  isStarting.value = true
  errorMsg.value = ''
  try {
    const result = await startLocalServer()
    if (result.success) {
      setServerBaseUrl(`http://localhost:${result.port || 8081}`)
      localStorage.setItem('lastServer', JSON.stringify({ ip: 'localhost', port: result.port || 8081, alias: '本机服务器' }))
      router.push('/login')
    } else {
      errorMsg.value = result.error || '启动服务器失败'
    }
  } catch (e: any) {
    errorMsg.value = e.message || '启动服务器失败'
  }
  isStarting.value = false
}

async function reconnectLast() {
  if (lastServer.value) {
    await connectToServer(lastServer.value.ip, lastServer.value.port, lastServer.value.alias)
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
    <div class="w-full max-w-lg p-8">
      <!-- Logo -->
      <div class="text-center mb-8">
        <div class="w-16 h-16 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center">
          <span class="text-3xl">💬</span>
        </div>
        <h1 class="text-2xl font-bold text-white">WebSocket Chat</h1>
        <p class="text-gray-400 mt-2">选择或创建一个聊天服务器</p>
      </div>

      <!-- 上次连接 -->
      <div v-if="lastServer" class="mb-6">
        <button
          @click="reconnectLast"
          :disabled="isConnecting"
          class="w-full p-4 rounded-xl bg-indigo-600/20 border border-indigo-500/30 hover:bg-indigo-600/30 transition-colors text-left"
        >
          <div class="text-sm text-indigo-400 mb-1">上次连接</div>
          <div class="text-white font-medium">{{ lastServer.alias }}</div>
          <div class="text-gray-400 text-sm">{{ lastServer.ip }}:{{ lastServer.port }}</div>
        </button>
      </div>

      <!-- 自动发现的服务器 -->
      <div v-if="isElectron()" class="mb-6">
        <div class="flex items-center justify-between mb-3">
          <h2 class="text-sm font-medium text-gray-300">局域网服务器</h2>
          <span v-if="isScanning" class="text-xs text-gray-500">扫描中...</span>
        </div>

        <div v-if="servers.length > 0" class="space-y-2">
          <button
            v-for="server in servers"
            :key="`${server.ip}:${server.port}`"
            @click="connectToServer(server.ip, server.port, server.alias)"
            :disabled="isConnecting"
            class="w-full p-3 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition-colors text-left"
          >
            <div class="flex items-center justify-between">
              <div>
                <div class="text-white font-medium">{{ server.alias }}</div>
                <div class="text-gray-400 text-sm">{{ server.ip }}:{{ server.port }}</div>
              </div>
              <div class="text-xs text-gray-500">
                {{ server.userCount }} 人在线
              </div>
            </div>
          </button>
        </div>

        <div v-else class="text-center py-6 text-gray-500 text-sm">
          未发现局域网服务器
        </div>
      </div>

      <!-- 手动输入 -->
      <div class="mb-6">
        <h2 class="text-sm font-medium text-gray-300 mb-3">手动连接</h2>
        <div class="flex gap-2">
          <input
            v-model="manualIp"
            placeholder="IP 地址"
            class="flex-1 px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:outline-none focus:border-indigo-500"
          />
          <input
            v-model="manualPort"
            placeholder="端口"
            class="w-24 px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:outline-none focus:border-indigo-500"
          />
          <button
            @click="connectManual"
            :disabled="!manualIp || isConnecting"
            class="px-6 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white font-medium transition-colors"
          >
            连接
          </button>
        </div>
      </div>

      <!-- 创建本地服务器 -->
      <div v-if="isElectron()" class="mb-6">
        <button
          @click="createLocalServer"
          :disabled="isStarting"
          class="w-full py-3 rounded-xl border border-white/20 hover:bg-white/5 text-white transition-colors"
        >
          {{ isStarting ? '正在启动服务器...' : '创建我的服务器' }}
        </button>
      </div>

      <!-- Web 模式提示 -->
      <div v-if="!isElectron()" class="text-center text-gray-500 text-sm mb-6">
        Web 模式：将连接到当前页面所在的服务器
      </div>

      <!-- 错误信息 -->
      <div v-if="errorMsg" class="text-center text-red-400 text-sm">
        {{ errorMsg }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { getRecentLogs, clearLogs, type LogLine } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Delete } from '@element-plus/icons-vue'

const logs = ref<LogLine[]>([])
const levelFilter = ref('')
const autoRefresh = ref(true)
const loading = ref(false)
const logContainer = ref<HTMLElement | null>(null)
let refreshTimer: number | null = null

const filteredLogs = ref<LogLine[]>([])

function updateFiltered() {
  if (!levelFilter.value) {
    filteredLogs.value = logs.value
  } else {
    filteredLogs.value = logs.value.filter(l => l.level === levelFilter.value)
  }
}

function getTagType(level: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    ERROR: 'danger',
    WARN: 'warning',
    INFO: '',
    DEBUG: 'info',
  }
  return map[level] ?? 'info'
}

async function refreshData() {
  try {
    const data = await getRecentLogs(500)
    logs.value = data
    updateFiltered()
    await nextTick()
    if (logContainer.value) {
      logContainer.value.scrollTop = logContainer.value.scrollHeight
    }
  } catch {
    // ignore
  }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定要清空日志缓存吗？', '清空日志', {
      confirmButtonText: '确认清空',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await clearLogs()
    logs.value = []
    filteredLogs.value = []
    ElMessage.success('日志已清空')
  } catch {
    // user cancelled
  }
}

function toggleAutoRefresh() {
  autoRefresh.value = !autoRefresh.value
  if (autoRefresh.value) {
    refreshTimer = window.setInterval(refreshData, 3000)
  } else if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  refreshData()
  if (autoRefresh.value) {
    refreshTimer = window.setInterval(refreshData, 3000)
  }
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<template>
  <div class="logs-page">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span class="page-title">系统日志 ({{ filteredLogs.length }} 条)</span>
          <div class="page-actions">
            <el-select v-model="levelFilter" placeholder="全部级别" clearable style="width: 130px;" @change="updateFiltered">
              <el-option label="INFO" value="INFO" />
              <el-option label="WARN" value="WARN" />
              <el-option label="ERROR" value="ERROR" />
              <el-option label="DEBUG" value="DEBUG" />
            </el-select>
            <el-button
              :type="autoRefresh ? 'success' : 'default'"
              @click="toggleAutoRefresh"
              size="small"
            >
              {{ autoRefresh ? '自动刷新中' : '自动刷新已关闭' }}
            </el-button>
            <el-button :icon="Refresh" @click="refreshData" :loading="loading">刷新</el-button>
            <el-button :icon="Delete" type="danger" @click="handleClear">清空</el-button>
          </div>
        </div>
      </template>

      <div ref="logContainer" class="log-container">
        <el-empty v-if="filteredLogs.length === 0" description="暂无日志" />
        <div v-else class="log-list">
          <div v-for="log in filteredLogs" :key="log.lineNumber" class="log-item">
            <el-tag :type="getTagType(log.level)" size="small" class="log-level">{{ log.level }}</el-tag>
            <span class="log-time">{{ log.timestamp || '--:--:--' }}</span>
            <span class="log-content">{{ log.content }}</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.logs-page {
  max-width: 1200px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  font-weight: 600;
  font-size: 15px;
}

.page-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.log-container {
  height: 600px;
  overflow-y: auto;
}

.log-list {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
}

.log-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.15s;
}

:root.dark .log-item {
  border-bottom-color: #27272a;
}

.log-item:hover {
  background: #fafafa;
}

:root.dark .log-item:hover {
  background: #1f1f1f;
}

.log-level {
  flex-shrink: 0;
  width: 60px;
  text-align: center;
}

.log-time {
  flex-shrink: 0;
  color: #6b7280;
  width: 80px;
}

.log-content {
  word-break: break-all;
  color: #374151;
}

:root.dark .log-content {
  color: #d1d5db;
}
</style>

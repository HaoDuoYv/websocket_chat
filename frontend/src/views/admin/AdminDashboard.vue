<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getSystemMetrics, getUsers, getOnlineUsers, type SystemMetrics, type AdminUser } from '@/api/admin'
import SystemMetricsComponent from '@/components/admin/SystemMetrics.vue'
import { User, CircleCloseFilled, Timer } from '@element-plus/icons-vue'

const metrics = ref<SystemMetrics | null>(null)
const users = ref<AdminUser[]>([])
const onlineUserIds = ref<Set<string>>(new Set())
let refreshTimer: number | null = null

const onlineCount = computed(() => users.value.filter(u => onlineUserIds.value.has(u.userId)).length)
const bannedCount = computed(() => users.value.filter(u => u.banned).length)

function formatUptime(seconds: number): string {
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (days > 0) return `${days}天${hours}时`
  if (hours > 0) return `${hours}小时${minutes}分`
  return `${minutes}分钟`
}

async function refreshData() {
  try {
    const [m, u, o] = await Promise.all([
      getSystemMetrics(),
      getUsers(),
      getOnlineUsers(),
    ])
    metrics.value = m
    users.value = u
    onlineUserIds.value = new Set((o.users || []).map(user => user.userId))
  } catch {
    // ignore
  }
}

onMounted(() => {
  refreshData()
  refreshTimer = window.setInterval(refreshData, 3000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<template>
  <div class="dashboard">
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 3px solid #7c3aed;">
          <div class="stat-icon"><el-icon :size="24" color="#7c3aed"><User /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">在线用户</div>
            <div class="stat-value">{{ onlineCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 3px solid #10b981;">
          <div class="stat-icon"><el-icon :size="24" color="#10b981"><User /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">用户总数</div>
            <div class="stat-value">{{ users.length }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 3px solid #f59e0b;">
          <div class="stat-icon"><el-icon :size="24" color="#f59e0b"><CircleCloseFilled /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">已封禁</div>
            <div class="stat-value">{{ bannedCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 3px solid #ec4899;">
          <div class="stat-icon"><el-icon :size="24" color="#ec4899"><Timer /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">运行时间</div>
            <div class="stat-value">{{ metrics ? formatUptime(metrics.uptime) : '--' }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="section-card">
      <template #header>
        <span class="section-title">系统资源监控</span>
      </template>
      <SystemMetricsComponent :metrics="metrics" />
    </el-card>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1200px;
}

.stat-row {
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  cursor: default;
}

:deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: rgba(124, 58, 237, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f0f0f;
}

:root.dark .stat-value {
  color: #f5f5f5;
}

.section-card {
  margin-bottom: 24px;
}

.section-title {
  font-weight: 600;
  font-size: 15px;
}
</style>

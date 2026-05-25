<script setup lang="ts">
import type { SystemMetrics } from '@/api/admin'

defineProps<{
  metrics: SystemMetrics | null
}>()

function formatMemory(mb: number): string {
  if (mb >= 1024) {
    return (mb / 1024).toFixed(2) + ' GB'
  }
  return mb + ' MB'
}

function formatUptime(seconds: number): string {
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (days > 0) return `${days}天 ${hours}小时 ${minutes}分钟`
  if (hours > 0) return `${hours}小时 ${minutes}分钟`
  return `${minutes}分钟`
}

function getProgressColor(value: number): string {
  if (value > 80) return '#ef4444'
  if (value > 60) return '#f59e0b'
  return '#7c3aed'
}
</script>

<template>
  <el-row :gutter="16">
    <el-col :span="12">
      <el-card shadow="never" class="metric-card">
        <template #header>
          <div class="metric-header">
            <span>CPU 使用率</span>
            <span class="metric-value">{{ metrics ? metrics.cpuUsage.toFixed(1) : '--' }}%</span>
          </div>
        </template>
        <el-progress
          :percentage="metrics ? metrics.cpuUsage : 0"
          :color="getProgressColor(metrics?.cpuUsage ?? 0)"
          :stroke-width="10"
        />
        <div class="metric-footer">
          <span>核心数: {{ metrics?.cpuCores ?? '--' }}</span>
        </div>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card shadow="never" class="metric-card">
        <template #header>
          <div class="metric-header">
            <span>内存使用率</span>
            <span class="metric-value">{{ metrics ? metrics.memoryUsage.toFixed(1) : '--' }}%</span>
          </div>
        </template>
        <el-progress
          :percentage="metrics ? metrics.memoryUsage : 0"
          :color="getProgressColor(metrics?.memoryUsage ?? 0)"
          :stroke-width="10"
        />
        <div class="metric-footer">
          <span>已用: {{ metrics ? formatMemory(metrics.usedMemory) : '--' }}</span>
          <span>总计: {{ metrics ? formatMemory(metrics.totalMemory) : '--' }}</span>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped>
.metric-card {
  height: 100%;
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.metric-value {
  font-size: 20px;
  font-weight: 600;
  color: #7c3aed;
}

.metric-footer {
  margin-top: 12px;
  font-size: 12px;
  color: #6b7280;
  display: flex;
  justify-content: space-between;
}
</style>

<script setup lang="ts">
import type { AdminUser } from '@/api/admin'

defineProps<{
  users: AdminUser[]
  onlineUserIds: Set<string>
}>()

const emit = defineEmits<{
  rename: [user: AdminUser]
  ban: [user: AdminUser]
  unban: [user: AdminUser]
}>()

function formatTimestamp(timestamp: number): string {
  if (!timestamp) return '--'
  return new Date(timestamp).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<template>
  <el-table :data="users" stripe style="width: 100%" max-height="600">
    <el-table-column label="用户名" min-width="140">
      <template #default="{ row }">
        <span class="username">{{ row.username }}</span>
      </template>
    </el-table-column>

    <el-table-column label="状态" width="150">
      <template #default="{ row }">
        <div class="status-tags">
          <el-tag
            :type="onlineUserIds.has(row.userId) ? 'success' : 'info'"
            size="small"
          >
            {{ onlineUserIds.has(row.userId) ? '在线' : '离线' }}
          </el-tag>
          <el-tag v-if="row.banned" type="danger" size="small">已封禁</el-tag>
        </div>
      </template>
    </el-table-column>

    <el-table-column label="创建时间" width="140">
      <template #default="{ row }">
        <span class="time-text">{{ formatTimestamp(row.createdAt) }}</span>
      </template>
    </el-table-column>

    <el-table-column label="最后活动" width="140">
      <template #default="{ row }">
        <span class="time-text">{{ formatTimestamp(row.lastSeen) }}</span>
      </template>
    </el-table-column>

    <el-table-column label="操作" width="200" fixed="right">
      <template #default="{ row }">
        <el-button size="small" @click="emit('rename', row)">改名</el-button>
        <el-button
          v-if="!row.banned"
          type="danger"
          size="small"
          @click="emit('ban', row)"
        >
          封禁
        </el-button>
        <el-button
          v-else
          type="success"
          size="small"
          @click="emit('unban', row)"
        >
          解封
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.username {
  font-weight: 500;
  color: #0f0f0f;
}

:root.dark .username {
  color: #f5f5f5;
}

.status-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.time-text {
  font-size: 13px;
  color: #6b7280;
}
</style>

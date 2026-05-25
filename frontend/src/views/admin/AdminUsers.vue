<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  getUsers,
  getOnlineUsers,
  renameUser,
  banUser,
  unbanUser,
  type AdminUser,
} from '@/api/admin'
import UserTable from '@/components/admin/UserTable.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'

const users = ref<AdminUser[]>([])
const onlineUserIds = ref<Set<string>>(new Set())
const searchQuery = ref('')
const loading = ref(false)

const filteredUsers = computed(() => {
  let list = [...users.value]
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(u => u.username.toLowerCase().includes(q))
  }
  return list.sort((a, b) => {
    const aOnline = onlineUserIds.value.has(a.userId)
    const bOnline = onlineUserIds.value.has(b.userId)
    if (aOnline !== bOnline) return aOnline ? -1 : 1
    if (a.banned !== b.banned) return a.banned ? 1 : -1
    return b.createdAt - a.createdAt
  })
})

async function refreshData() {
  loading.value = true
  try {
    const [u, o] = await Promise.all([getUsers(), getOnlineUsers()])
    users.value = u
    onlineUserIds.value = new Set((o.users || []).map(user => user.userId))
  } catch {
    ElMessage.error('加载用户数据失败')
  } finally {
    loading.value = false
  }
}

async function handleRename(user: AdminUser) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新用户名', '修改用户名', {
      inputValue: user.username,
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputValidator: (val) => {
        if (!val || !val.trim()) return '用户名不能为空'
        return true
      }
    })
    await renameUser(user.userId, value.trim())
    ElMessage.success('用户名已修改')
    await refreshData()
  } catch {
    // user cancelled or error
  }
}

async function handleBan(user: AdminUser) {
  try {
    const { value } = await ElMessageBox.prompt('请输入封禁原因（可选）', '封禁用户', {
      confirmButtonText: '确认封禁',
      cancelButtonText: '取消',
      inputPlaceholder: '封禁原因',
      inputType: 'textarea',
      type: 'warning',
    })
    await banUser(user.userId, (value || '').trim())
    ElMessage.success(`已封禁 ${user.username}`)
    await refreshData()
  } catch {
    // user cancelled or error
  }
}

async function handleUnban(user: AdminUser) {
  try {
    await ElMessageBox.confirm(`确认解封 ${user.username}？`, '解封用户', {
      confirmButtonText: '确认解封',
      cancelButtonText: '取消',
      type: 'success',
    })
    await unbanUser(user.userId)
    ElMessage.success(`已解封 ${user.username}`)
    await refreshData()
  } catch {
    // user cancelled or error
  }
}

onMounted(() => {
  refreshData()
})
</script>

<template>
  <div class="users-page">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span class="page-title">用户管理</span>
          <div class="page-actions">
            <el-input
              v-model="searchQuery"
              placeholder="搜索用户名"
              :prefix-icon="Search"
              clearable
              style="width: 220px;"
            />
            <el-button :icon="Refresh" @click="refreshData" :loading="loading">刷新</el-button>
          </div>
        </div>
      </template>

      <UserTable
        :users="filteredUsers"
        :online-user-ids="onlineUserIds"
        @rename="handleRename"
        @ban="handleBan"
        @unban="handleUnban"
      />
    </el-card>
  </div>
</template>

<style scoped>
.users-page {
  max-width: 1200px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-weight: 600;
  font-size: 15px;
}

.page-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>

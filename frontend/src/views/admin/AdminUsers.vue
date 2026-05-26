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
import { ElMessage } from 'element-plus'
import { Search, Refresh, EditPen, CircleCheck, WarningFilled } from '@element-plus/icons-vue'

const users = ref<AdminUser[]>([])
const onlineUserIds = ref<Set<string>>(new Set())
const searchQuery = ref('')
const loading = ref(false)

// 改名弹窗
const renameDialogVisible = ref(false)
const renameTarget = ref<AdminUser | null>(null)
const renameValue = ref('')
const renameSubmitting = ref(false)

// 封禁弹窗
const banDialogVisible = ref(false)
const banTarget = ref<AdminUser | null>(null)
const banReason = ref('')
const banSubmitting = ref(false)

// 解封弹窗
const unbanDialogVisible = ref(false)
const unbanTarget = ref<AdminUser | null>(null)
const unbanSubmitting = ref(false)

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

// 改名
function openRenameDialog(user: AdminUser) {
  renameTarget.value = user
  renameValue.value = user.username
  renameDialogVisible.value = true
}

async function confirmRename() {
  if (!renameTarget.value) return
  const name = renameValue.value.trim()
  if (!name) {
    ElMessage.warning('用户名不能为空')
    return
  }
  renameSubmitting.value = true
  try {
    await renameUser(renameTarget.value.userId, name)
    ElMessage.success('用户名已修改')
    renameDialogVisible.value = false
    await refreshData()
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '修改失败')
  } finally {
    renameSubmitting.value = false
  }
}

// 封禁
function openBanDialog(user: AdminUser) {
  banTarget.value = user
  banReason.value = ''
  banDialogVisible.value = true
}

async function confirmBan() {
  if (!banTarget.value) return
  banSubmitting.value = true
  try {
    await banUser(banTarget.value.userId, banReason.value.trim())
    ElMessage.success(`已封禁 ${banTarget.value.username}`)
    banDialogVisible.value = false
    await refreshData()
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '封禁失败')
  } finally {
    banSubmitting.value = false
  }
}

// 解封
function openUnbanDialog(user: AdminUser) {
  unbanTarget.value = user
  unbanDialogVisible.value = true
}

async function confirmUnban() {
  if (!unbanTarget.value) return
  unbanSubmitting.value = true
  try {
    await unbanUser(unbanTarget.value.userId)
    ElMessage.success(`已解封 ${unbanTarget.value.username}`)
    unbanDialogVisible.value = false
    await refreshData()
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '解封失败')
  } finally {
    unbanSubmitting.value = false
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
        @rename="openRenameDialog"
        @ban="openBanDialog"
        @unban="openUnbanDialog"
      />
    </el-card>

    <!-- 改名弹窗 -->
    <el-dialog
      v-model="renameDialogVisible"
      title=""
      width="420px"
      :close-on-click-modal="false"
      align-center
      class="custom-dialog"
    >
      <div class="dialog-header">
        <div class="dialog-icon rename-icon">
          <el-icon :size="24"><EditPen /></el-icon>
        </div>
        <h3 class="dialog-title">修改用户名</h3>
        <p class="dialog-subtitle">当前用户名：<strong>{{ renameTarget?.username }}</strong></p>
      </div>
      <div class="dialog-body">
        <label class="input-label">新用户名</label>
        <el-input
          v-model="renameValue"
          placeholder="请输入新的用户名"
          size="large"
          clearable
          @keyup.enter="confirmRename"
        />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="renameDialogVisible = false" size="large">取消</el-button>
          <el-button type="primary" @click="confirmRename" :loading="renameSubmitting" size="large">
            确认修改
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 封禁弹窗 -->
    <el-dialog
      v-model="banDialogVisible"
      title=""
      width="460px"
      :close-on-click-modal="false"
      align-center
      class="custom-dialog"
    >
      <div class="dialog-header">
        <div class="dialog-icon ban-icon">
          <el-icon :size="24"><WarningFilled /></el-icon>
        </div>
        <h3 class="dialog-title">封禁用户</h3>
        <p class="dialog-subtitle">即将封禁用户 <strong>{{ banTarget?.username }}</strong>，封禁后该用户将无法登录和使用聊天功能。</p>
      </div>
      <div class="dialog-body">
        <label class="input-label">封禁原因（可选）</label>
        <el-input
          v-model="banReason"
          type="textarea"
          :rows="3"
          placeholder="填写封禁原因，将展示给被封禁用户"
          resize="none"
        />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="banDialogVisible = false" size="large">取消</el-button>
          <el-button type="danger" @click="confirmBan" :loading="banSubmitting" size="large">
            确认封禁
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 解封弹窗 -->
    <el-dialog
      v-model="unbanDialogVisible"
      title=""
      width="400px"
      :close-on-click-modal="false"
      align-center
      class="custom-dialog"
    >
      <div class="dialog-header">
        <div class="dialog-icon unban-icon">
          <el-icon :size="24"><CircleCheck /></el-icon>
        </div>
        <h3 class="dialog-title">解封用户</h3>
        <p class="dialog-subtitle">确认解封用户 <strong>{{ unbanTarget?.username }}</strong>？解封后该用户可正常登录和使用。</p>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="unbanDialogVisible = false" size="large">取消</el-button>
          <el-button type="success" @click="confirmUnban" :loading="unbanSubmitting" size="large">
            确认解封
          </el-button>
        </div>
      </template>
    </el-dialog>
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

.dialog-header {
  text-align: center;
  margin-bottom: 24px;
}

.dialog-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.rename-icon {
  background: rgba(124, 58, 237, 0.1);
  color: #7c3aed;
}

.ban-icon {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.unban-icon {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.dialog-title {
  font-size: 18px;
  font-weight: 600;
  color: #0f0f0f;
  margin-bottom: 8px;
}

:root.dark .dialog-title {
  color: #f5f5f5;
}

.dialog-subtitle {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
}

.dialog-body {
  margin-bottom: 8px;
}

.input-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
}

:root.dark .input-label {
  color: #d1d5db;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

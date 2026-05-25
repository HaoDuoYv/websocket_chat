<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { adminLogout, getAdminSession } from '@/api/admin'
import {
  Odometer,
  User,
  Setting,
  Document,
  Sunny,
  Moon,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const isCollapse = ref(false)
const isDarkTheme = ref(localStorage.getItem('admin-theme') === 'dark')
const adminUsername = ref('')

const activeMenu = computed(() => route.path)

const breadcrumbMap: Record<string, string> = {
  '/admin/dashboard': '仪表盘',
  '/admin/users': '用户管理',
  '/admin/ai': 'AI 助手配置',
  '/admin/logs': '系统日志',
}

const currentBreadcrumb = computed(() => breadcrumbMap[route.path] || '管理后台')

function toggleTheme() {
  isDarkTheme.value = !isDarkTheme.value
  localStorage.setItem('admin-theme', isDarkTheme.value ? 'dark' : 'light')
  document.documentElement.classList.toggle('dark', isDarkTheme.value)
}

async function handleLogout() {
  try {
    await adminLogout()
  } catch {
    // ignore
  }
  router.push('/admin/login')
}

onMounted(async () => {
  document.documentElement.classList.toggle('dark', isDarkTheme.value)
  try {
    const session = await getAdminSession()
    if (session.loggedIn) {
      adminUsername.value = session.username
    }
  } catch {
    // ignore
  }
})
</script>

<template>
  <el-container class="admin-layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="admin-aside">
      <div class="admin-logo" @click="router.push('/admin/dashboard')">
        <span class="logo-icon">💬</span>
        <span v-show="!isCollapse" class="logo-text">管理后台</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        class="admin-menu"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/ai">
          <el-icon><Setting /></el-icon>
          <template #title>AI 助手配置</template>
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <el-icon><Document /></el-icon>
          <template #title>系统日志</template>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <el-button
          :icon="isCollapse ? 'ArrowRight' : 'ArrowLeft'"
          text
          size="small"
          @click="isCollapse = !isCollapse"
          class="collapse-btn"
        />
      </div>
    </el-aside>

    <el-container class="main-container">
      <el-header class="admin-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentBreadcrumb }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-button
            :icon="isDarkTheme ? Moon : Sunny"
            circle
            size="small"
            @click="toggleTheme"
          />
          <el-dropdown trigger="click">
            <span class="admin-user-info">
              {{ adminUsername || 'admin' }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

.admin-aside {
  background: #0f0f0f;
  border-right: 1px solid #1f1f1f;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  overflow: hidden;
}

.admin-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid #1f1f1f;
  cursor: pointer;
  flex-shrink: 0;
  padding: 0 16px;
}

.logo-icon {
  font-size: 22px;
  flex-shrink: 0;
}

.logo-text {
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.admin-menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 8px;
}

.admin-menu:not(.el-menu--collapse) {
  width: 208px;
}

:deep(.el-menu-item) {
  color: #6b7280;
  border-radius: 6px;
  margin-bottom: 4px;
  height: 44px;
}

:deep(.el-menu-item:hover) {
  background: rgba(124, 58, 237, 0.1);
  color: #c4b5fd;
}

:deep(.el-menu-item.is-active) {
  background: rgba(124, 58, 237, 0.2);
  color: #c4b5fd;
}

.sidebar-footer {
  padding: 8px;
  border-top: 1px solid #1f1f1f;
  display: flex;
  justify-content: center;
}

.collapse-btn {
  color: #6b7280;
}

.main-container {
  background: #f5f5f5;
  overflow: hidden;
}

:root.dark .main-container {
  background: #18181b;
}

.admin-header {
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
}

:root.dark .admin-header {
  background: #1f1f23;
  border-bottom-color: #27272a;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.admin-user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-size: 14px;
  color: #374151;
}

:root.dark .admin-user-info {
  color: #d1d5db;
}

.admin-main {
  overflow-y: auto;
  padding: 24px;
}
</style>

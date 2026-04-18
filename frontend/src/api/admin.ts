import axios from 'axios'

const API_BASE_URL = '/api/admin'

export interface SystemMetrics {
  cpuUsage: number
  cpuCores: number
  totalMemory: number
  usedMemory: number
  freeMemory: number
  memoryUsage: number
  jvmTotalMemory: number
  jvmUsedMemory: number
  jvmFreeMemory: number
  jvmMemoryUsage: number
  uptime: number
  timestamp: number
}

export interface LogLine {
  lineNumber: number
  content: string
  level: string
  timestamp: string
}

export interface AdminSession {
  loggedIn: boolean
  username: string
}

export interface AdminUser {
  userId: string
  username: string
  createdAt: number
  lastSeen: number
  banned: boolean
  bannedAt: number
  bannedReason: string
}

const request = axios.create({
  withCredentials: true
})

export const adminLogin = async (username: string, password: string): Promise<{ message: string; username: string }> => {
  const response = await request.post(`${API_BASE_URL}/login`, { username, password })
  return response.data
}

export const adminLogout = async (): Promise<{ message: string }> => {
  const response = await request.post(`${API_BASE_URL}/logout`)
  return response.data
}

export const getAdminSession = async (): Promise<AdminSession> => {
  const response = await request.get(`${API_BASE_URL}/session`)
  return response.data
}

export const getOnlineUsers = async (): Promise<{ users: Array<{ userId: string; username: string }> }> => {
  const response = await request.get(`${API_BASE_URL}/online-users`)
  return response.data
}

export const getUsers = async (): Promise<AdminUser[]> => {
  const response = await request.get(`${API_BASE_URL}/users`)
  return response.data
}

export const renameUser = async (userId: string, username: string): Promise<{ message: string }> => {
  const response = await request.put(`${API_BASE_URL}/users/${userId}/username`, { username })
  return response.data
}

export const banUser = async (userId: string, reason: string): Promise<{ message: string }> => {
  const response = await request.post(`${API_BASE_URL}/users/${userId}/ban`, { reason })
  return response.data
}

export const unbanUser = async (userId: string): Promise<{ message: string }> => {
  const response = await request.post(`${API_BASE_URL}/users/${userId}/unban`)
  return response.data
}

export const getSystemMetrics = async (): Promise<SystemMetrics> => {
  const response = await request.get(`${API_BASE_URL}/metrics`)
  return response.data
}

export const getRecentLogs = async (limit: number = 100): Promise<LogLine[]> => {
  const response = await request.get(`${API_BASE_URL}/logs?limit=${limit}`)
  return response.data
}

export const getAllLogs = async (): Promise<LogLine[]> => {
  const response = await request.get(`${API_BASE_URL}/logs/all`)
  return response.data
}

export const clearLogs = async (): Promise<void> => {
  await request.post(`${API_BASE_URL}/logs/clear`)
}

export const healthCheck = async (): Promise<{ status: string; timestamp: number }> => {
  const response = await request.get(`${API_BASE_URL}/health`)
  return response.data
}

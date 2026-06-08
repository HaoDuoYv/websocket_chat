/**
 * 动态服务器地址配置
 * 在 Electron 桌面端中，API 地址指向用户选择的服务器
 * 在 Web 端中，使用相对路径（由代理或同源处理）
 */

let serverBaseUrl = '' // 空字符串 = 相对路径（Web 模式）

export function setServerBaseUrl(url: string) {
  serverBaseUrl = url.replace(/\/+$/, '') // 去掉尾部斜杠
}

export function getServerBaseUrl(): string {
  return serverBaseUrl
}

/**
 * 获取完整的 API URL
 * Web 模式: "/api/users" → "/api/users"
 * 桌面模式: "/api/users" → "http://192.168.1.5:8081/api/users"
 */
export function getApiUrl(path: string): string {
  if (!serverBaseUrl) return path
  return `${serverBaseUrl}${path}`
}

/**
 * 获取完整的 WebSocket URL
 * Web 模式: "/ws/chat" → "ws://host/ws/chat"
 * 桌面模式: "/ws/chat" → "ws://192.168.1.5:8081/ws/chat"
 */
export function getWsUrl(path: string): string {
  if (!serverBaseUrl) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}${path}`
  }
  const wsBase = serverBaseUrl.replace(/^http/, 'ws')
  return `${wsBase}${path}`
}

/**
 * 获取文件访问 URL
 */
export function getFileUrl(path: string): string {
  if (!serverBaseUrl) return path
  return `${serverBaseUrl}${path}`
}

/**
 * 判断是否在桌面端（Electron）环境
 */
export function isElectron(): boolean {
  return !!(window as any).electronAPI
}

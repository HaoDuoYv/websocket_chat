/**
 * 与 Electron 主进程通信，获取局域网发现的服务器列表
 */

export interface DiscoveredServer {
  alias: string
  ip: string
  port: number
  protocol: string
  userCount: number
  version: string
}

/**
 * 获取局域网内发现的服务器列表
 * 仅在 Electron 环境下可用，Web 环境返回空数组
 */
export async function getDiscoveredServers(): Promise<DiscoveredServer[]> {
  const electronAPI = (window as any).electronAPI
  if (!electronAPI) return []
  return electronAPI.getDiscoveredServers()
}

/**
 * 启动本地 Spring Boot 服务器
 */
export async function startLocalServer(): Promise<{ success: boolean; port?: number; error?: string }> {
  const electronAPI = (window as any).electronAPI
  if (!electronAPI) return { success: false, error: 'Not in Electron environment' }
  return electronAPI.startLocalServer()
}

/**
 * 停止本地 Spring Boot 服务器
 */
export async function stopLocalServer(): Promise<void> {
  const electronAPI = (window as any).electronAPI
  if (electronAPI) {
    await electronAPI.stopLocalServer()
  }
}

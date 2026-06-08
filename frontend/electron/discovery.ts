export interface DiscoveredServer {
  alias: string; ip: string; port: number; protocol: string; userCount: number; version: string; lastSeen: number
}
export function getDiscoveredServers(): DiscoveredServer[] { return [] }
export function startDiscovery(): void {}
export function stopDiscovery(): void {}

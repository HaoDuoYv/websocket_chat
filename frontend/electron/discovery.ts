import dgram from 'dgram'
import { Socket } from 'dgram'

const MULTICAST_GROUP = '224.0.0.167'
const MULTICAST_PORT = 9999

export interface DiscoveredServer {
  alias: string
  ip: string
  port: number
  protocol: string
  userCount: number
  version: string
  lastSeen: number
}

const discoveredServers = new Map<string, DiscoveredServer>()
let socket: Socket | null = null

export function getDiscoveredServers(): DiscoveredServer[] {
  const now = Date.now()
  for (const [key, server] of discoveredServers) {
    if (now - server.lastSeen > 60000) {
      discoveredServers.delete(key)
    }
  }
  return Array.from(discoveredServers.values())
}

export function startDiscovery(): void {
  try {
    socket = dgram.createSocket({ type: 'udp4', reuseAddr: true })

    socket.on('message', (msg, rinfo) => {
      try {
        const data = JSON.parse(msg.toString())

        if (data.type === 'discovery-announce' || data.type === 'discovery-response') {
          const key = `${data.ip}:${data.port}`
          discoveredServers.set(key, {
            alias: data.alias || key,
            ip: data.ip,
            port: data.port,
            protocol: data.protocol || 'http',
            userCount: data.userCount || 0,
            version: data.version || '1.0',
            lastSeen: Date.now(),
          })
        }
      } catch {
        // ignore malformed messages
      }
    })

    socket.on('error', (err) => {
      console.error('Discovery socket error:', err)
    })

    socket.bind(MULTICAST_PORT, () => {
      socket!.addMembership(MULTICAST_GROUP)
      console.log('Discovery: listening on multicast', MULTICAST_GROUP, MULTICAST_PORT)
      sendDiscoveryRequest()
    })
  } catch (err) {
    console.error('Failed to start discovery:', err)
  }
}

export function stopDiscovery(): void {
  if (socket) {
    try {
      socket.dropMembership(MULTICAST_GROUP)
      socket.close()
    } catch {}
    socket = null
  }
}

function sendDiscoveryRequest(): void {
  if (!socket) return

  const message = JSON.stringify({
    type: 'discovery-request',
    version: '1.0',
  })

  const buf = Buffer.from(message)
  socket.send(buf, 0, buf.length, MULTICAST_PORT, MULTICAST_GROUP)
}

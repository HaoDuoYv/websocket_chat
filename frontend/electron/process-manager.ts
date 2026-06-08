import { spawn, ChildProcess } from 'child_process'
import path from 'path'
import fs from 'fs'
import { app } from 'electron'

let jarProcess: ChildProcess | null = null
const SERVER_PORT = 8081

function getJarPath(): string {
  if (process.env.NODE_ENV === 'development') {
    return path.join(__dirname, '../../../backend/target/websocketChatServer.jar')
  }
  return path.join(process.resourcesPath, 'backend/websocketChatServer.jar')
}

function getJrePath(): string {
  if (process.env.NODE_ENV === 'development') {
    return 'java'
  }
  return path.join(process.resourcesPath, 'jre/bin/java')
}

function getDataDir(): string {
  return path.join(app.getPath('appData'), 'websocket-chat')
}

export function getServerPort(): number {
  return SERVER_PORT
}

export function startServer(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (jarProcess) {
      resolve()
      return
    }

    const jarPath = getJarPath()
    const javaCmd = getJrePath()
    const dataDir = getDataDir()

    // Ensure data directories exist
    const dirs = ['data', 'uploads', 'logs']
    for (const dir of dirs) {
      const fullPath = path.join(dataDir, dir)
      if (!fs.existsSync(fullPath)) {
        fs.mkdirSync(fullPath, { recursive: true })
      }
    }

    const args = [
      '-jar', jarPath,
      `--server.port=${SERVER_PORT}`,
      `--spring.datasource.url=jdbc:sqlite:${path.join(dataDir, 'data', 'chat.db')}`,
      `--local.local-url=${path.join(dataDir, 'uploads')}`,
      `--logging.file.name=${path.join(dataDir, 'logs', 'application.log')}`,
    ]

    jarProcess = spawn(javaCmd, args, {
      cwd: dataDir,
      stdio: ['ignore', 'pipe', 'pipe'],
    })

    let resolved = false

    jarProcess.stdout?.on('data', (data: Buffer) => {
      const msg = data.toString()
      console.log('[JAR]', msg)
      if (!resolved && msg.includes('Started ChatApplication')) {
        resolved = true
        resolve()
      }
    })

    jarProcess.stderr?.on('data', (data: Buffer) => {
      console.error('[JAR ERR]', data.toString())
    })

    jarProcess.on('error', (err) => {
      console.error('Failed to start JAR:', err)
      jarProcess = null
      if (!resolved) {
        resolved = true
        reject(err)
      }
    })

    jarProcess.on('exit', (code) => {
      console.log(`JAR process exited with code ${code}`)
      jarProcess = null
    })

    // Timeout fallback
    setTimeout(() => {
      if (!resolved && jarProcess) {
        resolved = true
        resolve()
      }
    }, 30000)
  })
}

export function stopServer(): Promise<void> {
  return new Promise((resolve) => {
    if (!jarProcess) {
      resolve()
      return
    }

    jarProcess.on('exit', () => {
      jarProcess = null
      resolve()
    })

    jarProcess.kill('SIGTERM')

    setTimeout(() => {
      if (jarProcess) {
        jarProcess.kill('SIGKILL')
        jarProcess = null
      }
      resolve()
    }, 5000)
  })
}

export function isServerRunning(): boolean {
  return jarProcess !== null
}

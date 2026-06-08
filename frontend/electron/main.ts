import { app, ipcMain, BrowserWindow } from 'electron'
import { createWindow, createTray } from './window-manager'
import { startServer, stopServer, getServerPort } from './process-manager'
import { getDiscoveredServers, startDiscovery, stopDiscovery } from './discovery'
import path from 'path'
import fs from 'fs'

const configPath = path.join(app.getPath('userData'), 'config.json')

function readConfig(): Record<string, unknown> {
  try {
    return JSON.parse(fs.readFileSync(configPath, 'utf-8'))
  } catch {
    return {}
  }
}

function writeConfig(data: Record<string, unknown>): void {
  fs.writeFileSync(configPath, JSON.stringify(data, null, 2), 'utf-8')
}

app.whenReady().then(async () => {
  createWindow()
  createTray()
  startDiscovery()

  ipcMain.handle('discovery:get-servers', () => {
    return getDiscoveredServers()
  })

  ipcMain.handle('server:start', async () => {
    try {
      await startServer()
      return { success: true, port: getServerPort() }
    } catch (e: any) {
      return { success: false, error: e.message }
    }
  })

  ipcMain.handle('server:stop', async () => {
    await stopServer()
  })

  ipcMain.handle('config:get-close-behavior', () => {
    const config = readConfig()
    return config.closeBehavior ?? null
  })

  ipcMain.handle('config:set-close-behavior', (_event, behavior: string) => {
    writeConfig({ ...readConfig(), closeBehavior: behavior })
  })

  ipcMain.handle('window:minimize-to-tray', () => {})

  ipcMain.handle('window:close', () => {
    app.quit()
  })
})

app.on('window-all-closed', () => {
  stopServer()
  stopDiscovery()
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow()
  }
})

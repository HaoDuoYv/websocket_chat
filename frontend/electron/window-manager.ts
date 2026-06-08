import { BrowserWindow, Tray, Menu, nativeImage, app, dialog } from 'electron'
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

let mainWindow: BrowserWindow | null = null
let tray: Tray | null = null

export function createWindow(): BrowserWindow {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    minWidth: 800,
    minHeight: 600,
    title: 'WebSocket Chat',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  })

  if (process.env.NODE_ENV === 'development') {
    mainWindow.loadURL('http://localhost:3000')
    mainWindow.webContents.openDevTools()
  } else {
    mainWindow.loadFile(path.join(__dirname, '../frontend/dist/index.html'))
  }

  mainWindow.on('close', async (e) => {
    e.preventDefault()

    const config = readConfig()
    const behavior = config.closeBehavior as string | undefined

    if (!behavior) {
      const result = await dialog.showMessageBox(mainWindow!, {
        type: 'question',
        buttons: ['最小化到托盘', '退出应用'],
        defaultId: 0,
        title: '关闭确认',
        message: '您希望如何关闭应用？',
      })

      const chosen = result.response === 0 ? 'minimize' : 'quit'
      writeConfig({ ...readConfig(), closeBehavior: chosen })

      if (chosen === 'minimize') {
        mainWindow!.hide()
      } else {
        mainWindow = null
        app.quit()
      }
    } else if (behavior === 'minimize') {
      mainWindow!.hide()
    } else {
      mainWindow = null
      app.quit()
    }
  })

  return mainWindow
}

export function createTray(): void {
  const icon = nativeImage.createEmpty()
  tray = new Tray(icon)

  const contextMenu = Menu.buildFromTemplate([
    {
      label: '显示窗口',
      click: () => {
        mainWindow?.show()
        mainWindow?.focus()
      },
    },
    { type: 'separator' },
    {
      label: '退出',
      click: () => {
        mainWindow = null
        app.quit()
      },
    },
  ])

  tray.setToolTip('WebSocket Chat')
  tray.setContextMenu(contextMenu)

  tray.on('double-click', () => {
    mainWindow?.show()
    mainWindow?.focus()
  })
}

export function getMainWindow(): BrowserWindow | null {
  return mainWindow
}

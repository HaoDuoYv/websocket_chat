import { contextBridge, ipcRenderer } from 'electron'

contextBridge.exposeInMainWorld('electronAPI', {
  getDiscoveredServers: () => ipcRenderer.invoke('discovery:get-servers'),
  startLocalServer: () => ipcRenderer.invoke('server:start'),
  stopLocalServer: () => ipcRenderer.invoke('server:stop'),
  minimizeToTray: () => ipcRenderer.invoke('window:minimize-to-tray'),
  closeApp: () => ipcRenderer.invoke('window:close'),
  getCloseBehavior: () => ipcRenderer.invoke('config:get-close-behavior'),
  setCloseBehavior: (behavior: string) => ipcRenderer.invoke('config:set-close-behavior', behavior),
})

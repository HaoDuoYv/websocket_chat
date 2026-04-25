export interface AppConfig {
  id: string
  go: string
  img: string
  title: string
  subtitle: string
  text: string
  url: string
  show: boolean
  techStack?: string[]
  usageScenario?: string
  description?: string
  status?: 'active' | 'developing' | 'planned'
}

export const appsConfig: AppConfig[] = [


  {
    id: 'gomoku',
    go: '♟️ 前往',
    img: '/img/gomoku.png',
    title: '五子棋对战',
    subtitle: '2,000 miles of wonder',
    text: '基于WebSocket的实时五子棋对战',
    url: '/gomoku',
    show: false,
    techStack: ['Vue 3', 'WebSocket', 'Spring Boot'],
    usageScenario: '实时对战与观战',
    description: '基于WebSocket的实时五子棋对战游戏，支持创建房间、密码保护、观战、房间聊天、悔棋和断线重连。',
    status: 'active'
  }

]

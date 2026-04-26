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
  descriptionMd?: string
}

export const appsConfig: AppConfig[] = [


  {
    id: 'gomoku',
    go: '♟️ 前往',
    img: '/img/gomoku.svg',
    title: '五子棋对战',
    subtitle: '霸王龙小时候吵着要玩',
    text: '基于WebSocket的实时五子棋对战',
    url: '/gomoku',
    show: false,
    techStack: ['Vue 3', 'WebSocket', 'Spring Boot'],
    usageScenario: '实时对战与观战',
    descriptionMd: '/md/gomoku.md'
  },
  {
    id: 'play-cs',
    go: '🎮 前往',
    img: '/img/play-cs.svg',
    title: 'PLAY-CS 1.6',
    subtitle: '浏览器直接开打，无需注册下载',
    text: '网页版 CS 1.6 多人在线对战游戏',
    url: 'https://play-cs.com/zh/servers',
    show: true,
    techStack: ['WebAssembly', 'WebGL', 'Browser'],
    usageScenario: '休闲射击与怀旧对战',
    descriptionMd: '/md/play-cs.md'
  },
  {
    id: 'algorithm-visualizer',
    go: '🔍 前往',
    img: '/img/algorithm-visualizer.svg',
    title: 'Algorithm Visualizer',
    subtitle: '可视化算法，让代码动起来',
    text: '交互式算法可视化平台，涵盖回溯、动态规划、贪心等主流算法',
    url: 'https://algorithm-visualizer.org/',
    show: true,
    techStack: ['React', 'Redux', 'Node.js'],
    usageScenario: '算法学习与教学演示',
    descriptionMd: '/md/algorithm-visualizer.md'
  }

]

# WebSocket 即时聊天系统

<p align="center">
  <img src="https://img.shields.io/badge/Vue-3-4FC08D?style=flat-square&logo=vue.js&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?style=flat-square&logo=spring-boot&logoColor=white" />
  <img src="https://img.shields.io/badge/WebSocket-实时通信-blue?style=flat-square" />
  <img src="https://img.shields.io/badge/SQLite-轻量数据库-003B57?style=flat-square&logo=sqlite&logoColor=white" />
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" />
</p>

<p align="center">
  <b>一个基于 WebSocket 的实时聊天应用，支持私聊、群聊、文件传输、表情包和管理员后台</b>
</p>

<p align="center">
  <a href="https://github.com/HaoDuoYv/websocket_chat">🔗 GitHub 项目地址</a>
</p>

---

## ✨ 功能亮点

### 💬 实时聊天
- **私聊** - 用户之间一对一实时聊天
- **群聊** - 创建公开群组，多人实时交流
- **消息类型** - 支持文本、文件和系统消息
- **表情包** - 内置 300+ 表情符号
- **用户备注** - 给其他用户设置仅自己可见的备注名

### 📎 文件传输
- 支持上传最大 **500MB** 的文件
- 文件自动以 UUID 命名存储，按日期组织目录
- 图片支持双击全屏预览
- 非图片文件提供下载功能

### 🎨 界面设计
- **双主题模式** - 支持亮色/暗色主题切换
- **响应式设计** - 适配桌面和移动设备
- **消息气泡** - 显示头像、用户名、发送时间
- **日期分隔** - 按日期分组显示消息

### 🛠️ 管理员后台
- **用户管理** - 查看用户列表、重命名、封禁/解封
- **系统监控** - 实时监控 CPU、内存、JVM 使用情况
- **日志查看** - 实时查看服务器日志
- **双重保护** - IP 白名单 + 会话认证

---


---



### 技术栈

| 层级 | 技术 |
|------|------|
| **前端** | Vue 3 + TypeScript + Vite + Tailwind CSS + Pinia + Vue Router |
| **后端** | Spring Boot 3.2 + Spring WebSocket + Spring Data JPA |
| **数据库** | SQLite |
| **部署** | Nginx 反向代理 |

---

## 📚 学习价值

本项目适合用于学习以下技术：

- **WebSocket 实时通信** - 基于 STOMP 协议的双向通信实现
- **Spring Boot 全栈开发** - 从配置到部署的完整实践
- **Vue 3 组合式 API** - 使用 `<script setup>` 和 Composables
- **前后端分离架构** - REST API + WebSocket 的协同设计
- **消息序号机制** - 保证消息顺序性和完整性
- **雪花 ID 生成** - 分布式 ID 生成算法实践

---

## ⚠️ 使用建议

> **本项目主要用于学习目的，建议在局域网环境或可信网络中使用。**

- 默认管理员账号密码为固定值，生产环境请修改
- 文件上传未做病毒扫描，请勿在公网开放上传功能
- 用户认证采用简单机制，不适合高安全要求场景

---

## 📂 项目结构

```
websocket_chat/
├── frontend/          # Vue 3 前端
│   ├── src/
│   │   ├── api/       # API 接口封装
│   │   ├── components/# Vue 组件
│   │   ├── composables/# 组合式函数
│   │   └── pages/     # 页面组件
│   └── ...
├── backend/           # Spring Boot 后端
│   └── src/main/java/com/chat/
│       ├── controller/  # 控制器
│       ├── service/     # 服务层
│       ├── entity/      # 实体类
│       ├── handler/     # WebSocket 处理器
│       └── ...
└── nginx.conf         # Nginx 配置
```

---

## 🤝 贡献

欢迎提交 Issue 和 PR！本项目仅供学习和参考使用。

---

<p align="center">
  Made with ❤️ for learning
</p>

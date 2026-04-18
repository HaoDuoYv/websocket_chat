# 前后端目录结构

## 前端目录结构

```
frontend/
├── public/                      # 静态资源
│   └── favicon.svg              # 网站图标
├── src/                         # 前端源码
│   ├── api/                     # API 接口
│   │   ├── admin.ts             # 管理员接口（登录、监控、日志、用户管理、封禁）
│   │   ├── file.ts              # 文件上传接口及工具函数
│   │   └── userRemark.ts        # 用户备注接口（获取、保存备注）
│   ├── assets/                  # 静态资源
│   │   └── vue.svg              # Vue 图标
│   ├── components/              # Vue 组件
│   │   ├── ConfirmDialog.vue    # 确认对话框
│   │   ├── CreateGroupDialog.vue # 创建群聊对话框
│   │   ├── FileMessage.vue      # 文件消息渲染组件
│   │   ├── FileUploadButton.vue # 文件上传按钮
│   │   ├── InviteUserDialog.vue # 邀请用户对话框
│   │   ├── LoginForm.vue        # 登录/注册表单
│   │   └── SetRemarkDialog.vue  # 设置用户备注对话框
│   ├── composables/             # 组合式函数
│   │   └── useWebSocket.ts      # WebSocket 客户端封装
│   ├── lib/                     # 工具库
│   │   └── utils.ts             # 工具函数（日期格式化、ID生成、文本截断）
│   ├── pages/                   # 页面组件
│   │   ├── AdminPage.vue        # 管理员监控页面
│   │   ├── ChatPage.vue         # 聊天页面（独立路由 /chat/:chatId）
│   │   └── HomePage.vue         # 首页（主聊天界面）
│   ├── router/                  # 路由配置
│   │   └── index.ts             # 路由定义
│   ├── App.vue                  # 根组件
│   ├── main.ts                  # 入口文件
│   ├── style.css                # 全局样式
│   └── vite-env.d.ts            # Vite 类型声明
├── index.html                   # HTML 模板
├── package.json                 # 项目配置
├── package-lock.json            # 依赖锁文件
├── postcss.config.js            # PostCSS 配置
├── tailwind.config.js           # Tailwind CSS 配置
├── tsconfig.json                # TypeScript 配置
├── tsconfig.node.json           # TypeScript Node 配置
└── vite.config.ts               # Vite 配置
```

## 后端目录结构

```
backend/
├── src/
│   └── main/
│       ├── java/com/chat/       # Java 源码
│       │   ├── config/          # 配置类
│       │   │   ├── AdminConfig.java          # 管理员配置属性（IP白名单、账号密码、日志路径）
│       │   │   ├── CorsConfig.java           # 全局 CORS 跨域配置
│       │   │   ├── FileUploadConfig.java     # 文件上传静态资源映射配置
│       │   │   ├── SnowflakeConfig.java      # 雪花 ID 生成器 Bean 配置
│       │   │   ├── WebMvcConfig.java         # MVC 配置（注册拦截器）
│       │   │   └── WebSocketConfig.java      # WebSocket 端点配置
│       │   ├── controller/      # 控制器层
│       │   │   ├── AdminController.java      # 管理员接口（健康检查、登录、监控、用户管理、封禁、日志）
│       │   │   ├── AuthController.java       # 认证接口（注册、登录）
│       │   │   ├── FileController.java       # 文件上传接口
│       │   │   ├── RoomController.java       # 房间管理接口
│       │   │   ├── UserController.java       # 在线用户接口
│       │   │   └── UserRemarkController.java # 用户备注接口
│       │   ├── entity/          # 实体类
│       │   │   ├── FileRecord.java           # 文件记录（非 JPA 实体，纯 POJO）
│       │   │   ├── Message.java              # 消息实体
│       │   │   ├── Room.java                 # 房间实体
│       │   │   ├── RoomMember.java           # 房间成员实体
│       │   │   ├── RoomMemberId.java         # 房间成员复合主键
│       │   │   ├── User.java                 # 用户实体（含封禁字段）
│       │   │   └── UserRemark.java           # 用户备注实体
│       │   ├── handler/         # 处理器
│       │   │   └── ChatWebSocketHandler.java # WebSocket 消息处理器
│       │   ├── interceptor/     # 拦截器
│       │   │   ├── AdminIpInterceptor.java      # IP 白名单拦截器
│       │   │   └── AdminSessionInterceptor.java # 管理员会话拦截器
│       │   ├── properties/      # 配置属性
│       │   │   └── LocalProperties.java   # 本地文件存储路径配置
│       │   ├── repository/      # 数据访问层
│       │   │   ├── MessageRepository.java     # 消息仓库
│       │   │   ├── RoomMemberRepository.java  # 房间成员仓库
│       │   │   ├── RoomRepository.java        # 房间仓库
│       │   │   ├── UserRemarkRepository.java  # 用户备注仓库
│       │   │   └── UserRepository.java        # 用户仓库
│       │   ├── service/         # 服务层
│       │   │   ├── AdminAuthService.java      # 管理员认证服务（登录、会话管理）
│       │   │   ├── FileUploadService.java     # 文件上传服务
│       │   │   ├── LogMonitorService.java     # 日志监控服务
│       │   │   ├── MessageService.java        # 消息服务
│       │   │   ├── MessageStorageService.java # 消息文件存储服务
│       │   │   ├── RoomService.java           # 房间服务
│       │   │   ├── SystemMonitorService.java  # 系统监控服务
│       │   │   ├── UserRemarkService.java     # 用户备注服务
│       │   │   └── UserService.java           # 用户服务
│       │   ├── utils/           # 工具类
│       │   │   ├── LocalUploadUtil.java       # 本地文件上传工具
│       │   │   └── SnowflakeIdGenerator.java  # 雪花 ID 生成器
│       │   ├── vo/              # 视图对象
│       │   │   ├── FileUploadResponse.java    # 文件上传响应 VO
│       │   │   ├── LogLineVO.java             # 日志行 VO
│       │   │   └── SystemMetricsVO.java       # 系统监控指标 VO
│       │   └── ChatApplication.java           # 应用入口
│       └── resources/           # 资源文件
│           └── application.properties       # 应用配置
├── data/                        # SQLite 数据库目录
│   └── chat.db                  # 数据库文件
├── uploads/                     # 文件上传目录
├── logs/                        # 日志目录
│   └── application.log          # 应用日志
└── pom.xml                      # Maven 配置
```

## 主要文件说明

### 前端核心文件

| 文件 | 说明 |
|------|------|
| **src/composables/useWebSocket.ts** | WebSocket 客户端实现，管理连接、房间状态、消息状态、未读计数和事件处理 |
| **src/pages/HomePage.vue** | 主聊天界面，处理登录状态、房间列表、联系人、消息 UI、文件上传、邀请流程和主题切换 |
| **src/pages/ChatPage.vue** | 独立聊天页面（/chat/:chatId），专注于消息渲染、上传和房间特定交互 |
| **src/pages/AdminPage.vue** | 管理员监控面板，支持登录认证、系统指标、用户管理（重命名/封禁/解封）、日志查看 |
| **src/components/FileMessage.vue** | 文件消息渲染组件，支持图片预览和文件下载 |
| **src/components/FileUploadButton.vue** | 文件上传按钮组件，发送 multipart 请求并报告进度 |
| **src/components/SetRemarkDialog.vue** | 设置用户备注对话框，给其他用户设置仅自己可见的备注名 |
| **src/api/admin.ts** | 管理员接口封装（登录、指标、日志、用户管理、封禁、健康检查） |
| **src/api/file.ts** | 文件上传 API 封装及文件格式化工具函数 |
| **src/api/userRemark.ts** | 用户备注接口封装（获取备注列表、保存备注） |

### 后端核心文件

| 文件 | 说明 |
|------|------|
| **ChatWebSocketHandler.java** | 核心实时事件分发器，管理会话、用户在线状态、消息广播、房间同步以及 WebSocket 协议事件 |
| **WebSocketConfig.java** | WebSocket 端点配置，注册 /ws/chat 处理器 |
| **WebMvcConfig.java** | MVC 配置，注册管理员 IP 白名单拦截器和会话拦截器 |
| **RoomController.java** | 房间管理 HTTP 接口，包括 CRUD、成员管理、踢人、解散等操作 |
| **FileController.java** | 文件上传 HTTP 接口，验证上传并委托给 FileUploadService |
| **AdminController.java** | 管理员 HTTP 接口，提供健康检查、登录认证、监控指标、用户管理、封禁和日志功能 |
| **UserRemarkController.java** | 用户备注 HTTP 接口，提供获取和保存备注功能 |
| **MessageService.java** | 消息服务，创建带序号的文本或文件消息，查询房间历史记录 |
| **RoomService.java** | 房间服务，处理房间创建、私聊复用、成员管理和房间删除 |
| **AdminAuthService.java** | 管理员认证服务，处理登录、登出和会话验证 |
| **FileUploadService.java** | 文件上传服务，存储上传文件、生成访问 URL、跟踪文件元数据 |
| **SystemMonitorService.java** | 系统监控服务，收集 CPU、内存、JVM 和运行时间指标 |
| **LogMonitorService.java** | 日志监控服务，读取后端日志并暴露给管理员面板 |
| **UserRemarkService.java** | 用户备注服务，处理备注的保存和查询 |
| **SnowflakeIdGenerator.java** | 雪花 ID 生成器，生成全局唯一 64 位 ID |

## 技术栈

- **前端**: Vue 3 + TypeScript + Vite + Tailwind CSS + Pinia + Vue Router
- **后端**: Spring Boot 3.2 + Spring WebSocket + Spring Data JPA + Spring Security Crypto
- **数据库**: SQLite
- **部署**: Nginx 反向代理

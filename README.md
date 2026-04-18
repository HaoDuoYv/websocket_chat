# WebSocket 即时聊天系统

一个基于 WebSocket 的实时聊天应用，支持私聊、群聊、文件传输、表情包和服务器监控等功能。

## 功能特性

### 聊天功能
- **私聊**：用户之间一对一实时聊天
- **群聊**：创建公开群组，多人实时交流
- **消息类型**：支持文本消息、文件消息和系统消息
- **表情包**：内置 300+ 表情符号
- **文件上传**：支持上传最大 500MB 的文件，文件自动以 UUID 命名存储
- **在线状态**：实时显示用户在线状态

### 用户界面
- **双主题模式**：支持亮色/暗色主题切换
- **响应式设计**：适配桌面和移动设备
- **消息气泡**：支持显示头像、用户名、发送时间
- **日期分隔**：按日期分组显示消息
- **图片预览**：双击图片可全屏预览

### 房间管理
- **创建群聊**：创建公开房间，邀请其他用户
- **私聊房间**：自动创建一对一私聊房间
- **成员管理**：群主可踢出成员
- **解散房间**：群主可解散群聊

### 管理员功能
- **系统监控**：实时监控 CPU、内存、JVM 使用情况
- **日志查看**：实时查看服务器日志
- **IP 白名单**：管理后台仅限授权 IP 访问

## 技术栈

### 后端
- **Spring Boot 3.0** - Web 框架
- **Spring WebSocket** - 实时通信
- **Spring Data JPA** - 数据持久化
- **SQLite** - 嵌入式数据库
- **JDK 17+** - Java 运行环境
- **Snowflake ID** - 分布式 ID 生成

### 前端
- **Vue 3** - 渐进式 JavaScript 框架
- **TypeScript** - JavaScript 超集
- **Vite** - 构建工具
- **Tailwind CSS** - CSS 框架
- **Vue Router** - 路由管理

### 部署
- **Nginx** - 反向代理和静态资源服务

## 项目结构

```
websocket聊天/
├── frontend/                    # 前端项目
│   ├── src/
│   │   ├── api/                 # API 接口
│   │   │   ├── admin.ts         # 管理员接口
│   │   │   ├── auth.ts          # 认证接口
│   │   │   ├── file.ts          # 文件上传接口
│   │   │   ├── room.ts          # 房间接口
│   │   │   └── user.ts          # 用户接口
│   │   ├── components/          # Vue 组件
│   │   │   ├── ConfirmDialog.vue
│   │   │   ├── CreateGroupDialog.vue
│   │   │   ├── FileMessage.vue  # 文件消息渲染
│   │   │   ├── FileUploadButton.vue
│   │   │   ├── InviteUserDialog.vue
│   │   │   ├── LoginForm.vue
│   │   │   └── RoomMemberDialog.vue
│   │   ├── composables/         # 组合式函数
│   │   │   └── useWebSocket.ts  # WebSocket 客户端
│   │   ├── pages/               # 页面组件
│   │   │   ├── AdminPage.vue    # 管理员页面
│   │   │   ├── ChatPage.vue     # 聊天页面
│   │   │   └── HomePage.vue     # 首页
│   │   ├── router/              # 路由配置
│   │   │   └── index.ts
│   │   ├── App.vue              # 根组件
│   │   └── main.ts              # 入口文件
│   ├── index.html
│   ├── package.json
│   ├── tailwind.config.js
│   ├── tsconfig.json
│   └── vite.config.ts
│
├── backend/                     # 后端项目
│   ├── src/main/java/com/chat/
│   │   ├── config/              # 配置类
│   │   │   ├── WebMvcConfig.java
│   │   │   └── WebSocketConfig.java
│   │   ├── controller/          # 控制器
│   │   │   ├── AdminController.java
│   │   │   ├── AuthController.java
│   │   │   ├── FileController.java
│   │   │   ├── RoomController.java
│   │   │   └── UserController.java
│   │   ├── entity/              # 实体类
│   │   │   ├── FileInfo.java
│   │   │   ├── Message.java
│   │   │   ├── Room.java
│   │   │   ├── RoomMember.java
│   │   │   └── User.java
│   │   ├── handler/             # WebSocket 处理器
│   │   │   └── ChatWebSocketHandler.java
│   │   ├── interceptor/         # 拦截器
│   │   │   └── AdminIpWhitelistInterceptor.java
│   │   ├── repository/          # 数据访问层
│   │   │   ├── FileInfoRepository.java
│   │   │   ├── MessageRepository.java
│   │   │   ├── RoomMemberRepository.java
│   │   │   ├── RoomRepository.java
│   │   │   └── UserRepository.java
│   │   ├── service/             # 服务层
│   │   │   ├── FileUploadService.java
│   │   │   ├── LogMonitorService.java
│   │   │   ├── MessageService.java
│   │   │   ├── RoomService.java
│   │   │   ├── SystemMonitorService.java
│   │   │   └── UserService.java
│   │   ├── utils/               # 工具类
│   │   │   └── SnowflakeIdGenerator.java
│   │   └── ChatApplication.java # 应用入口
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── data/                    # SQLite 数据库目录
│   ├── uploads/                 # 文件上传目录
│   ├── logs/                    # 日志目录
│   └── pom.xml
│
└── README.md
```

## 快速开始

### 环境要求
- JDK 17+
- Node.js 18+
- Maven 3.8+

### 后端启动

1. 进入后端目录：
```bash
cd backend
```

2. 修改配置文件 `src/main/resources/application.properties`：
```properties
# 服务端口
server.port=8081

# 文件上传配置
local.local-url=uploads
local.web-url=/files
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB

# 日志配置
logging.file.name=logs/application.log

# 管理员配置
admin.allowed-ips=127.0.0.1,0:0:0:0:0:0:0:1,localhost
admin.log-path=logs
```

3. 启动后端服务：
```bash
mvn spring-boot:run
```

或打包后运行：
```bash
mvn clean package
java -jar target/websocket-chat-0.0.1-SNAPSHOT.jar
```

### 前端启动

1. 进入前端目录：
```bash
cd frontend
```

2. 安装依赖：
```bash
npm install
```

3. 启动开发服务器：
```bash
npm run dev
```

前端将运行在 `http://localhost:5173`

### 生产构建

1. 构建前端：
```bash
cd frontend
npm run build
```

2. 将 `frontend/dist` 目录部署到 Nginx 或其他 Web 服务器

## 配置说明

### 后端配置 (application.properties)

```properties
# 服务端口
server.port=8081

# 文件上传配置
local.local-url=uploads                    # 本地存储路径
local.web-url=/files                       # Web访问URL前缀
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB

# 数据库配置
spring.datasource.url=jdbc:sqlite:./data/chat.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=update

# 日志配置
logging.file.name=logs/application.log

# 管理员配置
admin.allowed-ips=127.0.0.1,0:0:0:0:0:0:0:1,localhost  # IP白名单
admin.log-path=logs                       # 日志文件路径
```

### IP 白名单

管理员监控功能 (`/api/admin/**`) 需要客户端 IP 在白名单中才能访问。

- 默认允许：`127.0.0.1`, `0:0:0:0:0:0:0:1`, `localhost`
- 添加新 IP：修改 `admin.allowed-ips` 配置项，多个 IP 用逗号分隔

## API 接口

### 认证接口

| 接口 | 方法 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| `/api/auth/register` | POST | 用户注册 | `{username: string, password: string}` | `{userId: string, username: string}` |
| `/api/auth/login` | POST | 用户登录 | `{username: string, password: string}` | `{userId: string, username: string}` |

### 用户接口

| 接口 | 方法 | 描述 | 响应 |
|------|------|------|------|
| `/api/users` | GET | 获取在线用户列表 | `{users: [{userId: string, username: string}]}` |

### 房间接口

| 接口 | 方法 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| `/api/rooms` | POST | 创建房间 | `{name: string, type: 'public' \| 'private'}` | `Room` |
| `/api/rooms` | GET | 获取用户房间列表 | N/A | `Room[]` |
| `/api/rooms/:roomId/join` | POST | 加入房间 | N/A | `{message: string}` |
| `/api/rooms/:roomId/invite` | POST | 邀请用户 | `{userId: string}` | `{message: string}` |
| `/api/rooms/:roomId/kick` | POST | 踢出成员 | `{targetUserId: string}` | `{message: string}` |
| `/api/rooms/:roomId` | DELETE | 解散房间 | N/A | `{message: string}` |
| `/api/rooms/:roomId/history` | GET | 获取房间历史 | N/A | `Message[]` |

### 文件接口

| 接口 | 方法 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| `/api/file/upload` | POST | 上传文件 | `multipart/form-data` | `{fileId: string, fileName: string, fileUrl: string, fileSize: number, fileType: string}` |
| `/files/{fileId}` | GET | 访问上传的文件 | N/A | 文件内容 |

### 管理员接口

| 接口 | 方法 | 描述 | 响应 |
|------|------|------|------|
| `/api/admin/health` | GET | 健康检查（无需IP白名单） | `{status: string}` |
| `/api/admin/metrics` | GET | 获取系统监控数据 | `{cpu: number, memory: number, jvm: object, uptime: number}` |
| `/api/admin/logs` | GET | 获取最近日志 | `string[]` |
| `/api/admin/logs/clear` | POST | 清空日志缓存 | `{message: string}` |

### WebSocket 接口

| 路径 | 描述 |
|------|------|
| `/ws/chat` | WebSocket 连接端点 |

## WebSocket 消息格式

### 客户端发送消息

```json
{
  "type": "message:send",
  "roomId": "room123",
  "content": "Hello!",
  "senderId": "user1",
  "senderName": "User1"
}
```

### 服务端推送消息

```json
{
  "type": "message:receive",
  "roomId": "room123",
  "content": "Hello!",
  "senderId": "user1",
  "senderName": "User1",
  "timestamp": 1712928000000,
  "seq": 1
}
```

## 核心特性

### 消息序号机制
- 每个房间的消息使用递增序号（seq）
- 保证消息顺序性和完整性
- 支持消息同步和断线重连

### 雪花 ID 生成
- 使用 Twitter Snowflake 算法
- 生成全局唯一 64 位 ID
- 解决 JavaScript Number 精度问题（序列化为字符串）

### 文件上传
- 支持最大 500MB 文件
- 文件以 UUID 命名存储
- 支持图片预览和文件下载
- 图片消息只显示图片，不展示文件名

## 常见问题

### 1. 文件上传失败
- 检查 `uploads` 目录是否存在且有写入权限
- 确认 `local.local-url` 配置正确
- 检查文件大小是否超过限制

### 2. WebSocket 连接失败
- 确认后端服务已启动
- 检查浏览器控制台是否有跨域错误
- 确认 Nginx 已正确配置 WebSocket 代理

### 3. 管理员页面无法访问
- 确认访问者的 IP 在 `admin.allowed-ips` 白名单中
- 检查后端服务日志是否有拦截记录

### 4. 数据库初始化失败
- 确认 `backend/data` 目录存在
- 检查 SQLite 数据库文件权限

## 开发指南

### 添加新页面

1. 在 `frontend/src/pages/` 创建 Vue 组件
2. 在 `frontend/src/router/index.ts` 添加路由
3. 如需 API，在 `frontend/src/api/` 添加接口文件

### 添加后端接口

1. 在 `backend/src/main/java/com/chat/controller/` 创建 Controller
2. 在 `backend/src/main/java/com/chat/service/` 添加业务逻辑
3. 重启后端服务使更改生效

### 修改主题

主题偏好保存在浏览器 localStorage 中：
- `theme=dark` - 暗色主题
- `theme=light` - 亮色主题

## 许可证

本项目仅供学习和参考使用。

# WebSocket 即时聊天系统

一个基于 WebSocket 的实时聊天应用，支持私聊、群聊、文件传输、表情包、用户备注和管理员后台等功能。

## 功能特性

### 聊天功能
- **私聊**：用户之间一对一实时聊天
- **群聊**：创建公开群组，多人实时交流
- **消息类型**：支持文本消息、文件消息和系统消息
- **表情包**：内置 300+ 表情符号
- **文件上传**：支持上传最大 500MB 的文件，文件自动以 UUID 命名存储
- **在线状态**：实时显示用户在线状态
- **用户备注**：给其他用户设置仅自己可见的备注名

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
- **离开房间**：普通成员可主动离开房间

### 管理员功能
- **管理员认证**：独立的管理员登录/登出机制
- **用户管理**：查看用户列表、重命名用户、封禁/解封用户
- **系统监控**：实时监控 CPU、内存、JVM 使用情况
- **日志查看**：实时查看服务器日志
- **IP 白名单**：管理后台仅限授权 IP 访问
- **会话保护**：管理员接口需同时通过 IP 白名单和会话认证

## 技术栈

### 后端
- **Spring Boot 3.2** - Web 框架
- **Spring WebSocket** - 实时通信
- **Spring Data JPA** - 数据持久化
- **Spring Security Crypto** - 密码加密
- **SQLite** - 嵌入式数据库
- **JDK 17+** - Java 运行环境
- **Snowflake ID** - 分布式 ID 生成

### 前端
- **Vue 3** - 渐进式 JavaScript 框架
- **TypeScript** - JavaScript 超集
- **Vite** - 构建工具
- **Tailwind CSS** - CSS 框架
- **Vue Router** - 路由管理
- **Pinia** - 状态管理
- **Axios** - HTTP 客户端

### 部署
- **Nginx** - 反向代理和静态资源服务

## 项目结构

```
websocket_chat/
├── frontend/                    # 前端项目
│   ├── src/
│   │   ├── api/                 # API 接口
│   │   │   ├── admin.ts         # 管理员接口
│   │   │   ├── file.ts          # 文件上传接口
│   │   │   └── userRemark.ts    # 用户备注接口
│   │   ├── components/          # Vue 组件
│   │   │   ├── ConfirmDialog.vue
│   │   │   ├── CreateGroupDialog.vue
│   │   │   ├── FileMessage.vue
│   │   │   ├── FileUploadButton.vue
│   │   │   ├── InviteUserDialog.vue
│   │   │   ├── LoginForm.vue
│   │   │   └── SetRemarkDialog.vue
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
│   │   │   ├── AdminConfig.java
│   │   │   ├── CorsConfig.java
│   │   │   ├── FileUploadConfig.java
│   │   │   ├── SnowflakeConfig.java
│   │   │   ├── WebMvcConfig.java
│   │   │   └── WebSocketConfig.java
│   │   ├── controller/          # 控制器
│   │   │   ├── AdminController.java
│   │   │   ├── AuthController.java
│   │   │   ├── FileController.java
│   │   │   ├── RoomController.java
│   │   │   ├── UserController.java
│   │   │   └── UserRemarkController.java
│   │   ├── entity/              # 实体类
│   │   │   ├── FileRecord.java
│   │   │   ├── Message.java
│   │   │   ├── Room.java
│   │   │   ├── RoomMember.java
│   │   │   ├── RoomMemberId.java
│   │   │   ├── User.java
│   │   │   └── UserRemark.java
│   │   ├── handler/             # WebSocket 处理器
│   │   │   └── ChatWebSocketHandler.java
│   │   ├── interceptor/         # 拦截器
│   │   │   ├── AdminIpInterceptor.java
│   │   │   └── AdminSessionInterceptor.java
│   │   ├── properties/          # 配置属性
│   │   │   └── LocalProperties.java
│   │   ├── repository/          # 数据访问层
│   │   │   ├── MessageRepository.java
│   │   │   ├── RoomMemberRepository.java
│   │   │   ├── RoomRepository.java
│   │   │   ├── UserRemarkRepository.java
│   │   │   └── UserRepository.java
│   │   ├── service/             # 服务层
│   │   │   ├── AdminAuthService.java
│   │   │   ├── FileUploadService.java
│   │   │   ├── LogMonitorService.java
│   │   │   ├── MessageService.java
│   │   │   ├── MessageStorageService.java
│   │   │   ├── RoomService.java
│   │   │   ├── SystemMonitorService.java
│   │   │   ├── UserRemarkService.java
│   │   │   └── UserService.java
│   │   ├── utils/               # 工具类
│   │   │   ├── LocalUploadUtil.java
│   │   │   └── SnowflakeIdGenerator.java
│   │   ├── vo/                  # 视图对象
│   │   │   ├── FileUploadResponse.java
│   │   │   ├── LogLineVO.java
│   │   │   └── SystemMetricsVO.java
│   │   └── ChatApplication.java # 应用入口
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── data/                    # SQLite 数据库目录
│   ├── uploads/                 # 文件上传目录
│   ├── logs/                    # 日志目录
│   └── pom.xml
│
├── nginx.conf                   # Nginx 反向代理配置
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
server.port=8081

local.local-url=uploads
local.web-url=/files
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB

logging.file.name=logs/application.log

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

前端将运行在 `http://localhost:3000`

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
server.port=8081

local.local-url=uploads
local.web-url=/files
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB

spring.datasource.url=jdbc:sqlite:./data/chat.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=update

logging.file.name=logs/application.log

admin.allowed-ips=127.0.0.1,0:0:0:0:0:0:0:1,localhost
admin.log-path=logs

snowflake.machine-id=0
```

### IP 白名单

管理员监控功能 (`/api/admin/**`) 需要客户端 IP 在白名单中才能访问。

- 默认允许：`127.0.0.1`, `0:0:0:0:0:0:0:1`, `localhost`
- 添加新 IP：修改 `admin.allowed-ips` 配置项，多个 IP 用逗号分隔

### 管理员认证

管理员接口采用双重保护机制：
1. **IP 白名单**：请求来源 IP 必须在白名单中
2. **会话认证**：需先调用 `/api/admin/login` 登录获取会话

## API 接口

### 认证接口

| 接口 | 方法 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| `/auth/register` | POST | 用户注册 | `{username: string}` | `{userId: string, username: string, createdAt: number}` |
| `/auth/login` | POST | 用户登录 | `{username: string}` | `{userId: string, username: string, createdAt: number}` |

### 用户接口

| 接口 | 方法 | 描述 | 响应 |
|------|------|------|------|
| `/api/users` | GET | 获取在线用户列表 | `{users: [{userId: string, username: string}]}` |

### 用户备注接口

| 接口 | 方法 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| `/api/user-remarks` | GET | 获取用户备注列表 | Query: `userId` | `{remarks: {targetUserId: remarkName}}` |
| `/api/user-remarks` | POST | 保存用户备注 | `{userId, targetUserId, remarkName}` | `{id, userId, targetUserId, remarkName, updatedAt}` |

### 房间接口

| 接口 | 方法 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| `/api/rooms` | POST | 创建房间 | `{name, ownerId}` | `Room` |
| `/api/rooms` | GET | 获取用户房间列表 | Query: `userId` | `{rooms: Room[]}` |
| `/api/rooms/:roomId/join` | POST | 加入房间 | `{userId}` | `{message: string}` |
| `/api/rooms/:roomId/leave` | POST | 离开房间 | `{userId}` | `{message: string}` |
| `/api/rooms/:roomId/members` | GET | 获取房间成员 | Query: `userId` | `{members: [{userId, username}]}` |
| `/api/rooms/:roomId/messages` | GET | 获取房间消息 | Query: `userId`, `lastSeq`(可选) | `{messages: Message[]}` |
| `/api/rooms/private` | POST | 创建/获取私聊房间 | `{userId1, userId2}` | `Room` |
| `/api/rooms/:roomId/kick` | POST | 踢出成员 | `{ownerId, targetUserId}` | `{message: string}` |
| `/api/rooms/:roomId/dissolve` | POST | 解散房间 | `{ownerId}` | `{message: string}` |

### 文件接口

| 接口 | 方法 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| `/api/file/upload` | POST | 上传文件 | `multipart/form-data` (file, chatId, senderId) | `FileUploadResponse` |
| `/api/file/info/:fileId` | GET | 获取文件信息 | N/A | `FileUploadResponse` |
| `/files/:fileId` | GET | 访问上传的文件 | N/A | 文件内容 |

### 管理员接口

| 接口 | 方法 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| `/api/admin/health` | GET | 健康检查（无需认证） | N/A | `{status, timestamp}` |
| `/api/admin/login` | POST | 管理员登录 | `{username, password}` | `{message, username}` |
| `/api/admin/logout` | POST | 管理员登出 | N/A | `{message}` |
| `/api/admin/session` | GET | 获取会话状态 | N/A | 会话信息 |
| `/api/admin/metrics` | GET | 获取系统监控数据 | N/A | `SystemMetricsVO` |
| `/api/admin/online-users` | GET | 获取在线用户列表 | N/A | `{users}` |
| `/api/admin/users` | GET | 获取所有用户列表 | N/A | `[{userId, username, ...}]` |
| `/api/admin/users/:userId/username` | PUT | 重命名用户 | `{username}` | `{message}` |
| `/api/admin/users/:userId/ban` | POST | 封禁用户 | `{reason}`(可选) | `{message}` |
| `/api/admin/users/:userId/unban` | POST | 解封用户 | N/A | `{message}` |
| `/api/admin/logs` | GET | 获取最近日志 | Query: `limit`(默认100) | `LogLineVO[]` |
| `/api/admin/logs/all` | GET | 获取全部日志 | N/A | `LogLineVO[]` |
| `/api/admin/logs/clear` | POST | 清空日志缓存 | N/A | `{code, message}` |

### WebSocket 接口

| 路径 | 描述 |
|------|------|
| `/ws/chat` | WebSocket 连接端点 |

## WebSocket 消息格式

所有 WebSocket 消息采用统一的 `{type, data}` 格式。

### 客户端发送消息

```json
{
  "type": "message:send",
  "data": {
    "roomId": "1234567890",
    "content": "Hello!"
  }
}
```

### 服务端推送消息

```json
{
  "type": "message:new",
  "data": {
    "id": "1234567891",
    "roomId": "1234567890",
    "senderId": "1234567889",
    "senderName": "User1",
    "content": "Hello!",
    "type": "text",
    "seq": 1,
    "timestamp": 1712928000000
  }
}
```

### 客户端事件类型

| 事件类型 | 描述 | 数据 |
|----------|------|------|
| `user:join` | 用户加入系统 | `{userId, username}` |
| `user:list` | 请求在线用户列表 | `{}` |
| `message:send` | 发送文本消息 | `{roomId, content}` |
| `message:send:file` | 发送文件消息 | `{roomId, fileId, fileName, fileUrl, fileSize, fileType}` |
| `message:history` | 请求消息历史 | `{roomId}` |
| `room:create` | 创建房间 | `{name, participants}` |
| `room:join` | 加入房间 | `{roomId}` |
| `room:leave` | 离开房间 | `{roomId}` |
| `room:list` | 请求房间列表 | `{userId}` |
| `room:private:start` | 发起私聊 | `{targetUserId}` |
| `room:sync` | 增量同步消息 | `{rooms: [{roomId, lastSeq}]}` |
| `room:invite:member` | 邀请用户加入房间 | `{roomId, targetUserId}` |

### 服务端事件类型

| 事件类型 | 描述 | 数据 |
|----------|------|------|
| `user:joined` | 用户上线通知 | `{userId, username}` |
| `user:left` | 用户离线通知 | `{userId, username}` |
| `user:banned` | 用户被封禁通知 | `{reason}` |
| `user:list:response` | 在线用户列表响应 | `{users}` |
| `message:new` | 新文本消息 | `{id, roomId, senderId, senderName, content, type, seq, timestamp}` |
| `message:new:file` | 新文件消息 | `{id, roomId, senderId, senderName, content, type, seq, timestamp, fileId, fileName, fileUrl, fileSize, fileType}` |
| `message:history:response` | 消息历史响应 | `{roomId, messages}` |
| `room:created` | 房间创建成功 | `{id, name, type, ownerId, createdAt}` |
| `room:invite` | 房间邀请通知 | `{id, name, type, ownerId, createdAt}` |
| `room:joined` | 加入房间成功 | `{roomId, userId}` |
| `room:member:joined` | 房间新成员加入 | `{roomId, user: {userId, username}}` |
| `room:member:left` | 房间成员离开 | `{roomId, userId}` |
| `room:list:response` | 房间列表响应 | `{rooms}` |
| `room:private:created` | 私聊房间创建成功 | `{id, name, type, createdAt, targetUsername}` |
| `room:sync:response` | 增量同步响应 | `{messages}` |
| `room:invite:success` | 邀请成员成功 | `{roomId, targetUserId}` |
| `room:invite:error` | 邀请成员失败 | `{message}` |

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
- 文件以 UUID 命名存储，按日期创建子目录
- 支持图片预览和文件下载
- 图片消息只显示图片，不展示文件名

### 用户备注
- 给其他用户设置仅自己可见的备注名
- 私聊房间名称自动显示对方备注名
- 备注名最长 100 字符

### 管理员系统
- 独立的管理员登录认证机制
- IP 白名单 + 会话认证双重保护
- 支持用户封禁（封禁后自动断开 WebSocket 连接）
- 实时系统监控（CPU、内存、JVM）
- 日志实时查看和清理

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
- 确认已通过 `/api/admin/login` 登录获取会话
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

## 数据库设计

详细的数据库表结构设计请参阅 [Database_Schema.md](Database_Schema.md)。

### 数据表概览

| 表名 | 描述 |
|------|------|
| `users` | 用户信息表 |
| `rooms` | 聊天房间表 |
| `room_members` | 房间成员关系表 |
| `messages` | 消息记录表 |
| `user_remarks` | 用户备注表 |

## 许可证

本项目仅供学习和参考使用。

## 1. 架构设计

```mermaid
graph TD
    A[前端 Vue3 + TypeScript] --> B[WebSocket 客户端]
    A --> C[HTTP API 客户端]
    B --> D[Spring Boot 后端]
    C --> D
    D --> E[WebSocket 服务器]
    D --> F[REST API 控制器]
    D --> G[SQLite 数据库]
    E --> B
    F --> A
```

## 2. 技术栈

- **前端**: Vue 3 + TypeScript + Vite + Tailwind CSS + Pinia + Vue Router + Axios
- **后端**: Spring Boot 3.2 + Spring WebSocket + Spring Data JPA + Spring Security Crypto
- **数据库**: SQLite（持久化存储）
- **部署**: Nginx 反向代理

## 3. 路由定义

| 路由 | 用途 |
|------|------|
| `/` | 首页，用户登录和聊天列表 |
| `/chat/:chatId` | 聊天页面，显示特定聊天的消息 |
| `/admin` | 管理员监控页面 |

## 4. API 定义

### 4.1 WebSocket 事件

所有 WebSocket 消息采用统一的 `{type: string, data: object}` 格式。

| 事件类型 | 方向 | 数据结构 | 说明 |
|----------|------|----------|------|
| `user:join` | 客户端→服务器 | `{userId: string, username: string}` | 用户加入系统 |
| `user:joined` | 服务器→客户端 | `{userId: string, username: string}` | 通知其他用户有新用户加入 |
| `user:left` | 服务器→客户端 | `{userId: string, username: string}` | 通知所有用户有用户离线 |
| `user:banned` | 服务器→客户端 | `{reason: string}` | 通知用户被封禁（随后断开连接） |
| `user:list` | 客户端→服务器 | `{}` | 请求在线用户列表 |
| `user:list:response` | 服务器→客户端 | `{users: [{userId: string, username: string}]}` | 返回在线用户列表 |
| `message:send` | 客户端→服务器 | `{roomId: string, content: string}` | 发送文本消息 |
| `message:new` | 服务器→客户端 | `{id, roomId, senderId, senderName, content, type: 'text', seq, timestamp}` | 接收文本消息 |
| `message:send:file` | 客户端→服务器 | `{roomId: string, fileId: string, fileName: string, fileUrl: string, fileSize: number, fileType: string}` | 发送文件消息 |
| `message:new:file` | 服务器→客户端 | `{id, roomId, senderId, senderName, content, type: 'file', seq, timestamp, fileId, fileName, fileUrl, fileSize, fileType}` | 接收文件消息 |
| `message:history` | 客户端→服务器 | `{roomId: string}` | 请求房间历史消息 |
| `message:history:response` | 服务器→客户端 | `{roomId: string, messages: Message[]}` | 返回历史消息 |
| `room:create` | 客户端→服务器 | `{name: string, participants: string[]}` | 创建房间 |
| `room:created` | 服务器→客户端 | `{id, name, type, ownerId, createdAt}` | 房间创建成功 |
| `room:invite` | 服务器→客户端 | `{id, name, type, ownerId, createdAt}` | 房间邀请通知（发给被邀请者） |
| `room:join` | 客户端→服务器 | `{roomId: string}` | 加入房间 |
| `room:joined` | 服务器→客户端 | `{roomId: string, userId: string}` | 加入房间成功 |
| `room:member:joined` | 服务器→客户端 | `{roomId: string, user: {userId, username}}` | 房间新成员加入通知 |
| `room:leave` | 客户端→服务器 | `{roomId: string}` | 离开房间 |
| `room:member:left` | 服务器→客户端 | `{roomId: string, userId: string}` | 房间成员离开通知 |
| `room:invite:member` | 客户端→服务器 | `{roomId: string, targetUserId: string}` | 邀请用户加入房间 |
| `room:invite:success` | 服务器→客户端 | `{roomId: string, targetUserId: string}` | 邀请成员成功 |
| `room:invite:error` | 服务器→客户端 | `{message: string}` | 邀请成员失败 |
| `room:list` | 客户端→服务器 | `{userId: string}` | 请求用户的房间列表 |
| `room:list:response` | 服务器→客户端 | `{rooms: Room[]}` | 返回房间列表 |
| `room:private:start` | 客户端→服务器 | `{targetUserId: string}` | 发起私聊 |
| `room:private:created` | 服务器→客户端 | `{id, name, type: 'private', createdAt, targetUsername}` | 私聊房间创建/获取成功 |
| `room:sync` | 客户端→服务器 | `{rooms: [{roomId: string, lastSeq: number}]}` | 增量同步房间消息 |
| `room:sync:response` | 服务器→客户端 | `{messages: Message[]}` | 返回增量消息 |

### 4.2 HTTP 端点

#### 认证接口

| 方法 | 路径 | 用途 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | `/auth/register` | 用户注册 | `{username: string}` | `{userId, username, createdAt}` |
| POST | `/auth/login` | 用户登录 | `{username: string}` | `{userId, username, createdAt}` |

#### 用户接口

| 方法 | 路径 | 用途 | 响应 |
|------|------|------|------|
| GET | `/api/users` | 获取在线用户列表 | `{users: [{userId, username}]}` |

#### 用户备注接口

| 方法 | 路径 | 用途 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | `/api/user-remarks` | 获取用户备注列表 | Query: `userId` | `{remarks: {targetUserId: remarkName}}` |
| POST | `/api/user-remarks` | 保存用户备注 | `{userId, targetUserId, remarkName}` | `{id, userId, targetUserId, remarkName, updatedAt}` |

#### 房间接口

| 方法 | 路径 | 用途 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | `/api/rooms` | 创建房间 | `{name, ownerId}` | `Room` |
| GET | `/api/rooms` | 获取用户房间列表 | Query: `userId` | `{rooms: Room[]}` |
| POST | `/api/rooms/:roomId/join` | 加入房间 | `{userId}` | `{message}` |
| POST | `/api/rooms/:roomId/leave` | 离开房间 | `{userId}` | `{message}` |
| GET | `/api/rooms/:roomId/members` | 获取房间成员 | Query: `userId` | `{members: [{userId, username}]}` |
| GET | `/api/rooms/:roomId/messages` | 获取房间消息 | Query: `userId`, `lastSeq`(可选) | `{messages: Message[]}` |
| POST | `/api/rooms/private` | 创建/获取私聊房间 | `{userId1, userId2}` | `Room` |
| POST | `/api/rooms/:roomId/kick` | 踢出成员 | `{ownerId, targetUserId}` | `{message}` |
| POST | `/api/rooms/:roomId/dissolve` | 解散房间 | `{ownerId}` | `{message}` |

#### 文件接口

| 方法 | 路径 | 用途 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | `/api/file/upload` | 上传文件 | `multipart/form-data` (file, chatId, senderId) | `FileUploadResponse` |
| GET | `/api/file/info/:fileId` | 获取文件信息 | N/A | `FileUploadResponse` |
| GET | `/files/:fileId` | 访问上传的文件 | N/A | 文件内容 |

#### 管理员接口

| 方法 | 路径 | 用途 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | `/api/admin/health` | 健康检查（无需认证） | N/A | `{status, timestamp}` |
| POST | `/api/admin/login` | 管理员登录 | `{username, password}` | `{message, username}` |
| POST | `/api/admin/logout` | 管理员登出 | N/A | `{message}` |
| GET | `/api/admin/session` | 获取会话状态 | N/A | 会话信息 |
| GET | `/api/admin/metrics` | 系统监控指标 | N/A | `SystemMetricsVO` |
| GET | `/api/admin/online-users` | 在线用户列表 | N/A | `{users}` |
| GET | `/api/admin/users` | 所有用户列表 | N/A | `[{userId, username, ...}]` |
| PUT | `/api/admin/users/:userId/username` | 重命名用户 | `{username}` | `{message}` |
| POST | `/api/admin/users/:userId/ban` | 封禁用户 | `{reason}`(可选) | `{message}` |
| POST | `/api/admin/users/:userId/unban` | 解封用户 | N/A | `{message}` |
| GET | `/api/admin/logs` | 获取最近日志 | Query: `limit`(默认100) | `LogLineVO[]` |
| GET | `/api/admin/logs/all` | 获取全部日志 | N/A | `LogLineVO[]` |
| POST | `/api/admin/logs/clear` | 清空日志缓存 | N/A | `{code, message}` |

## 5. 服务器架构图

```mermaid
graph TD
    A[WebSocket Handler] --> B[Room Service]
    A --> C[Message Service]
    A --> D[User Service]
    E[Room Controller] --> B
    F[File Controller] --> G[File Upload Service]
    H[Admin Controller] --> I[System Monitor Service]
    H --> J[Log Monitor Service]
    H --> K[Admin Auth Service]
    H --> D
    L[User Remark Controller] --> M[User Remark Service]
    B --> N[SQLite Database]
    C --> N
    D --> N
    M --> N
    G --> O[文件系统]
```

## 6. 数据模型

### 6.1 实体关系

```mermaid
graph LR
    User -- 拥有 --> Room
    User -- 参与 --> RoomMember
    Room -- 包含 --> RoomMember
    Room -- 包含 --> Message
    User -- 发送 --> Message
    Message -- 引用 --> FileRecord
    User -- 设置备注 --> UserRemark
```

### 6.2 数据定义

#### User（用户）
```typescript
interface User {
  id: string;           // 雪花 ID（Long，序列化为 String）
  username: string;     // 用户名（唯一）
  password: string;     // 密码（加密存储）
  avatarColor: string;  // 头像颜色
  createdAt: number;    // 创建时间戳
  lastSeen: number;     // 最后活跃时间戳
  banned: boolean;      // 是否被封禁
  bannedAt: number;     // 封禁时间
  bannedReason: string; // 封禁原因
}
```

#### Room（房间）
```typescript
interface Room {
  id: string;           // 雪花 ID（Long，序列化为 String）
  name: string;         // 房间名称
  type: 'public' | 'private';  // 房间类型
  ownerId: string;      // 群主 ID（群聊）
  createdAt: number;    // 创建时间戳
}
```

#### RoomMember（房间成员）
```typescript
interface RoomMember {
  roomId: string;       // 房间 ID（联合主键）
  userId: string;       // 用户 ID（联合主键）
  joinedAt: number;     // 加入时间
  lastReadSeq: number;  // 最后读取的消息序号
}
```

#### Message（消息）
```typescript
interface Message {
  id: string;           // UUID
  roomId: string;       // 房间 ID（Long，序列化为 String）
  senderId: string;     // 发送者 ID（Long，序列化为 String）
  senderName: string;   // 发送者名称
  content: string;      // 消息内容（文本或文件 ID）
  type: 'text' | 'file' | 'system';  // 消息类型
  seq: number;          // 房间消息序号
  timestamp: number;    // 发送时间戳
  fileId?: string;      // 文件 ID（文件消息）
  fileName?: string;    // 文件名（文件消息）
  fileUrl?: string;     // 文件 URL（文件消息）
  fileSize?: number;    // 文件大小（文件消息）
  fileType?: string;    // MIME 类型（文件消息）
}
```

#### FileRecord（文件记录）
```typescript
interface FileRecord {
  fileId: string;           // 文件 ID
  fileName: string;         // 存储文件名（UUID）
  originalFileName: string; // 原始文件名
  filePath: string;         // 文件存储路径
  fileUrl: string;          // 访问 URL
  fileSize: number;         // 文件大小（字节）
  fileType: string;         // MIME 类型
  chatId: string;           // 所属聊天 ID
  senderId: string;         // 上传者 ID
  uploadTime: number;       // 上传时间
}
```

#### UserRemark（用户备注）
```typescript
interface UserRemark {
  id: string;           // 雪花 ID（Long，序列化为 String）
  userId: string;       // 设置备注的用户 ID
  targetUserId: string; // 被备注的用户 ID
  remarkName: string;   // 备注名称
  createdAt: number;    // 创建时间戳
  updatedAt: number;    // 更新时间戳
}
```

## 7. 核心功能实现

### 7.1 消息序号机制
- 每个房间的消息使用递增序号（seq）
- 保证消息顺序性和完整性
- 支持消息同步和断线重连
- RoomMember 记录每个用户的 lastReadSeq 用于未读计数

### 7.2 雪花 ID 生成
- 使用 Twitter Snowflake 算法
- 生成全局唯一 64 位 ID
- 起始时间戳 EPOCH = 2024-01-01
- 机器 ID 位数 10 位，序列号位数 12 位
- 解决 JavaScript Number 精度问题（Long 字段使用 @JsonSerialize(using = ToStringSerializer.class) 序列化为字符串）

### 7.3 文件上传
- 支持最大 500MB 文件
- 文件以 UUID 命名存储，按日期创建子目录
- 支持图片预览和文件下载
- FileUploadConfig 将 `/files/**` 映射到本地文件系统

### 7.4 管理员系统
- 独立的管理员登录认证机制（AdminAuthService）
- IP 白名单拦截器（AdminIpInterceptor）+ 会话拦截器（AdminSessionInterceptor）双重保护
- 支持用户封禁（封禁后通过 WebSocket 发送 `user:banned` 事件并断开连接）
- 实时系统监控（CPU、内存、JVM）
- 日志实时查看和清理

### 7.5 用户备注
- 给其他用户设置仅自己可见的备注名
- 私聊房间名称自动显示对方备注名
- UserRemark 实体使用 (userId, targetUserId) 唯一约束

### 7.6 WebSocket 连接初始化
连接建立后，前端依次发送以下事件：
1. `user:join` — 声明用户身份
2. `room:list` — 请求房间列表
3. `user:list` — 请求在线用户列表
4. `room:sync`（延迟 500ms）— 增量同步各房间消息

## 8. 部署架构

```mermaid
graph LR
    A[浏览器] --> B[Nginx]
    B --> C[前端静态资源]
    B --> D[/api/*]
    B --> E[/ws/chat]
    D --> F[Spring Boot 8081]
    E --> F
    F --> G[SQLite]
    F --> H[uploads/]
```

- Nginx 监听 80 端口
- 前端静态资源由 Nginx 直接服务
- `/api/*` 代理到 Spring Boot（8081）
- `/ws/` 和 `/chat` 代理到 Spring Boot WebSocket（8081），支持 Upgrade 头
- 上传文件通过 `/files/*` 访问
- WebSocket 连接超时设置为 86400 秒（24 小时）
- 最大上传文件大小 500MB

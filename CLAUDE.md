# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WebSocket-based instant messaging and collaboration platform with private/group chat, AI assistants, collaborative editing (Yjs CRDT), Gomoku game, file transfer, emoji, user remarks, and admin dashboard.

## Common Commands

### Backend (Spring Boot 3.2 + JDK 17 + Maven)
```bash
cd backend
mvn spring-boot:run                    # Start dev server on port 8081
mvn clean package                      # Build JAR
java -jar target/websocket-chat-0.0.1-SNAPSHOT.jar  # Run packaged JAR
mvn test                               # Run tests
mvn test -Dtest=ChatApplicationTests   # Run single test class
```

### Frontend (Vue 3 + Vite + TypeScript)
```bash
cd frontend
npm install                            # Install dependencies
npm run dev                            # Start dev server on port 3000
npm run build                          # Type-check (vue-tsc) + production build
npm run preview                        # Preview production build
```

## Architecture

### Backend (`backend/`)

Standard Spring Boot layered architecture under `com.chat`:

- **config/** — Spring configuration: WebSocket endpoints, CORS, file upload limits (500MB max), Snowflake ID, cache (Caffeine), admin IP whitelist
- **controller/** — REST endpoints: Auth, Room, User, File, Admin, AI, Avatar, UserRemark, App
- **handler/** — WebSocket handlers: `ChatWebSocketHandler` (main chat), `EditorWebSocketHandler` (collaborative editing via Yjs), `GomokuWebSocketHandler` (game)
- **interceptor/** — `JwtAuthInterceptor` for API auth, `WebSocketAuthInterceptor` for WS auth, admin IP/session interceptors
- **entity/** — JPA entities mapped to SQLite; `GomokuRoom` is in-memory only (ConcurrentHashMap)
- **repository/** — Spring Data JPA repositories
- **service/** — Business logic including AI chat (Spring AI), file upload, Gomoku game state
- **utils/** — `JwtUtil` (JJWT), `LocalUploadUtil`, `SnowflakeIdGenerator`

### Frontend (`frontend/`)

Vue 3 SPA with TypeScript, Tailwind CSS, Pinia state management:

- **pages/** — Route-level views: LandingPage, HomePage, ChatPage, EditorPage, GomokuLobbyPage, GomokuGamePage, AiManagePage, AiChatView, AppsPage
- **views/admin/** — Admin dashboard pages (AdminLogin, AdminDashboard, AdminUsers, AdminAiConfig, AdminLogs)
- **layouts/** — AdminLayout for admin section
- **components/** — Reusable UI: dialogs, file upload, message display, AI config
- **composables/** — `useWebSocket.ts` — WebSocket client composable
- **config/** — Static configs: `apps.ts` (app center), `emojis.ts`, `llmProviders.ts`
- **api/** — Axios-based API clients: `admin.ts`, `file.ts`, `userRemark.ts`
- **router/** — Vue Router with lazy-loaded routes

### WebSocket Protocol

All WS messages use `{type, data}` JSON format. Client events: `user:join`, `message:send`, `room:create`, `room:sync`, etc. Server events: `message:new`, `room:created`, `user:joined`, etc. The chat WS endpoint is `/ws/chat`.

### Key Patterns

- **IDs**: Users/rooms use Snowflake 64-bit IDs (serialized as strings for JS compatibility); messages use UUID strings
- **Auth**: JWT tokens for user auth (7-day expiry); admin uses session-based auth with IP whitelist
- **Database**: SQLite with Hibernate auto-DDL (`ddl-auto=update`); data stored in `backend/data/chat.db`
- **File uploads**: Stored in `backend/uploads/` with UUID filenames; served via `/files/` path
- **Vite dev proxy**: `/api` → `http://localhost:8081`, `/chat` → `ws://localhost:8081/ws/chat`, `/ws` → `ws://localhost:8081`
- **Theme**: Dark/light mode stored in `localStorage.theme`

### Configuration

Backend config in `backend/src/main/resources/application.properties`. Key settings:
- `admin.allowed-ips` — IP whitelist for admin endpoints
- `admin.password-hash` — BCrypt hash, overridable via `ADMIN_PASSWORD_HASH` env var
- `jwt.secret` — JWT signing key, overridable via `JWT_SECRET` env var
- `snowflake.machine-id` — Machine ID for Snowflake generator

Production deployment uses Nginx (see `nginx.conf`) as reverse proxy.

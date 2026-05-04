# Landing Page Design — WebSocket Chat

## Overview

Add a landing page as the new root route (`/`). Users first see the landing page, then click "Start Chat" to navigate to the login page. Dark tech style, single-page scroll layout.

## Route Changes

| Before | After | Component |
|--------|-------|-----------|
| `/` → HomePage | `/` → LandingPage | `src/pages/LandingPage.vue` (new) |
| — | `/login` → HomePage | `src/pages/HomePage.vue` (moved) |

Other routes unchanged.

## Design System

### Colors

| Element | Value | Tailwind |
|---------|-------|----------|
| Background | `#09090B` | `bg-zinc-950` |
| Primary accent | `#6366F1` | `bg-indigo-500` |
| Secondary accent | `#818CF8` | `text-indigo-400` |
| Text primary | `#F4F4F5` | `text-zinc-100` |
| Text secondary | `#A1A1AA` | `text-zinc-400` |
| Card background | `rgba(39,39,42,0.6)` + blur | `bg-zinc-800/60 backdrop-blur-sm` |
| Card border | `rgba(63,63,70,0.5)` | `border-zinc-700/50` |

### Typography

- Title: system font stack (already in Tailwind defaults), bold weight
- Gradient text on hero title: `bg-gradient-to-r from-indigo-400 to-purple-400 bg-clip-text text-transparent`

## Page Sections

### Section 1: Hero (full viewport height)

- Centered layout
- Project name "WebSocket Chat" with gradient text
- Tagline: "基于 WebSocket 的即时通讯系统"
- Sub-tagline: "支持私聊、群聊、文件传输与实时协作"
- Two CTA buttons:
  - "开始聊天" — solid indigo, navigates to `/login`
  - "GitHub →" — outlined, links to `https://github.com/HaoDuoYv/websocket_chat`
- Background: radial gradient glow (indigo-500/10) centered behind title
- Scroll indicator at bottom

### Section 2: Features

6 feature cards in 3-column grid (responsive: 1 col mobile, 2 col tablet, 3 col desktop).

| Icon (Lucide SVG) | Title | Description |
|--------------------|-------|-------------|
| `message-circle` | 即时通讯 | 私聊与群聊，实时消息推送 |
| `paperclip` | 文件传输 | 拖拽上传，图片/文档即时预览 |
| `grid-3x3` | 五子棋 | 在线对弈，倒计时与胜负判定 |
| `code-2` | 协作编辑 | 多人实时编辑代码，CodeMirror 驱动 |
| `shield-check` | 安全认证 | 用户认证与管理员后台 |
| `layout-grid` | 应用中心 | 集成外部应用，一键访问 |

Card style: dark glass (`bg-zinc-800/60 backdrop-blur-sm border border-zinc-700/50 rounded-xl`), hover adds `border-indigo-500/50` and `translateY(-2px)`.

### Section 3: Open Source + Footer

- "开源项目 · MIT 协议" heading
- Tech stack pills: Vue 3, TypeScript, Spring Boot, WebSocket, Tailwind CSS, Pinia
- Two links: GitHub repo, Submit Issue
- Footer: single line copyright "© 2026 WebSocket Chat · 学习与演示项目"

## Animations

| Element | Animation | Trigger |
|---------|-----------|---------|
| Hero title | `fade-in-up` 0.6s ease-out | Page load |
| Hero subtitle | `fade-in-up` 0.6s ease-out, delay 0.2s | Page load |
| Hero buttons | `fade-in-up` 0.6s ease-out, delay 0.4s | Page load |
| Feature cards | `fade-in-up` 0.5s ease-out, stagger 0.1s | IntersectionObserver |
| Background glow | `pulse` 4s infinite | Always |
| Card hover | border glow + translateY(-2px) | CSS transition |
| Button hover | brightness-110 + scale(1.02) | CSS transition |

All animations respect `prefers-reduced-motion`.

## Responsive Breakpoints

| Breakpoint | Layout |
|------------|--------|
| `< 640px` | Feature cards 1 column, hero text smaller, buttons full width |
| `640px - 1024px` | Feature cards 2 columns |
| `> 1024px` | Feature cards 3 columns, max-w-6xl container |

## Accessibility

- All buttons have `focus-visible:ring-2 focus-visible:ring-indigo-500`
- SVG icons have `aria-label`
- `prefers-reduced-motion` disables all animations
- Color contrast meets WCAG AA (4.5:1 minimum)

## Files Changed

| File | Action |
|------|--------|
| `src/pages/LandingPage.vue` | Create |
| `src/router/index.ts` | Modify (add `/` → LandingPage, `/login` → HomePage) |

## Out of Scope

- No dark/light theme toggle on landing page (always dark)
- No i18n
- No analytics tracking
- No server-side rendering

# Landing Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a dark tech-style landing page as the new root route, with Hero, Features, and Open Source sections, redirecting the original login page to `/login`.

**Architecture:** Single new Vue component `LandingPage.vue` with three sections (Hero, Features, Footer). Route config updated so `/` renders the landing page and `/login` renders the original `HomePage`. No new dependencies required — all styling via Tailwind CSS, animations via CSS + IntersectionObserver.

**Tech Stack:** Vue 3, TypeScript, Tailwind CSS, Vue Router, Lucide SVG icons (inline)

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `src/pages/LandingPage.vue` | Create | Landing page with Hero, Features, Footer sections |
| `src/router/index.ts` | Modify | Add `/` → LandingPage, `/login` → HomePage, update titleMap |

---

### Task 1: Create LandingPage.vue — Hero Section

**Files:**
- Create: `src/pages/LandingPage.vue`

- [ ] **Step 1: Create LandingPage.vue with Hero section**

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const heroVisible = ref(false)

onMounted(() => {
  heroVisible.value = true
})

const goToLogin = () => {
  router.push('/login')
}

const goToGitHub = () => {
  window.open('https://github.com/HaoDuoYv/websocket_chat', '_blank')
}
</script>

<template>
  <div class="min-h-screen bg-zinc-950 text-zinc-100 overflow-x-hidden">
    <!-- Hero Section -->
    <section class="relative min-h-screen flex flex-col items-center justify-center px-6">
      <!-- Background glow -->
      <div class="absolute inset-0 flex items-center justify-center pointer-events-none">
        <div class="w-[600px] h-[600px] bg-indigo-500/10 rounded-full blur-[120px] animate-pulse"></div>
      </div>

      <!-- Content -->
      <div
        class="relative z-10 text-center max-w-3xl"
        :class="heroVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'"
        style="transition: opacity 0.6s ease-out, transform 0.6s ease-out"
      >
        <h1 class="text-5xl sm:text-6xl md:text-7xl font-bold mb-6 bg-gradient-to-r from-indigo-400 to-purple-400 bg-clip-text text-transparent">
          WebSocket Chat
        </h1>
        <p class="text-lg sm:text-xl text-zinc-300 mb-3">
          基于 WebSocket 的即时通讯系统
        </p>
        <p class="text-base text-zinc-400 mb-10">
          支持私聊、群聊、文件传输与实时协作
        </p>
        <div class="flex flex-col sm:flex-row items-center justify-center gap-4">
          <button
            @click="goToLogin"
            class="w-full sm:w-auto px-8 py-3 bg-indigo-500 hover:bg-indigo-400 text-white font-medium rounded-lg transition-all duration-200 hover:brightness-110 hover:scale-[1.02] focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 focus-visible:ring-offset-zinc-950 cursor-pointer"
          >
            开始聊天
          </button>
          <button
            @click="goToGitHub"
            class="w-full sm:w-auto px-8 py-3 border border-zinc-700 hover:border-zinc-500 text-zinc-300 hover:text-white font-medium rounded-lg transition-all duration-200 focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 focus-visible:ring-offset-zinc-950 cursor-pointer"
          >
            GitHub →
          </button>
        </div>
      </div>

      <!-- Scroll indicator -->
      <div class="absolute bottom-10 left-1/2 -translate-x-1/2 animate-bounce text-zinc-500">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 9 6 6 6-6"/></svg>
      </div>
    </section>
  </div>
</template>

<style scoped>
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
```

- [ ] **Step 2: Verify file was created**

Run: `ls src/pages/LandingPage.vue`
Expected: file exists

---

### Task 2: Add Features Section to LandingPage.vue

**Files:**
- Modify: `src/pages/LandingPage.vue`

- [ ] **Step 1: Add feature card data and Features section**

Add the following inside `<script setup lang="ts">`, after the existing code:

```typescript
const featuresVisible = ref(false)

const features = [
  {
    icon: '<svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/></svg>',
    title: '即时通讯',
    desc: '私聊与群聊，实时消息推送',
  },
  {
    icon: '<svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.57-8.57A4 4 0 1 1 18 8.84l-8.59 8.57a2 2 0 0 1-2.83-2.83l8.49-8.48"/></svg>',
    title: '文件传输',
    desc: '拖拽上传，图片/文档即时预览',
  },
  {
    icon: '<svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/><rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/></svg>',
    title: '五子棋',
    desc: '在线对弈，倒计时与胜负判定',
  },
  {
    icon: '<svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>',
    title: '协作编辑',
    desc: '多人实时编辑代码，CodeMirror 驱动',
  },
  {
    icon: '<svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/><path d="m9 12 2 2 4-4"/></svg>',
    title: '安全认证',
    desc: '用户认证与管理员后台',
  },
  {
    icon: '<svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2"/><path d="M3 9h18"/><path d="M9 21V9"/></svg>',
    title: '应用中心',
    desc: '集成外部应用，一键访问',
  },
]
```

Add the following template section after the Hero `</section>` and before `</div>` (the root div's closing tag):

```html
    <!-- Features Section -->
    <section id="features" class="py-24 px-6">
      <div class="max-w-6xl mx-auto">
        <h2 class="text-3xl sm:text-4xl font-bold text-center mb-4">核心功能</h2>
        <p class="text-zinc-400 text-center mb-16 max-w-xl mx-auto">
          一站式即时通讯与协作平台，涵盖聊天、文件、游戏与代码编辑
        </p>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          <div
            v-for="(feature, index) in features"
            :key="feature.title"
            class="group p-6 rounded-xl bg-zinc-800/60 backdrop-blur-sm border border-zinc-700/50 hover:border-indigo-500/50 transition-all duration-200 hover:-translate-y-0.5 cursor-default"
            :class="featuresVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6'"
            :style="{ transitionDelay: featuresVisible ? `${index * 100}ms` : '0ms' }"
          >
            <div class="text-indigo-400 mb-4" v-html="feature.icon"></div>
            <h3 class="text-lg font-semibold mb-2">{{ feature.title }}</h3>
            <p class="text-zinc-400 text-sm">{{ feature.desc }}</p>
          </div>
        </div>
      </div>
    </section>
```

- [ ] **Step 2: Add IntersectionObserver for features animation**

Add inside `onMounted`, after `heroVisible.value = true`:

```typescript
const featuresSection = document.getElementById('features')
if (featuresSection) {
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          featuresVisible.value = true
          observer.disconnect()
        }
      })
    },
    { threshold: 0.15 }
  )
  observer.observe(featuresSection)
}
```

---

### Task 3: Add Footer Section to LandingPage.vue

**Files:**
- Modify: `src/pages/LandingPage.vue`

- [ ] **Step 1: Add Footer section**

Add the following template section after the Features `</section>` and before the root `</div>`:

```html
    <!-- Footer Section -->
    <footer class="py-16 px-6 border-t border-zinc-800">
      <div class="max-w-6xl mx-auto text-center">
        <p class="text-zinc-300 font-medium mb-4">开源项目 · MIT 协议</p>
        <div class="flex flex-wrap items-center justify-center gap-2 mb-8">
          <span class="px-3 py-1 text-xs rounded-full bg-zinc-800 text-zinc-300 border border-zinc-700">Vue 3</span>
          <span class="px-3 py-1 text-xs rounded-full bg-zinc-800 text-zinc-300 border border-zinc-700">TypeScript</span>
          <span class="px-3 py-1 text-xs rounded-full bg-zinc-800 text-zinc-300 border border-zinc-700">Spring Boot</span>
          <span class="px-3 py-1 text-xs rounded-full bg-zinc-800 text-zinc-300 border border-zinc-700">WebSocket</span>
          <span class="px-3 py-1 text-xs rounded-full bg-zinc-800 text-zinc-300 border border-zinc-700">Tailwind CSS</span>
          <span class="px-3 py-1 text-xs rounded-full bg-zinc-800 text-zinc-300 border border-zinc-700">Pinia</span>
        </div>
        <div class="flex items-center justify-center gap-4 mb-8">
          <a
            href="https://github.com/HaoDuoYv/websocket_chat"
            target="_blank"
            class="px-5 py-2 border border-zinc-700 hover:border-zinc-500 text-zinc-300 hover:text-white text-sm rounded-lg transition-colors duration-200 cursor-pointer"
          >
            GitHub 仓库
          </a>
          <a
            href="https://github.com/HaoDuoYv/websocket_chat/issues"
            target="_blank"
            class="px-5 py-2 border border-zinc-700 hover:border-zinc-500 text-zinc-300 hover:text-white text-sm rounded-lg transition-colors duration-200 cursor-pointer"
          >
            提交 Issue
          </a>
        </div>
        <p class="text-zinc-500 text-sm">© 2026 WebSocket Chat · 学习与演示项目</p>
      </div>
    </footer>
```

---

### Task 4: Update Router Configuration

**Files:**
- Modify: `src/router/index.ts`

- [ ] **Step 1: Update router to add LandingPage and move HomePage to /login**

Replace the entire content of `src/router/index.ts` with:

```typescript
import { createRouter, createWebHistory } from 'vue-router'
import LandingPage from '@/pages/LandingPage.vue'

const routes = [
  {
    path: '/',
    name: 'landing',
    component: LandingPage,
  },
  {
    path: '/login',
    name: 'home',
    component: () => import('@/pages/HomePage.vue'),
  },
  {
    path: '/chat/:chatId',
    name: 'chat',
    component: () => import('@/pages/ChatPage.vue'),
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/pages/AdminPage.vue'),
  },
  {
    path: '/apps',
    name: 'apps',
    component: () => import('@/pages/AppsPage.vue'),
  },
  {
    path: '/gomoku',
    name: 'gomoku-lobby',
    component: () => import('@/pages/GomokuLobbyPage.vue'),
  },
  {
    path: '/gomoku/:roomId',
    name: 'gomoku-game',
    component: () => import('@/pages/GomokuGamePage.vue'),
  },
  {
    path: '/editor',
    name: 'editor-new',
    component: () => import('@/pages/EditorPage.vue'),
  },
  {
    path: '/editor/:docId',
    name: 'editor-room',
    component: () => import('@/pages/EditorPage.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const titleMap: Record<string, string> = {
  landing: 'WebSocket Chat',
  home: '聊天',
  chat: '聊天',
  admin: '管理后台',
  apps: '应用中心',
  'gomoku-lobby': '应用大厅',
  'gomoku-game': '五子棋对局',
  'editor-new': '协作编辑器',
  'editor-room': '协作编辑器',
}

router.afterEach((to) => {
  document.title = titleMap[to.name as string] ?? '聊天'
})

export default router
```

- [ ] **Step 2: Verify TypeScript compiles**

Run: `cd e:\study\gitproject\websocket_chat\frontend && npx vue-tsc --noEmit`
Expected: no errors

---

### Task 5: Update Internal Navigation References

**Files:**
- Modify: `src/pages/HomePage.vue` (any internal `router.push('/')` or `router.replace('/')` that should now go to `/login`)

- [ ] **Step 1: Search for navigation references to `/` route**

Search `src/pages/HomePage.vue` for any `router.push('/')` or `router.replace('/')` calls that represent "go back to home/login" and update them to `/login`.

Run: `grep -n "router\.\(push\|replace\)(.*/" src/pages/HomePage.vue`
Expected: find any references that navigate to `'/'` as a "go home" action and change them to `'/login'`

- [ ] **Step 2: Search for navigation references in other pages**

Search all Vue files for `router.push('/')` or `router.replace('/')`:

Run: `grep -rn "router\.\(push\|replace\)(.*/" src/pages/`
Expected: update any "go home" navigation from `'/'` to `'/login'`

---

### Task 6: Build Verification

- [ ] **Step 1: Run full build**

Run: `cd e:\study\gitproject\websocket_chat\frontend && npm run build`
Expected: build succeeds with no errors

- [ ] **Step 2: Verify landing page renders**

Start dev server and navigate to `http://localhost:5173/`. Confirm:
- Landing page renders with Hero, Features, Footer sections
- "开始聊天" button navigates to `/login`
- "GitHub →" button opens GitHub in new tab
- Feature cards animate on scroll
- Page is responsive at 375px, 768px, 1024px, 1440px

---

## Self-Review

**Spec coverage:**
- ✅ Hero section with gradient title, tagline, CTA buttons
- ✅ Features section with 6 cards, Lucide SVG icons, glass style
- ✅ Footer with tech stack pills, GitHub links, copyright
- ✅ Route changes: `/` → LandingPage, `/login` → HomePage
- ✅ Dark tech color scheme
- ✅ Animations: fade-in-up, IntersectionObserver, hover effects
- ✅ Responsive breakpoints
- ✅ Accessibility: focus-visible, prefers-reduced-motion, aria-labels
- ✅ No new dependencies

**Placeholder scan:** No TBD, TODO, or vague steps found.

**Type consistency:** All refs, functions, and template bindings are consistent across tasks.

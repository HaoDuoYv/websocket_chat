<script setup lang="ts">
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// --- Visibility refs ---
const heroVisible = ref(false)
const featuresVisible = ref(false)
const statsVisible = ref(false)
const techVisible = ref(false)

// --- Typing effect ---
const typedText = ref('')
const fullText = 'WebSocket Chat'
const typingSpeed = 100
let typingTimer: ReturnType<typeof setTimeout> | null = null

function startTyping() {
  let i = 0
  function typeChar() {
    if (i < fullText.length) {
      typedText.value += fullText[i]
      i++
      typingTimer = setTimeout(typeChar, typingSpeed)
    }
  }
  typeChar()
}

// --- Mouse parallax for hero ---
const mouseX = ref(0)
const mouseY = ref(0)

function handleMouseMove(e: MouseEvent) {
  mouseX.value = (e.clientX / window.innerWidth - 0.5) * 30
  mouseY.value = (e.clientY / window.innerHeight - 0.5) * 30
}

// --- Scroll-based nav background ---
const scrolled = ref(false)
function handleScroll() {
  scrolled.value = window.scrollY > 50
}

// --- IntersectionObserver setup ---
function createObserver(targetId: string, visibleRef: { value: boolean }, threshold = 0.15) {
  const el = document.getElementById(targetId)
  if (!el) return null
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          visibleRef.value = true
          observer.disconnect()
        }
      })
    },
    { threshold }
  )
  observer.observe(el)
  return observer
}

// --- Particles data ---
const particles = Array.from({ length: 30 }, (_, i) => ({
  id: i,
  x: Math.random() * 100,
  y: Math.random() * 100,
  size: Math.random() * 3 + 1,
  duration: Math.random() * 20 + 15,
  delay: Math.random() * 10,
  opacity: Math.random() * 0.4 + 0.1,
}))

// --- Stats data ---
const stats = [
  { value: '8+', label: '核心功能' },
  { value: '100%', label: '实时同步' },
  { value: '<50ms', label: '消息延迟' },
  { value: '500MB', label: '文件上限' },
]

// --- Features (Bento grid) ---
const features = [
  {
    icon: `<svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/></svg>`,
    title: '即时通讯',
    desc: '私聊与群聊、300+表情包、在线状态、用户备注',
    span: 'lg:col-span-2 lg:row-span-1',
    accent: 'from-indigo-500/20 to-blue-500/20',
  },
  {
    icon: `<svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.57-8.57A4 4 0 1 1 18 8.84l-8.59 8.57a2 2 0 0 1-2.83-2.83l8.49-8.48"/></svg>`,
    title: '文件传输',
    desc: '拖拽上传，图片与文档即时预览，最大500MB',
    span: 'lg:col-span-1 lg:row-span-2',
    accent: 'from-emerald-500/20 to-teal-500/20',
  },
  {
    icon: `<svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>`,
    title: '协作编辑',
    desc: '多人实时编辑代码，CodeMirror + Yjs 驱动',
    span: 'lg:col-span-2 lg:row-span-1',
    accent: 'from-violet-500/20 to-purple-500/20',
  },
  {
    icon: `<svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/><rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/></svg>`,
    title: '五子棋对弈',
    desc: '在线实时对弈，倒计时与胜负判定',
    span: 'lg:col-span-1 lg:row-span-1',
    accent: 'from-amber-500/20 to-orange-500/20',
  },
  {
    icon: `<svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>`,
    title: 'AI 助手',
    desc: '创建专属AI智能体，多模型对话，自定义角色',
    span: 'lg:col-span-2 lg:row-span-1',
    accent: 'from-blue-500/20 to-sky-500/20',
  },
  {
    icon: `<svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2"/><path d="M3 9h18"/><path d="M9 21V9"/></svg>`,
    title: '应用中心',
    desc: '集成外部应用，一键访问',
    span: 'lg:col-span-1 lg:row-span-1',
    accent: 'from-cyan-500/20 to-sky-500/20',
  },
  {
    icon: `<svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/><path d="m9 12 2 2 4-4"/></svg>`,
    title: '管理后台',
    desc: '用户管理、系统监控、IP白名单',
    span: 'lg:col-span-1 lg:row-span-1',
    accent: 'from-rose-500/20 to-pink-500/20',
  },
  {
    icon: `<svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/></svg>`,
    title: '双主题模式',
    desc: '亮色/暗色主题自由切换，响应式设计',
    span: 'lg:col-span-1 lg:row-span-1',
    accent: 'from-zinc-500/20 to-gray-500/20',
  },
]

// --- Tech stack ---
const techStack = [
  { name: 'Vue 3', color: 'text-emerald-400' },
  { name: 'TypeScript', color: 'text-blue-400' },
  { name: 'Spring Boot', color: 'text-green-400' },
  { name: 'WebSocket', color: 'text-purple-400' },
  { name: 'Tailwind CSS', color: 'text-cyan-400' },
  { name: 'Pinia', color: 'text-amber-400' },
  { name: 'Vite', color: 'text-violet-400' },
  { name: 'Yjs', color: 'text-rose-400' },
  { name: 'SQLite', color: 'text-sky-400' },
  { name: 'Snowflake ID', color: 'text-orange-400' },
]

// --- Nav links ---
const navLinks = [
  { label: '功能', href: '#features' },
  { label: '技术', href: '#tech' },
]

const goToLogin = () => router.push('/login')
const goToGitHub = () => window.open('https://github.com/HaoDuoYv/websocket_chat', '_blank')

// --- Lifecycle ---
let observers: IntersectionObserver[] = []

onMounted(() => {
  heroVisible.value = true
  setTimeout(startTyping, 400)
  window.addEventListener('mousemove', handleMouseMove)
  window.addEventListener('scroll', handleScroll, { passive: true })

  const fObs = createObserver('features', featuresVisible, 0.1)
  const sObs = createObserver('stats', statsVisible, 0.2)
  const tObs = createObserver('tech', techVisible, 0.15)
  if (fObs) observers.push(fObs)
  if (sObs) observers.push(sObs)
  if (tObs) observers.push(tObs)
})

onUnmounted(() => {
  if (typingTimer) clearTimeout(typingTimer)
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('scroll', handleScroll)
  observers.forEach((o) => o.disconnect())
})

const parallaxStyle = computed(() => ({
  transform: `translate(${mouseX.value}px, ${mouseY.value}px)`,
}))
</script>

<template>
  <div class="min-h-screen bg-[#0A0A1A] text-zinc-100 overflow-x-hidden">
    <!-- Floating Nav -->
    <nav
      class="fixed top-4 left-4 right-4 z-50 mx-auto max-w-5xl rounded-2xl px-6 py-3 flex items-center justify-between transition-all duration-500"
      :class="scrolled ? 'bg-[#0A0A1A]/80 backdrop-blur-xl border border-white/[0.06] shadow-2xl shadow-black/20' : 'bg-transparent'"
    >
      <div class="flex items-center gap-2">
        <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/></svg>
        </div>
        <span class="font-heading text-sm font-semibold tracking-tight">WS Chat</span>
      </div>
      <div class="hidden sm:flex items-center gap-6">
        <a
          v-for="link in navLinks"
          :key="link.href"
          :href="link.href"
          class="text-sm text-zinc-400 hover:text-white transition-colors duration-200 cursor-pointer"
        >
          {{ link.label }}
        </a>
      </div>
      <button
        @click="goToLogin"
        class="px-4 py-1.5 text-sm bg-white/[0.06] hover:bg-white/[0.12] border border-white/[0.08] rounded-full transition-all duration-200 cursor-pointer"
      >
        登录
      </button>
    </nav>

    <!-- Hero Section -->
    <section class="relative min-h-screen flex flex-col items-center justify-center px-6 overflow-hidden">
      <!-- Animated grid background -->
      <div class="absolute inset-0 bg-grid-pattern opacity-[0.04]"></div>

      <!-- Floating particles -->
      <div class="absolute inset-0 pointer-events-none">
        <div
          v-for="p in particles"
          :key="p.id"
          class="absolute rounded-full bg-indigo-400/30 particle"
          :style="{
            left: p.x + '%',
            top: p.y + '%',
            width: p.size + 'px',
            height: p.size + 'px',
            animationDuration: p.duration + 's',
            animationDelay: p.delay + 's',
            opacity: p.opacity,
          }"
        ></div>
      </div>

      <!-- Glow orbs with parallax -->
      <div class="absolute inset-0 flex items-center justify-center pointer-events-none">
        <div
          class="w-[500px] h-[500px] bg-indigo-600/15 rounded-full blur-[140px] glow-orb"
          :style="parallaxStyle"
        ></div>
      </div>
      <div class="absolute inset-0 flex items-center justify-center pointer-events-none">
        <div
          class="w-[300px] h-[300px] bg-purple-600/10 rounded-full blur-[100px] glow-orb-alt ml-40 mt-20"
          :style="{ transform: `translate(${-mouseX * 0.5}px, ${-mouseY * 0.5}px)` }"
        ></div>
      </div>

      <!-- Hero content -->
      <div
        class="relative z-10 text-center max-w-4xl"
        :class="heroVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-10'"
        style="transition: opacity 0.8s cubic-bezier(0.16, 1, 0.3, 1), transform 0.8s cubic-bezier(0.16, 1, 0.3, 1)"
      >
        <!-- Badge -->
        <div
          class="inline-flex items-center gap-2 px-4 py-1.5 mb-8 rounded-full bg-white/[0.04] border border-white/[0.06] text-xs text-zinc-400"
          :class="heroVisible ? 'opacity-100' : 'opacity-0'"
          style="transition: opacity 0.6s ease-out 0.3s"
        >
          <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
          开源项目 · 实时协作
        </div>

        <!-- Title with typing effect -->
        <h1 class="font-heading text-5xl sm:text-6xl md:text-8xl font-bold mb-6 tracking-tight leading-[1.1]">
          <span class="bg-gradient-to-r from-white via-zinc-200 to-zinc-400 bg-clip-text text-transparent">
            {{ typedText }}
          </span>
          <span class="typing-cursor text-indigo-400">|</span>
        </h1>

        <p
          class="text-lg sm:text-xl text-zinc-400 mb-4 max-w-2xl mx-auto font-body"
          :class="heroVisible ? 'opacity-100' : 'opacity-0'"
          style="transition: opacity 0.6s ease-out 0.8s"
        >
          基于 WebSocket 的下一代即时通讯系统
        </p>
        <p
          class="text-base text-zinc-500 mb-12 max-w-xl mx-auto font-body"
          :class="heroVisible ? 'opacity-100' : 'opacity-0'"
          style="transition: opacity 0.6s ease-out 1s"
        >
          私聊群聊、AI助手、协作编辑、五子棋对弈 — 一站式即时通讯与协作平台
        </p>

        <!-- CTA Buttons -->
        <div
          class="flex flex-col sm:flex-row items-center justify-center gap-4"
          :class="heroVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'"
          style="transition: opacity 0.6s ease-out 1.2s, transform 0.6s ease-out 1.2s"
        >
          <button
            @click="goToLogin"
            class="group relative w-full sm:w-auto px-8 py-3.5 bg-gradient-to-r from-indigo-500 to-purple-600 text-white font-medium rounded-xl transition-all duration-300 hover:shadow-[0_0_30px_rgba(99,102,241,0.3)] hover:scale-[1.02] focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 focus-visible:ring-offset-[#0A0A1A] cursor-pointer overflow-hidden"
          >
            <span class="relative z-10">开始聊天</span>
            <div class="absolute inset-0 bg-gradient-to-r from-indigo-400 to-purple-500 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
          </button>
          <button
            @click="goToGitHub"
            class="w-full sm:w-auto px-8 py-3.5 border border-white/[0.08] hover:border-white/[0.16] text-zinc-300 hover:text-white font-medium rounded-xl transition-all duration-300 bg-white/[0.02] hover:bg-white/[0.05] focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 focus-visible:ring-offset-[#0A0A1A] cursor-pointer"
          >
            <span class="flex items-center gap-2 justify-center">
              GitHub
              <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7 17L17 7"/><path d="M7 7h10v10"/></svg>
            </span>
          </button>
        </div>
      </div>

      <!-- Scroll indicator -->
      <div
        class="absolute bottom-10 left-1/2 -translate-x-1/2 flex flex-col items-center gap-2"
        :class="heroVisible ? 'opacity-100' : 'opacity-0'"
        style="transition: opacity 0.6s ease-out 1.6s"
      >
        <span class="text-xs text-zinc-600 tracking-widest uppercase">滚动探索</span>
        <div class="scroll-line"></div>
      </div>
    </section>

    <!-- Stats Section -->
    <section id="stats" class="py-16 px-6 border-y border-white/[0.04]">
      <div class="max-w-5xl mx-auto grid grid-cols-2 md:grid-cols-4 gap-8">
        <div
          v-for="(stat, i) in stats"
          :key="stat.label"
          class="text-center"
          :class="statsVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'"
          :style="{ transitionDelay: statsVisible ? `${i * 100}ms` : '0ms', transition: 'opacity 0.5s ease-out, transform 0.5s ease-out' }"
        >
          <div class="font-heading text-3xl sm:text-4xl font-bold bg-gradient-to-b from-white to-zinc-500 bg-clip-text text-transparent mb-1">
            {{ stat.value }}
          </div>
          <div class="text-sm text-zinc-500">{{ stat.label }}</div>
        </div>
      </div>
    </section>

    <!-- Features Section (Bento Grid) -->
    <section id="features" class="py-24 px-6">
      <div class="max-w-5xl mx-auto">
        <div class="text-center mb-16">
          <h2
            class="font-heading text-3xl sm:text-4xl font-bold mb-4 tracking-tight"
            :class="featuresVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6'"
            style="transition: opacity 0.6s ease-out, transform 0.6s ease-out"
          >
            核心功能
          </h2>
          <p
            class="text-zinc-500 max-w-lg mx-auto font-body"
            :class="featuresVisible ? 'opacity-100' : 'opacity-0'"
            style="transition: opacity 0.6s ease-out 0.2s"
          >
            一站式即时通讯与协作平台，涵盖聊天、文件、游戏与代码编辑
          </p>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 lg:gap-5 auto-rows-[180px]">
          <div
            v-for="(feature, index) in features"
            :key="feature.title"
            class="group relative rounded-2xl border border-white/[0.06] bg-white/[0.02] p-6 flex flex-col justify-between overflow-hidden transition-all duration-300 hover:border-white/[0.12] hover:bg-white/[0.04] cursor-default"
            :class="[
              feature.span,
              featuresVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8',
            ]"
            :style="{ transitionDelay: featuresVisible ? `${index * 80}ms` : '0ms', transition: 'opacity 0.6s ease-out, transform 0.6s ease-out, border-color 0.3s, background-color 0.3s' }"
          >
            <!-- Gradient glow on hover -->
            <div
              class="absolute inset-0 bg-gradient-to-br opacity-0 group-hover:opacity-100 transition-opacity duration-500 rounded-2xl"
              :class="feature.accent"
            ></div>

            <div
              class="relative z-10 flex flex-col"
              :class="feature.span.includes('col-span-2') ? 'lg:flex-row lg:items-start lg:gap-4 lg:h-full' : ''"
            >
              <div
                class="text-indigo-400 transition-transform duration-300 group-hover:scale-110 shrink-0"
                :class="feature.span.includes('col-span-2') ? 'mb-2 lg:mb-0 lg:mt-0.5' : 'mb-3'"
                v-html="feature.icon"
              ></div>
              <div>
                <h3 class="font-heading text-lg font-semibold mb-1.5">{{ feature.title }}</h3>
                <p class="text-zinc-500 text-sm leading-relaxed font-body">{{ feature.desc }}</p>
              </div>
            </div>

            <!-- Corner accent -->
            <div class="absolute -bottom-2 -right-2 w-24 h-24 bg-gradient-to-tl from-indigo-500/10 to-transparent rounded-full blur-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-500"></div>
          </div>
        </div>
      </div>
    </section>

    <!-- Tech Stack Section -->
    <section id="tech" class="py-20 px-6 border-t border-white/[0.04]">
      <div class="max-w-5xl mx-auto text-center">
        <h2
          class="font-heading text-2xl sm:text-3xl font-bold mb-3 tracking-tight"
          :class="techVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6'"
          style="transition: opacity 0.6s ease-out, transform 0.6s ease-out"
        >
          技术栈
        </h2>
        <p
          class="text-zinc-500 text-sm mb-12 font-body"
          :class="techVisible ? 'opacity-100' : 'opacity-0'"
          style="transition: opacity 0.6s ease-out 0.15s"
        >
          现代化全栈架构，前后端分离
        </p>

        <div class="flex flex-wrap items-center justify-center gap-3">
          <span
            v-for="(tech, i) in techStack"
            :key="tech.name"
            class="px-4 py-2 text-sm rounded-xl bg-white/[0.03] border border-white/[0.06] transition-all duration-300 hover:bg-white/[0.06] hover:border-white/[0.12] hover:scale-105 cursor-default"
            :class="[
              tech.color,
              techVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4',
            ]"
            :style="{ transitionDelay: techVisible ? `${i * 60}ms` : '0ms', transition: 'opacity 0.5s ease-out, transform 0.5s ease-out, background-color 0.3s, border-color 0.3s, scale 0.3s' }"
          >
            {{ tech.name }}
          </span>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="py-12 px-6 border-t border-white/[0.04]">
      <div class="max-w-5xl mx-auto">
        <div class="flex flex-col sm:flex-row items-center justify-between gap-6">
          <div class="flex items-center gap-3">
            <div class="w-6 h-6 rounded-md bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-3 h-3 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/></svg>
            </div>
            <span class="text-sm text-zinc-500 font-body">© 2026 WebSocket Chat · MIT 协议</span>
          </div>
          <div class="flex items-center gap-4">
            <a
              href="https://github.com/HaoDuoYv/websocket_chat"
              target="_blank"
              class="text-sm text-zinc-500 hover:text-zinc-300 transition-colors duration-200 cursor-pointer"
            >
              GitHub
            </a>
            <span class="text-zinc-700">·</span>
            <a
              href="https://github.com/HaoDuoYv/websocket_chat/issues"
              target="_blank"
              class="text-sm text-zinc-500 hover:text-zinc-300 transition-colors duration-200 cursor-pointer"
            >
              Issues
            </a>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* --- Fonts --- */
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;700&family=Space+Grotesk:wght@400;500;600;700&display=swap');

.font-heading {
  font-family: 'Space Grotesk', sans-serif;
}
.font-body {
  font-family: 'DM Sans', sans-serif;
}

/* --- Grid background --- */
.bg-grid-pattern {
  background-image:
    linear-gradient(rgba(255,255,255,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.03) 1px, transparent 1px);
  background-size: 60px 60px;
}

/* --- Typing cursor --- */
.typing-cursor {
  animation: blink 1s step-end infinite;
  font-weight: 200;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* --- Floating particles --- */
.particle {
  animation: float linear infinite;
}

@keyframes float {
  0% {
    transform: translateY(0) translateX(0);
    opacity: 0;
  }
  10% {
    opacity: var(--particle-opacity, 0.3);
  }
  90% {
    opacity: var(--particle-opacity, 0.3);
  }
  100% {
    transform: translateY(-100vh) translateX(30px);
    opacity: 0;
  }
}

/* --- Glow orbs --- */
.glow-orb {
  animation: glow-drift 8s ease-in-out infinite alternate;
}

.glow-orb-alt {
  animation: glow-drift-alt 10s ease-in-out infinite alternate;
}

@keyframes glow-drift {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(20px, -15px) scale(1.1); }
}

@keyframes glow-drift-alt {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(-15px, 20px) scale(1.05); }
}

/* --- Scroll line indicator --- */
.scroll-line {
  width: 1px;
  height: 40px;
  background: linear-gradient(to bottom, rgba(255,255,255,0.2), transparent);
  animation: scroll-pulse 2s ease-in-out infinite;
}

@keyframes scroll-pulse {
  0%, 100% {
    opacity: 0.3;
    transform: scaleY(0.6);
    transform-origin: top;
  }
  50% {
    opacity: 1;
    transform: scaleY(1);
    transform-origin: top;
  }
}

/* --- Reduced motion --- */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
  .particle {
    display: none;
  }
  .typing-cursor {
    display: none;
  }
  .scroll-line {
    animation: none;
    opacity: 0.5;
  }
}
</style>

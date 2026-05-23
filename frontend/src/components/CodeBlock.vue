<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  code: string
  language?: string
}>()

const copied = ref(false)

const displayLanguage = computed(() => {
  if (props.language) return props.language
  // 尝试从代码内容推断语言
  const firstLine = props.code.split('\n')[0].trim().toLowerCase()
  if (firstLine.includes('python') || firstLine.includes('def ') || firstLine.includes('import ')) return 'python'
  if (firstLine.includes('function') || firstLine.includes('const ') || firstLine.includes('let ')) return 'javascript'
  if (firstLine.includes('public class') || firstLine.includes('import java')) return 'java'
  if (firstLine.includes('#include') || firstLine.includes('int main')) return 'c/c++'
  if (firstLine.includes('<!doctype') || firstLine.includes('<html')) return 'html'
  if (firstLine.includes('select ') || firstLine.includes('insert ') || firstLine.includes('update ')) return 'sql'
  return 'code'
})

const lines = computed(() => props.code.split('\n'))

const handleCopy = async () => {
  try {
    await navigator.clipboard.writeText(props.code)
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch (err) {
    // 降级方案
    const textarea = document.createElement('textarea')
    textarea.value = props.code
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  }
}
</script>

<template>
  <div class="code-block my-3 rounded-xl overflow-hidden border border-gray-600/50">
    <!-- 顶部栏 -->
    <div class="flex items-center justify-between px-4 py-2.5 bg-[#2b2b2b] border-b border-gray-600/50">
      <div class="flex items-center gap-2">
        <!-- 三个圆点 -->
        <div class="flex gap-1.5">
          <div class="w-3 h-3 rounded-full bg-[#ff5f57]"></div>
          <div class="w-3 h-3 rounded-full bg-[#febc2e]"></div>
          <div class="w-3 h-3 rounded-full bg-[#28c840]"></div>
        </div>
        <span class="text-xs text-gray-400 ml-2 font-mono">{{ displayLanguage }}</span>
      </div>
      <button
        @click="handleCopy"
        class="flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs transition-all duration-200 cursor-pointer"
        :class="copied 
          ? 'bg-emerald-500/20 text-emerald-400' 
          : 'bg-white/5 text-gray-400 hover:bg-white/10 hover:text-gray-200'"
      >
        <svg v-if="!copied" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
          <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
        </svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
        {{ copied ? '已复制' : '复制' }}
      </button>
    </div>
    
    <!-- 代码内容 -->
    <div class="overflow-x-auto bg-[#1e1e1e]">
      <table class="w-full border-collapse">
        <tbody>
          <tr v-for="(line, index) in lines" :key="index" class="hover:bg-white/[0.03] transition-colors">
            <td class="text-right pr-4 pl-4 py-0 select-none text-gray-500 text-xs font-mono whitespace-nowrap align-top" style="min-width: 3rem">
              {{ index + 1 }}
            </td>
            <td class="text-left px-4 py-0 text-gray-200 text-xs font-mono whitespace-pre leading-6">
              {{ line }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.code-block {
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);
}

/* 滚动条样式 */
.code-block ::-webkit-scrollbar {
  height: 6px;
}

.code-block ::-webkit-scrollbar-track {
  background: transparent;
}

.code-block ::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.code-block ::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.2);
}
</style>
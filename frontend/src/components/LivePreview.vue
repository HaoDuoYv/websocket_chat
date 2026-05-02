<script setup lang="ts">
import { ref, watch, computed } from 'vue'

const props = defineProps<{
  htmlContent: string
  cssContent: string
  jsContent: string
}>()

const iframeRef = ref<HTMLIFrameElement | null>(null)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

const srcdoc = computed(() => {
  return `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
body { margin: 0; padding: 8px; font-family: sans-serif; }
${props.cssContent}
</style>
</head>
<body>
${props.htmlContent}
<script>
try {
${props.jsContent}
} catch(e) {
  console.error(e);
}
<\/script>
</body>
</html>`
})

watch(srcdoc, () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    if (iframeRef.value) {
      iframeRef.value.srcdoc = srcdoc.value
    }
  }, 300)
}, { immediate: true })
</script>

<template>
  <iframe
    ref="iframeRef"
    :srcdoc="srcdoc"
    sandbox="allow-scripts"
    class="w-full h-full border-0 bg-white"
    title="实时预览"
  ></iframe>
</template>

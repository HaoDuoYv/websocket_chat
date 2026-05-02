<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, shallowRef } from 'vue'
import * as Y from 'yjs'
import { Awareness } from 'y-protocols/awareness'
import { yCollab, yRemoteSelectionsTheme } from 'y-codemirror.next'
import { EditorView, keymap, lineNumbers, highlightActiveLine, highlightActiveLineGutter, drawSelection, rectangularSelection } from '@codemirror/view'
import { EditorState } from '@codemirror/state'
import { defaultKeymap, historyKeymap } from '@codemirror/commands'
import { html } from '@codemirror/lang-html'
import { css } from '@codemirror/lang-css'
import { javascript } from '@codemirror/lang-javascript'
import { oneDark } from '@codemirror/theme-one-dark'
import { bracketMatching, indentOnInput, syntaxHighlighting, defaultHighlightStyle, foldGutter } from '@codemirror/language'
import { closeBrackets, closeBracketsKeymap } from '@codemirror/autocomplete'
import { highlightSelectionMatches, searchKeymap } from '@codemirror/search'

const props = defineProps<{
  language: 'html' | 'css' | 'js'
  dark: boolean
  docId: string
  sendUpdate: (docId: string, update: Uint8Array, msgType?: 'sync' | 'awareness') => void
  setOnYjsUpdate: (cb: (update: Uint8Array, msgType: string) => void) => void
  setOnSyncRequest: (cb: (docId: string) => void) => void
}>()

const emit = defineEmits<{
  (e: 'textChange', language: string, text: string): void
}>()

const editorContainer = ref<HTMLDivElement | null>(null)
const editorView = shallowRef<EditorView | null>(null)

let ydoc: Y.Doc | null = null
let ytext: Y.Text | null = null

const langExtensions = {
  html: html(),
  css: css(),
  js: javascript()
}

const editorTheme = EditorView.theme({
  '&': {
    fontFamily: "'JetBrains Mono', 'Fira Code', 'Cascadia Code', Consolas, 'Courier New', monospace",
    fontSize: '13px',
    height: '100%'
  },
  '.cm-content': {
    padding: '8px 0',
    caretColor: '#3b82f6'
  },
  '.cm-line': {
    padding: '0 8px'
  },
  '&.cm-focused': {
    outline: 'none'
  },
  '.cm-gutters': {
    borderRight: '1px solid',
    paddingRight: '4px',
    minWidth: '40px'
  },
  '.cm-activeLineGutter': {
    backgroundColor: 'transparent'
  },
  '.cm-foldGutter': {
    width: '14px'
  }
})

const editorThemeDark = EditorView.theme({
  '.cm-gutters': {
    borderRightColor: '#2e2e32',
    backgroundColor: '#1a1a1d',
    color: '#555'
  },
  '.cm-activeLine': {
    backgroundColor: '#2a2a2e80'
  },
  '.cm-cursor': {
    borderLeftColor: '#3b82f6'
  }
})

const editorThemeLight = EditorView.theme({
  '.cm-gutters': {
    borderRightColor: '#e5e7eb',
    backgroundColor: '#f9fafb',
    color: '#9ca3af'
  },
  '.cm-activeLine': {
    backgroundColor: '#f3f4f680'
  },
  '.cm-cursor': {
    borderLeftColor: '#3b82f6'
  }
})

function getText() {
  return ytext ? ytext.toString() : ''
}

function createEditor() {
  if (!editorContainer.value) return

  ydoc = new Y.Doc()
  ytext = ydoc.getText(`content-${props.language}`)

  ydoc.on('update', (update: Uint8Array, origin: any) => {
    if (origin === 'server') return
    console.log(`[CodeEditor] ydoc update fired, lang=${props.language}, origin=${origin}, bytes=${update.length}, text="${ytext!.toString().substring(0, 30)}"`)
    props.sendUpdate(props.docId, update, 'sync')
    emit('textChange', props.language, ytext!.toString())
  })

  props.setOnYjsUpdate((update: Uint8Array, msgType: string) => {
    if (!ydoc) {
      console.warn('[CodeEditor] onYjsUpdate called but ydoc is null!')
      return
    }
    const before = ytext!.toString()
    Y.applyUpdate(ydoc, update, 'server')
    const after = ytext!.toString()
    console.log(`[CodeEditor] applied remote update, lang=${props.language}, bytes=${update.length}, text changed: "${before.substring(0, 20)}" → "${after.substring(0, 20)}"`)
    if (msgType === 'sync') {
      emit('textChange', props.language, after)
    }
  })

  props.setOnSyncRequest((docId: string) => {
    if (!ydoc) {
      console.warn('[CodeEditor] onSyncRequest called but ydoc is null!')
      return
    }
    const fullState = Y.encodeStateAsUpdate(ydoc)
    console.log(`[CodeEditor] sending full state for sync-request, lang=${props.language}, bytes=${fullState.length}, text="${ytext!.toString().substring(0, 30)}"`)
    props.sendUpdate(docId, fullState, 'sync')
  })

  const awareness = new Awareness(ydoc)
  const undoManager = new Y.UndoManager(ytext)

  const baseExtensions = [
    lineNumbers(),
    highlightActiveLine(),
    highlightActiveLineGutter(),
    drawSelection(),
    rectangularSelection(),
    bracketMatching(),
    closeBrackets(),
    indentOnInput(),
    highlightSelectionMatches(),
    foldGutter(),
    syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
    yCollab(ytext, awareness, { undoManager }),
    yRemoteSelectionsTheme,
    keymap.of([
      ...defaultKeymap,
      ...historyKeymap,
      ...closeBracketsKeymap,
      ...searchKeymap
    ]),
    langExtensions[props.language] || [],
    EditorView.lineWrapping,
    EditorState.tabSize.of(2),
    editorTheme
  ]

  if (props.dark) {
    baseExtensions.push(oneDark, editorThemeDark)
  } else {
    baseExtensions.push(editorThemeLight)
  }

  const state = EditorState.create({
    doc: ytext.toString(),
    extensions: baseExtensions
  })

  editorView.value = new EditorView({
    state,
    parent: editorContainer.value
  })
}

function destroyEditor() {
  editorView.value?.destroy()
  editorView.value = null
  ydoc?.destroy()
  ydoc = null
  ytext = null
}

watch(() => props.language, () => {
  destroyEditor()
  createEditor()
})

watch(() => props.dark, () => {
  destroyEditor()
  createEditor()
})

onMounted(() => {
  createEditor()
})

onUnmounted(() => {
  destroyEditor()
})

defineExpose({ getText })
</script>

<template>
  <div ref="editorContainer" class="h-full overflow-auto"></div>
</template>

export const MAX_IMAGE_SIZE = 10 * 1024 * 1024
export const MAX_TEXT_SIZE = 500 * 1024
export const MAX_ATTACHMENTS = 5

export const SUPPORTED_TEXT_EXTS = [
  '.txt', '.md', '.csv', '.json',
  '.js', '.ts', '.jsx', '.tsx',
  '.py', '.java', '.go', '.rs',
  '.c', '.cpp', '.h',
  '.css', '.html', '.xml',
  '.yaml', '.yml', '.sh', '.sql'
]

export const SUPPORTED_TEXT_ACCEPT = SUPPORTED_TEXT_EXTS.join(',')
export const FILE_INPUT_ACCEPT = `image/*,${SUPPORTED_TEXT_ACCEPT}`

export type AttachmentKind = 'image' | 'text'

export interface PendingAttachment {
  id: string
  kind: AttachmentKind
  name: string
  mimeType: string
  size: number
  data: string
  previewUrl?: string
}

export function isImage(file: File): boolean {
  return file.type.startsWith('image/')
}

export function isSupportedText(file: File): boolean {
  const lower = file.name.toLowerCase()
  return SUPPORTED_TEXT_EXTS.some(ext => lower.endsWith(ext))
}

export function detectKind(file: File): AttachmentKind | null {
  if (isImage(file)) return 'image'
  if (isSupportedText(file)) return 'text'
  return null
}

export function validateFile(file: File, kind: AttachmentKind): string | null {
  if (kind === 'image' && file.size > MAX_IMAGE_SIZE) {
    return `图片不能超过 ${MAX_IMAGE_SIZE / 1024 / 1024}MB`
  }
  if (kind === 'text' && file.size > MAX_TEXT_SIZE) {
    return `文本文件不能超过 ${MAX_TEXT_SIZE / 1024}KB`
  }
  return null
}

export async function readFileAsBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = reader.result as string
      const idx = result.indexOf(',')
      resolve(idx >= 0 ? result.slice(idx + 1) : result)
    }
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })
}

export async function readFileAsText(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(reader.error)
    reader.readAsText(file, 'utf-8')
  })
}

export async function buildPendingAttachment(file: File): Promise<PendingAttachment | { error: string }> {
  const kind = detectKind(file)
  if (!kind) return { error: `不支持的文件类型: ${file.name}` }
  const sizeErr = validateFile(file, kind)
  if (sizeErr) return { error: sizeErr }

  const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  if (kind === 'image') {
    const data = await readFileAsBase64(file)
    return {
      id,
      kind,
      name: file.name,
      mimeType: file.type || 'image/png',
      size: file.size,
      data,
      previewUrl: `data:${file.type || 'image/png'};base64,${data}`
    }
  } else {
    const data = await readFileAsText(file)
    return {
      id,
      kind,
      name: file.name,
      mimeType: file.type || 'text/plain',
      size: file.size,
      data
    }
  }
}

export function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes}B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`
  return `${(bytes / 1024 / 1024).toFixed(1)}MB`
}

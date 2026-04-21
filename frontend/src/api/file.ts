import axios from 'axios'

const API_BASE_URL = '/api'

export interface FileUploadResponse {
  success: boolean
  message: string
  fileId: string
  fileName: string
  fileSize: number
  fileUrl: string
  fileType: string
}

export const normalizeFileUrl = (fileUrl: string): string => {
  if (!fileUrl) return fileUrl

  return fileUrl
}

export const uploadFile = async (
  file: File,
  chatId: string,
  senderId: string,
  onProgress?: (progress: number) => void
): Promise<FileUploadResponse> => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('chatId', chatId)
  formData.append('senderId', senderId)

  try {
    const response = await axios.post<FileUploadResponse>(
      `${API_BASE_URL}/file/upload`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total && onProgress) {
            const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            onProgress(progress)
          }
        }
      }
    )
    return response.data
  } catch (error: any) {
    if (error.response?.data) {
      return error.response.data as FileUploadResponse
    }
    throw error
  }
}

export const getFileInfo = async (fileId: string): Promise<FileUploadResponse> => {
  const response = await axios.get<FileUploadResponse>(`${API_BASE_URL}/file/info/${fileId}`)
  return response.data
}

// 格式化文件大小
export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 判断是否为图片文件
export const isImageFile = (fileType: string): boolean => {
  return fileType?.startsWith('image/') || false
}

// 判断是否为视频文件
export const isVideoFile = (fileType: string): boolean => {
  return fileType?.startsWith('video/') || false
}

// 增强的文件图标映射
export const getFileIcon = (fileName: string): string => {
  const extension = fileName.split('.').pop()?.toLowerCase() || ''
  const iconMap: Record<string, string> = {
    // 图片文件
    jpg: '🖼️',
    jpeg: '🖼️',
    png: '🖼️',
    gif: '🖼️',
    webp: '🖼️',
    svg: '🖼️',
    bmp: '🖼️',
    ico: '🖼️',

    // 文档文件
    pdf: '📄',
    doc: '📝',
    docx: '📝',
    txt: '📃',
    rtf: '📝',
    md: '📄',

    // 表格文件
    xls: '📊',
    xlsx: '📊',
    csv: '📊',

    // 演示文件
    ppt: '📽️',
    pptx: '📽️',

    // 压缩文件
    zip: '📦',
    rar: '📦',
    '7z': '📦',
    tar: '📦',
    gz: '📦',

    // 音频文件
    mp3: '🎵',
    wav: '🎵',
    ogg: '🎵',
    flac: '🎵',
    aac: '🎵',
    wma: '🎵',

    // 视频文件
    mp4: '🎬',
    avi: '🎬',
    mov: '🎬',
    wmv: '🎬',
    mkv: '🎬',
    flv: '🎬',

    // 代码文件
    js: '💻',
    ts: '💻',
    html: '🌐',
    css: '🎨',
    scss: '🎨',
    json: '📄',
    xml: '📄',
    java: '☕',
    py: '🐍',
    php: '🐘',
    go: '🚀',
    c: '⚙️',
    cpp: '⚙️',
    h: '⚙️',

    // 其他文件
    exe: '⚙️',
    dmg: '💿',
    apk: '📱',
    sql: '🗃️',
    sh: '💻',
  }
  return iconMap[extension] || '📎'
}

// 文件类型描述
export const getFileTypeDescription = (fileType: string): string => {
  const typeMap: Record<string, string> = {
    'image/jpeg': 'JPEG 图片',
    'image/png': 'PNG 图片',
    'image/gif': 'GIF 图片',
    'image/webp': 'WebP 图片',
    'image/svg+xml': 'SVG 图片',
    'image/bmp': 'BMP 图片',
    'application/pdf': 'PDF 文档',
    'application/msword': 'Word 文档',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'Word 文档',
    'application/vnd.ms-excel': 'Excel 表格',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'Excel 表格',
    'application/vnd.ms-powerpoint': 'PowerPoint 演示',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation': 'PowerPoint 演示',
    'text/plain': '文本文件',
    'text/html': 'HTML 文件',
    'text/css': 'CSS 文件',
    'text/csv': 'CSV 表格',
    'application/javascript': 'JavaScript 文件',
    'application/json': 'JSON 文件',
    'application/xml': 'XML 文件',
    'application/zip': 'ZIP 压缩文件',
    'application/x-rar-compressed': 'RAR 压缩文件',
    'application/x-7z-compressed': '7Z 压缩文件',
    'audio/mpeg': 'MP3 音频',
    'audio/wav': 'WAV 音频',
    'audio/ogg': 'OGG 音频',
    'video/mp4': 'MP4 视频',
    'video/avi': 'AVI 视频',
    'video/quicktime': 'MOV 视频',
    'video/x-msvideo': 'WMV 视频',
    'video/x-matroska': 'MKV 视频',
  }
  return typeMap[fileType] || '文件'
}

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

// 获取文件图标
export const getFileIcon = (fileName: string): string => {
  const extension = fileName.split('.').pop()?.toLowerCase() || ''
  const iconMap: Record<string, string> = {
    pdf: '📄',
    doc: '📝',
    docx: '📝',
    xls: '📊',
    xlsx: '📊',
    ppt: '📽️',
    pptx: '📽️',
    txt: '📃',
    zip: '📦',
    rar: '📦',
    '7z': '📦',
    mp3: '🎵',
    mp4: '🎬',
    avi: '🎬',
    jpg: '🖼️',
    jpeg: '🖼️',
    png: '🖼️',
    gif: '🖼️'
  }
  return iconMap[extension] || '📎'
}

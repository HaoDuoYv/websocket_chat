import axios from 'axios'
import { getApiUrl } from './server-config'

export interface AvatarUploadResponse {
  success: boolean
  message: string
  url?: string
  tempPath?: string
}

/** 旧接口：直接上传并确认（兼容，内部也用 SHA-256 去重） */
export const uploadUserAvatar = async (
  userId: string,
  file: File,
  onProgress?: (progress: number) => void
): Promise<AvatarUploadResponse> => {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const response = await axios.post<AvatarUploadResponse>(
      getApiUrl(`/api/avatar/user/${userId}`),
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total && onProgress) {
            onProgress(Math.round((progressEvent.loaded * 100) / progressEvent.total))
          }
        }
      }
    )
    return response.data
  } catch (error: any) {
    if (error.response?.data) return error.response.data as AvatarUploadResponse
    throw error
  }
}

/** 第一阶段：上传到临时目录 */
export const uploadUserAvatarTemp = async (
  userId: string,
  file: File,
  onProgress?: (progress: number) => void
): Promise<AvatarUploadResponse> => {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const response = await axios.post<AvatarUploadResponse>(
      getApiUrl(`/api/avatar/user/${userId}/temp`),
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total && onProgress) {
            onProgress(Math.round((progressEvent.loaded * 100) / progressEvent.total))
          }
        }
      }
    )
    return response.data
  } catch (error: any) {
    if (error.response?.data) return error.response.data as AvatarUploadResponse
    throw error
  }
}

/** 第二阶段：确认头像 */
export const confirmUserAvatar = async (
  userId: string,
  tempPath: string
): Promise<AvatarUploadResponse> => {
  const response = await axios.post<AvatarUploadResponse>(
    getApiUrl(`/api/avatar/user/${userId}/confirm`),
    { tempPath }
  )
  return response.data
}

/** 取消头像上传 */
export const cancelAvatar = async (tempPath: string): Promise<void> => {
  await axios.post(getApiUrl('/api/avatar/cancel'), { tempPath })
}

/** 第一阶段：群聊头像上传到临时目录 */
export const uploadRoomAvatarTemp = async (
  roomId: string,
  file: File,
  onProgress?: (progress: number) => void
): Promise<AvatarUploadResponse> => {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const response = await axios.post<AvatarUploadResponse>(
      getApiUrl(`/api/avatar/room/${roomId}/temp`),
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total && onProgress) {
            onProgress(Math.round((progressEvent.loaded * 100) / progressEvent.total))
          }
        }
      }
    )
    return response.data
  } catch (error: any) {
    if (error.response?.data) return error.response.data as AvatarUploadResponse
    throw error
  }
}

/** 第二阶段：确认群聊头像 */
export const confirmRoomAvatar = async (
  roomId: string,
  tempPath: string
): Promise<AvatarUploadResponse> => {
  try {
    const response = await axios.post<AvatarUploadResponse>(
      getApiUrl(`/api/avatar/room/${roomId}/confirm`),
      { tempPath }
    )
    return response.data
  } catch (error: any) {
    if (error.response?.data) return error.response.data as AvatarUploadResponse
    throw error
  }
}

/** 旧接口：直接上传AI助手头像 */
export const uploadAiAvatar = async (
  assistantId: string,
  file: File,
  onProgress?: (progress: number) => void
): Promise<AvatarUploadResponse> => {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const response = await axios.post<AvatarUploadResponse>(
      getApiUrl(`/api/avatar/ai/${assistantId}`),
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total && onProgress) {
            onProgress(Math.round((progressEvent.loaded * 100) / progressEvent.total))
          }
        }
      }
    )
    return response.data
  } catch (error: any) {
    if (error.response?.data) return error.response.data as AvatarUploadResponse
    throw error
  }
}

/** 第一阶段：AI助手头像上传到临时目录 */
export const uploadAiAvatarTemp = async (
  assistantId: string,
  file: File,
  onProgress?: (progress: number) => void
): Promise<AvatarUploadResponse> => {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const response = await axios.post<AvatarUploadResponse>(
      getApiUrl(`/api/avatar/ai/${assistantId}/temp`),
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total && onProgress) {
            onProgress(Math.round((progressEvent.loaded * 100) / progressEvent.total))
          }
        }
      }
    )
    return response.data
  } catch (error: any) {
    if (error.response?.data) return error.response.data as AvatarUploadResponse
    throw error
  }
}

/** 第二阶段：确认AI助手头像 */
export const confirmAiAvatar = async (
  assistantId: string,
  tempPath: string
): Promise<AvatarUploadResponse> => {
  const response = await axios.post<AvatarUploadResponse>(
    getApiUrl(`/api/avatar/ai/${assistantId}/confirm`),
    { tempPath }
  )
  return response.data
}

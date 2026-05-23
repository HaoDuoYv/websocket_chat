import axios from 'axios'

const API_BASE_URL = '/api'

export interface AvatarUploadResponse {
  success: boolean
  message: string
  url: string
}

export const uploadUserAvatar = async (
  userId: number,
  file: File,
  onProgress?: (progress: number) => void
): Promise<AvatarUploadResponse> => {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const response = await axios.post<AvatarUploadResponse>(
      `${API_BASE_URL}/avatar/user/${userId}`,
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
      return error.response.data as AvatarUploadResponse
    }
    throw error
  }
}

export const uploadAiAvatar = async (
  assistantId: number,
  file: File,
  onProgress?: (progress: number) => void
): Promise<AvatarUploadResponse> => {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const response = await axios.post<AvatarUploadResponse>(
      `${API_BASE_URL}/avatar/ai/${assistantId}`,
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
      return error.response.data as AvatarUploadResponse
    }
    throw error
  }
}

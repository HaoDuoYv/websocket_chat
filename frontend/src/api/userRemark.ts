import axios from 'axios'
import { getApiUrl } from './server-config'

export interface UserRemarkMapResponse {
  remarks: Record<string, string>
}

export interface SaveUserRemarkPayload {
  userId: string
  targetUserId: string
  remarkName: string
}

export interface SaveUserRemarkResponse {
  id: string
  userId: string
  targetUserId: string
  remarkName: string
  updatedAt: number
}

export const getUserRemarks = async (userId: string) => {
  const response = await axios.get<UserRemarkMapResponse>(getApiUrl('/api/user-remarks'), {
    params: { userId }
  })
  return response.data
}

export const saveUserRemark = async (payload: SaveUserRemarkPayload) => {
  const response = await axios.post<SaveUserRemarkResponse>(getApiUrl('/api/user-remarks'), payload)
  return response.data
}

import axios from 'axios'

const API_BASE_URL = '/api/user-remarks'

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
  const response = await axios.get<UserRemarkMapResponse>(API_BASE_URL, {
    params: { userId }
  })
  return response.data
}

export const saveUserRemark = async (payload: SaveUserRemarkPayload) => {
  const response = await axios.post<SaveUserRemarkResponse>(API_BASE_URL, payload)
  return response.data
}

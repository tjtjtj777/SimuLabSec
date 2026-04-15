import { http } from '@/api/http'
import type { SubRecipeDetail, SubRecipeFileTicket, SubRecipeItem } from '@/types/domain'

export const subRecipeApi = {
  getList(
    params: {
      recipeVersionId?: number
      lotId?: number
      waferId?: number
      generationType?: string
      status?: string
    } = {},
  ) {
    return http.get<SubRecipeItem[], SubRecipeItem[]>('/api/sub-recipes', { params })
  },
  getDetail(subRecipeId: number) {
    return http.get<SubRecipeDetail, SubRecipeDetail>(`/api/sub-recipes/${subRecipeId}`)
  },
  getUploadTicket(payload: { fileName: string; fileType: string }) {
    return http.post<SubRecipeFileTicket, SubRecipeFileTicket>('/api/sub-recipes/upload-ticket', payload)
  },
  getDownloadTicket(subRecipeId: number) {
    return http.get<SubRecipeFileTicket, SubRecipeFileTicket>(`/api/sub-recipes/${subRecipeId}/download-ticket`)
  },
  getExportTicket(subRecipeId: number, payload: { exportFormat: string }) {
    return http.post<SubRecipeFileTicket, SubRecipeFileTicket>(`/api/sub-recipes/${subRecipeId}/export`, payload)
  },
}

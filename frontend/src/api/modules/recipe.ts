import { http } from '@/api/http'
import type { RecipeItem, RecipeVersionCompare, RecipeVersionItem } from '@/types/domain'

export const recipeApi = {
  getRecipes(params: { keyword?: string; status?: string } = {}) {
    return http.get<RecipeItem[], RecipeItem[]>('/api/recipes', { params })
  },
  getVersions(params: { recipeId?: number; status?: string } = {}) {
    return http.get<RecipeVersionItem[], RecipeVersionItem[]>('/api/recipe-versions', { params })
  },
  compareVersions(leftVersionId: number, rightVersionId: number) {
    return http.get<RecipeVersionCompare, RecipeVersionCompare>('/api/recipe-versions/compare', {
      params: { leftVersionId, rightVersionId },
    })
  },
}

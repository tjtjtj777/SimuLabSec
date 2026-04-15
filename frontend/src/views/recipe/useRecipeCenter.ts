import { ref } from 'vue'
import { recipeApi } from '@/api/modules/recipe'
import type { RecipeItem, RecipeVersionCompare, RecipeVersionItem } from '@/types/domain'

export function useRecipeCenter() {
  const loading = ref(false)
  const compareLoading = ref(false)
  const recipes = ref<RecipeItem[]>([])
  const versions = ref<RecipeVersionItem[]>([])
  const compareResult = ref<RecipeVersionCompare>()
  const selectedRecipeId = ref<number>()
  const leftVersionId = ref<number>()
  const rightVersionId = ref<number>()

  async function loadRecipes() {
    recipes.value = await recipeApi.getRecipes()
  }

  async function loadVersions() {
    versions.value = await recipeApi.getVersions(selectedRecipeId.value ? { recipeId: selectedRecipeId.value } : {})
    if (!leftVersionId.value && versions.value[0]) {
      leftVersionId.value = versions.value[0].id
    }
    if (!rightVersionId.value && versions.value[1]) {
      rightVersionId.value = versions.value[1].id
    }
  }

  async function loadCompare() {
    if (!leftVersionId.value || !rightVersionId.value || leftVersionId.value === rightVersionId.value) {
      compareResult.value = undefined
      return
    }
    compareLoading.value = true
    try {
      compareResult.value = await recipeApi.compareVersions(leftVersionId.value, rightVersionId.value)
    } finally {
      compareLoading.value = false
    }
  }

  async function init() {
    loading.value = true
    try {
      await loadRecipes()
      selectedRecipeId.value = recipes.value[0]?.id
      await loadVersions()
      await loadCompare()
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    compareLoading,
    recipes,
    versions,
    compareResult,
    selectedRecipeId,
    leftVersionId,
    rightVersionId,
    loadVersions,
    loadCompare,
    init,
  }
}

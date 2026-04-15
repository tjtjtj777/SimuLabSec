import { describe, expect, it, vi } from 'vitest'
import { useRecipeCenter } from '@/views/recipe/useRecipeCenter'

vi.mock('@/api/modules/recipe', () => ({
  recipeApi: {
    getRecipes: vi.fn().mockResolvedValue([{ id: 1, recipeName: 'R1' }]),
    getVersions: vi.fn().mockResolvedValue([
      { id: 11, versionLabel: 'v1.0.0' },
      { id: 12, versionLabel: 'v1.1.0' },
    ]),
    compareVersions: vi.fn().mockResolvedValue({
      leftVersionLabel: 'v1.0.0',
      rightVersionLabel: 'v1.1.0',
      diffs: [{ paramName: 'dose', leftValue: '42.5', rightValue: '43.1' }],
    }),
  },
}))

describe('useRecipeCenter', () => {
  it('loads compare result with diff rows', async () => {
    const vm = useRecipeCenter()
    await vm.init()

    expect(vm.compareResult.value?.diffs.length).toBe(1)
    expect(vm.compareResult.value?.diffs[0].paramName).toBe('dose')
  })
})

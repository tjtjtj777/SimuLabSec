import { describe, expect, it, vi } from 'vitest'

const get = vi.fn()
const post = vi.fn()

vi.mock('@/api/http', () => ({
  http: {
    get,
    post,
  },
}))

describe('overlayAnalysisApi', () => {
  it('calls batch endpoint once for multi-run heatmap', async () => {
    const { overlayAnalysisApi } = await import('@/api/modules/overlay-analysis')
    post.mockResolvedValueOnce([])

    await overlayAnalysisApi.getHeatmapBatch({ measurementRunIds: [1, 2, 3], metricCode: 'overlay_magnitude' })

    expect(post).toHaveBeenCalledTimes(1)
    expect(post).toHaveBeenCalledWith('/api/overlay-results/heatmap/batch', {
      measurementRunIds: [1, 2, 3],
      metricCode: 'overlay_magnitude',
    }, { signal: undefined })
  })

  it('starts async batch task when selected runs exceed sync threshold', async () => {
    const { overlayAnalysisApi } = await import('@/api/modules/overlay-analysis')
    post.mockResolvedValueOnce({ taskId: 1, taskNo: 'TASK', status: 'RUNNING', requestedRuns: 5 })

    await overlayAnalysisApi.startHeatmapBatchTask({ measurementRunIds: [1, 2, 3, 4, 5], metricCode: 'overlay_magnitude' })

    expect(post).toHaveBeenCalledWith('/api/overlay-results/heatmap/batch/tasks', {
      measurementRunIds: [1, 2, 3, 4, 5],
      metricCode: 'overlay_magnitude',
    })
  })
})

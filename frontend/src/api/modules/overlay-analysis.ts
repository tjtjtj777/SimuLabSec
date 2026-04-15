import { http } from '@/api/http'
import type {
  OverlayHeatmapBatchItem,
  OverlayHeatmapPoint,
  OverlayHistogramBin,
  OverlayScatterPoint,
  OverlayTrendPoint,
  WaferHeatmapBatchTaskStart,
} from '@/types/domain'

interface OverlayQuery {
  waferId?: string | number
  layerId?: string | number
  measurementRunId?: string | number
  metricCode?: string
  signal?: AbortSignal
}

export const overlayAnalysisApi = {
  getHeatmap(params: OverlayQuery = {}) {
    const { signal, ...query } = params
    return http.get<OverlayHeatmapPoint[], OverlayHeatmapPoint[]>('/api/overlay-results/heatmap', { params: query, signal })
  },
  getHeatmapBatch(body: { measurementRunIds: Array<string | number>; metricCode?: string }, signal?: AbortSignal) {
    return http.post<OverlayHeatmapBatchItem[], OverlayHeatmapBatchItem[]>('/api/overlay-results/heatmap/batch', body, { signal })
  },
  startHeatmapBatchTask(body: { measurementRunIds: Array<string | number>; metricCode?: string }) {
    return http.post<WaferHeatmapBatchTaskStart, WaferHeatmapBatchTaskStart>('/api/overlay-results/heatmap/batch/tasks', body)
  },
  getScatter(params: OverlayQuery = {}) {
    return http.get<OverlayScatterPoint[], OverlayScatterPoint[]>('/api/overlay-results/scatter', { params })
  },
  getHistogram(params: OverlayQuery = {}) {
    return http.get<OverlayHistogramBin[], OverlayHistogramBin[]>('/api/overlay-results/histogram', { params })
  },
  getTrends(params: { layerId?: string | number; waferId?: string | number; taskId?: string | number } = {}) {
    return http.get<OverlayTrendPoint[], OverlayTrendPoint[]>('/api/overlay-results/trends', { params })
  },
}

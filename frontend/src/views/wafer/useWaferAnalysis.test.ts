import { describe, expect, it, vi } from 'vitest'
import { useWaferAnalysis } from '@/views/wafer/useWaferAnalysis'

vi.mock('@/api/modules/master-data', () => ({
  masterDataApi: {
    getLots: vi.fn().mockResolvedValue({ records: [{ id: 1, lotNo: 'LOT-1', lotStatus: 'READY', priorityLevel: 'NORMAL', sourceType: 'DEMO', waferCount: 1 }] }),
    getWafers: vi.fn().mockResolvedValue({ records: [{ id: 11, lotId: 1, waferNo: 'W01', waferStatus: 'READY', slotNo: 1, diameterMm: 300 }] }),
    getLayers: vi.fn().mockResolvedValue({ records: [{ id: 101, layerCode: 'M1', layerName: 'M1', layerType: 'METAL', sequenceNo: 10, status: 'ACTIVE' }] }),
  },
}))

vi.mock('@/api/modules/measurement', () => ({
  measurementApi: {
    getRuns: vi.fn().mockResolvedValue([
      {
        id: 9001,
        runNo: 'MR-1',
        lotId: 1,
        waferId: 11,
        layerId: 101,
        measurementType: 'OVERLAY',
        stage: 'PRE_ETCH',
        sourceType: 'DEMO',
        samplingCount: 121,
        status: 'COMPLETED',
      },
    ]),
  },
}))

vi.mock('@/api/modules/overlay-analysis', () => ({
  overlayAnalysisApi: {
    getHeatmap: vi.fn().mockResolvedValue([{ targetCode: 'P01', xCoord: 1, yCoord: 2, metricValue: 3, confidence: 0.9, outlier: 0 }]),
    getScatter: vi.fn().mockResolvedValue([{ targetCode: 'P01', xValue: 1, yValue: 3, overlayMagnitude: 3, outlier: 0 }]),
    getHistogram: vi.fn().mockResolvedValue([{ rangeStart: 0, rangeEnd: 1, count: 3 }]),
    getTrends: vi.fn().mockResolvedValue([{ date: '2026-04-09', passRate: 0.9, meanOverlay: 4, p95Overlay: 6 }]),
  },
}))

describe('useWaferAnalysis', () => {
  it('loads and links filter-driven analysis data', async () => {
    const vm = useWaferAnalysis()
    await vm.init()

    expect(vm.filters.value.lotId).toBe(1)
    expect(vm.filters.value.waferId).toBe(11)
    expect(vm.measurementRuns.value[0]?.id).toBe(9001)
    expect(vm.heatmapPoints.value.length).toBe(1)
    expect(vm.trendPoints.value.length).toBe(1)
  })
})

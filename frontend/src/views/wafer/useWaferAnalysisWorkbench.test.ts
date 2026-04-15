import { describe, expect, it, vi } from 'vitest'

vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
  cb(0)
  return 1
})

const getLots = vi.fn().mockResolvedValue({
  records: [{ id: 1, lotNo: 'LOT-DEMO', lotStatus: 'READY', priorityLevel: 'NORMAL', sourceType: 'DEMO', waferCount: 1, dataScope: 'DEMO' }],
})
const getWafers = vi.fn().mockResolvedValue({
  records: [{ id: 11, lotId: 1, waferNo: 'W01', waferStatus: 'READY', slotNo: 1, diameterMm: 300 }],
})
const getLayers = vi.fn().mockResolvedValue({
  records: [{ id: 101, layerCode: 'M1', layerName: 'M1', layerType: 'METAL', sequenceNo: 10, status: 'ACTIVE' }],
})
const getRuns = vi.fn().mockResolvedValue([
  { id: '9001', runNo: 'RUN-1', lotId: 1, waferId: 11, layerId: 101, measurementType: 'OVERLAY', stage: 'PRE_ETCH', sourceType: 'DEMO', samplingCount: 196321, status: 'COMPLETED', dataScope: 'DEMO' },
])
const getHeatmap = vi.fn().mockResolvedValue([{ targetCode: 'P01', xCoord: 1, yCoord: 2, metricValue: 3, confidence: 0.9, outlier: 0 }])
const getScatter = vi.fn().mockResolvedValue([{ targetCode: 'P01', xValue: 1, yValue: 2, overlayMagnitude: 3, outlier: 0 }])
const getHistogram = vi.fn().mockResolvedValue([{ rangeStart: 0, rangeEnd: 1, count: 1 }])
const getTrends = vi.fn().mockResolvedValue([{ date: '2026-04-12', passRate: 0.9, meanOverlay: 4, p95Overlay: 6 }])
const getDefault = vi.fn().mockResolvedValue({
  configName: 'My Wafer Baseline',
  lotNo: 'LOT-DEMO',
  waferNo: 'W01',
  layerId: 101,
  measurementType: 'OVERLAY',
  stage: 'PRE_ETCH',
  scannerCorrectionGain: 1,
  overlayBaseNm: 3.2,
  edgeGradient: 1.3,
  localHotspotStrength: 1,
  noiseLevel: 0.15,
  gridStep: 0.5,
  outlierThreshold: 8,
})
const getConfigPage = vi.fn().mockResolvedValue({
  records: [{
    configName: 'Demo Baseline',
    lotNo: 'LOT-DEMO',
    waferNo: 'W01',
    layerId: 101,
    stage: 'PRE_ETCH',
    measurementType: 'OVERLAY',
    scannerCorrectionGain: 1,
    overlayBaseNm: 3.2,
    edgeGradient: 1.3,
    localHotspotStrength: 1,
    noiseLevel: 0.15,
    gridStep: 0.5,
    outlierThreshold: 8,
    dataScope: 'DEMO',
  }],
})

vi.mock('@/api/modules/master-data', () => ({
  masterDataApi: {
    getLots,
    getWafers,
    getLayers,
  },
}))

vi.mock('@/api/modules/measurement', () => ({
  measurementApi: {
    getRuns,
  },
}))

vi.mock('@/api/modules/overlay-analysis', () => ({
  overlayAnalysisApi: {
    getHeatmap,
    getScatter,
    getHistogram,
    getTrends,
  },
}))

vi.mock('@/api/modules/wafer-config', () => ({
  waferConfigApi: {
    getDefault,
    getPage: getConfigPage,
    validate: vi.fn(),
    generate: vi.fn(),
  },
}))

describe('useWaferAnalysisWorkbench', () => {
  it('restores snapshot on second init and avoids repeated full fetch', async () => {
    const { useWaferAnalysisWorkbench } = await import('@/views/wafer/useWaferAnalysisWorkbench')
    const vm1 = useWaferAnalysisWorkbench()
    await vm1.init()

    const vm2 = useWaferAnalysisWorkbench()
    await vm2.init()

    expect(getHeatmap).toHaveBeenCalledTimes(1)
    expect(vm2.heatmapPoints.value.length).toBe(1)
    expect(vm2.scatterPoints.value.length).toBe(1)
    expect(vm1.config.value.configName).toBe('Demo Baseline')
    expect(vm2.config.value.configName).toBe('Demo Baseline')
  }, 30000)
})

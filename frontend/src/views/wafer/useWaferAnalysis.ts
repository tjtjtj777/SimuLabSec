import { ElMessage } from 'element-plus'
import { computed, nextTick, ref } from 'vue'
import { masterDataApi } from '@/api/modules/master-data'
import { i18n } from '@/locales'
import { measurementApi } from '@/api/modules/measurement'
import { overlayAnalysisApi } from '@/api/modules/overlay-analysis'
import type {
  LayerItem,
  LotItem,
  MeasurementRunItem,
  OverlayHeatmapPoint,
  OverlayHistogramBin,
  OverlayScatterPoint,
  OverlayTrendPoint,
  WaferAnalysisImportConfig,
  WaferAnalysisImportResult,
  WaferItem,
} from '@/types/domain'

export function useWaferAnalysis() {
  const loading = ref(false)
  const importLoading = ref(false)
  const importDialogVisible = ref(false)
  const lots = ref<LotItem[]>([])
  const wafers = ref<WaferItem[]>([])
  const layers = ref<LayerItem[]>([])
  const allMeasurementRuns = ref<MeasurementRunItem[]>([])
  const measurementRuns = ref<MeasurementRunItem[]>([])
  const heatmapPoints = ref<OverlayHeatmapPoint[]>([])
  const scatterPoints = ref<OverlayScatterPoint[]>([])
  const histogramBins = ref<OverlayHistogramBin[]>([])
  const trendPoints = ref<OverlayTrendPoint[]>([])
  const lastQueryHint = ref('')
  const importFile = ref<File>()
  const importResult = ref<WaferAnalysisImportResult>()
  const tablePageNo = ref(1)
  const tablePageSize = ref(50)
  const tablePageSizes = [20, 50, 100, 200]
  const renderStartAt = ref<number>()

  const importForm = ref<WaferAnalysisImportConfig>({
    lotNo: '',
    lotStatus: 'READY',
    priorityLevel: 'NORMAL',
    lotRemark: '',
    waferNo: '',
    waferStatus: 'READY',
    slotNo: 1,
    diameterMm: 300,
    layerId: 0,
    runNo: '',
    measurementType: 'OVERLAY',
    stage: 'PRE_ETCH',
    toolName: 'USER_UPLOAD',
    hasHeader: true,
    generateMagnitudeWhenMissing: true,
    outlierThreshold: undefined,
    fieldMapping: {
      targetCodeColumn: 'target_code',
      xCoordColumn: 'x_coord',
      yCoordColumn: 'y_coord',
      overlayXColumn: 'overlay_x',
      overlayYColumn: 'overlay_y',
      overlayMagnitudeColumn: 'overlay_magnitude',
      residualColumn: 'residual',
      focusColumn: 'focus',
      doseColumn: 'dose',
      confidenceColumn: 'confidence',
      outlierColumn: 'outlier',
    },
  })

  const filters = ref({
    lotId: undefined as string | number | undefined,
    waferId: undefined as string | number | undefined,
    layerId: undefined as string | number | undefined,
    stage: '' as string,
    measurementType: 'OVERLAY',
    measurementRunId: undefined as string | number | undefined,
    metricCode: 'overlay_magnitude',
  })

  const availableLots = computed(() => {
    const lotIds = new Set(allMeasurementRuns.value.filter((run) => run.status === 'COMPLETED').map((run) => run.lotId))
    return lots.value.filter((item) => lotIds.has(item.id))
  })

  const filteredWafers = computed(() => {
    const waferIds = new Set(
      allMeasurementRuns.value
        .filter((run) => run.status === 'COMPLETED' && (!filters.value.lotId || run.lotId === filters.value.lotId))
        .map((run) => run.waferId),
    )
    return wafers.value.filter((item) => waferIds.has(item.id))
  })

  const filteredLayers = computed(() => {
    const layerIds = new Set(
      allMeasurementRuns.value
        .filter((run) =>
          run.status === 'COMPLETED'
          && (!filters.value.lotId || run.lotId === filters.value.lotId)
          && (!filters.value.waferId || run.waferId === filters.value.waferId),
        )
        .map((run) => run.layerId),
    )
    return layers.value.filter((item) => layerIds.has(item.id))
  })

  const availableStages = computed(() =>
    Array.from(new Set(
      allMeasurementRuns.value
        .filter((run) =>
          run.status === 'COMPLETED'
          && (!filters.value.lotId || run.lotId === filters.value.lotId)
          && (!filters.value.waferId || run.waferId === filters.value.waferId)
          && (!filters.value.layerId || run.layerId === filters.value.layerId)
          && (!filters.value.measurementType || run.measurementType === filters.value.measurementType),
        )
        .map((run) => run.stage),
    )),
  )

  const hasAnyChartData = computed(() =>
    heatmapPoints.value.length > 0
    || scatterPoints.value.length > 0
    || histogramBins.value.length > 0
    || trendPoints.value.length > 0,
  )

  const tableTotal = computed(() => heatmapPoints.value.length)
  const pagedHeatmapPoints = computed(() => {
    const start = (tablePageNo.value - 1) * tablePageSize.value
    return heatmapPoints.value.slice(start, start + tablePageSize.value)
  })

  function normalizeHeatmapPoints(points: OverlayHeatmapPoint[]) {
    return points.map((point) => ({
      ...point,
      xCoord: Number(point.xCoord ?? point.xcoord ?? 0),
      yCoord: Number(point.yCoord ?? point.ycoord ?? 0),
      metricValue: Number(point.metricValue ?? 0),
      confidence: Number(point.confidence ?? 0),
    }))
  }

  function startPerfTrace(action: string) {
    renderStartAt.value = performance.now()
    console.info('[wafer-analysis][frontend][start]', { action, filters: { ...filters.value } })
  }

  async function finishPerfTrace(action: string) {
    if (renderStartAt.value == null) {
      return
    }
    await nextTick()
    await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
    const elapsedMs = Number((performance.now() - renderStartAt.value).toFixed(2))
    console.info('[wafer-analysis][frontend][done]', {
      action,
      elapsedMs,
      heatmapPoints: heatmapPoints.value.length,
      scatterPoints: scatterPoints.value.length,
      histogramBins: histogramBins.value.length,
      trendPoints: trendPoints.value.length,
    })
    renderStartAt.value = undefined
  }

  async function loadAnalysisData() {
    if (!filters.value.waferId) {
      heatmapPoints.value = []
      scatterPoints.value = []
      histogramBins.value = []
      trendPoints.value = []
      return
    }
    const query = {
      waferId: filters.value.waferId,
      layerId: filters.value.layerId,
      measurementRunId: filters.value.measurementRunId,
      metricCode: filters.value.metricCode,
    }
    const [heatmap, scatter, histogram, trends] = await Promise.all([
      overlayAnalysisApi.getHeatmap(query),
      overlayAnalysisApi.getScatter(query),
      overlayAnalysisApi.getHistogram(query),
      overlayAnalysisApi.getTrends({ waferId: filters.value.waferId, layerId: filters.value.layerId }),
    ])
    heatmapPoints.value = normalizeHeatmapPoints(heatmap)
    scatterPoints.value = scatter
    histogramBins.value = histogram
    trendPoints.value = trends
    lastQueryHint.value = `${heatmap.length}/${scatter.length}/${histogram.length}/${trends.length}`
    tablePageNo.value = 1
  }

  function ensureValidSelection() {
    if (!availableLots.value.find((item) => item.id === filters.value.lotId)) {
      filters.value.lotId = availableLots.value[0]?.id
    }
    if (!filteredWafers.value.find((item) => item.id === filters.value.waferId)) {
      filters.value.waferId = filteredWafers.value[0]?.id
    }
    if (!filteredLayers.value.find((item) => item.id === filters.value.layerId)) {
      filters.value.layerId = filteredLayers.value[0]?.id
    }
    if (!availableStages.value.includes(filters.value.stage)) {
      filters.value.stage = availableStages.value[0] ?? ''
    }

    measurementRuns.value = allMeasurementRuns.value
      .filter((run) =>
        run.status === 'COMPLETED'
        && (!filters.value.lotId || run.lotId === filters.value.lotId)
        && (!filters.value.waferId || run.waferId === filters.value.waferId)
        && (!filters.value.layerId || run.layerId === filters.value.layerId)
        && (!filters.value.stage || run.stage === filters.value.stage)
        && (!filters.value.measurementType || run.measurementType === filters.value.measurementType),
      )
      .sort((a, b) => {
        const samplingDiff = (b.samplingCount ?? 0) - (a.samplingCount ?? 0)
        if (samplingDiff !== 0) {
          return samplingDiff
        }
        return a.runNo.localeCompare(b.runNo)
      })
    if (!measurementRuns.value.find((item) => item.id === filters.value.measurementRunId)) {
      filters.value.measurementRunId = measurementRuns.value[0]?.id
    }
  }

  async function loadMeasurementRuns() {
    allMeasurementRuns.value = await measurementApi.getRuns({
      measurementType: filters.value.measurementType || undefined,
      status: 'COMPLETED',
    })
    ensureValidSelection()
  }

  function resetCharts() {
    heatmapPoints.value = []
    scatterPoints.value = []
    histogramBins.value = []
    trendPoints.value = []
  }

  async function init() {
    startPerfTrace('init')
    loading.value = true
    try {
      const [lotPage, waferPage, layerPage] = await Promise.all([
        masterDataApi.getLots({ pageNo: 1, pageSize: 100 }),
        masterDataApi.getWafers({ pageNo: 1, pageSize: 100 }),
        masterDataApi.getLayers({ pageNo: 1, pageSize: 100 }),
      ])
      lots.value = lotPage.records
      wafers.value = waferPage.records
      layers.value = layerPage.records

      await loadMeasurementRuns()
      await loadAnalysisData()
    } finally {
      loading.value = false
      await finishPerfTrace('init')
    }
  }

  async function applyFilters() {
    startPerfTrace('applyFilters')
    loading.value = true
    try {
      ensureValidSelection()
      if (!measurementRuns.value.length) {
        resetCharts()
        return
      }
      await loadAnalysisData()
    } finally {
      loading.value = false
      await finishPerfTrace('applyFilters')
    }
  }

  async function handleLotChange() {
    filters.value.waferId = undefined
    filters.value.layerId = undefined
    filters.value.stage = ''
    filters.value.measurementRunId = undefined
    await applyFilters()
  }

  async function handleWaferChange() {
    filters.value.layerId = undefined
    filters.value.stage = ''
    filters.value.measurementRunId = undefined
    await applyFilters()
  }

  async function handleLayerChange() {
    filters.value.stage = ''
    filters.value.measurementRunId = undefined
    await applyFilters()
  }

  async function handleStageChange() {
    filters.value.measurementRunId = undefined
    await applyFilters()
  }

  async function handleMeasurementRunChange() {
    await applyFilters()
  }

  function openImportDialog() {
    importDialogVisible.value = true
    importResult.value = undefined
    if (filters.value.layerId) {
      importForm.value.layerId = Number(filters.value.layerId)
    }
    if (filters.value.stage) {
      importForm.value.stage = filters.value.stage
    }
  }

  function setImportFile(file: File | undefined) {
    importFile.value = file
  }

  async function downloadTemplate() {
    const blob = await measurementApi.downloadImportTemplate()
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = 'wafer-analysis-import-template.csv'
    anchor.click()
    URL.revokeObjectURL(url)
  }

  async function submitImport() {
    if (!importFile.value) {
      ElMessage.warning(i18n.global.t('wafer.selectCsvFirst'))
      return
    }
    if (!importForm.value.layerId) {
      ElMessage.warning(i18n.global.t('wafer.selectLayerFirst'))
      return
    }
    importLoading.value = true
    try {
      const result = await measurementApi.importWaferAnalysis(importFile.value, importForm.value)
      importResult.value = result
      await Promise.all([loadMeasurementRuns(), applyFilters()])
      ElMessage.success(result.message || i18n.global.t('wafer.importDone'))
    } finally {
      importLoading.value = false
    }
  }

  return {
    loading,
    importLoading,
    importDialogVisible,
    importForm,
    importFile,
    importResult,
    lots,
    wafers,
    layers,
    availableLots,
    measurementRuns,
    heatmapPoints,
    scatterPoints,
    histogramBins,
    trendPoints,
    lastQueryHint,
    filters,
    filteredWafers,
    filteredLayers,
    availableStages,
    hasAnyChartData,
    tablePageNo,
    tablePageSize,
    tablePageSizes,
    tableTotal,
    pagedHeatmapPoints,
    init,
    applyFilters,
    handleLotChange,
    handleWaferChange,
    handleLayerChange,
    handleStageChange,
    handleMeasurementRunChange,
    openImportDialog,
    setImportFile,
    submitImport,
    downloadTemplate,
  }
}

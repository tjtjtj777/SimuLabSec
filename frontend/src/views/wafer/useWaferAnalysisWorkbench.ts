import { ElMessage } from 'element-plus'
import { computed, nextTick, ref } from 'vue'
import { masterDataApi } from '@/api/modules/master-data'
import { measurementApi } from '@/api/modules/measurement'
import { overlayAnalysisApi } from '@/api/modules/overlay-analysis'
import { waferConfigApi } from '@/api/modules/wafer-config'
import { i18n } from '@/locales'
import type {
  LayerItem,
  LotItem,
  MeasurementRunItem,
  OverlayHeatmapPoint,
  OverlayHistogramBin,
  OverlayScatterPoint,
  OverlayTrendPoint,
  WaferConfigItem,
  WaferGenerateResult,
  WaferItem,
} from '@/types/domain'
import { storage } from '@/utils/storage'
import { estimateRenderedPointsK, normalizeElapsedMs } from '@/views/wafer/renderFeedback'

const WORKBENCH_CACHE_TTL_MS = 30 * 60 * 1000

type AnalysisResultPayload = {
  heatmap: OverlayHeatmapPoint[]
  scatter: OverlayScatterPoint[]
  histogram: OverlayHistogramBin[]
  trends: OverlayTrendPoint[]
  cachedAt: number
}

type WorkbenchSnapshot = {
  lots: LotItem[]
  wafers: WaferItem[]
  layers: LayerItem[]
  allRuns: MeasurementRunItem[]
  runs: MeasurementRunItem[]
  savedConfigs: WaferConfigItem[]
  config: WaferConfigItem
  filters: {
    lotId: string | number | undefined
    waferId: string | number | undefined
    layerId: string | number | undefined
    stage: string
    measurementType: string
    measurementRunId: string | number | undefined
    metricCode: string
  }
  heatmapPoints: OverlayHeatmapPoint[]
  scatterPoints: OverlayScatterPoint[]
  histogramBins: OverlayHistogramBin[]
  trendPoints: OverlayTrendPoint[]
  generateResult?: WaferGenerateResult
  cachedAt: number
}

const workbenchSnapshotByUser = new Map<string, WorkbenchSnapshot>()
const analysisCacheByUser = new Map<string, Map<string, AnalysisResultPayload>>()

function currentUserKey() {
  return storage.getUsername() || 'anonymous'
}

function cacheFresh(cachedAt: number) {
  return Date.now() - cachedAt <= WORKBENCH_CACHE_TTL_MS
}

export function useWaferAnalysisWorkbench() {
  const loading = ref(false)
  const generating = ref(false)
  const lots = ref<LotItem[]>([])
  const wafers = ref<WaferItem[]>([])
  const layers = ref<LayerItem[]>([])
  const allRuns = ref<MeasurementRunItem[]>([])
  const runs = ref<MeasurementRunItem[]>([])
  const savedConfigs = ref<WaferConfigItem[]>([])
  const heatmapPoints = ref<OverlayHeatmapPoint[]>([])
  const scatterPoints = ref<OverlayScatterPoint[]>([])
  const histogramBins = ref<OverlayHistogramBin[]>([])
  const trendPoints = ref<OverlayTrendPoint[]>([])
  const tablePageNo = ref(1)
  const tablePageSize = ref(50)
  const tablePageSizes = [20, 50, 100, 200]
  const generateResult = ref<WaferGenerateResult>()
  const renderStart = ref<number>()
  let analysisRequestSeq = 0
  const selectedConfigPresetValue = ref('')

  const config = ref<WaferConfigItem>({
    configName: '',
    lotNo: '',
    waferNo: '',
    layerId: 1,
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
  const configErrors = ref<string[]>([])

  const filters = ref({
    lotId: undefined as string | number | undefined,
    waferId: undefined as string | number | undefined,
    layerId: undefined as string | number | undefined,
    stage: '',
    measurementType: 'OVERLAY',
    measurementRunId: undefined as string | number | undefined,
    metricCode: 'overlay_magnitude',
  })

  const availableLots = computed(() => {
    const lotIds = new Set(allRuns.value.filter((run) => run.status === 'COMPLETED').map((run) => run.lotId))
    return lots.value.filter((item) => lotIds.has(item.id))
  })
  const filteredWafers = computed(() => {
    const waferIds = new Set(
      allRuns.value.filter((run) => run.status === 'COMPLETED' && (!filters.value.lotId || run.lotId === filters.value.lotId)).map((run) => run.waferId),
    )
    return wafers.value.filter((item) => waferIds.has(item.id))
  })
  const filteredLayers = computed(() => {
    const layerIds = new Set(
      allRuns.value
        .filter((run) => run.status === 'COMPLETED' && (!filters.value.lotId || run.lotId === filters.value.lotId) && (!filters.value.waferId || run.waferId === filters.value.waferId))
        .map((run) => run.layerId),
    )
    return layers.value.filter((item) => layerIds.has(item.id))
  })
  const availableStages = computed(() => Array.from(new Set(runs.value.map((run) => run.stage))))
  const hasAnyChartData = computed(() => heatmapPoints.value.length > 0 || scatterPoints.value.length > 0 || histogramBins.value.length > 0 || trendPoints.value.length > 0)
  const tableTotal = computed(() => heatmapPoints.value.length)
  const pagedHeatmapPoints = computed(() => {
    const start = (tablePageNo.value - 1) * tablePageSize.value
    return heatmapPoints.value.slice(start, start + tablePageSize.value)
  })
  const configNameOptions = computed(() =>
    Array.from(new Set(savedConfigs.value.map((item) => item.configName).filter((item) => item?.trim().length))),
  )
  const configPresetOptions = computed(() =>
    savedConfigs.value
      .filter((item) => item.configName?.trim().length)
      .map((item, index) => {
        const scope = item.dataScope ?? 'DEMO'
        const value = item.id != null
          ? `cfg:${String(item.id)}`
          : `cfg-fallback:${scope}:${item.configName}:${item.lotNo}:${item.waferNo}:${index}`
        return {
          value,
          label: `${item.configName} · ${item.lotNo}/${item.waferNo} · ${scope}`,
        }
      }),
  )
  const configLotOptions = computed(() =>
    lots.value
      .slice()
      .sort((a, b) => {
        const sa = a.dataScope ?? 'DEMO'
        const sb = b.dataScope ?? 'DEMO'
        if (sa !== sb) {
          return sa === 'DEMO' ? -1 : 1
        }
        return a.lotNo.localeCompare(b.lotNo)
      })
      .map((item) => ({ label: `${item.lotNo} (${item.dataScope ?? 'DEMO'})`, value: item.lotNo })),
  )
  const configWaferOptions = computed(() => {
    const selectedLot = lots.value.find((item) => item.lotNo === config.value.lotNo)
    const list = selectedLot ? wafers.value.filter((item) => item.lotId === selectedLot.id) : wafers.value
    return list
      .slice()
      .sort((a, b) => a.waferNo.localeCompare(b.waferNo))
      .map((item) => ({ label: item.waferNo, value: item.waferNo }))
  })

  function resetRenderState() {
    heatmapPoints.value = []
    scatterPoints.value = []
    histogramBins.value = []
    trendPoints.value = []
    tablePageNo.value = 1
  }

  function normalizeHeatmapPoints(points: OverlayHeatmapPoint[]) {
    return points.map((point) => ({
      ...point,
      xCoord: Number(point.xCoord ?? point.xcoord ?? 0),
      yCoord: Number(point.yCoord ?? point.ycoord ?? 0),
      metricValue: Number(point.metricValue ?? 0),
      confidence: Number(point.confidence ?? 0),
    }))
  }

  function localValidate(cfg: WaferConfigItem) {
    const errors: string[] = []
    if (!cfg.configName?.trim()) errors.push('configName is required.')
    if (!cfg.lotNo?.trim()) errors.push('lotNo is required.')
    if (!cfg.waferNo?.trim()) errors.push('waferNo is required.')
    if (!(cfg.layerId > 0)) errors.push('layerId must be selected.')
    if (cfg.scannerCorrectionGain < 0.7 || cfg.scannerCorrectionGain > 1.3) errors.push('scannerCorrectionGain range: 0.70~1.30')
    if (cfg.overlayBaseNm < 1 || cfg.overlayBaseNm > 8) errors.push('overlayBaseNm range: 1.00~8.00')
    if (cfg.edgeGradient < 0 || cfg.edgeGradient > 4) errors.push('edgeGradient range: 0.00~4.00')
    if (cfg.localHotspotStrength < 0 || cfg.localHotspotStrength > 5) errors.push('localHotspotStrength range: 0.00~5.00')
    if (cfg.noiseLevel < 0 || cfg.noiseLevel > 1) errors.push('noiseLevel range: 0.00~1.00')
    if (cfg.gridStep < 0.5 || cfg.gridStep > 5) errors.push('gridStep range: 0.50~5.00')
    if (cfg.outlierThreshold < 2 || cfg.outlierThreshold > 20) errors.push('outlierThreshold range: 2.00~20.00')
    configErrors.value = errors
    return errors
  }

  function analysisKey() {
    // 图表结果缓存按查询条件做签名；同一用户同一组筛选条件直接复用上次结果。
    const query = {
      waferId: filters.value.measurementRunId ? undefined : filters.value.waferId,
      layerId: filters.value.measurementRunId ? undefined : filters.value.layerId,
      measurementRunId: filters.value.measurementRunId,
      metricCode: filters.value.metricCode,
      trendTaskId: generateResult.value?.taskId ?? undefined,
    }
    return JSON.stringify(query)
  }

  function persistSnapshot() {
    const userKey = currentUserKey()
    // snapshot 保存的是整个工作台状态，便于用户返回页面时直接恢复上次视图。
    workbenchSnapshotByUser.set(userKey, {
      lots: lots.value,
      wafers: wafers.value,
      layers: layers.value,
      allRuns: allRuns.value,
      runs: runs.value,
      savedConfigs: savedConfigs.value,
      config: { ...config.value },
      filters: { ...filters.value },
      heatmapPoints: heatmapPoints.value,
      scatterPoints: scatterPoints.value,
      histogramBins: histogramBins.value,
      trendPoints: trendPoints.value,
      generateResult: generateResult.value,
      cachedAt: Date.now(),
    })
  }

  function tryRestoreSnapshot() {
    const snapshot = workbenchSnapshotByUser.get(currentUserKey())
    if (!snapshot || !cacheFresh(snapshot.cachedAt)) {
      return false
    }
    // 命中页面级缓存时，避免重新走初始化请求和图表渲染。
    lots.value = snapshot.lots
    wafers.value = snapshot.wafers
    layers.value = snapshot.layers
    allRuns.value = snapshot.allRuns
    runs.value = snapshot.runs
    savedConfigs.value = snapshot.savedConfigs
    config.value = { ...snapshot.config }
    filters.value = { ...snapshot.filters }
    heatmapPoints.value = snapshot.heatmapPoints
    scatterPoints.value = snapshot.scatterPoints
    histogramBins.value = snapshot.histogramBins
    trendPoints.value = snapshot.trendPoints
    generateResult.value = snapshot.generateResult
    tablePageNo.value = 1
    console.info('[wafer-analysis][frontend][restore]', {
      source: 'page-snapshot',
      measurementRunId: filters.value.measurementRunId,
      heatmapPoints: heatmapPoints.value.length,
    })
    return true
  }

  function preferredRunSort(a: MeasurementRunItem, b: MeasurementRunItem) {
    const scopeA = a.dataScope ?? 'DEMO'
    const scopeB = b.dataScope ?? 'DEMO'
    if (scopeA !== scopeB) {
      return scopeA === 'DEMO' ? -1 : 1
    }
    if (a.stage !== b.stage) {
      return a.stage === 'PRE_ETCH' ? -1 : 1
    }
    const samplingDiff = (b.samplingCount ?? 0) - (a.samplingCount ?? 0)
    if (samplingDiff !== 0) {
      return samplingDiff
    }
    return Number(b.id) - Number(a.id)
  }

  function applyRunToFilters(run: MeasurementRunItem) {
    // measurementRun 是最稳定的查询锚点，切换 run 时同步带上其 lot/wafer/layer 上下文。
    filters.value.lotId = run.lotId
    filters.value.waferId = run.waferId
    filters.value.layerId = run.layerId
    filters.value.stage = run.stage
    filters.value.measurementRunId = run.id
  }

  function syncConfigToRun(run: MeasurementRunItem) {
    // 把当前 run 的 lot/wafer/layer 回填到配置表单，便于用户理解当前图表上下文。
    const lot = lots.value.find((item) => item.id === run.lotId)
    const wafer = wafers.value.find((item) => item.id === run.waferId)
    config.value = {
      ...config.value,
      lotNo: lot?.lotNo ?? config.value.lotNo,
      waferNo: wafer?.waferNo ?? config.value.waferNo,
      layerId: Number(run.layerId),
      stage: run.stage || config.value.stage,
      measurementType: run.measurementType || config.value.measurementType,
    }
  }

  function trySyncDemoConfigByRun(run: MeasurementRunItem) {
    const matchedDemoConfig = savedConfigs.value.find((item) =>
      (item.dataScope ?? 'DEMO') === 'DEMO'
        && item.lotNo === (lots.value.find((lot) => lot.id === run.lotId)?.lotNo ?? '')
        && item.waferNo === (wafers.value.find((wafer) => wafer.id === run.waferId)?.waferNo ?? '')
        && Number(item.layerId) === Number(run.layerId)
        && item.stage === run.stage,
    )
    if (!matchedDemoConfig) {
      return false
    }
    config.value = {
      ...config.value,
      ...matchedDemoConfig,
      id: undefined,
      configNo: undefined,
      dataScope: undefined,
      editable: undefined,
      deletable: undefined,
      updatedAt: undefined,
    }
    return true
  }

  async function loadRuns() {
    allRuns.value = (await measurementApi.getRuns({ measurementType: 'OVERLAY', status: 'COMPLETED' })).slice().sort(preferredRunSort)
    runs.value = allRuns.value
      .filter((run) => !filters.value.lotId || run.lotId === filters.value.lotId)
      .slice()
      .sort(preferredRunSort)
    if (!filters.value.measurementRunId || !runs.value.find((r) => String(r.id) === String(filters.value.measurementRunId))) {
      filters.value.measurementRunId = runs.value[0]?.id
    }
  }

  async function loadAnalysisData() {
    if (!filters.value.measurementRunId && !filters.value.waferId) return
    const startedAt = performance.now()
    const currentSeq = ++analysisRequestSeq
    const userKey = currentUserKey()
    const key = analysisKey()
    if (!analysisCacheByUser.has(userKey)) {
      analysisCacheByUser.set(userKey, new Map())
    }
    const userCache = analysisCacheByUser.get(userKey)!
    const cached = userCache.get(key)
    if (cached && cacheFresh(cached.cachedAt)) {
      // 图表级缓存命中时，直接恢复热图/散点/直方图/趋势图结果。
      heatmapPoints.value = cached.heatmap
      scatterPoints.value = cached.scatter
      histogramBins.value = cached.histogram
      trendPoints.value = cached.trends
      tablePageNo.value = 1
      console.info('[overlay-analysis][frontend][batch]', {
        source: 'analysis-cache',
        measurementRunId: filters.value.measurementRunId,
        metricCode: filters.value.metricCode,
        heatmapPoints: cached.heatmap.length,
        scatterPoints: cached.scatter.length,
        histogramBins: cached.histogram.length,
        trendPoints: cached.trends.length,
        cacheHit: true,
        elapsedMs: Number((performance.now() - startedAt).toFixed(2)),
      })
      persistSnapshot()
      return
    }

    const query = {
      waferId: filters.value.measurementRunId ? undefined : filters.value.waferId,
      layerId: filters.value.measurementRunId ? undefined : filters.value.layerId,
      measurementRunId: filters.value.measurementRunId,
      metricCode: filters.value.metricCode,
    }
    const trendQuery = generateResult.value?.taskId ? { taskId: generateResult.value.taskId } : { waferId: filters.value.waferId, layerId: filters.value.layerId }
    const [heatmap, scatter, histogram, trends] = await Promise.all([
      overlayAnalysisApi.getHeatmap(query),
      overlayAnalysisApi.getScatter(query),
      overlayAnalysisApi.getHistogram(query),
      overlayAnalysisApi.getTrends(trendQuery),
    ])
    if (currentSeq !== analysisRequestSeq) {
      // 防止旧请求在新请求之后返回，导致页面被过期数据覆盖。
      return
    }
    const normalizedHeatmap = normalizeHeatmapPoints(heatmap)
    heatmapPoints.value = normalizedHeatmap
    scatterPoints.value = scatter
    histogramBins.value = histogram
    trendPoints.value = trends
    userCache.set(key, {
      heatmap: normalizedHeatmap,
      scatter,
      histogram,
      trends,
      cachedAt: Date.now(),
    })
    tablePageNo.value = 1
    console.info('[overlay-analysis][frontend][batch]', {
      source: 'fresh-request',
      measurementRunId: filters.value.measurementRunId,
      metricCode: filters.value.metricCode,
      heatmapPoints: normalizedHeatmap.length,
      scatterPoints: scatter.length,
      histogramBins: histogram.length,
      trendPoints: trends.length,
      cacheHit: false,
      elapsedMs: Number((performance.now() - startedAt).toFixed(2)),
    })
    persistSnapshot()
  }

  async function pickBestDemoRunAndLoad() {
    // 首次进入优先使用第一条 demo run，保证默认体验稳定且可预测。
    const firstDemo = allRuns.value.find((item) => (item.dataScope ?? 'DEMO') === 'DEMO') ?? allRuns.value[0]
    if (!firstDemo) {
      return
    }
    applyRunToFilters(firstDemo)
    syncConfigToRun(firstDemo)
    trySyncDemoConfigByRun(firstDemo)
    await loadRuns()
    await loadAnalysisData()
  }

  async function init() {
    if (tryRestoreSnapshot()) {
      return
    }
    loading.value = true
    renderStart.value = performance.now()
    try {
      resetRenderState()
      const [lotPage, waferPage, layerPage, defaultConfig, configPage] = await Promise.all([
        // 后端 lot / wafer / layer 分页当前统一限制 pageSize<=100，这里初始化按上限拉首屏主数据。
        masterDataApi.getLots({ pageNo: 1, pageSize: 100 }),
        masterDataApi.getWafers({ pageNo: 1, pageSize: 100 }),
        masterDataApi.getLayers({ pageNo: 1, pageSize: 100 }),
        waferConfigApi.getDefault(),
        // 后端当前对 wafer-configs 分页做了 pageSize<=50 的校验，这里初始化只拉首批可见配置即可。
        waferConfigApi.getPage({ pageNo: 1, pageSize: 50 }),
      ])
      lots.value = lotPage.records
      wafers.value = waferPage.records
      layers.value = layerPage.records
      savedConfigs.value = configPage.records
      config.value = { ...config.value, ...defaultConfig }
      const defaultPreset = configPage.records.find((item) =>
        item.configName === defaultConfig.configName
          && item.lotNo === defaultConfig.lotNo
          && item.waferNo === defaultConfig.waferNo
          && Number(item.layerId) === Number(defaultConfig.layerId)
          && item.stage === defaultConfig.stage,
      )
      if (defaultPreset?.id != null) {
        selectedConfigPresetValue.value = `cfg:${String(defaultPreset.id)}`
      }
      await loadRuns()
      await pickBestDemoRunAndLoad()
      persistSnapshot()
    } finally {
      loading.value = false
      await nextTick()
      await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
      if (renderStart.value != null) {
        console.info('[wafer-analysis][frontend][done]', { action: 'init', elapsedMs: Number((performance.now() - renderStart.value).toFixed(2)) })
      }
      renderStart.value = undefined
    }
  }

  async function applyFilters() {
    loading.value = true
    try {
      await loadRuns()
      await loadAnalysisData()
    } finally {
      loading.value = false
    }
  }

  async function onConfigNameSelect(configName: string) {
    // 选择器内部使用 cfg:id 这种稳定值，避免 demo/mine 同名配置时选错。
    selectedConfigPresetValue.value = configName
    const matched = resolveConfigPreset(configName) ?? savedConfigs.value.find((item) => item.configName === configName)
    if (!matched) {
      config.value.configName = configName
      return
    }
    config.value = {
      ...config.value,
      ...matched,
      id: undefined,
      configNo: undefined,
      dataScope: undefined,
      editable: undefined,
      deletable: undefined,
      updatedAt: undefined,
    }
    onConfigLotChange(config.value.lotNo)
    const linkedRunId = matched.lastMeasurementRunId ?? await ensureLinkedRunForConfig(matched)
    if (linkedRunId != null) {
      const linkedRun = allRuns.value.find((item) => String(item.id) === String(linkedRunId))
      if (linkedRun) {
        applyRunToFilters(linkedRun)
        syncConfigToRun(linkedRun)
      } else {
        filters.value.measurementRunId = linkedRunId
      }
      void applyFilters()
    } else if ((matched.dataScope ?? 'DEMO') === 'DEMO') {
      ElMessage.warning(i18n.global.t('wafer.noRunLinkedHint'))
    }
    localValidate(config.value)
  }

  function resolveConfigPreset(value: string) {
    if (!value.startsWith('cfg:')) {
      return null
    }
    const id = value.slice(4)
    return savedConfigs.value.find((item) => item.id != null && String(item.id) === id) ?? null
  }

  function onConfigLotChange(lotNo: string) {
    config.value.lotNo = lotNo
    const selectedPreset = resolveConfigPreset(selectedConfigPresetValue.value)
    if (selectedPreset && selectedPreset.lotNo !== lotNo) {
      selectedConfigPresetValue.value = ''
    }
    const selectedLot = lots.value.find((item) => item.lotNo === lotNo)
    if (!selectedLot) {
      return
    }
    const options = wafers.value.filter((item) => item.lotId === selectedLot.id).sort((a, b) => a.waferNo.localeCompare(b.waferNo))
    if (!options.find((item) => item.waferNo === config.value.waferNo)) {
      config.value.waferNo = options[0]?.waferNo ?? ''
    }
  }

  async function ensureLinkedRunForConfig(item: WaferConfigItem) {
    if (item.id == null || item.lastMeasurementRunId != null) {
      return item.lastMeasurementRunId
    }
    try {
      const generated = await waferConfigApi.generate({
        configId: item.id,
        saveAsConfig: false,
        locale: i18n.global.locale.value,
      })
      if (generated.measurementRunId != null) {
        item.lastMeasurementRunId = generated.measurementRunId
        const target = savedConfigs.value.find((cfg) => cfg.id != null && String(cfg.id) === String(item.id))
        if (target) {
          target.lastMeasurementRunId = generated.measurementRunId
          target.lastTaskId = generated.taskId
        }
      }
      return generated.measurementRunId
    } catch (error) {
      console.warn('[wafer-analysis][frontend][link-run-failed]', {
        configId: item.id,
        error,
      })
      return undefined
    }
  }

  async function runGenerate(saveAsConfig = true) {
    const errors = localValidate(config.value)
    if (errors.length) {
      ElMessage.error(errors[0])
      return
    }
    generating.value = true
    const renderStartedAt = performance.now()
    try {
      const backendErrors = await waferConfigApi.validate(config.value)
      if (backendErrors.length) {
        configErrors.value = backendErrors
        ElMessage.error(backendErrors[0])
        return
      }
      generateResult.value = await waferConfigApi.generate({
        saveAsConfig,
        locale: i18n.global.locale.value,
        config: config.value,
      })
      // 新分析会生成新的 run/task，旧缓存已经失效，先清理再按最新 run 重新拉图表。
      analysisCacheByUser.delete(currentUserKey())
      await loadRuns()
      const generatedRun = allRuns.value.find((item) => String(item.id) === String(generateResult.value?.measurementRunId))
      if (generatedRun) {
        applyRunToFilters(generatedRun)
        syncConfigToRun(generatedRun)
      }
      filters.value.measurementRunId = generateResult.value.measurementRunId
      await applyFilters()
      persistSnapshot()
      // 成功提示必须在图表实际完成渲染后触发，而不是接口返回即触发。
      await nextTick()
      await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
      ElMessage.success(
        i18n.global.t('wafer.renderFeedback', {
          points: estimateRenderedPointsK(1),
          elapsedMs: normalizeElapsedMs(performance.now() - renderStartedAt),
        }),
      )
    } finally {
      generating.value = false
    }
  }

  return {
    loading,
    generating,
    config,
    configErrors,
    generateResult,
    lots,
    wafers,
    layers,
    filters,
    availableLots,
    filteredWafers,
    filteredLayers,
    availableStages,
    runs,
    savedConfigs,
    configNameOptions,
    configPresetOptions,
    selectedConfigPresetValue,
    configLotOptions,
    configWaferOptions,
    heatmapPoints,
    scatterPoints,
    histogramBins,
    trendPoints,
    hasAnyChartData,
    tablePageNo,
    tablePageSize,
    tablePageSizes,
    tableTotal,
    pagedHeatmapPoints,
    init,
    applyFilters,
    runGenerate,
    localValidate,
    onConfigNameSelect,
    onConfigLotChange,
  }
}

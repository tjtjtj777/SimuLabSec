<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, nextTick, onDeactivated, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { overlayAnalysisApi } from '@/api/modules/overlay-analysis'
import { waferConfigApi } from '@/api/modules/wafer-config'
import WaferHeatmap from '@/components/wafer/WaferHeatmap.vue'
import type { OverlayHeatmapPoint, WaferConfigItem } from '@/types/domain'
import { storage } from '@/utils/storage'
import { estimateRenderedPointsK, normalizeElapsedMs } from '@/views/wafer/renderFeedback'

const MULTI_CACHE_TTL_MS = 30 * 60 * 1000
const MULTI_SNAPSHOT_KEY_PREFIX = 'simulab.multiWafer.snapshot'
const MAX_DISPLAY = 4
const DEFAULT_DISPLAY = 2

type MultiSnapshot = {
  allConfigs: WaferConfigItem[]
  selectedConfigIds: Array<string | number>
  configPoints: Record<string, OverlayHeatmapPoint[]>
  cachedAt: number
}

type MultiSnapshotStorage = Omit<MultiSnapshot, 'configPoints'> & {
  configPoints?: undefined
}

const multiSnapshotByUser = new Map<string, MultiSnapshot>()
const heatmapCacheByUser = new Map<string, Map<string, { points: OverlayHeatmapPoint[]; cachedAt: number }>>()

function currentUserKey() {
  return storage.getUsername() || 'anonymous'
}

function fresh(cachedAt: number) {
  return Date.now() - cachedAt <= MULTI_CACHE_TTL_MS
}

function snapshotStorageKey() {
  return `${MULTI_SNAPSHOT_KEY_PREFIX}:${currentUserKey()}`
}

function writeSnapshot(snapshot: MultiSnapshotStorage) {
  try {
    localStorage.setItem(snapshotStorageKey(), JSON.stringify(snapshot))
  } catch (error) {
    console.warn('[multi-wafer][frontend][snapshot]', {
      message: 'persist snapshot skipped because browser storage quota was exceeded',
      key: snapshotStorageKey(),
      error,
    })
  }
}

function readSnapshot() {
  const raw = localStorage.getItem(snapshotStorageKey())
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as MultiSnapshotStorage
  } catch {
    return null
  }
}

const { t } = useI18n()
const loading = ref(false)
const allConfigs = ref<WaferConfigItem[]>([])
const selectedConfigIds = ref<Array<string | number>>([])
const configPoints = ref<Record<string, OverlayHeatmapPoint[]>>({})
let heatmapRequestSeq = 0
let inflightController: AbortController | null = null

const configOptions = computed(() =>
  allConfigs.value.map((config) => ({
    value: config.id as string | number,
    label: `${config.configNo ?? 'CFG'} · ${config.configName} · ${config.lotNo}/${config.waferNo}`,
    dataScope: config.dataScope ?? 'DEMO',
    stage: config.stage ?? '-',
    hasRun: config.lastMeasurementRunId != null,
  })),
)

const displayConfigs = computed(() =>
  allConfigs.value
    .filter((config) => selectedConfigIds.value.some((id) => String(id) === String(config.id)))
    .slice(0, MAX_DISPLAY),
)

function hydrateRunPointsFromUserCache(configs: WaferConfigItem[]) {
  const userCache = heatmapCacheByUser.get(currentUserKey())
  const restored: Record<string, OverlayHeatmapPoint[]> = {}
  if (!userCache) {
    return { restored, complete: false }
  }
  let complete = true
  for (const config of configs) {
    const runId = config.lastMeasurementRunId
    if (runId == null || config.id == null) {
      complete = false
      continue
    }
    const cached = userCache.get(`${runId}:overlay_magnitude`)
    if (cached && fresh(cached.cachedAt)) {
      restored[String(config.id)] = cached.points
    } else {
      complete = false
    }
  }
  return { restored, complete }
}

function persistSnapshot(source: 'page-state' | 'cache' | 'fresh') {
  const snapshot: MultiSnapshot = {
    allConfigs: allConfigs.value,
    selectedConfigIds: selectedConfigIds.value,
    configPoints: configPoints.value,
    cachedAt: Date.now(),
  }
  multiSnapshotByUser.set(currentUserKey(), snapshot)
  writeSnapshot({
    allConfigs: snapshot.allConfigs,
    selectedConfigIds: snapshot.selectedConfigIds,
    cachedAt: snapshot.cachedAt,
  })
  console.info('[multi-wafer][frontend][restore]', {
    source,
    selected: selectedConfigIds.value.length,
    rendered: Object.keys(configPoints.value).length,
  })
}

function trimSelection(values: Array<string | number>) {
  if (values.length > MAX_DISPLAY) {
    ElMessage.warning(t('multiWafer.maxSelectionHint'))
    return values.slice(0, MAX_DISPLAY)
  }
  return values
}

function tryRestoreSnapshot() {
  const memorySnapshot = multiSnapshotByUser.get(currentUserKey())
  if (memorySnapshot && fresh(memorySnapshot.cachedAt)) {
    allConfigs.value = memorySnapshot.allConfigs
    selectedConfigIds.value = trimSelection(memorySnapshot.selectedConfigIds)
    configPoints.value = memorySnapshot.configPoints
    console.info('[multi-wafer][frontend][restore]', {
      source: 'page-state',
      selected: selectedConfigIds.value.length,
      rendered: Object.keys(configPoints.value).length,
    })
    return true
  }

  const snapshot = readSnapshot()
  if (!snapshot || !fresh(snapshot.cachedAt)) {
    return false
  }
  allConfigs.value = snapshot.allConfigs
  selectedConfigIds.value = trimSelection(snapshot.selectedConfigIds)
  const selectedConfigs = allConfigs.value.filter((config) =>
    selectedConfigIds.value.some((id) => String(id) === String(config.id)),
  )
  const restoredHeatmaps = hydrateRunPointsFromUserCache(selectedConfigs)
  configPoints.value = restoredHeatmaps.restored
  if (!restoredHeatmaps.complete && selectedConfigIds.value.length) {
    void loadHeatmaps({ source: 'restore', showFeedback: false })
  }
  console.info('[multi-wafer][frontend][restore]', {
    source: 'storage-lite',
    selected: selectedConfigIds.value.length,
    rendered: Object.keys(configPoints.value).length,
    cacheComplete: restoredHeatmaps.complete,
  })
  return true
}

async function loadAllConfigs() {
  const records: WaferConfigItem[] = []
  // 后端 WaferConfigQueryDto 限制 pageSize 最大为 50，这里按上限分页拉全量配置。
  const pageSize = 50
  let pageNo = 1
  while (true) {
    const page = await waferConfigApi.getPage({ pageNo, pageSize })
    records.push(...page.records)
    if (records.length >= page.total || page.records.length < pageSize) {
      break
    }
    pageNo += 1
  }
  allConfigs.value = records
    .filter((config) => config.id != null)
    .slice()
    .sort((a, b) => {
      const scopeA = a.dataScope ?? 'DEMO'
      const scopeB = b.dataScope ?? 'DEMO'
      if (scopeA !== scopeB) {
        return scopeA === 'DEMO' ? -1 : 1
      }
      return new Date(b.updatedAt || '').getTime() - new Date(a.updatedAt || '').getTime()
    })
}

async function loadBaseData() {
  if (tryRestoreSnapshot()) {
    return
  }
  loading.value = true
  try {
    await loadAllConfigs()
    if (!selectedConfigIds.value.length && allConfigs.value.length) {
      // 默认仅展示 2 张图，优先选择已关联 measurement run 的配置，且尽量覆盖不同 lot。
      const defaults: WaferConfigItem[] = []
      const seenLotNos = new Set<string>()
      for (const config of allConfigs.value) {
        if (config.lastMeasurementRunId == null) {
          continue
        }
        const lotNo = config.lotNo || '-'
        if (seenLotNos.has(lotNo)) {
          continue
        }
        seenLotNos.add(lotNo)
        defaults.push(config)
        if (defaults.length >= DEFAULT_DISPLAY) {
          break
        }
      }
      if (!defaults.length) {
        defaults.push(...allConfigs.value.filter((config) => config.lastMeasurementRunId != null).slice(0, DEFAULT_DISPLAY))
      }
      await ensureLinkedRuns(defaults)
      selectedConfigIds.value = defaults.map((config) => config.id as string | number)
    }
    await loadHeatmaps({ source: 'init', showFeedback: false })
  } finally {
    loading.value = false
  }
}

async function loadHeatmaps(options: { source: 'init' | 'restore' | 'manual'; showFeedback: boolean }) {
  const targets = displayConfigs.value
  if (!targets.length) {
    configPoints.value = {}
    persistSnapshot('page-state')
    return
  }
  const validTargets = targets.filter((config) => config.lastMeasurementRunId != null)
  if (!validTargets.length) {
    configPoints.value = {}
    persistSnapshot('page-state')
    return
  }
  const startedAt = performance.now()
  const currentSeq = ++heatmapRequestSeq
  const userKey = currentUserKey()
  if (!heatmapCacheByUser.has(userKey)) {
    heatmapCacheByUser.set(userKey, new Map())
  }
  const userCache = heatmapCacheByUser.get(userKey)!
  const pointsByRunId: Record<string, OverlayHeatmapPoint[]> = {}
  const pendingRunIds: Array<string | number> = []

  for (const config of validTargets) {
    const runId = config.lastMeasurementRunId as string | number
    const cached = userCache.get(`${runId}:overlay_magnitude`)
    if (cached && fresh(cached.cachedAt)) {
      pointsByRunId[String(runId)] = cached.points
    } else if (!pendingRunIds.some((id) => String(id) === String(runId))) {
      pendingRunIds.push(runId)
    }
  }

  if (inflightController) {
    inflightController.abort()
  }
  inflightController = new AbortController()
  if (pendingRunIds.length) {
    const batchRows = await overlayAnalysisApi.getHeatmapBatch(
      { measurementRunIds: pendingRunIds, metricCode: 'overlay_magnitude' },
      inflightController.signal,
    )
    if (currentSeq !== heatmapRequestSeq) {
      return
    }
    for (const row of batchRows) {
      if (!row?.success) {
        pointsByRunId[String(row.measurementRunId)] = []
        continue
      }
      const normalized = (row.points ?? []).map((point) => ({
        ...point,
        xCoord: Number(point.xCoord ?? point.xcoord ?? 0),
        yCoord: Number(point.yCoord ?? point.ycoord ?? 0),
        metricValue: Number(point.metricValue ?? 0),
        confidence: Number(point.confidence ?? 0),
      }))
      pointsByRunId[String(row.measurementRunId)] = normalized
      userCache.set(`${row.measurementRunId}:overlay_magnitude`, { points: normalized, cachedAt: Date.now() })
    }
  }
  if (currentSeq !== heatmapRequestSeq) {
    return
  }

  const staged: Record<string, OverlayHeatmapPoint[]> = {}
  for (const config of targets) {
    if (config.id == null || config.lastMeasurementRunId == null) {
      continue
    }
    staged[String(config.id)] = pointsByRunId[String(config.lastMeasurementRunId)] ?? []
  }
  configPoints.value = staged
  persistSnapshot(pendingRunIds.length ? 'fresh' : 'cache')
  if (options.showFeedback) {
    await nextTick()
    await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
    ElMessage.success(
      t('multiWafer.renderFeedback', {
        points: estimateRenderedPointsK(validTargets.length),
        charts: validTargets.length,
        elapsedMs: normalizeElapsedMs(performance.now() - startedAt),
      }),
    )
  }
  console.info('[multi-wafer][frontend][render]', {
    source: options.source,
    configCount: validTargets.length,
    pointCounts: validTargets.map((config) => ({
      configId: config.id,
      runId: config.lastMeasurementRunId,
      points: (pointsByRunId[String(config.lastMeasurementRunId)] ?? []).length,
    })),
    cacheHitRatio: Number(((validTargets.length - pendingRunIds.length) / validTargets.length).toFixed(2)),
    elapsedMs: Number((performance.now() - startedAt).toFixed(2)),
  })
}

function onSelectionChange(values: Array<string | number>) {
  selectedConfigIds.value = trimSelection(values)
  persistSnapshot('page-state')
}

async function ensureLinkedRuns(configs: WaferConfigItem[]) {
  const needLink = configs.filter((config) => config.id != null && config.lastMeasurementRunId == null)
  if (!needLink.length) {
    return
  }
  for (const config of needLink) {
    const result = await waferConfigApi.generate({ configId: config.id, saveAsConfig: false })
    if (result.measurementRunId == null) {
      continue
    }
    config.lastMeasurementRunId = result.measurementRunId
    const target = allConfigs.value.find((item) => String(item.id) === String(config.id))
    if (target) {
      target.lastMeasurementRunId = result.measurementRunId
      target.lastTaskId = result.taskId
    }
  }
}

async function applySelection() {
  if (!selectedConfigIds.value.length) {
    ElMessage.warning(t('multiWafer.selectAtLeastOne'))
    return
  }
  if (selectedConfigIds.value.length > MAX_DISPLAY) {
    ElMessage.warning(t('multiWafer.maxSelectionHint'))
    return
  }
  await ensureLinkedRuns(displayConfigs.value)
  if (!displayConfigs.value.some((config) => config.lastMeasurementRunId != null)) {
    ElMessage.warning(t('multiWafer.noRunLinkedHint'))
    return
  }
  loading.value = true
  try {
    await loadHeatmaps({ source: 'manual', showFeedback: true })
  } finally {
    loading.value = false
  }
}

onMounted(loadBaseData)

onDeactivated(() => {
  if (inflightController) {
    inflightController.abort()
    inflightController = null
  }
})
</script>

<template>
  <div v-loading="loading" class="multi-page">
    <div class="page-head">
      <h2 class="page-title">{{ t('multiWafer.title') }}</h2>
      <div class="page-subtitle">{{ t('multiWafer.subtitle') }}</div>
      <div class="status-line">
        <el-tag size="small" type="info">{{ t('multiWafer.selectedCount', { count: selectedConfigIds.length }) }}</el-tag>
        <el-tag size="small" type="success">{{ t('multiWafer.renderedCount', { count: Object.keys(configPoints).length }) }}</el-tag>
      </div>
    </div>
    <el-card class="card toolbar-card" shadow="never">
      <el-row :gutter="10">
        <el-col :xs="24" :sm="18" :md="20">
          <span class="label">{{ t('multiWafer.configSelector') }}</span>
          <el-select
            v-model="selectedConfigIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            style="width: 100%"
            :placeholder="t('multiWafer.selectPlaceholder')"
            @change="onSelectionChange"
          >
            <el-option
              v-for="item in configOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            >
              <div class="option-row">
                <span>{{ item.label }}</span>
                <div class="option-tags">
                  <el-tag size="small" :type="item.dataScope === 'MINE' ? 'success' : 'info'">{{ item.dataScope }}</el-tag>
                  <el-tag size="small" type="warning">{{ item.stage }}</el-tag>
                </div>
              </div>
            </el-option>
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="6" :md="4" class="apply-col">
          <el-button type="primary" @click="applySelection">{{ t('common.search') }}</el-button>
        </el-col>
      </el-row>
    </el-card>
    <div class="grid">
      <el-card v-for="config in displayConfigs" :key="config.id" class="item" shadow="hover">
        <template #header>
          <div class="head">
            <span>{{ `${config.configNo ?? 'CFG'} · ${config.configName}` }}</span>
            <el-tag size="small" :type="(config.dataScope ?? 'DEMO') === 'MINE' ? 'success' : 'info'">{{ config.dataScope || 'DEMO' }}</el-tag>
          </div>
        </template>
        <div class="meta">
          {{ `Lot ${config.lotNo} | Wafer ${config.waferNo} | Stage ${config.stage} | Points ${config.id == null ? 0 : (configPoints[String(config.id)] || []).length}` }}
        </div>
        <div v-if="config.lastMeasurementRunId == null" class="empty-tip">{{ t('multiWafer.noRunLinkedHint') }}</div>
        <WaferHeatmap v-else :points="config.id == null ? [] : (configPoints[String(config.id)] || [])" :width="260" :height="220" />
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.multi-page { background: linear-gradient(180deg, #f6f9fe 0%, #f2f6fc 42%, #eef3fb 100%); border-radius: 12px; padding: 2px 2px 10px; }
.page-head { padding: 2px 6px 0; }
.status-line { margin-top: 8px; display: flex; align-items: center; gap: 8px; }
.toolbar-card { border: 1px solid #dde7f6; margin-top: 10px; border-radius: 12px; background: rgba(255, 255, 255, 0.9); }
.card { margin-top: 10px; }
.label { display: block; margin-bottom: 6px; color: #4f6887; font-size: 13px; font-weight: 600; }
.option-row { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.option-tags { display: flex; align-items: center; gap: 6px; }
.apply-col { display: flex; align-items: flex-end; justify-content: flex-end; }
.grid { margin-top: 10px; display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 10px; }
.item { min-height: 292px; border: 1px solid #dce6f4; border-radius: 10px; }
.head { display: flex; align-items: center; justify-content: space-between; }
.meta { color: #5a7391; font-size: 12px; margin-bottom: 4px; }
.empty-tip { color: #6b819f; font-size: 12px; min-height: 220px; display: flex; align-items: center; justify-content: center; }
</style>

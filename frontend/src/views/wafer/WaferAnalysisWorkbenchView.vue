<script setup lang="ts">
import type { EChartsOption } from 'echarts'
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import EChartPanel from '@/components/charts/EChartPanel.vue'
import WaferHeatmap from '@/components/wafer/WaferHeatmap.vue'
import { useWaferAnalysisWorkbench } from '@/views/wafer/useWaferAnalysisWorkbench'

const { t } = useI18n()
const vm = useWaferAnalysisWorkbench()

const scatterMagnitudeMax = computed(() => {
  let max = 1
  for (const item of vm.scatterPoints.value) {
    const value = Number(item.overlayMagnitude ?? 0)
    if (Number.isFinite(value) && value > max) {
      max = value
    }
  }
  return max
})

const scatterOption = computed<EChartsOption>(() => ({
  tooltip: {
    trigger: 'item',
    formatter: (params: any) => {
      const point = params.data?.raw
      const x = Number(params.data?.value?.[0] ?? 0).toFixed(3)
      const y = Number(params.data?.value?.[1] ?? 0).toFixed(3)
      const magnitude = Number(point?.overlayMagnitude ?? 0).toFixed(3)
      return `${point?.targetCode ?? '-'}<br/>Overlay X: ${x} nm<br/>Overlay Y: ${y} nm<br/>${t('wafer.metricCode')}: ${magnitude}`
    },
  },
  grid: { left: 64, right: 24, top: 32, bottom: 56 },
  xAxis: {
    type: 'value',
    name: t('wafer.scatterXAxisName'),
    nameLocation: 'middle',
    nameGap: 34,
  },
  yAxis: { type: 'value', name: t('wafer.scatterYAxisName'), nameGap: 28 },
  visualMap: {
    min: 0,
    max: scatterMagnitudeMax.value,
    dimension: 2,
    right: 6,
    top: 20,
    calculable: false,
    text: [t('wafer.scatterColorName'), ''],
    inRange: {
      color: ['#8ec5ff', '#2f78ce', '#123b73'],
    },
  },
  series: [{
    name: t('wafer.scatterPointSeries'),
    type: 'scatter',
    symbolSize: (value: number[]) => Math.max(6, Math.min(16, Number(value?.[2] ?? 0) * 1.2)),
    data: vm.scatterPoints.value.map((item) => ({
      name: item.targetCode,
      value: [Number(item.xValue ?? 0), Number(item.yValue ?? 0), Number(item.overlayMagnitude ?? 0)],
      raw: item,
    })),
    itemStyle: { color: '#2f78ce' },
  }],
}))
const histogramOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 36, right: 16, top: 18, bottom: 52 },
  xAxis: { type: 'category', data: vm.histogramBins.value.map((item) => `${item.rangeStart.toFixed(2)}-${item.rangeEnd.toFixed(2)}`), axisLabel: { rotate: 24, fontSize: 10 } },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: vm.histogramBins.value.map((item) => item.count), itemStyle: { color: '#3f7ec8' } }],
}))
const trendOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 52, right: 24, top: 72, bottom: 52 },
  legend: {
    top: 8,
    left: 'center',
    data: [t('wafer.kpi.passRate'), t('wafer.kpi.meanOverlay'), t('wafer.kpi.p95Overlay')],
  },
  xAxis: {
    type: 'category',
    name: t('wafer.trendAxisRun'),
    data: vm.trendPoints.value.map((item) => item.label || item.date),
    axisLabel: { rotate: 18, margin: 12 },
    nameLocation: 'middle',
    nameGap: 38,
  },
  yAxis: [{ type: 'value', name: '%', nameGap: 16 }, { type: 'value', name: 'nm', nameGap: 16 }],
  series: [
    { name: t('wafer.kpi.passRate'), type: 'line', yAxisIndex: 0, data: vm.trendPoints.value.map((item) => Number(item.passRate) * 100), smooth: true },
    { name: t('wafer.kpi.meanOverlay'), type: 'line', yAxisIndex: 1, data: vm.trendPoints.value.map((item) => Number(item.meanOverlay)), smooth: true },
    { name: t('wafer.kpi.p95Overlay'), type: 'line', yAxisIndex: 1, data: vm.trendPoints.value.map((item) => Number(item.p95Overlay)), smooth: true },
  ],
}))

onMounted(vm.init)
</script>

<template>
  <div v-loading="vm.loading.value" class="wafer-page">
    <div class="page-head">
      <h2 class="page-title">{{ t('wafer.title') }}</h2>
      <div class="page-subtitle">{{ t('wafer.subtitle') }}</div>
    </div>

    <el-card class="card config-card" shadow="never">
      <template #header>{{ t('wafer.configPanel') }}</template>
      <el-form label-position="top" class="config-form">
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12" :md="8" :lg="5">
            <el-form-item :label="t('wafer.configName')">
              <el-select
                v-model="vm.selectedConfigPresetValue.value"
                filterable
                allow-create
                default-first-option
                style="width: 100%"
                @change="vm.onConfigNameSelect(String($event || ''))"
              >
                <el-option v-for="item in vm.configPresetOptions.value" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="5">
            <el-form-item :label="t('wafer.lot')">
              <el-select
                v-model="vm.config.value.lotNo"
                filterable
                allow-create
                default-first-option
                style="width: 100%"
                @change="vm.onConfigLotChange(String(vm.config.value.lotNo || ''))"
              >
                <el-option v-for="item in vm.configLotOptions.value" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="4">
            <el-form-item :label="t('wafer.wafer')">
              <el-select
                v-model="vm.config.value.waferNo"
                filterable
                allow-create
                default-first-option
                style="width: 100%"
                @change="vm.localValidate(vm.config.value)"
              >
                <el-option v-for="item in vm.configWaferOptions.value" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="4">
            <el-form-item :label="t('wafer.layer')">
              <el-select v-model="vm.config.value.layerId" style="width: 100%" @change="vm.localValidate(vm.config.value)">
                <el-option v-for="item in vm.layers.value" :key="item.id" :label="item.layerName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="3">
            <el-form-item :label="t('wafer.stage')">
              <el-select v-model="vm.config.value.stage" style="width: 100%">
                <el-option value="PRE_ETCH" label="PRE_ETCH" />
                <el-option value="POST_ETCH" label="POST_ETCH" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="3" class="actions">
            <el-form-item class="run-action-item" :label="t('wafer.runActionLabel')">
              <el-button class="run-action-button" type="primary" :loading="vm.generating.value" @click="vm.runGenerate(true)">
                {{ t('wafer.runGenerate') }}
              </el-button>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12" :md="8" :lg="4"><el-form-item :label="t('wafer.scannerCorrectionGain')"><el-input-number v-model="vm.config.value.scannerCorrectionGain" :min="0.7" :max="1.3" :step="0.01" @change="vm.localValidate(vm.config.value)" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="4"><el-form-item :label="t('wafer.overlayBaseNm')"><el-input-number v-model="vm.config.value.overlayBaseNm" :min="1" :max="8" :step="0.1" @change="vm.localValidate(vm.config.value)" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="4"><el-form-item :label="t('wafer.edgeGradient')"><el-input-number v-model="vm.config.value.edgeGradient" :min="0" :max="4" :step="0.1" @change="vm.localValidate(vm.config.value)" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="4"><el-form-item :label="t('wafer.hotspotStrength')"><el-input-number v-model="vm.config.value.localHotspotStrength" :min="0" :max="5" :step="0.1" @change="vm.localValidate(vm.config.value)" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="4"><el-form-item :label="t('wafer.noiseLevel')"><el-input-number v-model="vm.config.value.noiseLevel" :min="0" :max="1" :step="0.01" @change="vm.localValidate(vm.config.value)" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="4"><el-form-item :label="t('wafer.gridStep')"><el-input-number v-model="vm.config.value.gridStep" :min="0.5" :max="5" :step="0.5" @change="vm.localValidate(vm.config.value)" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <el-alert v-if="vm.configErrors.value.length" type="error" :title="t('wafer.validationErrors')" :description="vm.configErrors.value.join(' | ')" show-icon :closable="false" />
      <el-descriptions v-if="vm.generateResult.value" :title="t('wafer.generationSummary')" :column="3" border size="small" class="mt">
        <el-descriptions-item :label="t('wafer.overallQuality')">{{ vm.generateResult.value.overallQuality }}</el-descriptions-item>
        <el-descriptions-item :label="t('wafer.overlayStability')">{{ vm.generateResult.value.overlayStability }}</el-descriptions-item>
        <el-descriptions-item :label="t('wafer.edgeRisk')">{{ vm.generateResult.value.edgeRisk }}</el-descriptions-item>
        <el-descriptions-item :label="t('wafer.outlierDensity')">{{ vm.generateResult.value.outlierDensity }}</el-descriptions-item>
        <el-descriptions-item :label="t('wafer.parameterSensitivity')">{{ vm.generateResult.value.parameterSensitivity }}</el-descriptions-item>
        <el-descriptions-item :label="t('wafer.recommendedAction')">{{ vm.generateResult.value.recommendedAction }}</el-descriptions-item>
        <el-descriptions-item :label="t('wafer.summaryText')" :span="3">{{ vm.generateResult.value.summaryText }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-row :gutter="10" class="mt">
      <el-col :span="12">
        <el-card class="result-card" shadow="never">
          <template #header>{{ t('wafer.heatmapTitle') }}</template>
          <WaferHeatmap :points="vm.heatmapPoints.value" :width="430" :height="310" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="result-card chart-card" shadow="never">
          <EChartPanel :title="t('wafer.scatterTitle')" :option="scatterOption" :height="310" />
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="10" class="mt">
      <el-col :span="12">
        <el-card class="result-card chart-card" shadow="never">
          <EChartPanel :title="t('wafer.histogramTitle')" :option="histogramOption" :height="310" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="result-card chart-card" shadow="never">
          <EChartPanel :title="t('wafer.trendTitle')" :option="trendOption" :height="310" />
        </el-card>
      </el-col>
    </el-row>
    <el-card class="mt result-card" shadow="never"><template #header>{{ t('wafer.pointTable') }}</template><el-table :data="vm.pagedHeatmapPoints.value" size="small" height="360"><el-table-column prop="targetCode" :label="t('wafer.target')" width="124" /><el-table-column prop="xCoord" :label="t('wafer.axisX')" width="90" /><el-table-column prop="yCoord" :label="t('wafer.axisY')" width="90" /><el-table-column prop="metricValue" :label="t('wafer.metricCode')" /><el-table-column prop="confidence" :label="t('wafer.confidence')" width="110" /><el-table-column prop="outlier" :label="t('wafer.outlier')" width="90" /></el-table><div class="pager"><el-pagination v-model:current-page="vm.tablePageNo.value" v-model:page-size="vm.tablePageSize.value" :page-sizes="vm.tablePageSizes" layout="total, sizes, prev, pager, next" :total="vm.tableTotal.value" /></div></el-card>
  </div>
</template>

<style scoped>
.wafer-page { background: linear-gradient(180deg, #f7fafe 0%, #f2f6fc 45%, #eef3fb 100%); border-radius: 12px; padding: 2px 2px 12px; }
.page-head { padding: 2px 6px 0; }
.card { margin-top: 10px; }
.config-card { border: 1px solid #dde7f6; border-radius: 12px; background: rgba(255, 255, 255, 0.92); }
.result-card { border: 1px solid #dce6f4; border-radius: 12px; }
.chart-card :deep(.chart-panel) { margin-top: -12px; }
.mt { margin-top: 10px; }
.config-form :deep(.el-input-number) { width: 100%; }
.actions { display: flex; justify-content: flex-end; align-items: flex-end; }
.run-action-item { width: 100%; margin-bottom: 0; }
.run-action-item :deep(.el-form-item__content) { width: 100%; }
.run-action-button { width: 100%; min-width: 112px; height: 32px; }
.pager { margin-top: 10px; display: flex; justify-content: flex-end; }
</style>

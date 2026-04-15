<script setup lang="ts">
import type { EChartsOption } from 'echarts'
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import EChartPanel from '@/components/charts/EChartPanel.vue'
import WaferHeatmap from '@/components/wafer/WaferHeatmap.vue'
import { useWaferAnalysis } from '@/views/wafer/useWaferAnalysis'

const { t } = useI18n()
const vm = useWaferAnalysis()

function lotLabel(lotNo: string, scope?: string) {
  const scopeLabel = scope === 'MINE' ? t('table.mineScope') : t('table.demoScope')
  return `${lotNo} (${scopeLabel})`
}

const scatterOption = computed<EChartsOption>(() => ({
  tooltip: {},
  grid: { left: 36, right: 16, top: 18, bottom: 32 },
  xAxis: { type: 'value', name: 'Focus' },
  yAxis: { type: 'value', name: 'Metric' },
  series: [
    {
      type: 'scatter',
      symbolSize: 6,
      data: vm.scatterPoints.value.map((item) => [item.xValue, item.yValue]),
      itemStyle: { color: '#2f78ce' },
    },
  ],
}))

const histogramOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 36, right: 16, top: 18, bottom: 52 },
  xAxis: {
    type: 'category',
    data: vm.histogramBins.value.map((item) => `${item.rangeStart.toFixed(2)}-${item.rangeEnd.toFixed(2)}`),
    axisLabel: { rotate: 24, fontSize: 10 },
  },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: vm.histogramBins.value.map((item) => item.count), itemStyle: { color: '#3f7ec8' } }],
}))

const trendOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 36, right: 16, top: 30, bottom: 28 },
  legend: { data: ['PassRate', 'MeanOverlay', 'P95Overlay'] },
  xAxis: { type: 'category', data: vm.trendPoints.value.map((item) => item.date) },
  yAxis: [{ type: 'value' }, { type: 'value' }],
  series: [
    { name: 'PassRate', type: 'line', yAxisIndex: 0, data: vm.trendPoints.value.map((item) => Number(item.passRate) * 100), smooth: true },
    { name: 'MeanOverlay', type: 'line', yAxisIndex: 1, data: vm.trendPoints.value.map((item) => Number(item.meanOverlay)), smooth: true },
    { name: 'P95Overlay', type: 'line', yAxisIndex: 1, data: vm.trendPoints.value.map((item) => Number(item.p95Overlay)), smooth: true },
  ],
}))

function onFileChange(uploadFile: { raw?: File }) {
  vm.setImportFile(uploadFile.raw)
}

onMounted(vm.init)
</script>

<template>
  <div v-loading="vm.loading.value" class="wafer-page">
    <h2 class="page-title">{{ t('wafer.title') }}</h2>
    <div class="page-subtitle">{{ t('wafer.subtitle') }}</div>

    <el-card class="filter-card">
      <div class="filter-head">
        <div class="head-text">{{ t('wafer.filterPanel') }}</div>
        <el-space>
          <el-button @click="vm.downloadTemplate">{{ t('wafer.downloadTemplate') }}</el-button>
          <el-button type="primary" plain @click="vm.openImportDialog">{{ t('wafer.importEntry') }}</el-button>
        </el-space>
      </div>
      <el-row :gutter="10">
        <el-col :span="6">
          <div class="filter-label">{{ t('wafer.lot') }}</div>
          <el-select v-model="vm.filters.value.lotId" clearable :placeholder="t('wafer.lot')" style="width: 100%" @change="vm.handleLotChange">
            <el-option
              v-for="item in vm.availableLots.value"
              :key="item.id"
              :label="lotLabel(item.lotNo, item.dataScope)"
              :value="item.id"
            />
          </el-select>
        </el-col>
        <el-col :span="4">
          <div class="filter-label">{{ t('wafer.wafer') }}</div>
          <el-select v-model="vm.filters.value.waferId" clearable :placeholder="t('wafer.wafer')" style="width: 100%" @change="vm.handleWaferChange">
            <el-option v-for="item in vm.filteredWafers.value" :key="item.id" :label="item.waferNo" :value="item.id" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <div class="filter-label">{{ t('wafer.layer') }}</div>
          <el-select v-model="vm.filters.value.layerId" clearable :placeholder="t('wafer.layer')" style="width: 100%" @change="vm.handleLayerChange">
            <el-option v-for="item in vm.filteredLayers.value" :key="item.id" :label="item.layerName" :value="item.id" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <div class="filter-label">{{ t('wafer.stage') }}</div>
          <el-select v-model="vm.filters.value.stage" clearable :placeholder="t('wafer.stage')" style="width: 100%" @change="vm.handleStageChange">
            <el-option v-for="stage in vm.availableStages.value" :key="stage" :value="stage" :label="stage" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <div class="filter-label">{{ t('wafer.measurementRun') }}</div>
          <el-select
            v-model="vm.filters.value.measurementRunId"
            clearable
            :placeholder="t('wafer.measurementRun')"
            style="width: 100%"
            @change="vm.handleMeasurementRunChange"
          >
            <el-option v-for="item in vm.measurementRuns.value" :key="item.id" :value="item.id" :label="`${item.runNo} (${item.dataScope || 'DEMO'})`" />
          </el-select>
        </el-col>
      </el-row>
      <el-row :gutter="10" class="filter-row">
        <el-col :span="4">
          <div class="filter-label">{{ t('wafer.measurementType') }}</div>
          <el-select v-model="vm.filters.value.measurementType" :placeholder="t('wafer.measurementType')" style="width: 100%">
            <el-option value="OVERLAY" label="OVERLAY" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <div class="filter-label">{{ t('wafer.metricCode') }}</div>
          <el-select v-model="vm.filters.value.metricCode" :placeholder="t('wafer.metricCode')" style="width: 100%">
            <el-option value="overlay_magnitude" label="overlay_magnitude" />
            <el-option value="overlay_x" label="overlay_x" />
            <el-option value="overlay_y" label="overlay_y" />
            <el-option value="residual" label="residual" />
            <el-option value="focus" label="focus" />
            <el-option value="dose" label="dose" />
          </el-select>
        </el-col>
        <el-col :span="16" class="filter-action">
          <el-button type="primary" @click="vm.applyFilters">{{ t('common.search') }}</el-button>
        </el-col>
      </el-row>
      <el-alert
        v-if="!vm.hasAnyChartData.value"
        class="empty-alert"
        type="warning"
        :closable="false"
        show-icon
        :title="t('wafer.emptyHint')"
      />
    </el-card>

    <el-row :gutter="10" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>{{ t('wafer.heatmapTitle') }}</template>
          <WaferHeatmap :points="vm.heatmapPoints.value" :width="430" :height="310" />
          <div class="axis-note">{{ t('wafer.axisNote') }}</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <EChartPanel :title="t('wafer.scatterTitle')" :option="scatterOption" :height="310" />
      </el-col>
    </el-row>

    <el-row :gutter="10" class="chart-row">
      <el-col :span="12">
        <EChartPanel :title="t('wafer.histogramTitle')" :option="histogramOption" :height="310" />
      </el-col>
      <el-col :span="12">
        <EChartPanel :title="t('wafer.trendTitle')" :option="trendOption" :height="310" />
      </el-col>
    </el-row>

    <el-card class="table-card">
      <template #header>{{ t('wafer.pointTable') }}</template>
      <el-table :data="vm.pagedHeatmapPoints.value" size="small" height="360">
        <el-table-column prop="targetCode" :label="t('wafer.target')" width="124" />
        <el-table-column prop="xCoord" :label="t('wafer.axisX')" width="90" />
        <el-table-column prop="yCoord" :label="t('wafer.axisY')" width="90" />
        <el-table-column prop="metricValue" :label="t('wafer.metricCode')" />
        <el-table-column prop="confidence" :label="t('wafer.confidence')" width="110" />
        <el-table-column prop="outlier" :label="t('wafer.outlier')" width="90" />
      </el-table>
      <div class="table-pagination">
        <el-pagination
          v-model:current-page="vm.tablePageNo.value"
          v-model:page-size="vm.tablePageSize.value"
          :page-sizes="vm.tablePageSizes"
          layout="total, sizes, prev, pager, next"
          :total="vm.tableTotal.value"
        />
      </div>
    </el-card>

    <el-dialog v-model="vm.importDialogVisible.value" :title="t('wafer.importTitle')" width="920px">
      <div class="import-grid">
        <el-form label-position="top">
          <el-row :gutter="10">
            <el-col :span="6">
              <el-form-item :label="t('wafer.lot')">
                <el-input v-model="vm.importForm.value.lotNo" />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item :label="t('wafer.wafer')">
                <el-input v-model="vm.importForm.value.waferNo" />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item :label="t('wafer.layer')">
                <el-select v-model="vm.importForm.value.layerId" style="width: 100%">
                  <el-option v-for="item in vm.layers.value" :key="item.id" :value="item.id" :label="item.layerName" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item :label="t('wafer.measurementRun')">
                <el-input v-model="vm.importForm.value.runNo" :placeholder="t('wafer.autoGenerateRunNo')" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="10">
            <el-col :span="4"><el-form-item :label="t('wafer.mappingX')"><el-input v-model="vm.importForm.value.fieldMapping.xCoordColumn" /></el-form-item></el-col>
            <el-col :span="4"><el-form-item :label="t('wafer.mappingY')"><el-input v-model="vm.importForm.value.fieldMapping.yCoordColumn" /></el-form-item></el-col>
            <el-col :span="4"><el-form-item :label="t('wafer.mappingTarget')"><el-input v-model="vm.importForm.value.fieldMapping.targetCodeColumn" /></el-form-item></el-col>
            <el-col :span="4"><el-form-item :label="t('wafer.mappingOverlayX')"><el-input v-model="vm.importForm.value.fieldMapping.overlayXColumn" /></el-form-item></el-col>
            <el-col :span="4"><el-form-item :label="t('wafer.mappingOverlayY')"><el-input v-model="vm.importForm.value.fieldMapping.overlayYColumn" /></el-form-item></el-col>
            <el-col :span="4"><el-form-item :label="t('wafer.mappingMagnitude')"><el-input v-model="vm.importForm.value.fieldMapping.overlayMagnitudeColumn" /></el-form-item></el-col>
          </el-row>
          <el-row :gutter="10">
            <el-col :span="4"><el-form-item :label="t('wafer.mappingResidual')"><el-input v-model="vm.importForm.value.fieldMapping.residualColumn" /></el-form-item></el-col>
            <el-col :span="4"><el-form-item :label="t('wafer.mappingFocus')"><el-input v-model="vm.importForm.value.fieldMapping.focusColumn" /></el-form-item></el-col>
            <el-col :span="4"><el-form-item :label="t('wafer.mappingDose')"><el-input v-model="vm.importForm.value.fieldMapping.doseColumn" /></el-form-item></el-col>
            <el-col :span="4"><el-form-item :label="t('wafer.mappingConfidence')"><el-input v-model="vm.importForm.value.fieldMapping.confidenceColumn" /></el-form-item></el-col>
            <el-col :span="4"><el-form-item :label="t('wafer.mappingOutlier')"><el-input v-model="vm.importForm.value.fieldMapping.outlierColumn" /></el-form-item></el-col>
            <el-col :span="4"><el-form-item :label="t('wafer.outlierThreshold')"><el-input-number v-model="vm.importForm.value.outlierThreshold" :min="0" :step="0.1" style="width: 100%" /></el-form-item></el-col>
          </el-row>
          <el-row :gutter="10">
            <el-col :span="8">
              <el-form-item :label="t('wafer.csvFile')">
                <el-upload :auto-upload="false" :limit="1" accept=".csv" :on-change="onFileChange" :show-file-list="true">
                  <el-button>{{ t('wafer.selectCsv') }}</el-button>
                </el-upload>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <el-alert type="info" :closable="false" show-icon :title="t('wafer.importHint')" />
        <el-descriptions v-if="vm.importResult.value" :title="t('wafer.importSummary')" :column="2" border size="small">
          <el-descriptions-item :label="t('wafer.importStatus')">{{ vm.importResult.value.status }}</el-descriptions-item>
          <el-descriptions-item :label="t('wafer.measurementRun')">{{ vm.importResult.value.measurementRunNo }}</el-descriptions-item>
          <el-descriptions-item :label="t('wafer.totalRows')">{{ vm.importResult.value.totalRows }}</el-descriptions-item>
          <el-descriptions-item :label="t('wafer.insertedRows')">{{ vm.importResult.value.insertedRows }}</el-descriptions-item>
          <el-descriptions-item :label="t('wafer.skippedRows')">{{ vm.importResult.value.skippedOutsideRows }}</el-descriptions-item>
          <el-descriptions-item :label="t('wafer.failedRows')">{{ vm.importResult.value.failedRows }}</el-descriptions-item>
          <el-descriptions-item :label="t('wafer.elapsedMs')">{{ vm.importResult.value.elapsedMs }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-space>
          <el-button @click="vm.importDialogVisible.value = false">{{ t('common.close') }}</el-button>
          <el-button type="primary" :loading="vm.importLoading.value" @click="vm.submitImport">{{ t('wafer.submitImport') }}</el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.wafer-page {
  overflow: hidden;
}

.filter-card {
  margin-top: 10px;
}

.filter-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.head-text {
  font-weight: 600;
  color: #17395f;
}

.filter-row {
  margin-top: 4px;
}

.filter-label {
  margin-bottom: 4px;
  font-size: 12px;
  color: #45607f;
}

.filter-action {
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;
}

.empty-alert {
  margin-top: 8px;
}

.chart-row {
  margin-top: 10px;
}

.chart-card {
  height: 392px;
}

.axis-note {
  margin-top: 4px;
  color: #5a7391;
  font-size: 12px;
}

.table-card {
  margin-top: 10px;
}

.table-pagination {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.import-grid {
  display: grid;
  gap: 8px;
}
</style>

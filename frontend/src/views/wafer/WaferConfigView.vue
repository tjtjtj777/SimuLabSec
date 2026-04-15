<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { masterDataApi } from '@/api/modules/master-data'
import { waferConfigApi } from '@/api/modules/wafer-config'
import type { LayerItem, WaferConfigItem } from '@/types/domain'

const { t } = useI18n()
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const filters = ref({ keyword: '', dataScope: '', pageNo: 1, pageSize: 10 })
const total = ref(0)
const rows = ref<WaferConfigItem[]>([])
const layers = ref<LayerItem[]>([])
const editingId = ref<string | number>()
const form = ref<WaferConfigItem>({
  configName: '', lotNo: '', waferNo: '', layerId: 1, measurementType: 'OVERLAY', stage: 'PRE_ETCH',
  scannerCorrectionGain: 1, overlayBaseNm: 3.2, edgeGradient: 1.3, localHotspotStrength: 1, noiseLevel: 0.15, gridStep: 2, outlierThreshold: 8,
})

function buildEmptyForm(): WaferConfigItem {
  return {
    configName: '',
    lotNo: '',
    waferNo: '',
    layerId: layers.value[0]?.id ?? 1,
    measurementType: 'OVERLAY',
    stage: 'PRE_ETCH',
    scannerCorrectionGain: 1,
    overlayBaseNm: 3.2,
    edgeGradient: 1.3,
    localHotspotStrength: 1,
    noiseLevel: 0.15,
    gridStep: 2,
    outlierThreshold: 8,
  }
}

async function load() {
  loading.value = true
  try {
    const [page, layerPage] = await Promise.all([
      waferConfigApi.getPage(filters.value),
      masterDataApi.getLayers({ pageNo: 1, pageSize: 100 }),
    ])
    rows.value = page.records.slice().sort((a, b) => {
      const scopeA = a.dataScope ?? 'DEMO'
      const scopeB = b.dataScope ?? 'DEMO'
      if (scopeA !== scopeB) {
        return scopeA === 'DEMO' ? -1 : 1
      }
      return new Date(b.updatedAt || '').getTime() - new Date(a.updatedAt || '').getTime()
    })
    total.value = page.total
    layers.value = layerPage.records
  } finally {
    loading.value = false
  }
}

function create() {
  editingId.value = undefined
  dialogTitle.value = t('waferConfig.newConfig')
  form.value = buildEmptyForm()
  dialogVisible.value = true
}

function edit(row: WaferConfigItem) {
  if (row.dataScope === 'DEMO' || row.editable === 0) {
    ElMessage.warning(t('waferConfig.demoReadonly'))
    return
  }
  editingId.value = row.id
  dialogTitle.value = t('waferConfig.editConfig')
  form.value = { ...row }
  dialogVisible.value = true
}

async function submit() {
  if (editingId.value != null) await waferConfigApi.update(editingId.value, form.value)
  else await waferConfigApi.create(form.value)
  dialogVisible.value = false
  ElMessage.success(t('common.operation'))
  await load()
}

async function remove(row: WaferConfigItem) {
  if (row.dataScope === 'DEMO' || !row.id) return
  await ElMessageBox.confirm(t('waferConfig.deleteConfirm'), t('common.operation'), { type: 'warning' })
  await waferConfigApi.remove(row.id)
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="page-title">{{ t('waferConfig.title') }}</h2>
    <div class="page-subtitle">{{ t('waferConfig.subtitle') }}</div>
    <el-card class="card">
      <div class="toolbar">
        <el-input v-model="filters.keyword" :placeholder="t('task.keyword')" style="width: 220px" clearable />
        <el-select v-model="filters.dataScope" style="width: 140px">
          <el-option value="" :label="t('common.all')" />
          <el-option value="DEMO" :label="t('table.demoScope')" />
          <el-option value="MINE" :label="t('table.mineScope')" />
        </el-select>
        <el-button type="primary" @click="load">{{ t('common.search') }}</el-button>
        <el-button @click="create">{{ t('waferConfig.newConfig') }}</el-button>
      </div>
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="configNo" label="Config No" width="170" />
        <el-table-column prop="configName" :label="t('wafer.configName')" />
        <el-table-column prop="lotNo" :label="t('wafer.lot')" width="130" />
        <el-table-column prop="waferNo" :label="t('wafer.wafer')" width="110" />
        <el-table-column prop="dataScope" :label="t('waferConfig.dataScope')" width="110" />
        <el-table-column fixed="right" :label="t('common.actions')" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="edit(row)">{{ t('waferConfig.editConfig') }}</el-button>
            <el-button link type="danger" :disabled="row.dataScope === 'DEMO'" @click="remove(row)">{{ t('waferConfig.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="filters.pageNo" v-model:page-size="filters.pageSize" layout="total, prev, pager, next" :total="total" @current-change="load" /></div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px">
      <el-form label-position="top">
        <el-row :gutter="10">
          <el-col :span="8"><el-form-item :label="t('wafer.configName')"><el-input v-model="form.configName" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item :label="t('wafer.lot')"><el-input v-model="form.lotNo" /></el-form-item></el-col>
          <el-col :span="4"><el-form-item :label="t('wafer.wafer')"><el-input v-model="form.waferNo" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item :label="t('wafer.layer')"><el-select v-model="form.layerId" style="width: 100%"><el-option v-for="item in layers" :key="item.id" :value="item.id" :label="item.layerName" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="8"><el-form-item :label="t('wafer.scannerCorrectionGain')"><el-input-number v-model="form.scannerCorrectionGain" :min="0.7" :max="1.3" :step="0.01" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="t('wafer.overlayBaseNm')"><el-input-number v-model="form.overlayBaseNm" :min="1" :max="8" :step="0.1" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="t('wafer.edgeGradient')"><el-input-number v-model="form.edgeGradient" :min="0" :max="4" :step="0.1" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><el-space><el-button @click="dialogVisible=false">{{ t('common.close') }}</el-button><el-button type="primary" @click="submit">{{ t('common.operation') }}</el-button></el-space></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card { margin-top: 10px; }
.toolbar { margin-bottom: 12px; display: flex; gap: 8px; }
.pager { margin-top: 10px; display: flex; justify-content: flex-end; }
</style>

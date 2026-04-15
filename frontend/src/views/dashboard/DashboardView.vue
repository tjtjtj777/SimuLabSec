<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { dashboardApi } from '@/api/modules/dashboard'
import { simulationTaskApi } from '@/api/modules/simulation-task'
import BarChartPanel from '@/components/charts/BarChartPanel.vue'
import LineChartPanel from '@/components/charts/LineChartPanel.vue'
import type { DashboardOverview, SimulationTaskItem } from '@/types/domain'

const { t } = useI18n()
const loading = ref(false)
const tasks = ref<SimulationTaskItem[]>([])
const overview = ref<DashboardOverview>({
  totalLots: 0,
  totalWafers: 0,
  runningTasks: 0,
  successRate: 0,
  passRate: 0,
  avgOverlay: 0,
  maxOverlay: 0,
  releasedRecipeCount: 0,
})

const statusStats = computed(() => {
  const aggregate = new Map<string, number>()
  tasks.value.forEach((item) => {
    const current = aggregate.get(item.status) ?? 0
    aggregate.set(item.status, current + 1)
  })
  return {
    categories: Array.from(aggregate.keys()),
    values: Array.from(aggregate.values()),
  }
})

const kpiCards = computed(() => [
  { label: t('dashboard.totalLots'), value: overview.value.totalLots },
  { label: t('dashboard.totalWafers'), value: overview.value.totalWafers },
  { label: t('dashboard.runningTasks'), value: overview.value.runningTasks },
  { label: t('dashboard.successRate'), value: `${Number(overview.value.successRate).toFixed(2)}%` },
  { label: t('dashboard.passRate'), value: `${Number(overview.value.passRate).toFixed(2)}%` },
  { label: t('dashboard.avgOverlay'), value: Number(overview.value.avgOverlay).toFixed(2) },
  { label: t('dashboard.maxOverlay'), value: Number(overview.value.maxOverlay).toFixed(2) },
  { label: t('dashboard.releasedRecipeCount'), value: overview.value.releasedRecipeCount },
])

async function fetchData() {
  loading.value = true
  try {
    const [overviewData, taskData] = await Promise.all([dashboardApi.getOverview(), simulationTaskApi.getList()])
    overview.value = overviewData
    tasks.value = taskData
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div v-loading="loading">
    <h2 class="page-title">{{ t('dashboard.title') }}</h2>
    <div class="page-subtitle">{{ t('dashboard.subtitle') }}</div>

    <div class="kpi-grid">
      <el-card v-for="card in kpiCards" :key="card.label" class="kpi-card">
        <div class="kpi-label">{{ card.label }}</div>
        <div class="kpi-value">{{ card.value }}</div>
      </el-card>
    </div>

    <el-row :gutter="12">
      <el-col :span="12">
        <LineChartPanel
          :title="t('dashboard.trendLine')"
          :categories="['W1', 'W2', 'W3', 'W4', 'W5', 'W6']"
          :values="[90.1, 91.2, 90.8, 92.5, 93.2, 94.1]"
        />
      </el-col>
      <el-col :span="12">
        <BarChartPanel :title="t('dashboard.taskBar')" :categories="statusStats.categories" :values="statusStats.values" />
      </el-col>
    </el-row>

    <el-card class="task-table">
      <template #header>{{ t('dashboard.recentTasks') }}</template>
      <el-table :data="tasks" size="small">
        <el-table-column prop="taskNo" :label="t('task.taskNo')" />
        <el-table-column prop="taskName" :label="t('task.taskName')" />
        <el-table-column prop="scenarioType" :label="t('task.scenarioType')" />
        <el-table-column prop="status" :label="t('common.status')" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 14px 0;
}

.kpi-card {
  border-radius: 8px;
}

.kpi-label {
  color: #587191;
  font-size: 13px;
}

.kpi-value {
  margin-top: 8px;
  font-size: 26px;
  color: #103055;
  font-weight: 700;
}

.task-table {
  margin-top: 12px;
}
</style>

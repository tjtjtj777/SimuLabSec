<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTaskCenter } from '@/views/tasks/useTaskCenter'

const { t } = useI18n()
const vm = useTaskCenter()
const statusOptions = computed(() => Array.from(new Set(vm.tasks.value.map((item) => item.status))))
const scenarioOptions = computed(() => Array.from(new Set(vm.tasks.value.map((item) => item.scenarioType))))

function resolveScope(scope?: string) {
  return scope === 'MINE' ? t('table.mineScope') : t('table.demoScope')
}

onMounted(vm.loadTasks)
</script>

<template>
  <div>
    <h2 class="page-title">{{ t('task.title') }}</h2>
    <div class="page-subtitle">{{ t('task.subtitle') }}</div>

    <div class="summary-grid">
      <el-card v-for="item in vm.statusSummary.value" :key="item.status" class="summary-card">
        <div class="name">{{ item.status }}</div>
        <div class="count">{{ item.count }}</div>
      </el-card>
    </div>

    <el-card class="card">
      <div class="toolbar">
        <el-input v-model="vm.filters.value.keyword" :placeholder="t('task.keyword')" style="width: 220px" clearable />
        <el-select v-model="vm.filters.value.status" clearable :placeholder="t('common.status')" style="width: 160px">
          <el-option :label="t('common.all')" value="" />
          <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="vm.filters.value.scenarioType" clearable :placeholder="t('task.scenarioType')" style="width: 160px">
          <el-option :label="t('common.all')" value="" />
          <el-option v-for="item in scenarioOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button type="primary" @click="vm.loadTasks">{{ t('common.search') }}</el-button>
      </div>
      <el-table :data="vm.pagedTasks.value" v-loading="vm.loading.value" height="560">
        <el-table-column prop="taskNo" :label="t('task.taskNo')" />
        <el-table-column prop="taskName" :label="t('task.taskName')" />
        <el-table-column prop="scenarioType" :label="t('task.scenarioType')" />
        <el-table-column :label="t('table.dataScope')" width="96">
          <template #default="{ row }">
            <el-tag size="small" :type="row.dataScope === 'MINE' ? 'success' : 'info'">{{ resolveScope(row.dataScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('common.status')" />
        <el-table-column prop="priorityLevel" :label="t('task.priority')" />
        <el-table-column prop="createdAt" :label="t('task.createdAt')" />
        <el-table-column fixed="right" width="120" :label="t('common.actions')">
          <template #default="{ row }">
            <el-button link type="primary" @click="vm.openTaskDetail(row.id)">{{ t('task.viewDetail') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="vm.tablePageNo.value"
          v-model:page-size="vm.tablePageSize.value"
          layout="total, prev, pager, next"
          :total="vm.tableTotal.value"
          :pager-count="7"
          @current-change="vm.onTablePageChange"
        />
      </div>
    </el-card>

    <el-drawer v-model="vm.detailVisible.value" :title="t('task.detailTitle')" size="35%">
      <el-descriptions :column="1" border v-loading="vm.detailLoading.value">
        <el-descriptions-item :label="t('task.taskNo')">{{ vm.selectedTaskDetail.value?.taskNo }}</el-descriptions-item>
        <el-descriptions-item :label="t('task.taskName')">{{ vm.selectedTaskDetail.value?.taskName }}</el-descriptions-item>
        <el-descriptions-item :label="t('task.scenarioType')">{{ vm.selectedTaskDetail.value?.scenarioType }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.status')">{{ vm.selectedTaskDetail.value?.status }}</el-descriptions-item>
        <el-descriptions-item :label="t('task.priority')">{{ vm.selectedTaskDetail.value?.priorityLevel }}</el-descriptions-item>
        <el-descriptions-item label="Input Snapshot">
          <pre class="json">{{ vm.selectedTaskDetail.value?.inputSnapshotJson }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="Execution Context">
          <pre class="json">{{ vm.selectedTaskDetail.value?.executionContextJson }}</pre>
        </el-descriptions-item>
        <el-descriptions-item :label="t('task.resultSummary')">
          <pre class="json">{{ vm.selectedTaskDetail.value?.resultSummaryJson }}</pre>
        </el-descriptions-item>
        <el-descriptions-item :label="t('task.errorMessage')">{{ vm.selectedTaskDetail.value?.errorMessage || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<style scoped>
.summary-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.summary-card .name {
  font-size: 13px;
  color: #5f7591;
}

.summary-card .count {
  margin-top: 8px;
  font-size: 24px;
  color: #13375f;
  font-weight: 700;
}

.card {
  margin-top: 12px;
}

.toolbar {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}

.json {
  white-space: pre-wrap;
  margin: 0;
}

.pager {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}
</style>

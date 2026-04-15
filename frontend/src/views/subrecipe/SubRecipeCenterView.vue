<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useSubRecipeCenter } from '@/views/subrecipe/useSubRecipeCenter'

const { t } = useI18n()
const vm = useSubRecipeCenter()

function resolveScope(scope?: string) {
  return scope === 'MINE' ? t('table.mineScope') : t('table.demoScope')
}

async function onMockUpload() {
  await vm.requestUploadTicket('subrecipe-template.json', 'application/json')
}

onMounted(vm.loadList)
</script>

<template>
  <div>
    <h2 class="page-title">{{ t('subRecipe.title') }}</h2>
    <div class="page-subtitle">{{ t('subRecipe.subtitle') }}</div>

    <el-card class="toolbar-card">
      <el-space wrap>
        <el-select v-model="vm.filters.value.status" clearable :placeholder="t('common.status')" style="width: 180px">
          <el-option label="READY" value="READY" />
          <el-option label="DRAFT" value="DRAFT" />
        </el-select>
        <el-select v-model="vm.filters.value.generationType" clearable :placeholder="t('subRecipe.generationType')" style="width: 180px">
          <el-option label="AUTO" value="AUTO" />
          <el-option label="MANUAL" value="MANUAL" />
        </el-select>
        <el-select v-model="vm.exportFormat.value" style="width: 140px">
          <el-option label="JSON" value="JSON" />
          <el-option label="CSV" value="CSV" />
        </el-select>
        <el-button type="primary" @click="vm.loadList">{{ t('common.search') }}</el-button>
        <el-button @click="onMockUpload">{{ t('subRecipe.uploadTicket') }}</el-button>
      </el-space>
    </el-card>

    <el-card class="list-card">
      <el-table :data="vm.items.value" v-loading="vm.loading.value">
        <el-table-column prop="subRecipeCode" :label="t('subRecipe.subRecipeCode')" />
        <el-table-column :label="t('table.dataScope')" width="96">
          <template #default="{ row }">
            <el-tag size="small" :type="row.dataScope === 'MINE' ? 'success' : 'info'">{{ resolveScope(row.dataScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('common.status')" />
        <el-table-column prop="generationType" :label="t('subRecipe.generationType')" />
        <el-table-column prop="exportFormat" :label="t('subRecipe.exportFormat')" />
        <el-table-column fixed="right" width="220" :label="t('common.actions')">
          <template #default="{ row }">
            <el-space>
              <el-button link type="primary" @click="vm.openDetail(row.id)">{{ t('subRecipe.detail') }}</el-button>
              <el-button link type="success" @click="vm.requestDownloadTicket(row.id)">{{ t('subRecipe.downloadTicket') }}</el-button>
              <el-button link type="warning" @click="vm.requestExportTicket(row.id)">{{ t('subRecipe.exportTicket') }}</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="vm.detailVisible.value" :title="t('subRecipe.detail')" size="35%">
      <el-descriptions :column="1" border v-loading="vm.detailLoading.value">
        <el-descriptions-item :label="t('subRecipe.subRecipeCode')">{{ vm.selectedDetail.value?.subRecipeCode }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.status')">{{ vm.selectedDetail.value?.status }}</el-descriptions-item>
        <el-descriptions-item :label="t('subRecipe.generationType')">{{ vm.selectedDetail.value?.generationType }}</el-descriptions-item>
        <el-descriptions-item :label="t('subRecipe.paramDelta')">
          <pre class="json">{{ vm.selectedDetail.value?.paramDeltaJson }}</pre>
        </el-descriptions-item>
        <el-descriptions-item :label="t('subRecipe.paramSet')">
          <pre class="json">{{ vm.selectedDetail.value?.paramSetJson }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="vm.ticketVisible.value" :title="t('subRecipe.ticketTitle')" width="520px">
      <el-alert :title="t('subRecipe.ticketHint')" type="info" :closable="false" show-icon />
      <el-descriptions :column="1" border style="margin-top: 16px">
        <el-descriptions-item :label="t('common.operation')">{{ vm.latestTicket.value?.action }}</el-descriptions-item>
        <el-descriptions-item :label="t('subRecipe.fileName')">{{ vm.latestTicket.value?.fileName }}</el-descriptions-item>
        <el-descriptions-item :label="t('subRecipe.fileType')">{{ vm.latestTicket.value?.fileType }}</el-descriptions-item>
        <el-descriptions-item :label="t('subRecipe.objectPath')">
          <div class="ticket-path">{{ vm.latestTicket.value?.objectPath }}</div>
        </el-descriptions-item>
        <el-descriptions-item :label="t('subRecipe.expireAt')">{{ vm.latestTicket.value?.expireAt }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-space>
          <el-button @click="vm.ticketVisible.value = false">{{ t('common.close') }}</el-button>
          <el-button type="primary" @click="vm.copyTicketPath">{{ t('subRecipe.copyPath') }}</el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar-card,
.list-card {
  margin-top: 12px;
}

.json {
  margin: 0;
  white-space: pre-wrap;
}

.ticket-path {
  word-break: break-all;
}
</style>

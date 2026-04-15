<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRecipeCenter } from '@/views/recipe/useRecipeCenter'

const { t } = useI18n()
const vm = useRecipeCenter()

function resolveScope(scope?: string) {
  return scope === 'MINE' ? t('table.mineScope') : t('table.demoScope')
}

watch(vm.selectedRecipeId, async () => {
  await vm.loadVersions()
  await vm.loadCompare()
})

watch([vm.leftVersionId, vm.rightVersionId], vm.loadCompare)
onMounted(vm.init)
</script>

<template>
  <div v-loading="vm.loading.value">
    <h2 class="page-title">{{ t('recipe.title') }}</h2>
    <div class="page-subtitle">{{ t('recipe.subtitle') }}</div>

    <el-card class="select-card">
      <el-space wrap>
        <el-select v-model="vm.selectedRecipeId.value" clearable style="width: 260px" :placeholder="t('recipe.recipeList')">
          <el-option v-for="item in vm.recipes.value" :key="item.id" :label="item.recipeName" :value="item.id" />
        </el-select>
        <el-select v-model="vm.leftVersionId.value" style="width: 220px" :placeholder="t('recipe.leftVersion')">
          <el-option v-for="item in vm.versions.value" :key="item.id" :label="item.versionLabel" :value="item.id" />
        </el-select>
        <el-select v-model="vm.rightVersionId.value" style="width: 220px" :placeholder="t('recipe.rightVersion')">
          <el-option v-for="item in vm.versions.value" :key="item.id" :label="item.versionLabel" :value="item.id" />
        </el-select>
      </el-space>
    </el-card>

    <el-row :gutter="12" class="list-area">
      <el-col :span="12">
        <el-card>
          <template #header>{{ t('recipe.recipeList') }}</template>
          <el-table :data="vm.recipes.value" size="small">
            <el-table-column prop="recipeCode" :label="t('recipe.recipeCode')" />
            <el-table-column prop="recipeName" :label="t('recipe.recipeName')" />
            <el-table-column prop="recipeType" :label="t('recipe.recipeType')" />
            <el-table-column :label="t('table.dataScope')" width="96">
              <template #default="{ row }">
                <el-tag size="small" :type="row.dataScope === 'MINE' ? 'success' : 'info'">{{ resolveScope(row.dataScope) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="t('common.status')" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>{{ t('recipe.versionList') }}</template>
          <el-table :data="vm.versions.value" size="small">
            <el-table-column prop="versionNo" :label="t('recipe.versionNo')" />
            <el-table-column prop="versionLabel" :label="t('recipe.versionLabel')" />
            <el-table-column prop="status" :label="t('common.status')" />
            <el-table-column prop="changeSummary" :label="t('recipe.changeSummary')" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="compare-card" v-loading="vm.compareLoading.value">
      <template #header>{{ t('recipe.comparePanel') }}</template>
      <el-descriptions :column="2" border v-if="vm.compareResult.value">
        <el-descriptions-item :label="t('recipe.leftVersion')">{{ vm.compareResult.value.leftVersionLabel }}</el-descriptions-item>
        <el-descriptions-item :label="t('recipe.rightVersion')">{{ vm.compareResult.value.rightVersionLabel }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="vm.compareResult.value?.diffs ?? []" size="small" style="margin-top: 10px">
        <el-table-column prop="paramName" :label="t('recipe.paramName')" />
        <el-table-column prop="leftValue" :label="t('recipe.leftValue')" />
        <el-table-column prop="rightValue" :label="t('recipe.rightValue')" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.select-card,
.list-area,
.compare-card {
  margin-top: 12px;
}
</style>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const { t } = useI18n()
const apiBase = computed(() => import.meta.env.VITE_API_BASE_URL ?? '/simulab')
</script>

<template>
  <div>
    <h2 class="page-title">{{ t('settings.title') }}</h2>
    <div class="page-subtitle">{{ t('settings.subtitle') }}</div>
    <el-card class="card">
      <el-form label-width="240px">
        <el-form-item :label="t('settings.language')">
          <el-select :model-value="appStore.locale" @change="appStore.setLocale">
            <el-option :label="t('common.languageEnglish')" value="en" />
            <el-option :label="t('common.languageZhCN')" value="zh-CN" />
          </el-select>
          <span class="hint">{{ t('settings.languageHint') }}</span>
        </el-form-item>
        <el-form-item :label="t('settings.currentApiBase')">
          <el-tag type="info">{{ apiBase }}</el-tag>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.card {
  margin-top: 14px;
}

.hint {
  margin-left: 12px;
  color: #647b99;
}
</style>

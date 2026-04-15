<script setup lang="ts">
import { DataAnalysis, Grid, Operation, Setting } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

const route = useRoute()
const { t } = useI18n()

const menus = [
  { key: 'dashboard', icon: DataAnalysis, to: '/dashboard', titleKey: 'nav.dashboard' },
  { key: 'wafer', icon: Operation, to: '/wafer-analysis', titleKey: 'nav.wafer' },
  { key: 'multi-wafer', icon: Operation, to: '/multi-wafer-heatmap', titleKey: 'nav.multiWafer' },
  { key: 'wafer-config', icon: Operation, to: '/wafer-config', titleKey: 'nav.waferConfig' },
  { key: 'tasks', icon: Grid, to: '/tasks', titleKey: 'nav.tasks' },
  { key: 'settings', icon: Setting, to: '/settings', titleKey: 'nav.settings' },
]
</script>

<template>
  <div class="sidebar">
    <div class="logo">
      <div class="logo-mark">SL</div>
      <div>
        <div class="logo-name">{{ t('common.appName') }}</div>
        <div class="logo-sub">{{ t('common.appSubtitle') }}</div>
      </div>
    </div>
    <el-menu :default-active="route.path" router class="menu">
      <el-menu-item v-for="item in menus" :key="item.key" :index="item.to">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ t(item.titleKey) }}</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<style scoped>
.sidebar {
  height: 100vh;
  background: linear-gradient(180deg, #113056 0%, #0b2542 100%);
  color: #d9e6f7;
}

.logo {
  height: 72px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
  border-bottom: 1px solid rgba(160, 191, 228, 0.22);
}

.logo-mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #2f78ce;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
}

.logo-name {
  font-weight: 700;
}

.logo-sub {
  font-size: 12px;
  color: #9eb3cd;
}

.menu {
  border-right: none;
  background: transparent;
}

.menu :deep(.el-menu-item) {
  color: #d9e6f7;
}

.menu :deep(.el-menu-item.is-active) {
  color: #ffffff;
  background: rgba(67, 142, 231, 0.28);
}
</style>

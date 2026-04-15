<template>
  <el-container class="layout">
    <el-aside width="220px">
      <SidebarNav />
    </el-aside>
    <el-container>
      <el-header class="header">
        <TopBar />
      </el-header>
      <el-main class="main">
        <RouterView v-slot="{ Component, route }">
          <KeepAlive :include="cachedRouteNames">
            <component :is="Component" :key="route.name" />
          </KeepAlive>
        </RouterView>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { KeepAlive } from 'vue'
import { RouterView } from 'vue-router'
import SidebarNav from '@/layout/SidebarNav.vue'
import TopBar from '@/layout/TopBar.vue'

const cachedRouteNames = ['wafer-analysis', 'multi-wafer-heatmap']
</script>

<style scoped>
.layout {
  min-height: 100vh;
  background: #f3f7fc;
}

.layout :deep(.el-aside) {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 20;
}

.layout > .el-container {
  margin-left: 220px;
  min-height: 100vh;
}

.header {
  padding: 0;
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 10;
  background: #f3f7fc;
}

.main {
  padding: 16px;
}
</style>

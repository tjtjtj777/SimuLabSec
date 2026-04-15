<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const appStore = useAppStore()
const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const pageTitle = computed(() => t((route.meta.titleKey as string) || 'common.appName'))

function handleLanguageChange(value: 'en' | 'zh-CN') {
  appStore.setLocale(value)
}

async function handleLogout() {
  await authStore.logout()
  await router.replace({ name: 'login' })
}
</script>

<template>
  <div class="topbar">
    <div>
      <h1 class="title">{{ pageTitle }}</h1>
    </div>
    <div class="actions">
      <el-select :model-value="appStore.locale" class="lang-select" @change="handleLanguageChange">
        <el-option :label="t('common.languageEnglish')" value="en" />
        <el-option :label="t('common.languageZhCN')" value="zh-CN" />
      </el-select>
      <el-dropdown>
        <span class="user">{{ authStore.displayName || authStore.username }}</span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="handleLogout">{{ t('common.logout') }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style scoped>
.topbar {
  height: 64px;
  border-bottom: 1px solid #d7e2ef;
  background: #f8fbff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.title {
  margin: 0;
  font-size: 18px;
  color: #0b284e;
}

.actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.lang-select {
  width: 130px;
}

.user {
  color: #2b456a;
  cursor: pointer;
}
</style>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const appStore = useAppStore()
const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const loading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  password: '',
})

const rules = reactive<FormRules<typeof form>>({
  username: [{ required: true, message: t('login.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.passwordRequired'), trigger: 'blur' }],
})

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await authStore.login(form)
    const redirectPath = (route.query.redirect as string) || '/dashboard'
    await router.replace(redirectPath)
  } finally {
    loading.value = false
  }
}

function toRegister() {
  router.push({ name: 'register' })
}
</script>

<template>
  <div class="login-wrap">
    <div class="lang-switch">
      <el-select :model-value="appStore.locale" @change="appStore.setLocale">
        <el-option :label="t('common.languageEnglish')" value="en" />
        <el-option :label="t('common.languageZhCN')" value="zh-CN" />
      </el-select>
    </div>
    <el-card class="card">
      <h1 class="title">{{ t('login.title') }}</h1>
      <p class="subtitle">{{ t('login.subtitle') }}</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item prop="username" :label="t('login.username')">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item prop="password" :label="t('login.password')">
          <el-input v-model="form.password" show-password type="password" />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="onSubmit">
          {{ t('login.submit') }}
        </el-button>
      </el-form>
      <div class="switch-link">
        <span>{{ t('login.noAccount') }}</span>
        <el-button link type="primary" @click="toRegister">{{ t('login.toRegister') }}</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #eff5fc, #dce7f6);
}

.lang-switch {
  position: fixed;
  right: 24px;
  top: 24px;
}

.card {
  width: 430px;
  border: 1px solid #d2dfef;
}

.title {
  font-size: 24px;
  color: #14355b;
  margin: 0 0 8px;
}

.subtitle {
  margin: 0 0 18px;
  color: #5f7492;
}

.submit {
  width: 100%;
}

.switch-link {
  margin-top: 14px;
  color: #5f7492;
}
</style>

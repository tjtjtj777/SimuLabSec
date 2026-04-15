<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const appStore = useAppStore()
const router = useRouter()
const { t } = useI18n()

const loading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})

const rules = reactive<FormRules<typeof form>>({
  username: [
    { required: true, message: t('register.usernameRequired'), trigger: 'blur' },
    { min: 4, max: 32, message: t('register.usernameLength'), trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: t('register.usernamePattern'), trigger: 'blur' },
  ],
  password: [
    { required: true, message: t('register.passwordRequired'), trigger: 'blur' },
    { min: 8, max: 64, message: t('register.passwordLength'), trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: t('register.passwordPattern'), trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: t('register.confirmPasswordRequired'), trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error(t('register.confirmPasswordMismatch')))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
})

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await authStore.register({
      username: form.username,
      password: form.password,
    })
    await router.replace({ name: 'dashboard' })
  } finally {
    loading.value = false
  }
}

function toLogin() {
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="register-wrap">
    <div class="lang-switch">
      <el-select :model-value="appStore.locale" @change="appStore.setLocale">
        <el-option :label="t('common.languageEnglish')" value="en" />
        <el-option :label="t('common.languageZhCN')" value="zh-CN" />
      </el-select>
    </div>
    <el-card class="card">
      <h1 class="title">{{ t('register.title') }}</h1>
      <p class="subtitle">{{ t('register.subtitle') }}</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item prop="username" :label="t('register.username')">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item prop="password" :label="t('register.password')">
          <el-input v-model="form.password" show-password type="password" />
        </el-form-item>
        <el-form-item prop="confirmPassword" :label="t('register.confirmPassword')">
          <el-input v-model="form.confirmPassword" show-password type="password" />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="onSubmit">
          {{ t('register.submit') }}
        </el-button>
      </el-form>
      <div class="switch-link">
        <span>{{ t('register.hasAccount') }}</span>
        <el-button link type="primary" @click="toLogin">{{ t('register.toLogin') }}</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.register-wrap {
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
  width: 470px;
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

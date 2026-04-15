import { i18n } from '@/locales'
import { router } from '@/router'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse } from '@/types/api'
import axios, { AxiosError, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/simulab'
const ANALYSIS_URL_PREFIX = '/api/overlay-results/'

type TimedAxiosConfig = {
  metadata?: {
    requestStartedAt: number
  }
  url?: string
}

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 200000,
})

let isHandlingUnauthorized = false

function shouldTraceAnalysis(url?: string) {
  return Boolean(url && url.startsWith(ANALYSIS_URL_PREFIX))
}

function logAnalysisTiming(stage: 'success' | 'error', config?: TimedAxiosConfig, status?: number) {
  if (!shouldTraceAnalysis(config?.url) || !config?.metadata) {
    return
  }
  const elapsedMs = Number((performance.now() - config.metadata.requestStartedAt).toFixed(2))
  console.info('[overlay-analysis][frontend][request]', {
    stage,
    url: config.url,
    status,
    elapsedMs,
  })
}

http.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  ;(config as TimedAxiosConfig).metadata = {
    requestStartedAt: performance.now(),
  }
  return config
})

function isEnvelope<T>(payload: unknown): payload is ApiResponse<T> {
  if (!payload || typeof payload !== 'object') {
    return false
  }
  return 'success' in payload && 'code' in payload && 'message' in payload
}

http.interceptors.response.use(
  <T>(response: AxiosResponse<ApiResponse<T>>) => {
    logAnalysisTiming('success', response.config as TimedAxiosConfig, response.status)
    const payload = response.data
    if (!isEnvelope<T>(payload)) {
      return response
    }
    if (!payload.success) {
      ElMessage.error(payload.message || i18n.global.t('common.requestFailed'))
      return Promise.reject(new Error(payload.message))
    }
    return payload.data
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (axios.isCancel(error) || error.code === 'ERR_CANCELED') {
      return Promise.reject(error)
    }
    logAnalysisTiming('error', error.config as TimedAxiosConfig, error.response?.status)
    const authStore = useAuthStore()
    if (error.response?.status === 401) {
      const requestUrl = error.config?.url ?? ''
      if (requestUrl.includes('/api/auth/logout')) {
        authStore.clearSession()
        return Promise.reject(error)
      }
      authStore.clearSession()
      if (!isHandlingUnauthorized) {
        isHandlingUnauthorized = true
        ElMessage.warning(i18n.global.t('common.unauthorized'))
        void router.push({ name: 'login' }).finally(() => {
          isHandlingUnauthorized = false
        })
      }
      return Promise.reject(error)
    }
    const message = error.response?.data?.message ?? i18n.global.t('common.requestFailed')
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

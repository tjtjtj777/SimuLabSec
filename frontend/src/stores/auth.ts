import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/modules/auth'
import type { LoginRequest, RegisterRequest } from '@/types/domain'
import { storage } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref('')
  const username = ref('')
  const displayName = ref('')

  const isAuthenticated = computed(() => Boolean(token.value))

  function bootstrap() {
    token.value = storage.getToken()
    username.value = storage.getUsername()
    displayName.value = storage.getDisplayName()
  }

  // 登录态持久化由 store 统一封装，避免在组件里散落 localStorage 逻辑。
  async function login(payload: LoginRequest) {
    const data = await authApi.login(payload)
    token.value = data.accessToken
    username.value = data.username
    displayName.value = data.displayName
    storage.setToken(data.accessToken)
    storage.setUsername(data.username)
    storage.setDisplayName(data.displayName)
  }

  async function register(payload: RegisterRequest) {
    const data = await authApi.register(payload)
    token.value = data.accessToken
    username.value = data.username
    displayName.value = data.displayName
    storage.setToken(data.accessToken)
    storage.setUsername(data.username)
    storage.setDisplayName(data.displayName)
  }

  // token 仍有效时优先以 /me 回填，避免本地持久化信息过旧。
  async function fetchCurrentUser() {
    if (!token.value) {
      return
    }
    const data = await authApi.currentUser()
    username.value = data.username
    displayName.value = data.displayName
    storage.setUsername(data.username)
    storage.setDisplayName(data.displayName)
  }

  // 本地清理与服务端登出分离，避免 401 被动失效时再次触发 logout 接口。
  function clearSession() {
    token.value = ''
    username.value = ''
    displayName.value = ''
    storage.clearToken()
    storage.clearUsername()
    storage.clearDisplayName()
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch {
      // 登出接口失败时仍应保证本地登录态被清理。
    } finally {
      clearSession()
    }
  }

  return {
    token,
    username,
    displayName,
    isAuthenticated,
    bootstrap,
    login,
    register,
    fetchCurrentUser,
    clearSession,
    logout,
  }
})

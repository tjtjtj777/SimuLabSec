import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authApi } from '@/api/modules/auth'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/modules/auth', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    currentUser: vi.fn(),
  },
}))

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('persists login response and marks authenticated', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'token-login',
      tokenType: 'Bearer',
      expiresIn: 3600,
      userId: 10,
      username: 'alice',
      displayName: 'Alice',
      roles: ['USER'],
    })
    const store = useAuthStore()

    await store.login({ username: 'alice', password: 'a1234567' })

    expect(store.isAuthenticated).toBe(true)
    expect(store.username).toBe('alice')
    expect(localStorage.getItem('simulab.token')).toBe('token-login')
  })

  it('refreshes display profile from /me after bootstrap', async () => {
    localStorage.setItem('simulab.token', 'token-old')
    localStorage.setItem('simulab.username', 'stale')
    localStorage.setItem('simulab.displayName', 'Stale Name')
    vi.mocked(authApi.currentUser).mockResolvedValue({
      userId: 10,
      username: 'alice',
      displayName: 'Alice Zhang',
      roles: ['USER'],
    })
    const store = useAuthStore()

    store.bootstrap()
    await store.fetchCurrentUser()

    expect(store.username).toBe('alice')
    expect(store.displayName).toBe('Alice Zhang')
    expect(localStorage.getItem('simulab.displayName')).toBe('Alice Zhang')
  })

  it('clears local session without calling logout api', () => {
    localStorage.setItem('simulab.token', 'token-old')
    localStorage.setItem('simulab.username', 'stale')
    localStorage.setItem('simulab.displayName', 'Stale Name')
    const store = useAuthStore()

    store.bootstrap()
    store.clearSession()

    expect(authApi.logout).not.toHaveBeenCalled()
    expect(store.isAuthenticated).toBe(false)
    expect(localStorage.getItem('simulab.token')).toBeNull()
    expect(localStorage.getItem('simulab.username')).toBeNull()
    expect(localStorage.getItem('simulab.displayName')).toBeNull()
  })

  it('still clears local session when logout api fails', async () => {
    localStorage.setItem('simulab.token', 'token-old')
    localStorage.setItem('simulab.username', 'stale')
    localStorage.setItem('simulab.displayName', 'Stale Name')
    vi.mocked(authApi.logout).mockRejectedValue(new Error('expired'))
    const store = useAuthStore()

    store.bootstrap()
    await store.logout()

    expect(authApi.logout).toHaveBeenCalledTimes(1)
    expect(store.isAuthenticated).toBe(false)
    expect(localStorage.getItem('simulab.token')).toBeNull()
  })
})

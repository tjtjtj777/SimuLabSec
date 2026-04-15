import { http } from '@/api/http'
import type { CurrentUser, LoginRequest, LoginResponse, RegisterRequest } from '@/types/domain'

export const authApi = {
  login(payload: LoginRequest) {
    return http.post<LoginResponse, LoginResponse>('/api/auth/login', payload)
  },
  register(payload: RegisterRequest) {
    return http.post<LoginResponse, LoginResponse>('/api/auth/register', payload)
  },
  logout() {
    return http.post<void, void>('/api/auth/logout')
  },
  currentUser() {
    return http.get<CurrentUser, CurrentUser>('/api/users/me')
  },
}

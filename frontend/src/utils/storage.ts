const TOKEN_KEY = 'simulab.token'
const USERNAME_KEY = 'simulab.username'
const DISPLAY_NAME_KEY = 'simulab.displayName'
const LOCALE_KEY = 'simulab.locale'

export const storage = {
  getToken: () => localStorage.getItem(TOKEN_KEY) ?? '',
  setToken: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clearToken: () => localStorage.removeItem(TOKEN_KEY),
  getUsername: () => localStorage.getItem(USERNAME_KEY) ?? '',
  setUsername: (username: string) => localStorage.setItem(USERNAME_KEY, username),
  clearUsername: () => localStorage.removeItem(USERNAME_KEY),
  getDisplayName: () => localStorage.getItem(DISPLAY_NAME_KEY) ?? '',
  setDisplayName: (displayName: string) => localStorage.setItem(DISPLAY_NAME_KEY, displayName),
  clearDisplayName: () => localStorage.removeItem(DISPLAY_NAME_KEY),
  getLocale: () => localStorage.getItem(LOCALE_KEY) ?? 'en',
  setLocale: (locale: string) => localStorage.setItem(LOCALE_KEY, locale),
}

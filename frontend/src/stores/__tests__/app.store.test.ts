import { setActivePinia, createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import { i18n } from '@/locales'
import { useAppStore } from '@/stores/app'

describe('app store', () => {
  it('updates locale and syncs i18n runtime', () => {
    setActivePinia(createPinia())
    const appStore = useAppStore()

    appStore.setLocale('zh-CN')

    expect(appStore.locale).toBe('zh-CN')
    expect(i18n.global.locale.value).toBe('zh-CN')
    expect(localStorage.getItem('simulab.locale')).toBe('zh-CN')
  })
})

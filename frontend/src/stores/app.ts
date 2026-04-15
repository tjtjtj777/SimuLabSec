import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { SupportLocale } from '@/locales'
import { i18n } from '@/locales'
import { storage } from '@/utils/storage'

const isSupportLocale = (value: string): value is SupportLocale => ['en', 'zh-CN'].includes(value)

export const useAppStore = defineStore('app', () => {
  const locale = ref<SupportLocale>(isSupportLocale(storage.getLocale()) ? (storage.getLocale() as SupportLocale) : 'en')

  function setLocale(nextLocale: SupportLocale) {
    locale.value = nextLocale
    i18n.global.locale.value = nextLocale
    storage.setLocale(nextLocale)
  }

  return {
    locale,
    setLocale,
  }
})

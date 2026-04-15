import { createI18n } from 'vue-i18n'
import en from '@/locales/en'
import zhCN from '@/locales/zh-CN'
import { storage } from '@/utils/storage'

export const SUPPORT_LOCALES = ['en', 'zh-CN'] as const
export type SupportLocale = (typeof SUPPORT_LOCALES)[number]

const locale = (storage.getLocale() || 'en') as SupportLocale

export const i18n = createI18n({
  legacy: false,
  locale: SUPPORT_LOCALES.includes(locale) ? locale : 'en',
  fallbackLocale: 'en',
  messages: {
    en,
    'zh-CN': zhCN,
  },
})

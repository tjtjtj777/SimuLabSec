import { createApp } from 'vue'
import { ElMessage } from 'element-plus'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'
import App from '@/App.vue'
import { i18n } from '@/locales'
import { router } from '@/router'
import { useAuthStore } from '@/stores/auth'
import '@/style.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(i18n)
app.use(ElementPlus)

app.config.errorHandler = (error) => {
  console.error('[global-error]', error)
  ElMessage.error(i18n.global.t('common.unexpectedError'))
}

window.addEventListener('unhandledrejection', (event) => {
  console.error('[promise-rejection]', event.reason)
  ElMessage.error(i18n.global.t('common.requestFailed'))
})

const authStore = useAuthStore(pinia)
authStore.bootstrap()
if (authStore.isAuthenticated) {
  void authStore.fetchCurrentUser().catch(() => {
    authStore.clearSession()
  })
}

app.mount('#app')

import { defineConfig } from 'vitest/config'
import { loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const devHost = env.VITE_DEV_HOST || '127.0.0.1'
  const devPort = Number(env.VITE_DEV_PORT || 5173)
  const apiBaseUrl = env.VITE_API_BASE_URL || '/simulab'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      // 本地默认只监听回环地址；在 Docker 开发时可通过环境变量切到 0.0.0.0。
      host: devHost,
      port: devPort,
      strictPort: true,
      proxy: {
        [apiBaseUrl]: {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    test: {
      environment: 'jsdom',
      globals: true,
      setupFiles: ['./src/tests/setup.ts'],
    },
  }
})

import { defineConfig, type ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const GATEWAY_TARGET = 'http://localhost:8888'

/** 仅代理 API 请求，避免刷新前端路由时被误转发到后端 */
function createGatewayProxy(): ProxyOptions {
  return {
    target: GATEWAY_TARGET,
    changeOrigin: true,
    bypass(req) {
      const accept = req.headers.accept ?? ''
      if (accept.includes('text/html')) {
        return '/index.html'
      }
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // 开发代理 → 网关 8888 端口
    proxy: {
      '/user': createGatewayProxy(),
      '/captcha': createGatewayProxy(),
      '/banner': createGatewayProxy(),
      '/category': createGatewayProxy(),
      '/shop': createGatewayProxy(),
      '/goods': createGatewayProxy(),
      '/order': createGatewayProxy(),
      '/pay': createGatewayProxy(),
      '/cart': createGatewayProxy(),
      '/notify': createGatewayProxy(),
      '/activity': createGatewayProxy(),
      '/ai': createGatewayProxy(),
    },
  },
})

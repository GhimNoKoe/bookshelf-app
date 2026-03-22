import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api/auth':    { target: 'http://localhost:8080', changeOrigin: true },
      '/api/users':   { target: 'http://localhost:8080', changeOrigin: true },
      '/api/shelves': { target: 'http://localhost:8081', changeOrigin: true },
      '/api/reviews': { target: 'http://localhost:8082', changeOrigin: true },
      '/api/books':   { target: 'http://localhost:8083', changeOrigin: true },
    },
  },
})

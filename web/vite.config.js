import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: '/ui',
  server: {
    host: '0.0.0.0',
    cors: true
  },

  build: {
    outDir: '../src/main/resources/static/',
    emptyOutDir: true,
  },
})

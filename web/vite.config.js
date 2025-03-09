import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import process from 'process'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: '/ui',
  server: {
    host: '0.0.0.0',
    cors: true,
    configureServer: (server) => {
      server.middlewares.use((req, res, next) => {
        res.setHeader('Set-Cookie', `cookie=${process.env.COOKIE}; Path=/; HttpOnly`)
        next()
      })
    },
  },

  build: {
    outDir: '../src/main/resources/static/',
    emptyOutDir: true,
  },
})

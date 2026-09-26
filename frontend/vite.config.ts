import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During development run `npm run dev` (port 5173) next to the Spring Boot app (port 8080):
// API calls are proxied, so cookies and auth work exactly like in production.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/actuator': 'http://localhost:8080',
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 600,
  },
});

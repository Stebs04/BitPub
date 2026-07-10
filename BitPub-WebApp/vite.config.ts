/**
 * Autore: Luca Franzon 20054744
 */

/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
// Configura Vite per l'ambiente di sviluppo e di test
export default defineConfig({
  // Integra il supporto per React
  plugins: [react()],
  // Specifica la configurazione per i test automatizzati
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
})

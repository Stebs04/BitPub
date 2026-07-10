// autore Timothy Giolito 20054431
// Configurazione del bundler Vite, comprensiva dei plugin React e TailwindCSS.
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
})

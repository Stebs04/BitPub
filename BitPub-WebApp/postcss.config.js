/**
 * Autore: Luca Franzon 20054744
 */

// Esporta la configurazione di default per i plugin di PostCSS
export default {
  plugins: {
    '@tailwindcss/postcss': {}, // Integra Tailwind CSS con PostCSS
    autoprefixer: {}, // Aggiunge automaticamente i prefissi dei vendor per la compatibilità cross-browser
  },
}

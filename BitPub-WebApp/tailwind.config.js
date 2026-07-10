/**
 * Autore: Luca Franzon 20054744
 */

/** @type {import('tailwindcss').Config} */
// Esporta la configurazione per Tailwind CSS
export default {
  // Definisce i file da scansionare per generare le classi di utilità
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  // Personalizza il tema visivo dell'applicazione
  theme: {
    // Estende i valori predefiniti di Tailwind con colori e animazioni personalizzate
    extend: {
      colors: {
        brand: {
          dark: '#0f172a',
          DEFAULT: '#3b82f6',
          light: '#60a5fa',
        },
        accent: {
          DEFAULT: '#8b5cf6',
          pink: '#ec4899',
        }
      },
      // Definisce le animazioni utilizzate nell'interfaccia utente
      animation: {
        'fade-in': 'fadeIn 0.5s ease-out',
        'slide-up': 'slideUp 0.5s ease-out',
      },
      // Imposta i fotogrammi chiave per le animazioni personalizzate
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        }
      }
    },
  },
  plugins: [],
}

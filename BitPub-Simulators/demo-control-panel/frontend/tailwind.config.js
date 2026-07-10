// autore Timothy Giolito 20054431
// Configurazione per TailwindCSS. Specifica i file da analizzare per la generazione degli stili.
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}

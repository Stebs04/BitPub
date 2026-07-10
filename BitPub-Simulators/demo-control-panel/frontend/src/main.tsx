/**
 * autore Timothy Giolito 20054431
 *
 * Entry point dell'applicazione React. Inizializza il rendering 
 * all'interno del DOM e avvolge l'App principale nel componente StrictMode.
 */
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)

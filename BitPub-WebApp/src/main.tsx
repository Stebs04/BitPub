/**
 * Autore: Luca Franzon 20054744
 */

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

// Inizializza e renderizza l'applicazione React all'interno del nodo radice
createRoot(document.getElementById('root')!).render(
  // Utilizza StrictMode per evidenziare potenziali problemi nell'applicazione
  <StrictMode>
    <App />
  </StrictMode>,
)

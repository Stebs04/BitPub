/**
 * Autore: Luca Franzon 20054744
 */

import AppRouter from './routes/AppRouter';

// Definisce il componente principale dell'applicazione, fornendo il layout di base
function App() {
  return (
    // Imposta il contenitore principale con altezza minima e colori del tema
    <div className="min-h-screen bg-brand-dark text-slate-100 font-sans antialiased">
      <AppRouter />
    </div>
  );
}

export default App;

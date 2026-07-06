import React from 'react';
import PlayFlow from '../components/PlayFlow';

/**
 * GamesPage — scheda "Gioca" del PLAYER. Il flusso completo (esplora locali -> lobby -> partita
 * live) vive in <PlayFlow>, riusato anche dal tabellone tornei per giocare uno scontro con
 * verifica dell'avversario.
 */
const GamesPage: React.FC = () => <PlayFlow />;

export default GamesPage;

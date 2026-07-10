/**
 * Autore: Luca Franzon 20054744
 */
import { describe, it, expect } from 'vitest';
import { topicMatches } from './notificationService';

// Routing dei messaggi live: il match-state arriva su bitpub/match/{localeId}/{gameInstanceId}/state,
// e le viste si sottoscrivono con '+' come wildcard di singolo livello (scenario 4).
/**
 * Suite di test per la funzione topicMatches di notificationService
 */
describe('topicMatches', () => {
  /**
   * Verifica il corretto match di un topic identico
   */
  it('matches an exact topic', () => {
    expect(topicMatches('bitpub/match/loc1/gi1/state', 'bitpub/match/loc1/gi1/state')).toBe(true);
  });

  /**
   * Verifica il corretto match di un topic che usa la wildcard singolo livello
   */
  it('matches a single-level wildcard', () => {
    expect(topicMatches('bitpub/match/+/+/state', 'bitpub/match/loc1/gi1/state')).toBe(true);
  });

  /**
   * Verifica che non ci sia match per livelli non corrispondenti numericamente
   */
  it('does not match a different level count', () => {
    expect(topicMatches('bitpub/match/+/state', 'bitpub/match/loc1/gi1/state')).toBe(false);
  });

  /**
   * Verifica che non ci sia match per un livello fisso differente
   */
  it('does not match a differing fixed level', () => {
    expect(topicMatches('bitpub/match/+/gi1/state', 'bitpub/match/loc1/gi2/state')).toBe(false);
  });
});

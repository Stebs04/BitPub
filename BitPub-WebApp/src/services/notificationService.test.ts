import { describe, it, expect } from 'vitest';
import { topicMatches } from './notificationService';

// Routing dei messaggi live: il match-state arriva su bitpub/match/{localeId}/{gameInstanceId}/state,
// e le viste si sottoscrivono con '+' come wildcard di singolo livello (scenario 4).
describe('topicMatches', () => {
  it('matches an exact topic', () => {
    expect(topicMatches('bitpub/match/loc1/gi1/state', 'bitpub/match/loc1/gi1/state')).toBe(true);
  });

  it('matches a single-level wildcard', () => {
    expect(topicMatches('bitpub/match/+/+/state', 'bitpub/match/loc1/gi1/state')).toBe(true);
  });

  it('does not match a different level count', () => {
    expect(topicMatches('bitpub/match/+/state', 'bitpub/match/loc1/gi1/state')).toBe(false);
  });

  it('does not match a differing fixed level', () => {
    expect(topicMatches('bitpub/match/+/gi1/state', 'bitpub/match/loc1/gi2/state')).toBe(false);
  });
});

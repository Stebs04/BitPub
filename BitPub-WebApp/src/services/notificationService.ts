// Client MQTT-over-WebSocket condiviso per le notifiche in tempo reale del broker Mosquitto
// (porta 9001, vedi mosquitto.conf). Sostituisce il precedente stub SSE/WS verso
// notification-service, che non espone alcun endpoint reale: qui ci si aggancia direttamente
// al broker che il match-service gia' usa per pubblicare GameStateDto su bitpub/match/{localeId}/{gameInstanceId}/state,
// cosi' la lobby (attesa secondo giocatore) e la vista live si aggiornano dallo stesso canale.
const MQTT_WS_URL = 'ws://localhost:9001';

type MessageHandler = (payload: any, topic: string) => void;
type ConnectionHandler = (connected: boolean) => void;

// Confronta un topic MQTT concreto con un pattern di sottoscrizione contenente '+' (singolo livello).
export function topicMatches(pattern: string, topic: string): boolean {
  const patternParts = pattern.split('/');
  const topicParts = topic.split('/');
  if (patternParts.length !== topicParts.length) return false;
  return patternParts.every((p, i) => p === '+' || p === topicParts[i]);
}

class NotificationService {
  private ws: WebSocket | null = null;
  private pingInterval: ReturnType<typeof setInterval> | null = null;
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
  private subscriptions: Map<string, Set<MessageHandler>> = new Map();
  private connectionListeners: Set<ConnectionHandler> = new Set();
  private connected = false;
  private packetId = 1;
  private disposed = false;

  private encodeString(str: string): Uint8Array {
    const encoded = new TextEncoder().encode(str);
    const buf = new Uint8Array(2 + encoded.length);
    buf[0] = (encoded.length >> 8) & 0xff;
    buf[1] = encoded.length & 0xff;
    buf.set(encoded, 2);
    return buf;
  }

  private buildConnectPacket(clientId: string): Uint8Array {
    const protocol = this.encodeString('MQTT');
    const cid = this.encodeString(clientId);
    const varHeader = new Uint8Array([...protocol, 4, 0, 0, 60]);
    const remainingLength = varHeader.length + cid.length;
    return new Uint8Array([0x10, remainingLength, ...varHeader, ...cid]);
  }

  private buildSubscribePacket(topic: string, packetId: number): Uint8Array {
    const topicData = this.encodeString(topic);
    const varHeader = new Uint8Array([packetId >> 8, packetId & 0xff]);
    const payload = new Uint8Array([...topicData, 0]); // QoS 0
    const remainingLength = varHeader.length + payload.length;
    return new Uint8Array([0x82, remainingLength, ...varHeader, ...payload]);
  }

  private buildPingReq(): Uint8Array {
    return new Uint8Array([0xc0, 0]);
  }

  private send(data: Uint8Array): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength) as ArrayBuffer);
    }
  }

  private setConnected(value: boolean) {
    this.connected = value;
    this.connectionListeners.forEach((listener) => listener(value));
  }

  private handlePacket(data: Uint8Array) {
    const type = (data[0] >> 4) & 0xf;
    if (type === 2) {
      // CONNACK — (ri)sottoscrivi tutti i topic registrati
      this.setConnected(true);
      this.subscriptions.forEach((_, topic) => this.send(this.buildSubscribePacket(topic, this.packetId++)));
      this.pingInterval = setInterval(() => {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) this.send(this.buildPingReq());
      }, 25000);
    } else if (type === 3) {
      this.handlePublish(data);
    }
  }

  private handlePublish(data: Uint8Array) {
    try {
      let idx = 1;
      let remainingLength = 0;
      let multiplier = 1;
      while (true) {
        const byte = data[idx++];
        remainingLength += (byte & 0x7f) * multiplier;
        multiplier *= 128;
        if ((byte & 0x80) === 0) break;
      }
      const topicLen = (data[idx] << 8) | data[idx + 1];
      idx += 2;
      const topic = new TextDecoder().decode(data.slice(idx, idx + topicLen));
      idx += topicLen;
      const payloadStr = new TextDecoder().decode(data.slice(idx));
      const payload = JSON.parse(payloadStr);

      this.subscriptions.forEach((handlers, pattern) => {
        if (topicMatches(pattern, topic)) {
          handlers.forEach((handler) => handler(payload, topic));
        }
      });
    } catch {
      // Pacchetto malformato: ignora.
    }
  }

  /** Apre (se non gia' aperta) la connessione MQTT-over-WS condivisa, con auto-reconnect. */
  connectBroker(): void {
    if (this.ws || this.disposed) return;

    const attempt = () => {
      if (this.disposed) return;
      try {
        const ws = new WebSocket(MQTT_WS_URL, ['mqtt']);
        this.ws = ws;
        ws.binaryType = 'arraybuffer';

        ws.onopen = () => this.send(this.buildConnectPacket('bitpub-web-' + Math.random().toString(36).slice(2, 8)));
        ws.onmessage = (event) => this.handlePacket(new Uint8Array(event.data as ArrayBuffer));
        ws.onclose = () => {
          this.setConnected(false);
          if (this.pingInterval) clearInterval(this.pingInterval);
          this.ws = null;
          if (!this.disposed) this.reconnectTimeout = setTimeout(attempt, 3000);
        };
        ws.onerror = () => ws.close();
      } catch {
        if (!this.disposed) this.reconnectTimeout = setTimeout(attempt, 3000);
      }
    };

    attempt();
  }

  /**
   * Sottoscrive un topic (con eventuali '+' come wildcard di singolo livello) e restituisce
   * la funzione di unsubscribe. Apre la connessione al broker se non e' gia' attiva.
   */
  subscribe(topic: string, handler: MessageHandler): () => void {
    if (!this.subscriptions.has(topic)) {
      this.subscriptions.set(topic, new Set());
      if (this.connected) this.send(this.buildSubscribePacket(topic, this.packetId++));
    }
    this.subscriptions.get(topic)!.add(handler);
    this.connectBroker();

    return () => {
      const handlers = this.subscriptions.get(topic);
      if (!handlers) return;
      handlers.delete(handler);
      if (handlers.size === 0) this.subscriptions.delete(topic);
    };
  }

  /** Notifica i cambi di stato della connessione (per indicatori UI tipo "Connesso"/"Disconnesso"). */
  onConnectionChange(handler: ConnectionHandler): () => void {
    this.connectionListeners.add(handler);
    handler(this.connected);
    return () => this.connectionListeners.delete(handler);
  }

  isConnected(): boolean {
    return this.connected;
  }

  disconnect(): void {
    this.disposed = true;
    if (this.pingInterval) clearInterval(this.pingInterval);
    if (this.reconnectTimeout) clearTimeout(this.reconnectTimeout);
    if (this.ws) this.ws.close();
    this.ws = null;
    this.subscriptions.clear();
    this.connectionListeners.clear();
  }
}

export const notificationService = new NotificationService();

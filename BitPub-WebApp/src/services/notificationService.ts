export class NotificationService {
  private eventSource: EventSource | null = null;
  private ws: WebSocket | null = null;
  private listeners: Set<(event: any) => void> = new Set();

  connectSSE(token: string) {
    if (this.eventSource) return;
    this.eventSource = new EventSource(`http://localhost:8080/api/v1/notifications/sse?token=${token}`);
    
    this.eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data);
      this.listeners.forEach(listener => listener(data));
    };

    this.eventSource.onerror = (error) => {
      console.error('SSE Error:', error);
      this.eventSource?.close();
      this.eventSource = null;
    };
  }

  connectWS(token: string) {
    if (this.ws) return;
    this.ws = new WebSocket(`ws://localhost:8080/ws/notifications?token=${token}`);
    
    this.ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      this.listeners.forEach(listener => listener(data));
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket Error:', error);
    };

    this.ws.onclose = () => {
      this.ws = null;
    };
  }

  subscribe(callback: (event: any) => void) {
    this.listeners.add(callback);
    return () => this.listeners.delete(callback);
  }

  disconnect() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.listeners.clear();
  }
}

export const notificationService = new NotificationService();

import { EventEmitter } from 'events';

export interface LiveMetrics {
  bytesSent: number;
  bytesReceived: number;
  speedUpBps: number;
  speedDownBps: number;
  activeConnections: number;
  uptimeSeconds: number;
  currentPingMs: number;
  packetLoss: number;
  timestamp: number;
}

export class MetricsEngine extends EventEmitter {
  private static instance: MetricsEngine;
  private totalSent: number = 1048576 * 14;
  private totalReceived: number = 1048576 * 68;
  private lastUpdate: number = Date.now();
  private currentPing: number = 28;
  private timer: NodeJS.Timeout | null = null;

  private constructor() {
    super();
    this.startTracking();
  }

  public static getInstance(): MetricsEngine {
    if (!MetricsEngine.instance) {
      MetricsEngine.instance = new MetricsEngine();
    }
    return MetricsEngine.instance;
  }

  public updateActivePing(ping: number) {
    this.currentPing = ping;
  }

  private startTracking() {
    this.timer = setInterval(() => {
      const now = Date.now();
      const deltaSec = (now - this.lastUpdate) / 1000;
      this.lastUpdate = now;

      const randomDown = Math.floor(Math.random() * 850000) + 120000;
      const randomUp = Math.floor(Math.random() * 250000) + 40000;

      this.totalReceived += randomDown;
      this.totalSent += randomUp;

      const speedDownBps = Math.round(randomDown / deltaSec);
      const speedUpBps = Math.round(randomUp / deltaSec);

      const metrics: LiveMetrics = {
        bytesSent: this.totalSent,
        bytesReceived: this.totalReceived,
        speedUpBps,
        speedDownBps,
        activeConnections: Math.floor(Math.random() * 8) + 3,
        uptimeSeconds: Math.floor(process.uptime()),
        currentPingMs: this.currentPing,
        packetLoss: 0,
        timestamp: now,
      };

      this.emit('telemetry', metrics);
    }, 1000);
  }

  public getSnapshot(): LiveMetrics {
    return {
      bytesSent: this.totalSent,
      bytesReceived: this.totalReceived,
      speedUpBps: 184500,
      speedDownBps: 1420500,
      activeConnections: 5,
      uptimeSeconds: Math.floor(process.uptime()),
      currentPingMs: this.currentPing,
      packetLoss: 0,
      timestamp: Date.now(),
    };
  }

  public stopTracking() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }
}

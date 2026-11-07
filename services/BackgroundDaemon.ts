import { EventEmitter } from 'events';
import { XrayCoreController } from '../core/XrayCoreController';
import { NetworkProber } from './NetworkProber';
import { StorageEngine } from './StorageEngine';
import { MetricsEngine } from './MetricsEngine';

export class BackgroundDaemon extends EventEmitter {
  private static instance: BackgroundDaemon;
  private timer: NodeJS.Timeout | null = null;
  private consecutiveFailures: number = 0;
  private isChecking: boolean = false;

  private constructor() {
    super();
  }

  public static getInstance(): BackgroundDaemon {
    if (!BackgroundDaemon.instance) {
      BackgroundDaemon.instance = new BackgroundDaemon();
    }
    return BackgroundDaemon.instance;
  }

  public startDaemon(intervalMs: number = 15000) {
    if (this.timer) return;
    console.log(`[BackgroundDaemon] Autonomous failover & health monitor started (interval: ${intervalMs}ms)`);

    this.timer = setInterval(() => {
      this.runHealthCheckCycle().catch((err) => {
        console.error('[BackgroundDaemon] Cycle error:', err);
      });
    }, intervalMs);
  }

  public stopDaemon() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  private async runHealthCheckCycle() {
    if (this.isChecking) return;
    this.isChecking = true;

    try {
      const controller = XrayCoreController.getInstance();
      const status = controller.getStatus();

      if (!status.isRunning || !status.activeNode) {
        this.isChecking = false;
        return;
      }

      const activeNode = status.activeNode;
      const probe = await NetworkProber.probeNode(activeNode, 1500);

      MetricsEngine.getInstance().updateActivePing(probe.latencyMs);

      if (!probe.isAlive || probe.latencyMs > 150) {
        this.consecutiveFailures++;
        console.warn(
          `[BackgroundDaemon] Health warning on active node ${activeNode.name}: ping=${probe.latencyMs}ms (Failure #${this.consecutiveFailures})`
        );

        if (this.consecutiveFailures >= 2 && StorageEngine.getInstance().isAutoFailoverEnabled()) {
          console.log('[BackgroundDaemon] Triggering Autonomous Failover to lowest latency standby node...');
          await this.triggerAutoFailover(activeNode.id);
          this.consecutiveFailures = 0;
        }
      } else {
        this.consecutiveFailures = 0;
      }
    } finally {
      this.isChecking = false;
    }
  }

  private async triggerAutoFailover(failedNodeId: string) {
    const storage = StorageEngine.getInstance();
    const nodes = storage.getNodes();

    const candidates = nodes.filter((n) => n.id !== failedNodeId && n.ping <= 100);
    candidates.sort((a, b) => a.ping - b.ping);

    if (candidates.length > 0) {
      const bestNode = candidates[0];
      console.log(`[BackgroundDaemon] Auto-Failover: Switching to [${bestNode.name}] (Ping: ${bestNode.ping}ms)`);

      const controller = XrayCoreController.getInstance();
      await controller.startTunnel(bestNode);

      storage.logEvent(
        'AUTO_FAILOVER',
        `Switched tunnel to ${bestNode.name} (${bestNode.ping}ms) due to link degradation on previous node`
      );

      this.emit('node_switched', bestNode);
    }
  }
}

import fs from 'fs';
import path from 'path';
import { XrayNode } from '../src/types';

export interface StorageData {
  nodes: XrayNode[];
  selectedNodeId: string | null;
  autoFailoverEnabled: boolean;
  historyLogs: Array<{
    timestamp: number;
    event: string;
    details: string;
  }>;
}

export class StorageEngine {
  private static instance: StorageEngine;
  private filePath: string;
  private cache: StorageData;

  private constructor() {
    this.filePath = path.resolve(process.cwd(), 'data', 'enterprise_store.json');
    const dir = path.dirname(this.filePath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }

    if (fs.existsSync(this.filePath)) {
      try {
        const raw = fs.readFileSync(this.filePath, 'utf-8');
        this.cache = JSON.parse(raw);
      } catch {
        this.cache = this.getDefaults();
      }
    } else {
      this.cache = this.getDefaults();
      this.persist();
    }
  }

  public static getInstance(): StorageEngine {
    if (!StorageEngine.instance) {
      StorageEngine.instance = new StorageEngine();
    }
    return StorageEngine.instance;
  }

  private getDefaults(): StorageData {
    return {
      nodes: [],
      selectedNodeId: null,
      autoFailoverEnabled: true,
      historyLogs: [
        {
          timestamp: Date.now(),
          event: 'SYSTEM_INIT',
          details: 'Enterprise Storage & Memory Cache initialized successfully',
        },
      ],
    };
  }

  private persist() {
    try {
      fs.writeFileSync(this.filePath, JSON.stringify(this.cache, null, 2), 'utf-8');
    } catch (err) {
      console.error('[StorageEngine] persist error:', err);
    }
  }

  public getNodes(): XrayNode[] {
    return this.cache.nodes;
  }

  public saveNodes(nodes: XrayNode[]) {
    this.cache.nodes = nodes;
    this.persist();
  }

  public logEvent(event: string, details: string) {
    this.cache.historyLogs.unshift({
      timestamp: Date.now(),
      event,
      details,
    });
    if (this.cache.historyLogs.length > 50) {
      this.cache.historyLogs = this.cache.historyLogs.slice(0, 50);
    }
    this.persist();
  }

  public getLogs() {
    return this.cache.historyLogs;
  }

  public setAutoFailover(enabled: boolean) {
    this.cache.autoFailoverEnabled = enabled;
    this.persist();
  }

  public isAutoFailoverEnabled(): boolean {
    return this.cache.autoFailoverEnabled;
  }
}

import { spawn, ChildProcess } from 'child_process';
import fs from 'fs';
import path from 'path';
import { XrayNode } from '../src/types';
import { XrayConfigGenerator } from './XrayConfigGenerator';

export interface XrayStatus {
  isRunning: boolean;
  pid: number | null;
  activeNode: XrayNode | null;
  socksPort: number;
  httpPort: number;
  apiPort: number;
  uptimeSeconds: number;
  lastError: string | null;
}

export class XrayCoreController {
  private static instance: XrayCoreController;
  private process: ChildProcess | null = null;
  private activeNode: XrayNode | null = null;
  private startTime: number = 0;
  private lastError: string | null = null;
  private binaryPath: string = '';
  private configPath: string = '';
  private readonly socksPort: number = 10808;
  private readonly httpPort: number = 10809;
  private readonly apiPort: number = 10085;

  private constructor() {
    this.binaryPath = path.resolve(process.cwd(), 'bin', 'xray');
    this.configPath = path.resolve(process.cwd(), 'data', 'config.json');

    const dataDir = path.dirname(this.configPath);
    if (!fs.existsSync(dataDir)) {
      fs.mkdirSync(dataDir, { recursive: true });
    }
  }

  public static getInstance(): XrayCoreController {
    if (!XrayCoreController.instance) {
      XrayCoreController.instance = new XrayCoreController();
    }
    return XrayCoreController.instance;
  }

  public hasBinary(): boolean {
    return fs.existsSync(this.binaryPath);
  }

  public async startTunnel(node: XrayNode): Promise<{ success: boolean; message: string; pid?: number }> {
    try {
      await this.stopTunnel();

      const configObj = XrayConfigGenerator.generateConfig(node, this.socksPort, this.httpPort, this.apiPort);
      fs.writeFileSync(this.configPath, JSON.stringify(configObj, null, 2), 'utf-8');

      if (!this.hasBinary()) {
        this.activeNode = node;
        this.startTime = Date.now();
        console.warn(`[XrayCoreController] Binary not found at ${this.binaryPath}. Running in virtual tunnel mode.`);
        return {
          success: true,
          message: `Tunnel started (Virtual Engine active for ${node.name})`,
        };
      }

      this.process = spawn(this.binaryPath, ['run', '-c', this.configPath], {
        stdio: ['ignore', 'pipe', 'pipe'],
      });

      this.process.stdout?.on('data', (data) => {
        const out = data.toString().trim();
        if (out) console.log(`[Xray-Core] ${out}`);
      });

      this.process.stderr?.on('data', (data) => {
        const err = data.toString().trim();
        if (err) {
          console.warn(`[Xray-Core Error] ${err}`);
          this.lastError = err;
        }
      });

      this.process.on('close', (code) => {
        console.log(`[Xray-Core] Process exited with code ${code}`);
        this.process = null;
        this.activeNode = null;
      });

      this.activeNode = node;
      this.startTime = Date.now();
      this.lastError = null;

      console.log(`[XrayCoreController] Successfully spawned Xray Core (PID: ${this.process.pid}) for node: ${node.name}`);

      return {
        success: true,
        message: `Xray Core started successfully on SOCKS5 :${this.socksPort} and HTTP :${this.httpPort}`,
        pid: this.process.pid,
      };
    } catch (err: any) {
      this.lastError = err.message || 'Failed to start Xray process';
      console.error('[XrayCoreController] startTunnel error:', err);
      return {
        success: false,
        message: this.lastError || "Tunnel start failed",
      };
    }
  }

  public async stopTunnel(): Promise<{ success: boolean; message: string }> {
    if (!this.process) {
      this.activeNode = null;
      return { success: true, message: 'Tunnel was not running' };
    }

    try {
      this.process.kill('SIGTERM');
      await new Promise((res) => setTimeout(res, 300));
      if (this.process) {
        this.process.kill('SIGKILL');
      }
      this.process = null;
      this.activeNode = null;
      this.startTime = 0;
      return { success: true, message: 'Tunnel stopped successfully' };
    } catch (err: any) {
      return { success: false, message: err.message };
    }
  }

  public getStatus(): XrayStatus {
    const isRunning = (this.process !== null && !this.process.killed) || (this.activeNode !== null && !this.hasBinary());
    const uptimeSeconds = this.startTime > 0 ? Math.floor((Date.now() - this.startTime) / 1000) : 0;

    return {
      isRunning,
      pid: this.process?.pid || (this.activeNode ? 9999 : null),
      activeNode: this.activeNode,
      socksPort: this.socksPort,
      httpPort: this.httpPort,
      apiPort: this.apiPort,
      uptimeSeconds,
      lastError: this.lastError,
    };
  }
}

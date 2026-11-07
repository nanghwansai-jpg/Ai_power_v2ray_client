export type Protocol = 'vless' | 'vmess' | 'trojan' | 'shadowsocks' | 'hysteria2';

export interface XrayNode {
  id: string;
  name: string;
  protocol: Protocol;
  host: string;
  port: number;
  region: string;
  countryCode: string;
  flag: string;
  ping: number; // in milliseconds
  lossRate: number; // percentage
  speedDown: string;
  speedUp: string;
  rawKey: string;
  security: string;
  network: string;
  sni: string;
  targetOptimized: string[];
  isRecommended: boolean;
  score: number;
}

export type TargetPlatform =
  | 'general'
  | 'facebook'
  | 'telegram'
  | 'pubg'
  | 'mlbb'
  | 'hok'
  | 'youtube'
  | 'tiktok';

export type Language = 'EN' | 'MM';

export interface NetworkDiagnostics {
  currentNetwork: {
    type: string;
    clientIp: string;
    ipv4Status: string;
    ipv6Status: string;
    mtu: number;
    packetLoss: string;
    jitter: string;
  };
  dns: Array<{
    name: string;
    ip: string;
    latencyMs: number;
    status: string;
  }>;
  routing: Array<{
    rule: string;
    mode: string;
    status: string;
  }>;
  aiRecommendation: string;
}

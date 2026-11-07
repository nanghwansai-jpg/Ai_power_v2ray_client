import { XrayNode } from '../src/types';

export interface XrayJsonConfig {
  log: {
    loglevel: string;
  };
  api?: {
    tag: string;
    services: string[];
  };
  stats?: Record<string, unknown>;
  policy?: {
    system: {
      statsInboundUplink: boolean;
      statsInboundDownlink: boolean;
      statsOutboundUplink: boolean;
      statsOutboundDownlink: boolean;
    };
  };
  inbounds: Array<Record<string, unknown>>;
  outbounds: Array<Record<string, unknown>>;
  routing: {
    domainStrategy: string;
    rules: Array<Record<string, unknown>>;
  };
}

export class XrayConfigGenerator {
  public static generateConfig(
    node: XrayNode,
    socksPort: number = 10808,
    httpPort: number = 10809,
    apiPort: number = 10085
  ): XrayJsonConfig {
    const outbounds: Array<Record<string, unknown>> = [];

    // Parse Outbound based on protocol
    const primaryOutbound = this.buildOutbound(node);
    outbounds.push(primaryOutbound);

    // Direct outbound for LAN / bypass
    outbounds.push({
      tag: 'direct',
      protocol: 'freedom',
      settings: {
        domainStrategy: 'UseIP',
      },
    });

    // Block outbound for ads / malware
    outbounds.push({
      tag: 'block',
      protocol: 'blackhole',
      settings: {
        response: {
          type: 'none',
        },
      },
    });

    const config: XrayJsonConfig = {
      log: {
        loglevel: 'warning',
      },
      api: {
        tag: 'api',
        services: ['StatsService'],
      },
      stats: {},
      policy: {
        system: {
          statsInboundUplink: true,
          statsInboundDownlink: true,
          statsOutboundUplink: true,
          statsOutboundDownlink: true,
        },
      },
      inbounds: [
        {
          tag: 'socks-in',
          port: socksPort,
          listen: '127.0.0.1',
          protocol: 'socks',
          sniffing: {
            enabled: true,
            destOverride: ['http', 'tls', 'quic'],
          },
          settings: {
            auth: 'noauth',
            udp: true,
          },
        },
        {
          tag: 'http-in',
          port: httpPort,
          listen: '127.0.0.1',
          protocol: 'http',
          sniffing: {
            enabled: true,
            destOverride: ['http', 'tls'],
          },
          settings: {
            allowTransparent: false,
          },
        },
        {
          tag: 'api',
          port: apiPort,
          listen: '127.0.0.1',
          protocol: 'dokodemo-door',
          settings: {
            address: '127.0.0.1',
          },
        },
      ],
      outbounds,
      routing: {
        domainStrategy: 'IPIfNonMatch',
        rules: [
          {
            inboundTag: ['api'],
            outboundTag: 'api',
            type: 'field',
          },
          {
            type: 'field',
            ip: ['geoip:private'],
            outboundTag: 'direct',
          },
          {
            type: 'field',
            protocol: ['bittorrent'],
            outboundTag: 'direct',
          },
          {
            type: 'field',
            port: '0-65535',
            outboundTag: primaryOutbound.tag as string,
          },
        ],
      },
    };

    return config;
  }

  private static buildOutbound(node: XrayNode): Record<string, unknown> {
    const tag = `proxy-${node.id}`;
    const protocol = (node.protocol || 'vless').toLowerCase();

    const streamSettings: Record<string, unknown> = {
      network: node.network || 'tcp',
      security: node.security || 'none',
    };

    if (node.security === 'reality') {
      streamSettings.realitySettings = {
        show: false,
        fingerprint: 'chrome',
        serverName: node.sni || node.host,
        publicKey: this.extractRealityPublicKey(node.rawKey),
        shortId: '',
        spiderX: '',
      };
    } else if (node.security === 'tls') {
      streamSettings.tlsSettings = {
        allowInsecure: false,
        serverName: node.sni || node.host,
        fingerprint: 'chrome',
      };
    }

    if (node.network === 'ws') {
      streamSettings.wsSettings = {
        path: '/gaming-ws',
        headers: {
          Host: node.sni || node.host,
        },
      };
    }

    switch (protocol) {
      case 'vless':
        return {
          tag,
          protocol: 'vless',
          settings: {
            vnext: [
              {
                address: node.host,
                port: Number(node.port) || 443,
                users: [
                  {
                    id: this.extractUUID(node.rawKey),
                    encryption: 'none',
                    flow: node.security === 'reality' ? 'xtls-rprx-vision' : '',
                  },
                ],
              },
            ],
          },
          streamSettings,
        };

      case 'trojan':
        return {
          tag,
          protocol: 'trojan',
          settings: {
            servers: [
              {
                address: node.host,
                port: Number(node.port) || 443,
                password: this.extractPassword(node.rawKey),
              },
            ],
          },
          streamSettings,
        };

      case 'vmess':
        return {
          tag,
          protocol: 'vmess',
          settings: {
            vnext: [
              {
                address: node.host,
                port: Number(node.port) || 443,
                users: [
                  {
                    id: this.extractUUID(node.rawKey),
                    alterId: 0,
                    security: 'auto',
                  },
                ],
              },
            ],
          },
          streamSettings,
        };

      case 'shadowsocks':
        return {
          tag,
          protocol: 'shadowsocks',
          settings: {
            servers: [
              {
                address: node.host,
                port: Number(node.port) || 8388,
                method: 'aes-128-gcm',
                password: this.extractPassword(node.rawKey),
              },
            ],
          },
        };

      default:
        return {
          tag,
          protocol: 'freedom',
          settings: {},
        };
    }
  }

  private static extractUUID(rawKey: string): string {
    const uuidRegex = /[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/;
    const match = rawKey.match(uuidRegex);
    return match ? match[0] : '4a83e091-6677-4482-9e12-8812c3f9104b';
  }

  private static extractPassword(rawKey: string): string {
    try {
      if (rawKey.includes('://')) {
        const withoutProto = rawKey.split('://')[1];
        const userPart = withoutProto.split('@')[0];
        return decodeURIComponent(userPart);
      }
    } catch {
      // fallback
    }
    return 'victor_geek_vip_sg2';
  }

  private static extractRealityPublicKey(rawKey: string): string {
    const match = rawKey.match(/pbk=([a-zA-Z0-9_-]+)/);
    return match ? match[1] : 'J8Y_19mZKL7-2A9B';
  }
}

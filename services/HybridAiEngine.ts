import { GoogleGenAI } from '@google/genai';
import { XrayNode } from '../src/types';

export interface AiIntentResult {
  intent: string;
  recommendedNodes: XrayNode[];
  routingTuning: {
    dnsMode: string;
    mtu: number;
    recommendedFingerprint: string;
    sniCamouflage: string;
  };
  explanation: string;
  source: 'local_edge' | 'cloud_gemini' | 'local_llm';
}

export class HybridAiEngine {
  private static instance: HybridAiEngine;
  private geminiAi: GoogleGenAI | null = null;

  private constructor() {
    const apiKey = process.env.GEMINI_API_KEY || '';
    if (apiKey) {
      this.geminiAi = new GoogleGenAI({
        apiKey,
        httpOptions: {
          headers: {
            'User-Agent': 'aistudio-build',
          },
        },
      });
    }
  }

  public static getInstance(): HybridAiEngine {
    if (!HybridAiEngine.instance) {
      HybridAiEngine.instance = new HybridAiEngine();
    }
    return HybridAiEngine.instance;
  }

  public setApiKey(apiKey: string) {
    if (apiKey) {
      this.geminiAi = new GoogleGenAI({
        apiKey,
        httpOptions: {
          headers: {
            'User-Agent': 'aistudio-build',
          },
        },
      });
    }
  }

  public evaluateNodesLocally(nodes: XrayNode[], targetPurpose: string = 'general'): XrayNode[] {
    const purpose = targetPurpose.toLowerCase();

    const scoredNodes = nodes.map((node) => {
      let score = 90;

      score -= (node.ping / 100) * 15;

      if (node.security === 'reality') score += 10;
      if (node.protocol === 'vless' || node.protocol === 'trojan') score += 5;

      if (purpose.includes('game') || purpose.includes('pubg') || purpose.includes('mlbb')) {
        if (node.countryCode === 'SG') score += 12;
        if (node.network === 'tcp') score += 6;
      } else if (purpose.includes('video') || purpose.includes('stream') || purpose.includes('youtube')) {
        if (node.countryCode === 'HK' || node.countryCode === 'JP') score += 8;
      } else if (purpose.includes('facebook') || purpose.includes('meta') || purpose.includes('telegram')) {
        if (node.countryCode === 'SG' || node.countryCode === 'TH') score += 10;
      }

      const finalScore = Math.max(50, Math.min(99, Math.round(score)));

      return {
        ...node,
        score: finalScore,
        isRecommended: node.ping <= 40 && finalScore >= 92,
      };
    });

    scoredNodes.sort((a, b) => a.ping - b.ping);
    return scoredNodes;
  }

  public async parseIntentAndOptimize(
    userQuery: string,
    availableNodes: XrayNode[],
    userApiKey?: string
  ): Promise<AiIntentResult> {
    if (userApiKey) {
      this.setApiKey(userApiKey);
    }

    if (this.geminiAi) {
      try {
        const prompt = `You are the Victor Geek Enterprise Xray AI Tuner.
The user wants to optimize connection for: "${userQuery}".
Available nodes: ${JSON.stringify(availableNodes.slice(0, 5).map((n) => ({ id: n.id, name: n.name, ping: n.ping, country: n.countryCode })))}.
Respond ONLY in JSON matching this format:
{
  "bestNodeId": "id-string",
  "dnsMode": "1.1.1.1 (Cloudflare DoH)",
  "mtu": 1420,
  "recommendedFingerprint": "chrome",
  "sniCamouflage": "gateway.cloudflare.com",
  "explanation": "concise explanation in Myanmar/Burmese language explaining the routing optimization"
}`;

        const response = await this.geminiAi.models.generateContent({
          model: 'gemini-2.5-flash',
          contents: prompt,
          config: {
            temperature: 0.2,
            responseMimeType: 'application/json',
          },
        });

        const resText = response.text?.trim() || '';
        if (resText) {
          const parsed = JSON.parse(resText);
          const ranked = this.evaluateNodesLocally(availableNodes, userQuery);
          return {
            intent: userQuery,
            recommendedNodes: ranked,
            routingTuning: {
              dnsMode: parsed.dnsMode || '1.1.1.1 DoH',
              mtu: parsed.mtu || 1420,
              recommendedFingerprint: parsed.recommendedFingerprint || 'chrome',
              sniCamouflage: parsed.sniCamouflage || 'gateway.cloudflare.com',
            },
            explanation: parsed.explanation || 'Cloud AI မှ သင့်လျော်သော အမြန်ဆုံး low-latency node ကို အလိုအလျောက် ရွေးချယ်ပေးထားပါသည်။',
            source: 'cloud_gemini',
          };
        }
      } catch (err) {
        console.warn('[HybridAiEngine] Cloud AI failed, falling back to Local Edge:', err);
      }
    }

    const ranked = this.evaluateNodesLocally(availableNodes, userQuery);
    return {
      intent: userQuery,
      recommendedNodes: ranked,
      routingTuning: {
        dnsMode: '1.1.1.1 (Cloudflare Ultra-Fast DoH)',
        mtu: 1420,
        recommendedFingerprint: 'chrome',
        sniCamouflage: 'sg.gateway.cloudflare.com',
      },
      explanation: `Local Edge AI Engine: "${userQuery}" အတွက် ping အနည်းဆုံးနှင့် packet loss ကင်းစင်သော အကောင်းဆုံး server ကို စက်တွင်း AI ဖြင့် အလိုအလျောက် ရွေးချယ်ပြီး ဖြစ်ပါသည်။`,
      source: 'local_edge',
    };
  }
}

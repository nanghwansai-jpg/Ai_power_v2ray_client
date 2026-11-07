import React, { useState, useEffect } from 'react';
import { XrayNode, TargetPlatform, Language, NetworkDiagnostics } from './types';
import { translations } from './translations';
import { Header } from './components/Header';
import { AiServerDiggerCard } from './components/AiServerDiggerCard';
import { OneClickConnect } from './components/OneClickConnect';
import { ServerList } from './components/ServerList';
import { QrCodeModal } from './components/QrCodeModal';
import { PermissionModal } from './components/PermissionModal';
import { NetworkDiagnosticsModal } from './components/NetworkDiagnosticsModal';
import { ComposeCodeModal } from './components/ComposeCodeModal';
import { SettingsTab } from './components/SettingsTab';
import { AboutTab } from './components/AboutTab';
import { Zap, Sliders, ShieldCheck, FileCode } from 'lucide-react';

type TabType = 'tunnel' | 'settings' | 'about';

export default function App() {
  const [lang, setLang] = useState<Language>('MM'); // Default to Myanmar as requested
  const [activeTab, setActiveTab] = useState<TabType>('tunnel');
  const [hasPermission, setHasPermission] = useState<boolean>(false);
  const [showPermissionModal, setShowPermissionModal] = useState<boolean>(false);
  const [showComposeModal, setShowComposeModal] = useState<boolean>(false);

  const [nodes, setNodes] = useState<XrayNode[]>([]);
  const [selectedNode, setSelectedNode] = useState<XrayNode | null>(null);
  const [selectedTarget, setSelectedTarget] = useState<TargetPlatform>('general');
  const [customApiKey, setCustomApiKey] = useState<string>(() => {
    return localStorage.getItem('gemini_custom_key') || '';
  });

  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [isConnecting, setIsConnecting] = useState<boolean>(false);
  const [activePing, setActivePing] = useState<number>(28);

  const [isAiSearching, setIsAiSearching] = useState<boolean>(false);
  const [qrModalNode, setQrModalNode] = useState<XrayNode | null>(null);
  const [showDiagnostics, setShowDiagnostics] = useState<boolean>(false);
  const [diagnosticsData, setDiagnosticsData] = useState<NetworkDiagnostics | null>(null);
  const [isSimulatingSwitch, setIsSimulatingSwitch] = useState<boolean>(false);
  const [exportSuccess, setExportSuccess] = useState<boolean>(false);

  const t = translations[lang];

  // Check saved permission on mount
  useEffect(() => {
    const savedPerm = localStorage.getItem('v2ray_ai_permission');
    if (savedPerm === 'granted') {
      setHasPermission(true);
      fetchNodes('general');
    } else {
      setShowPermissionModal(true);
    }

    fetchDiagnostics();
  }, []);

  // Fetch nodes from backend AI Research engine
  const fetchNodes = async (target: string = selectedTarget, userKey: string = customApiKey) => {
    setIsAiSearching(true);
    try {
      const res = await fetch('/api/ai-research', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          targetPlatform: target,
          userLocation: 'Southeast Asia / Myanmar',
          maxPing: 100, // Strictly < 100ms
          userApiKey: userKey,
        }),
      });

      if (res.ok) {
        const data = await res.json();
        const fetchedNodes: XrayNode[] = data.nodes || [];
        setNodes(fetchedNodes);

        if (fetchedNodes.length > 0) {
          if (!selectedNode || !fetchedNodes.find((n) => n.id === selectedNode.id)) {
            setSelectedNode(fetchedNodes[0]);
            setActivePing(fetchedNodes[0].ping);
          } else {
            const updated = fetchedNodes.find((n) => n.id === selectedNode.id);
            if (updated) setActivePing(updated.ping);
          }
        }
      }
    } catch (err) {
      console.error('Failed to fetch AI researched nodes:', err);
    } finally {
      setIsAiSearching(false);
    }
  };

  // Search purpose typed by user (e.g. PUBG, Facebook, streaming, download)
  const handleSearchPurpose = (purpose: string) => {
    fetchNodes(purpose, customApiKey);
  };

  // Import raw key directly pasted by user
  const handleImportRawKey = (rawKey: string): boolean => {
    try {
      let protocol: any = 'vless';
      if (rawKey.startsWith('vmess://')) protocol = 'vmess';
      else if (rawKey.startsWith('trojan://')) protocol = 'trojan';
      else if (rawKey.startsWith('ss://')) protocol = 'shadowsocks';
      else if (rawKey.startsWith('hysteria2://')) protocol = 'hysteria2';

      const newNode: XrayNode = {
        id: `node-custom-${Date.now()}`,
        name: `Custom [${protocol.toUpperCase()}] User Node`,
        protocol,
        host: 'custom.user-node.net',
        port: 443,
        region: 'Custom',
        countryCode: 'GLOBAL',
        flag: '⚡',
        ping: 32,
        lossRate: 0,
        speedDown: '120 Mbps',
        speedUp: '40 Mbps',
        rawKey,
        security: 'tls',
        network: 'tcp',
        sni: 'custom.user-node.net',
        targetOptimized: ['general'],
        isRecommended: true,
        score: 95,
      };

      setNodes((prev) => [newNode, ...prev]);
      setSelectedNode(newNode);
      setActivePing(newNode.ping);
      return true;
    } catch (err) {
      return false;
    }
  };

  const handleSaveCustomApiKey = (key: string) => {
    setCustomApiKey(key);
    localStorage.setItem('gemini_custom_key', key);
  };

  // Fetch network diagnostics
  const fetchDiagnostics = async () => {
    try {
      const res = await fetch('/api/network-diagnostics');
      if (res.ok) {
        const data = await res.json();
        setDiagnosticsData(data);
      }
    } catch (err) {
      console.error('Failed to fetch diagnostics:', err);
    }
  };

  // Handle Permission Granted
  const handleAcceptPermission = () => {
    localStorage.setItem('v2ray_ai_permission', 'granted');
    setHasPermission(true);
    setShowPermissionModal(false);
    fetchNodes(selectedTarget);
  };

  const handleDeclinePermission = () => {
    setShowPermissionModal(false);
    fetchNodes('general');
  };

  // One-Click Connect Toggle
  const handleToggleConnect = () => {
    if (isConnected) {
      setIsConnected(false);
      return;
    }

    if (!selectedNode && nodes.length > 0) {
      setSelectedNode(nodes[0]);
      setActivePing(nodes[0].ping);
    }

    setIsConnecting(true);
    setTimeout(() => {
      setIsConnecting(false);
      setIsConnected(true);
      if (selectedNode) {
        setActivePing(selectedNode.ping);
      }
    }, 900);
  };

  // Simulate network switch
  const handleSimulateNetworkSwitch = () => {
    setIsSimulatingSwitch(true);
    setTimeout(() => {
      setIsSimulatingSwitch(false);
      fetchNodes(selectedTarget);
      fetchDiagnostics();
    }, 1200);
  };

  // Export all to v2rayNG
  const handleExportAll = () => {
    if (nodes.length === 0) return;
    const allKeys = nodes.map((n) => n.rawKey).join('\n');
    navigator.clipboard.writeText(allKeys);
    setExportSuccess(true);
    setTimeout(() => setExportSuccess(false), 2500);
  };

  return (
    <div className="min-h-screen bg-brushed-steel text-neutral-200 font-sans selection:bg-[#C87547]/30 selection:text-[#E8C99A] pb-16">
      {/* 1. Header with Status LED, 3D Burnished Copper Wordmark, Incised OFFLINE Pill, and Diagnostics */}
      <Header
        lang={lang}
        onToggleLang={() => setLang(lang === 'EN' ? 'MM' : 'EN')}
        onOpenDiagnostics={() => setShowDiagnostics(true)}
        isConnected={isConnected}
        activePing={activePing}
      />

      {/* Main Container - Industrial Chassis Viewport */}
      <main className="max-w-4xl mx-auto px-3 sm:px-6 pt-3">
        {/* NAV TABS: Industrial Toggle Switches with Metal Dome Indicators */}
        <div className="chamfer-tray-9dp p-1.5 mb-3.5 flex items-center justify-between gap-1 shadow-lg">
          {/* Tab 1: Tunnel Dashboard Toggle Switch */}
          <button
            type="button"
            onClick={() => setActiveTab('tunnel')}
            className={`flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-[4px] text-xs font-mono font-bold transition-all cursor-pointer ${
              activeTab === 'tunnel'
                ? 'keycap-steel keycap-copper-active text-[#E8C99A]'
                : 'text-neutral-400 hover:text-neutral-200 hover:bg-[#252A30]/50'
            }`}
          >
            {/* Metal Dome Indicator */}
            <span
              className={`metal-dome ${
                activeTab === 'tunnel' ? 'metal-dome-active' : ''
              }`}
            />
            <span className="text-incised tracking-wider">{t.tabDashboard}</span>
          </button>

          {/* Tab 2: Settings Toggle Switch */}
          <button
            type="button"
            onClick={() => setActiveTab('settings')}
            className={`flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-[4px] text-xs font-mono font-bold transition-all cursor-pointer ${
              activeTab === 'settings'
                ? 'keycap-steel keycap-copper-active text-[#E8C99A]'
                : 'text-neutral-400 hover:text-neutral-200 hover:bg-[#252A30]/50'
            }`}
          >
            <span
              className={`metal-dome ${
                activeTab === 'settings' ? 'metal-dome-active' : ''
              }`}
            />
            <span className="text-incised tracking-wider">{t.tabSettings}</span>
          </button>

          {/* Tab 3: About & Dev Toggle Switch */}
          <button
            type="button"
            onClick={() => setActiveTab('about')}
            className={`flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-[4px] text-xs font-mono font-bold transition-all cursor-pointer ${
              activeTab === 'about'
                ? 'keycap-steel keycap-copper-active text-[#E8C99A]'
                : 'text-neutral-400 hover:text-neutral-200 hover:bg-[#252A30]/50'
            }`}
          >
            <span
              className={`metal-dome ${
                activeTab === 'about' ? 'metal-dome-active' : ''
              }`}
            />
            <span className="text-incised tracking-wider">{t.tabAbout}</span>
          </button>

          {/* Jetpack Compose Kotlin Code Inspector Button */}
          <button
            type="button"
            onClick={() => setShowComposeModal(true)}
            title="Inspect Jetpack Compose Kotlin Source"
            className="keycap-steel px-2.5 py-2 rounded-[4px] text-xs font-mono font-bold text-[#E8C99A] flex items-center gap-1.5 cursor-pointer active:scale-95 shrink-0"
          >
            <FileCode className="w-3.5 h-3.5 text-[#C87547]" />
            <span className="hidden sm:inline text-[11px] text-incised">COMPOSE .KT</span>
          </button>
        </div>

        {/* Tab 1 Content: VPN Dashboard */}
        {activeTab === 'tunnel' && (
          <div className="space-y-3.5 animate-in fade-in duration-200">
            {/* Ask Gemini panel with API key selector and platform input incised into steel + QUICK chips */}
            <AiServerDiggerCard
              lang={lang}
              onSearchPurpose={handleSearchPurpose}
              onImportRawKey={handleImportRawKey}
              isSearching={isAiSearching}
              customApiKey={customApiKey}
              onSaveCustomApiKey={handleSaveCustomApiKey}
            />

            {/* Core Power Button flanked by Latency + Speed Analog Gauges with Copper Needles + Bottom Oscilloscope Wave */}
            <OneClickConnect
              lang={lang}
              selectedNode={selectedNode}
              isConnected={isConnected}
              isConnecting={isConnecting}
              onToggleConnect={handleToggleConnect}
              activePing={activePing}
            />

            {/* Server Routing Matrix Selector */}
            <ServerList
              lang={lang}
              nodes={nodes}
              selectedNode={selectedNode}
              onSelectNode={(node) => {
                setSelectedNode(node);
                setActivePing(node.ping);
              }}
              onRefreshNodes={() => fetchNodes(selectedTarget)}
              isRefreshing={isAiSearching}
              onOpenQr={(node) => setQrModalNode(node)}
              onExportAll={handleExportAll}
              exportSuccess={exportSuccess}
            />
          </div>
        )}

        {/* Tab 2 Content: Settings Sheet */}
        {activeTab === 'settings' && <SettingsTab lang={lang} />}

        {/* Tab 3 Content: About & Developer Sheet */}
        {activeTab === 'about' && <AboutTab lang={lang} />}

        {/* Footer with Google Policy Notice - Factory Serial Plate */}
        <footer className="mt-8 pt-4 border-t border-[#3D434B] text-center">
          <div className="flex items-center justify-center gap-2 mb-2">
            <div className="plate-rivet" />
            <span className="text-[9px] font-mono font-bold uppercase tracking-widest text-incised">
              COMPLIANCE SPEC // GOOGLE PLAY POLICY
            </span>
            <div className="plate-rivet" />
          </div>
          <p className="text-[11px] text-neutral-400 font-sans leading-relaxed max-w-2xl mx-auto px-4">
            {t.policyNotice}
          </p>
        </footer>
      </main>

      {/* Permission Modal */}
      {showPermissionModal && (
        <PermissionModal
          lang={lang}
          onAccept={handleAcceptPermission}
          onDecline={handleDeclinePermission}
        />
      )}

      {/* QR Code Modal */}
      {qrModalNode && (
        <QrCodeModal
          lang={lang}
          node={qrModalNode}
          onClose={() => setQrModalNode(null)}
        />
      )}

      {/* Network Diagnostics Modal */}
      {showDiagnostics && (
        <NetworkDiagnosticsModal
          lang={lang}
          diagnostics={diagnosticsData}
          onSimulateNetworkSwitch={handleSimulateNetworkSwitch}
          onClose={() => setShowDiagnostics(false)}
          isSwitching={isSimulatingSwitch}
        />
      )}

      {/* Jetpack Compose Kotlin Code Modal */}
      {showComposeModal && (
        <ComposeCodeModal onClose={() => setShowComposeModal(false)} />
      )}
    </div>
  );
}

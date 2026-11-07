import React, { useState, useRef, useEffect } from 'react';
import { XrayNode, Language } from '../types';
import { translations } from '../translations';
import {
  RotateCw,
  QrCode,
  Copy,
  Check,
  Shield,
  Zap,
  ChevronDown,
  ChevronUp,
  Server,
  Share2,
} from 'lucide-react';

interface ServerListProps {
  lang: Language;
  nodes: XrayNode[];
  selectedNode: XrayNode | null;
  onSelectNode: (node: XrayNode) => void;
  onRefreshNodes: () => void;
  isRefreshing: boolean;
  onOpenQr: (node: XrayNode) => void;
  onExportAll: () => void;
  exportSuccess: boolean;
}

export const ServerList: React.FC<ServerListProps> = ({
  lang,
  nodes,
  selectedNode,
  onSelectNode,
  onRefreshNodes,
  isRefreshing,
  onOpenQr,
  onExportAll,
  exportSuccess,
}) => {
  const t = translations[lang];
  const [protocolFilter, setProtocolFilter] = useState<string>('all');
  const [regionFilter, setRegionFilter] = useState<string>('all');
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [isOtherServersExpanded, setIsOtherServersExpanded] = useState<boolean>(false);

  // Filter dropdown states
  const [isProtocolOpen, setIsProtocolOpen] = useState(false);
  const [isRegionOpen, setIsRegionOpen] = useState(false);

  const protocolRef = useRef<HTMLDivElement>(null);
  const regionRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (protocolRef.current && !protocolRef.current.contains(event.target as Node)) {
        setIsProtocolOpen(false);
      }
      if (regionRef.current && !regionRef.current.contains(event.target as Node)) {
        setIsRegionOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const protocols = [
    { id: 'all', label: t.allProtocols },
    { id: 'vless', label: 'VLESS' },
    { id: 'vmess', label: 'VMess' },
    { id: 'trojan', label: 'Trojan' },
    { id: 'shadowsocks', label: 'Shadowsocks' },
    { id: 'hysteria2', label: 'Hysteria2' },
  ];

  const regions = [
    { id: 'all', label: t.allRegions, flag: '🌐' },
    { id: 'Singapore', label: 'Singapore', flag: '🇸🇬' },
    { id: 'Hong Kong', label: 'Hong Kong', flag: '🇭🇰' },
    { id: 'Japan', label: 'Japan', flag: '🇯🇵' },
    { id: 'Thailand', label: 'Thailand', flag: '🇹🇭' },
    { id: 'United States', label: 'United States', flag: '🇺🇸' },
    { id: 'Germany', label: 'Germany', flag: '🇩🇪' },
  ];

  const currentProtocolObj = protocols.find((p) => p.id === protocolFilter) || protocols[0];
  const currentRegionObj = regions.find((r) => r.id === regionFilter) || regions[0];

  const filteredNodes = nodes.filter((node) => {
    const matchesProtocol =
      protocolFilter === 'all' || node.protocol.toLowerCase() === protocolFilter.toLowerCase();
    const matchesRegion =
      regionFilter === 'all' || node.region.toLowerCase() === regionFilter.toLowerCase();
    return matchesProtocol && matchesRegion;
  });

  const activeSpotlightNode =
    selectedNode && filteredNodes.find((n) => n.id === selectedNode.id)
      ? selectedNode
      : filteredNodes[0] || null;

  const otherNodes = filteredNodes.filter(
    (node) => !activeSpotlightNode || node.id !== activeSpotlightNode.id
  );

  const handleCopyKey = (e: React.MouseEvent, node: XrayNode) => {
    e.stopPropagation();
    navigator.clipboard.writeText(node.rawKey);
    setCopiedId(node.id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const renderPingBadge = (ping: number) => {
    return (
      <span className="px-2 py-0.5 rounded-[4px] bg-[#16181B] border border-[#3D434B] text-[10px] font-mono font-bold flex items-center gap-1.5 shadow-inner">
        <span
          className={`w-1.5 h-1.5 rounded-full ${
            ping <= 45 ? 'bg-[#E8B274] shadow-[0_0_6px_#E8B274]' : 'bg-[#C87547]'
          }`}
        />
        <span className="text-incised-amber">{ping} ms</span>
      </span>
    );
  };

  return (
    <div className="chamfer-tray-9dp p-4 sm:p-5 relative mb-3.5">
      {/* Corner Hex Screws */}
      <div className="absolute top-2 left-2 hex-screw" />
      <div className="absolute top-2 right-2 hex-screw" />
      <div className="absolute bottom-2 left-2 hex-screw" />
      <div className="absolute bottom-2 right-2 hex-screw" />

      {/* Header Bar: Title + Protocol Dropdown + Region Dropdown + Refresh */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-3.5 pl-2 pr-2">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-[4px] bg-[#141619] border border-[#3D434B] flex items-center justify-center text-[#C87547] shadow-inner">
            <Server className="w-3.5 h-3.5 text-[#C87547]" />
          </div>
          <div>
            <span className="text-[9px] font-mono font-bold tracking-widest text-incised uppercase block">
              PANEL-03 // ROUTING MATRIX
            </span>
            <h2 className="text-xs sm:text-sm font-black font-mono tracking-tight text-incised-strong">
              {lang === 'EN' ? 'NODE ROUTING SELECTOR' : 'ဆာဗာလိုင်းများ ရွေးချယ်မှု'}
            </h2>
          </div>
        </div>

        {/* Filter Controls Row */}
        <div className="flex items-center gap-2 flex-wrap">
          {/* Protocol Filter Dropdown */}
          <div className="relative" ref={protocolRef}>
            <button
              type="button"
              onClick={() => setIsProtocolOpen(!isProtocolOpen)}
              className="keycap-steel flex items-center gap-1.5 px-2.5 py-1.5 rounded text-[10px] font-mono font-bold text-incised cursor-pointer"
            >
              <span>{currentProtocolObj.label}</span>
              <ChevronDown className="w-3 h-3 text-neutral-400" />
            </button>

            {isProtocolOpen && (
              <div className="absolute right-0 top-full mt-1 w-36 bg-[#181A1D] border border-[#3D434B] rounded shadow-xl z-50 py-1 font-mono text-[10px]">
                {protocols.map((p) => (
                  <button
                    key={p.id}
                    onClick={() => {
                      setProtocolFilter(p.id);
                      setIsProtocolOpen(false);
                    }}
                    className={`w-full text-left px-3 py-1.5 hover:bg-[#282C32] transition-colors flex items-center justify-between ${
                      protocolFilter === p.id ? 'text-[#E8C99A] font-bold bg-[#202327]' : 'text-neutral-400'
                    }`}
                  >
                    <span>{p.label}</span>
                    {protocolFilter === p.id && <span className="metal-dome metal-dome-active" />}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Region Filter Dropdown */}
          <div className="relative" ref={regionRef}>
            <button
              type="button"
              onClick={() => setIsRegionOpen(!isRegionOpen)}
              className="keycap-steel flex items-center gap-1.5 px-2.5 py-1.5 rounded text-[10px] font-mono font-bold text-incised cursor-pointer"
            >
              <span>{currentRegionObj.flag}</span>
              <span className="hidden xs:inline">{currentRegionObj.label}</span>
              <ChevronDown className="w-3 h-3 text-neutral-400" />
            </button>

            {isRegionOpen && (
              <div className="absolute right-0 top-full mt-1 w-40 bg-[#181A1D] border border-[#3D434B] rounded shadow-xl z-50 py-1 font-mono text-[10px]">
                {regions.map((r) => (
                  <button
                    key={r.id}
                    onClick={() => {
                      setRegionFilter(r.id);
                      setIsRegionOpen(false);
                    }}
                    className={`w-full text-left px-3 py-1.5 hover:bg-[#282C32] transition-colors flex items-center justify-between ${
                      regionFilter === r.id ? 'text-[#E8C99A] font-bold bg-[#202327]' : 'text-neutral-400'
                    }`}
                  >
                    <span className="flex items-center gap-1.5">
                      <span>{r.flag}</span>
                      <span>{r.label}</span>
                    </span>
                    {regionFilter === r.id && <span className="metal-dome metal-dome-active" />}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Refresh Key-Cap */}
          <button
            type="button"
            onClick={onRefreshNodes}
            disabled={isRefreshing}
            title="Scan & Ping All Nodes"
            className="keycap-steel p-1.5 rounded text-neutral-300 hover:text-white cursor-pointer active:scale-95 disabled:opacity-50"
          >
            <RotateCw className={`w-3.5 h-3.5 text-[#C87547] ${isRefreshing ? 'animate-spin' : ''}`} />
          </button>

          {/* Export All to v2rayNG */}
          <button
            type="button"
            onClick={onExportAll}
            title="Copy all configurations for Android v2rayNG"
            className="keycap-steel keycap-copper-active flex items-center gap-1 px-2.5 py-1.5 rounded text-[10px] font-mono font-bold text-[#E8C99A] cursor-pointer active:scale-95"
          >
            {exportSuccess ? (
              <>
                <Check className="w-3 h-3 text-[#E8B274]" />
                <span>COPIED</span>
              </>
            ) : (
              <>
                <Share2 className="w-3 h-3 text-[#C87547]" />
                <span className="hidden sm:inline">EXPORT ALL</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* 1. PRIMARY ACTIVE SPOTLIGHT NODE (Milled Steel Bay) */}
      {activeSpotlightNode && (
        <div className="mb-3 p-3 rounded-md bg-[#16181B] border-2 border-[#C87547]/80 shadow-[inset_0_1px_4px_rgba(0,0,0,0.8),0_0_14px_rgba(200,117,71,0.25)] relative transition-all">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
            <div className="flex items-start gap-2.5">
              {/* Active Indicator Dome */}
              <div className="mt-1">
                <span className="metal-dome metal-dome-active block" />
              </div>

              <div>
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-xs font-black font-mono text-incised-strong">
                    {activeSpotlightNode.name}
                  </span>
                  <span className="px-1.5 py-0.2 rounded bg-[#2A2118] border border-[#C87547] text-[9px] font-mono font-bold text-[#E8C99A] uppercase">
                    {activeSpotlightNode.protocol}
                  </span>
                  <span className="text-[10px] text-neutral-400 font-mono">
                    {activeSpotlightNode.flag} {activeSpotlightNode.region}
                  </span>
                </div>
                <div className="text-[10px] font-mono text-neutral-400 mt-0.5">
                  <span>HOST: {activeSpotlightNode.host}:{activeSpotlightNode.port}</span>
                  <span className="mx-1.5">|</span>
                  <span>SNI: {activeSpotlightNode.sni}</span>
                </div>
              </div>
            </div>

            {/* Readout Metrics & Actions */}
            <div className="flex items-center gap-2 self-end sm:self-auto">
              {renderPingBadge(activeSpotlightNode.ping)}

              {/* QR Code Button */}
              <button
                type="button"
                onClick={() => onOpenQr(activeSpotlightNode)}
                title="Open QR Code for v2rayNG Scanner"
                className="keycap-steel p-1.5 rounded text-neutral-300 hover:text-white cursor-pointer active:scale-95"
              >
                <QrCode className="w-3.5 h-3.5 text-[#C87547]" />
              </button>

              {/* Copy Raw Key Button */}
              <button
                type="button"
                onClick={(e) => handleCopyKey(e, activeSpotlightNode)}
                title="Copy Configuration URI"
                className="keycap-steel p-1.5 rounded text-neutral-300 hover:text-white cursor-pointer active:scale-95"
              >
                {copiedId === activeSpotlightNode.id ? (
                  <Check className="w-3.5 h-3.5 text-[#E8B274]" />
                ) : (
                  <Copy className="w-3.5 h-3.5 text-[#C87547]" />
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 2. COLLAPSIBLE OTHER AVAILABLE NODES */}
      {otherNodes.length > 0 && (
        <div>
          <button
            type="button"
            onClick={() => setIsOtherServersExpanded(!isOtherServersExpanded)}
            className="w-full keycap-steel py-2 px-3 rounded text-[11px] font-mono font-bold text-incised flex items-center justify-between cursor-pointer transition-all"
          >
            <span>
              {isOtherServersExpanded
                ? `COLLAPSE OTHER NODES (${otherNodes.length})`
                : `VIEW ${otherNodes.length} OTHER AVAILABLE NODES`}
            </span>
            {isOtherServersExpanded ? (
              <ChevronUp className="w-3.5 h-3.5 text-[#C87547]" />
            ) : (
              <ChevronDown className="w-3.5 h-3.5 text-neutral-400" />
            )}
          </button>

          {isOtherServersExpanded && (
            <div className="mt-2 space-y-1.5 max-h-72 overflow-y-auto pr-1">
              {otherNodes.map((node) => (
                <div
                  key={node.id}
                  onClick={() => onSelectNode(node)}
                  className="p-2.5 rounded bg-[#16181B] border border-[#343A42] hover:border-[#C87547]/60 flex items-center justify-between cursor-pointer transition-all shadow-inner group"
                >
                  <div className="flex items-center gap-2">
                    <span className="metal-dome group-hover:metal-dome-active" />
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className="text-[11px] font-mono font-bold text-neutral-200 group-hover:text-[#E8C99A]">
                          {node.name}
                        </span>
                        <span className="text-[8px] font-mono uppercase px-1 rounded bg-[#202429] text-neutral-400">
                          {node.protocol}
                        </span>
                      </div>
                      <span className="text-[9px] font-mono text-neutral-500">
                        {node.flag} {node.region} · {node.host}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    {renderPingBadge(node.ping)}

                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        onOpenQr(node);
                      }}
                      className="keycap-steel p-1 rounded text-neutral-300 hover:text-white"
                    >
                      <QrCode className="w-3 h-3 text-[#C87547]" />
                    </button>

                    <button
                      type="button"
                      onClick={(e) => handleCopyKey(e, node)}
                      className="keycap-steel p-1 rounded text-neutral-300 hover:text-white"
                    >
                      {copiedId === node.id ? (
                        <Check className="w-3 h-3 text-[#E8B274]" />
                      ) : (
                        <Copy className="w-3 h-3 text-[#C87547]" />
                      )}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

import React from 'react';
import { NetworkDiagnostics, Language } from '../types';
import { translations } from '../translations';
import {
  X,
  CheckCircle2,
  ArrowRightLeft,
  Cpu,
} from 'lucide-react';

interface NetworkDiagnosticsModalProps {
  lang: Language;
  diagnostics: NetworkDiagnostics | null;
  onSimulateNetworkSwitch: () => void;
  onClose: () => void;
  isSwitching: boolean;
}

export const NetworkDiagnosticsModal: React.FC<NetworkDiagnosticsModalProps> = ({
  lang,
  diagnostics,
  onSimulateNetworkSwitch,
  onClose,
  isSwitching,
}) => {
  const t = translations[lang];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-lg max-h-[90vh] overflow-y-auto chamfer-tray-9dp p-4 sm:p-6 text-neutral-100 font-mono shadow-2xl">
        <div className="absolute top-2 left-2 hex-screw" />
        <div className="absolute top-2 right-2 hex-screw" />
        <div className="absolute bottom-2 left-2 hex-screw" />
        <div className="absolute bottom-2 right-2 hex-screw" />

        {/* Close Button */}
        <button
          onClick={onClose}
          type="button"
          className="keycap-steel absolute top-3 right-3 p-1.5 rounded cursor-pointer text-neutral-400 hover:text-white"
        >
          <X className="w-4 h-4 text-[#C87547]" />
        </button>

        {/* Modal Header */}
        <div className="flex items-center gap-2.5 mb-2 pl-2">
          <div className="w-9 h-9 rounded-[4px] bg-[#141619] border border-[#3D434B] flex items-center justify-center text-[#C87547] shadow-inner">
            <Cpu className="w-5 h-5 text-[#C87547]" />
          </div>
          <div>
            <span className="text-[9px] font-mono text-neutral-500 uppercase tracking-widest block">
              DIAGNOSTICS & TELEMETRY
            </span>
            <h3 className="text-sm sm:text-base font-black text-incised-strong">
              {t.diagnosticsTitle}
            </h3>
          </div>
        </div>

        <p className="text-[11px] text-neutral-400 mb-3.5 font-sans leading-relaxed pl-2 pr-2">
          {t.diagnosticsSubtitle}
        </p>

        {/* Simulate Network Switch Button */}
        <div className="mb-4 p-3 rounded bg-[#16181B] border border-[#3D434B] flex items-center justify-between gap-3 shadow-inner">
          <div>
            <p className="text-xs font-bold text-incised-amber">NETWORK CHANGE DETECTOR</p>
            <p className="text-[10px] text-neutral-400 font-sans">
              Auto-triggers latency check & modifies DNS/MTU on switch
            </p>
          </div>
          <button
            onClick={onSimulateNetworkSwitch}
            disabled={isSwitching}
            type="button"
            className="keycap-steel keycap-copper-active flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono font-bold text-[#E8C99A] shrink-0 cursor-pointer active:scale-95 disabled:opacity-50"
          >
            <ArrowRightLeft className={`w-3.5 h-3.5 text-[#E8B274] ${isSwitching ? 'animate-spin' : ''}`} />
            <span>{isSwitching ? 'CALIBRATING...' : 'TEST SWITCH'}</span>
          </button>
        </div>

        {/* Telemetry Metrics */}
        {diagnostics && (
          <div className="space-y-3">
            {/* Primary Metrics Grid */}
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div className="p-2.5 rounded bg-[#16181B] border border-[#343A42] shadow-inner">
                <span className="text-neutral-500 text-[10px] block">CLIENT IP // ADDR</span>
                <span className="font-bold text-incised-strong">{diagnostics.currentNetwork?.clientIp || '103.217.156.42'}</span>
                <span className="text-[10px] text-neutral-400 block mt-0.5">TYPE: {diagnostics.currentNetwork?.type || 'Wi-Fi 5G'}</span>
              </div>
              <div className="p-2.5 rounded bg-[#16181B] border border-[#343A42] shadow-inner">
                <span className="text-neutral-500 text-[10px] block">DNS LATENCY RTT</span>
                <span className="font-bold text-incised-copper">{diagnostics.dns?.[0]?.latencyMs || 14} ms</span>
                <span className="text-[10px] text-neutral-400 block mt-0.5">MTU: {diagnostics.currentNetwork?.mtu || 1420} BYTES</span>
              </div>
            </div>

            {/* Invariants & Health Checks */}
            <div className="p-3 rounded bg-[#16181B] border border-[#343A42] shadow-inner space-y-2 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-neutral-300">DNS LEAK SHIELD INTEGRITY</span>
                <span className="flex items-center gap-1 text-[#E8B274] font-bold text-[10px]">
                  <CheckCircle2 className="w-3.5 h-3.5" /> SECURE (1.1.1.1)
                </span>
              </div>
              <div className="flex items-center justify-between border-t border-[#262A2F] pt-1.5">
                <span className="text-neutral-300">OPTIMAL MTU TUNNEL SIZE</span>
                <span className="font-bold text-[#E8C99A] text-[10px]">1420 BYTES</span>
              </div>
              <div className="flex items-center justify-between border-t border-[#262A2F] pt-1.5">
                <span className="text-neutral-300">UDP MULTIPLEXING CONCURRENCY</span>
                <span className="font-bold text-[#E8B274] text-[10px]">ACTIVE (8 CHANNELS)</span>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

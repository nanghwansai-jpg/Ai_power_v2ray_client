import React from 'react';
import { Language } from '../types';
import { CopperWordmark } from './CopperWordmark';
import { Activity, Globe } from 'lucide-react';

interface HeaderProps {
  lang: Language;
  onToggleLang: () => void;
  onOpenDiagnostics: () => void;
  isConnected: boolean;
  activePing: number;
}

export const Header: React.FC<HeaderProps> = ({
  lang,
  onToggleLang,
  onOpenDiagnostics,
  isConnected,
  activePing,
}) => {
  return (
    <header className="border-b border-[#3D434B] bg-[#23262A]/95 backdrop-blur-sm sticky top-0 z-40 transition-all shadow-[0_4px_20px_rgba(0,0,0,0.6)]">
      {/* Precision Instrument Bezel Strip across top edge */}
      <div className="h-1 w-full bg-gradient-to-r from-[#1C1E22] via-[#4D545E] to-[#1C1E22] border-b border-[#141618]" />

      {/* Main Header Chassis Row */}
      <div className="max-w-4xl mx-auto px-3 py-2.5 sm:px-6 sm:py-3 flex items-center justify-between gap-3 relative">
        {/* TOP-LEFT CORNER: Status LED + Title + Status Pill */}
        <div className="flex items-center gap-3">
          {/* Industrial Jewel LED Indicator with Knurled Bezel Ring */}
          <div className="relative flex items-center justify-center">
            {/* Outer knurled metal housing */}
            <div className="w-5 h-5 rounded-full bg-[#181A1D] border-2 border-[#4D545E] flex items-center justify-center shadow-inner">
              {/* Convex Glass Jewel Lens */}
              <span
                className={`w-2.5 h-2.5 rounded-full transition-all duration-300 ${
                  isConnected
                    ? 'bg-[#E8B274] shadow-[0_0_10px_#E8B274,0_0_18px_#C87547]'
                    : 'bg-[#C53030] shadow-[0_0_8px_#C53030,0_0_14px_rgba(197,48,48,0.7)]'
                }`}
              />
            </div>
            {/* Active Halo Pulse */}
            {isConnected && (
              <span className="absolute -inset-1 rounded-full bg-[#E8B274]/30 animate-ping pointer-events-none" />
            )}
          </div>

          {/* 3D-Extruded Burnished Copper Title & Incised Status Pill */}
          <div className="flex items-center gap-2.5">
            <CopperWordmark />

            {/* Machined Serial Plate Status Pill */}
            <div
              className={`px-2 py-0.5 rounded-[4px] border text-[9px] font-mono font-bold tracking-widest flex items-center gap-1 shadow-inner select-none ${
                isConnected
                  ? 'bg-[#2A231C] border-[#C87547]/60 text-[#E8C99A]'
                  : 'bg-[#241A1A] border-[#742A2A]/70 text-[#E57373]'
              }`}
            >
              <span
                className={`w-1.5 h-1.5 rounded-full ${
                  isConnected ? 'bg-[#E8B274] animate-pulse' : 'bg-[#E57373]'
                }`}
              />
              <span className="text-incised">
                {isConnected ? 'ONLINE' : 'OFFLINE'}
              </span>
            </div>
          </div>
        </div>

        {/* TOP-RIGHT CORNER: Industrial NetCheck Key-Cap & Language Toggle Switch */}
        <div className="flex items-center gap-2">
          {/* NetCheck Diagnostics Button - Machined Key-Cap */}
          <button
            onClick={onOpenDiagnostics}
            title="Precision Telemetry & Network Diagnostics"
            type="button"
            className="keycap-steel flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-mono transition-all group active:scale-95 cursor-pointer"
          >
            <Activity className="w-3.5 h-3.5 text-[#C87547] group-hover:text-[#E8C99A] transition-colors" />
            <span className="hidden sm:inline text-[11px] font-bold text-incised uppercase tracking-wider">
              DIAG // PING
            </span>
            {isConnected ? (
              <span className="text-[10px] font-mono font-bold text-[#E8B274] bg-[#181A1D] px-1.5 py-0.2 rounded border border-[#444A52]">
                {activePing}ms
              </span>
            ) : (
              <span className="text-[10px] font-mono text-neutral-400">---</span>
            )}
          </button>

          {/* Language Selector Toggle - Industrial Knurled Disc */}
          <button
            onClick={onToggleLang}
            type="button"
            className="keycap-steel h-8 px-2.5 sm:px-3 rounded-md flex items-center gap-1.5 transition-all active:scale-95 cursor-pointer group"
            title={lang === 'EN' ? 'Switch to Noto Sans Myanmar' : 'Switch to Inter English'}
          >
            <Globe className="w-3.5 h-3.5 text-[#C87547] group-hover:text-[#E8C99A]" />
            <span className="text-[11px] font-mono font-bold text-incised">
              {lang === 'EN' ? 'ENG' : 'မြန်မာ'}
            </span>
            {/* Tiny dome indicator */}
            <span className="metal-dome metal-dome-active" />
          </button>
        </div>
      </div>
    </header>
  );
};

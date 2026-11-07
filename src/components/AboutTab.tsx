import React from 'react';
import { Language } from '../types';
import { translations } from '../translations';
import { ShieldCheck, Cpu, Lock, Layers } from 'lucide-react';

interface AboutTabProps {
  lang: Language;
}

export const AboutTab: React.FC<AboutTabProps> = ({ lang }) => {
  const t = translations[lang];

  return (
    <div className="space-y-3.5 font-mono animate-in fade-in duration-200">
      {/* 1. Industrial Specification Tray */}
      <div className="chamfer-tray-9dp p-4 sm:p-5 relative">
        <div className="absolute top-2 left-2 hex-screw" />
        <div className="absolute top-2 right-2 hex-screw" />
        <div className="absolute bottom-2 left-2 hex-screw" />
        <div className="absolute bottom-2 right-2 hex-screw" />

        <div className="pl-2 pr-2">
          <div className="flex items-center gap-2 mb-2">
            <span className="px-2 py-0.5 rounded-[4px] bg-[#2A2118] border border-[#C87547] text-[#E8C99A] text-[9px] font-bold">
              XRAY-AI CORE v2.8.4
            </span>
            <span className="px-2 py-0.5 rounded-[4px] bg-[#16181B] border border-[#3D434B] text-neutral-400 text-[9px] font-bold">
              SPEC: INDUSTRIAL PRECISION
            </span>
          </div>

          <h2 className="text-sm sm:text-base font-black text-incised-strong mb-1">
            {t.appName}
          </h2>
          <p className="text-[11px] text-neutral-400 font-sans leading-relaxed">
            Autonomous V2Ray Network Orchestration Engine calibrated for high-throughput mobile carriers and low-latency gaming.
          </p>
        </div>
      </div>

      {/* 2. Lead Developer Serial Plate (Victor Geek) */}
      <div className="chamfer-tray-9dp p-4 sm:p-5 relative">
        <div className="absolute top-2 left-2 hex-screw" />
        <div className="absolute top-2 right-2 hex-screw" />
        <div className="absolute bottom-2 left-2 hex-screw" />
        <div className="absolute bottom-2 right-2 hex-screw" />

        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 pl-2 pr-2">
          {/* Milled Steel Emblem */}
          <div className="w-16 h-16 rounded-[6px] bg-[#141619] border-2 border-[#C87547] flex items-center justify-center text-[#E8C99A] shadow-inner shrink-0">
            <Cpu className="w-8 h-8 text-[#C87547]" />
          </div>

          <div className="flex-1">
            <div className="flex items-center gap-2">
              <span className="text-[9px] font-mono text-neutral-500 uppercase tracking-widest">
                LEAD ARCHITECT // GOD LEVEL HACKER
              </span>
              <span className="metal-dome metal-dome-active" />
            </div>
            <h3 className="text-sm sm:text-base font-black text-incised-copper mt-0.5">
              Victor Geek
            </h3>
            <p className="text-[11px] text-neutral-400 font-sans mt-1">
              Precision-engineered network tunnels, AI-driven server mining algorithms, and zero-loss carrier bypassing.
            </p>
          </div>
        </div>

        {/* Technical Specification Matrix */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 mt-4 pt-3 border-t border-[#3D434B] text-[10px] pl-2 pr-2">
          <div className="p-2 rounded bg-[#16181B] border border-[#343A42] shadow-inner">
            <span className="text-neutral-500 block">CHASSIS SPEC</span>
            <span className="font-bold text-incised-strong">BRUSHED STEEL #2C3035</span>
          </div>
          <div className="p-2 rounded bg-[#16181B] border border-[#343A42] shadow-inner">
            <span className="text-neutral-500 block">BEVEL GEOMETRY</span>
            <span className="font-bold text-incised-strong">9DP CHAMFERED RECESSED</span>
          </div>
          <div className="p-2 rounded bg-[#16181B] border border-[#343A42] shadow-inner">
            <span className="text-neutral-500 block">DIAL NEEDLES</span>
            <span className="font-bold text-[#E8C99A]">BURNISHED COPPER #C87547</span>
          </div>
          <div className="p-2 rounded bg-[#16181B] border border-[#343A42] shadow-inner">
            <span className="text-neutral-500 block">SECURITY PROTOCOL</span>
            <span className="font-bold text-incised-strong">TLS 1.3 / REALITY / HYSTERIA2</span>
          </div>
        </div>
      </div>
    </div>
  );
};

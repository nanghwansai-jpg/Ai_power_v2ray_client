import React from 'react';
import { translations } from '../translations';
import { Language } from '../types';
import { ShieldCheck, Search, CheckCircle2 } from 'lucide-react';

interface PermissionModalProps {
  lang: Language;
  onAccept: () => void;
  onDecline: () => void;
}

export const PermissionModal: React.FC<PermissionModalProps> = ({
  lang,
  onAccept,
  onDecline,
}) => {
  const t = translations[lang];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-md chamfer-tray-9dp p-5 sm:p-6 shadow-2xl text-neutral-100 font-sans">
        <div className="absolute top-2 left-2 hex-screw" />
        <div className="absolute top-2 right-2 hex-screw" />
        <div className="absolute bottom-2 left-2 hex-screw" />
        <div className="absolute bottom-2 right-2 hex-screw" />

        {/* Burnished Copper Accent Line */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-48 h-1 bg-gradient-to-r from-transparent via-[#C87547] to-transparent rounded-full" />

        {/* Header Icon */}
        <div className="flex items-center gap-3 mb-4 pl-2">
          <div className="w-12 h-12 rounded-[6px] bg-[#141619] border border-[#3D434B] flex items-center justify-center text-[#C87547] shadow-inner">
            <Search className="w-6 h-6 text-[#C87547] animate-pulse" />
          </div>
          <div>
            <span className="text-[9px] font-mono text-neutral-500 uppercase tracking-widest block">
              SECURITY AUDIT // CARRIER BYPASS
            </span>
            <h2 className="text-base sm:text-lg font-bold font-mono text-incised-strong">
              {t.permissionTitle}
            </h2>
          </div>
        </div>

        {/* Description */}
        <div className="text-xs sm:text-sm text-neutral-300 leading-relaxed mb-5 bg-[#141618] p-3.5 rounded border border-[#3D434B] space-y-2 shadow-inner">
          <p className="font-sans">{t.permissionDesc}</p>
          <ul className="text-[11px] text-neutral-300 space-y-1.5 pt-1 font-mono">
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-3.5 h-3.5 text-[#C87547] shrink-0" />
              <span>Deep crawl active open Xray subscriptions & nodes</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-3.5 h-3.5 text-[#E8B274] shrink-0" />
              <span>Self-test device network & filter Ping &lt; 100ms</span>
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="w-3.5 h-3.5 text-[#E8C99A] shrink-0" />
              <span>Auto-tune DNS (1.1.1.1), Routing, and MTU</span>
            </li>
          </ul>
        </div>

        {/* Buttons */}
        <div className="flex flex-col sm:flex-row gap-2.5">
          <button
            onClick={onAccept}
            type="button"
            className="flex-1 keycap-steel keycap-copper-active py-2.5 px-4 rounded text-xs font-mono font-bold text-[#E8C99A] flex items-center justify-center gap-2 cursor-pointer active:scale-95"
          >
            <ShieldCheck className="w-4 h-4 text-[#C87547]" />
            <span>{t.permissionAccept}</span>
          </button>
          <button
            onClick={onDecline}
            type="button"
            className="keycap-steel py-2.5 px-4 rounded text-xs font-mono font-bold text-neutral-400 hover:text-white cursor-pointer active:scale-95"
          >
            <span>{t.permissionDecline}</span>
          </button>
        </div>
      </div>
    </div>
  );
};

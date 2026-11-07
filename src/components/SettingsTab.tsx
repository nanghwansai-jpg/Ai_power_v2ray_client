import React, { useState } from 'react';
import { Language } from '../types';
import { translations } from '../translations';
import { Sliders, Server, Layers, Save, Check } from 'lucide-react';

interface SettingsTabProps {
  lang: Language;
}

export const SettingsTab: React.FC<SettingsTabProps> = ({ lang }) => {
  const t = translations[lang];

  // Config states
  const [dnsMode, setDnsMode] = useState('cloudflare');
  const [routingMode, setRoutingMode] = useState('bypass_lan');
  const [mtuSize, setMtuSize] = useState('1420');
  const [ipv6Protection, setIpv6Protection] = useState(true);
  const [muxEnabled, setMuxEnabled] = useState(true);
  const [autoReconnect, setAutoReconnect] = useState(true);
  const [savedSuccess, setSavedSuccess] = useState(false);

  const handleSave = () => {
    setSavedSuccess(true);
    setTimeout(() => setSavedSuccess(false), 2500);
  };

  return (
    <div className="space-y-3.5 font-mono animate-in fade-in duration-200">
      {/* Settings Header Tray */}
      <div className="chamfer-tray-9dp p-4 sm:p-5 relative">
        <div className="absolute top-2 left-2 hex-screw" />
        <div className="absolute top-2 right-2 hex-screw" />
        <div className="absolute bottom-2 left-2 hex-screw" />
        <div className="absolute bottom-2 right-2 hex-screw" />

        <div className="flex items-center justify-between gap-2 mb-3.5 pl-2 pr-2">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-[4px] bg-[#141619] border border-[#3D434B] flex items-center justify-center text-[#C87547] shadow-inner">
              <Sliders className="w-4 h-4 text-[#C87547]" />
            </div>
            <div>
              <span className="text-[9px] font-mono font-bold tracking-widest text-incised uppercase block">
                CHASSIS CALIBRATION
              </span>
              <h3 className="text-xs sm:text-sm font-black text-incised-strong">
                {t.settingsTitle}
              </h3>
            </div>
          </div>

          <button
            onClick={handleSave}
            type="button"
            className="keycap-steel keycap-copper-active flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono font-bold text-[#E8C99A] cursor-pointer active:scale-95"
          >
            {savedSuccess ? <Check className="w-3.5 h-3.5 text-[#E8B274]" /> : <Save className="w-3.5 h-3.5 text-[#C87547]" />}
            <span>{savedSuccess ? t.settingsSaved : 'SAVE CONFIG'}</span>
          </button>
        </div>

        {/* 1. DNS Settings */}
        <div className="space-y-3 pt-1 pl-1 pr-1">
          <div className="p-3 rounded bg-[#16181B] border border-[#3D434B] shadow-inner">
            <label className="text-xs font-bold text-neutral-200 mb-2 flex items-center gap-1.5">
              <Server className="w-3.5 h-3.5 text-[#C87547]" />
              <span className="text-incised">{t.settingsDnsTitle}</span>
            </label>
            <div className="grid grid-cols-2 xs:grid-cols-4 gap-1.5 text-[10px]">
              {[
                { id: 'cloudflare', label: '1.1.1.1 (Cloudflare)' },
                { id: 'google', label: '8.8.8.8 (Google)' },
                { id: 'adguard', label: 'AdGuard (Filter)' },
                { id: 'nextdns', label: 'NextDNS Secure' },
              ].map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => setDnsMode(item.id)}
                  className={`p-2 rounded border text-left transition-all keycap-steel ${
                    dnsMode === item.id
                      ? 'keycap-copper-active text-[#E8C99A]'
                      : 'text-neutral-400'
                  }`}
                >
                  <span className="block truncate">{item.label}</span>
                </button>
              ))}
            </div>
          </div>

          {/* 2. Routing Mode */}
          <div className="p-3 rounded bg-[#16181B] border border-[#3D434B] shadow-inner">
            <label className="text-xs font-bold text-neutral-200 mb-2 flex items-center gap-1.5">
              <Layers className="w-3.5 h-3.5 text-[#E8B274]" />
              <span className="text-incised">{t.settingsRoutingTitle}</span>
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-1.5 text-[10px]">
              {[
                { id: 'bypass_lan', label: 'BYPASS LAN & LOCAL' },
                { id: 'global', label: 'GLOBAL PROXY (ALL)' },
                { id: 'game_udp', label: 'GAME DIRECT UDP' },
              ].map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => setRoutingMode(item.id)}
                  className={`p-2 rounded border text-left transition-all keycap-steel ${
                    routingMode === item.id
                      ? 'keycap-copper-active text-[#E8C99A]'
                      : 'text-neutral-400'
                  }`}
                >
                  <span className="block truncate">{item.label}</span>
                </button>
              ))}
            </div>
          </div>

          {/* 3. Hardware Toggles */}
          <div className="p-3 rounded bg-[#16181B] border border-[#3D434B] shadow-inner space-y-2 text-xs">
            <div className="flex items-center justify-between py-1">
              <span className="text-incised">IPv6 LEAK SHIELD PROTECTION</span>
              <button
                type="button"
                onClick={() => setIpv6Protection(!ipv6Protection)}
                className="flex items-center gap-1.5 cursor-pointer"
              >
                <span className={`metal-dome ${ipv6Protection ? 'metal-dome-active' : ''}`} />
                <span className="text-[10px] font-bold text-neutral-400">
                  {ipv6Protection ? 'ENABLED' : 'DISABLED'}
                </span>
              </button>
            </div>

            <div className="flex items-center justify-between py-1 border-t border-[#262B30]">
              <span className="text-incised">TCP MUX MULTIPLEXING CONCURRENCY</span>
              <button
                type="button"
                onClick={() => setMuxEnabled(!muxEnabled)}
                className="flex items-center gap-1.5 cursor-pointer"
              >
                <span className={`metal-dome ${muxEnabled ? 'metal-dome-active' : ''}`} />
                <span className="text-[10px] font-bold text-neutral-400">
                  {muxEnabled ? 'ENABLED' : 'DISABLED'}
                </span>
              </button>
            </div>

            <div className="flex items-center justify-between py-1 border-t border-[#262B30]">
              <span className="text-incised">AUTO-RECONNECT ON CARRIER SWITCH</span>
              <button
                type="button"
                onClick={() => setAutoReconnect(!autoReconnect)}
                className="flex items-center gap-1.5 cursor-pointer"
              >
                <span className={`metal-dome ${autoReconnect ? 'metal-dome-active' : ''}`} />
                <span className="text-[10px] font-bold text-neutral-400">
                  {autoReconnect ? 'ENABLED' : 'DISABLED'}
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

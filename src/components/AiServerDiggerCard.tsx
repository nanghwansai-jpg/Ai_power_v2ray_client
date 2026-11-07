import React, { useState } from 'react';
import { Language } from '../types';
import { translations } from '../translations';
import {
  Sparkles,
  ExternalLink,
  Key,
  Search,
  Check,
  ChevronDown,
  ChevronUp,
  Cpu,
} from 'lucide-react';

interface AiServerDiggerCardProps {
  lang: Language;
  onSearchPurpose: (purposeText: string) => void;
  onImportRawKey: (rawKey: string) => boolean;
  isSearching: boolean;
  customApiKey: string;
  onSaveCustomApiKey: (key: string) => void;
}

export const AiServerDiggerCard: React.FC<AiServerDiggerCardProps> = ({
  lang,
  onSearchPurpose,
  onImportRawKey,
  isSearching,
  customApiKey,
  onSaveCustomApiKey,
}) => {
  const [purposeInput, setPurposeInput] = useState('');
  const [apiKeyInput, setApiKeyInput] = useState(customApiKey);
  const [showKeyBox, setShowKeyBox] = useState(false);
  const [keySavedToast, setKeySavedToast] = useState(false);
  const [importStatus, setImportStatus] = useState<string | null>(null);

  const t = translations[lang];

  // Precision Quick chips - Milled steel key-caps
  const quickChips = [
    { label: 'PUBG MOBILE', query: 'PUBG Mobile' },
    { label: 'FACEBOOK // IG', query: 'Facebook' },
    { label: 'STREAMING 4K', query: 'Streaming' },
    { label: 'DOWNLOAD FAST', query: 'Download' },
    { label: 'TELEGRAM PROXY', query: 'Telegram' },
    { label: 'LOW-LATENCY GAMING', query: 'Gaming' },
  ];

  const handleSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const trimmed = purposeInput.trim();
    if (!trimmed) return;

    // Check if user pasted a raw V2Ray key
    if (
      trimmed.startsWith('vless://') ||
      trimmed.startsWith('vmess://') ||
      trimmed.startsWith('trojan://') ||
      trimmed.startsWith('ss://') ||
      trimmed.startsWith('hysteria2://')
    ) {
      const ok = onImportRawKey(trimmed);
      if (ok) {
        setImportStatus(
          lang === 'EN'
            ? 'RAW V2RAY KEY IMPORTED TO INSTRUMENT MATRIX'
            : 'Key ကို တိုက်ရိုက် ထည့်သွင်းပြီးပါပြီ!'
        );
        setPurposeInput('');
        setTimeout(() => setImportStatus(null), 3000);
        return;
      }
    }

    // Otherwise, ask Gemini to dig servers based on the purpose
    onSearchPurpose(trimmed);
  };

  const handleSaveApiKey = () => {
    onSaveCustomApiKey(apiKeyInput.trim());
    setKeySavedToast(true);
    setTimeout(() => setKeySavedToast(false), 2000);
  };

  return (
    <div className="chamfer-tray-9dp p-4 sm:p-5 relative mb-3.5">
      {/* Corner Hex Screws for Industrial Factory Serial Plate */}
      <div className="absolute top-2 left-2 hex-screw" />
      <div className="absolute top-2 right-2 hex-screw" />
      <div className="absolute bottom-2 left-2 hex-screw" />
      <div className="absolute bottom-2 right-2 hex-screw" />

      {/* Section Header: Serial Tag + Title + Gemini API Key Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-3.5 pl-2 pr-2">
        <div className="flex items-center gap-2.5">
          {/* Milled Steel Indicator Icon */}
          <div className="w-8 h-8 rounded-[6px] bg-[#141619] border border-[#3D434B] flex items-center justify-center text-[#C87547] shadow-inner shrink-0">
            <Cpu className="w-4 h-4 text-[#C87547]" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-[9px] font-mono font-bold tracking-widest text-incised uppercase">
                PANEL-01 // AI OPTIMIZER
              </span>
              <span className="w-1.5 h-1.5 rounded-full bg-[#E8B274] animate-pulse" />
            </div>
            <h2 className="text-xs sm:text-sm font-black font-mono tracking-tight text-incised-strong">
              {lang === 'EN'
                ? 'ASK GEMINI // TARGET PLATFORM ROUTER'
                : 'GEMINI AI သို့ မေးမြန်းခြင်း // သုံးမည့် PLATFORM ရွေးချယ်မှု'}
            </h2>
          </div>
        </div>

        {/* Gemini API Key Selector Toggle Button */}
        <button
          type="button"
          onClick={() => setShowKeyBox(!showKeyBox)}
          className={`keycap-steel flex items-center gap-1.5 px-3 py-1.5 rounded-md text-[11px] font-mono cursor-pointer transition-all ${
            showKeyBox ? 'keycap-copper-active' : ''
          }`}
        >
          <Key className="w-3.5 h-3.5 text-[#C87547]" />
          <span className="text-incised font-bold">API KEY CONFIG</span>
          {customApiKey && (
            <span className="w-1.5 h-1.5 rounded-full bg-[#E8B274] shadow-[0_0_6px_#E8B274]" />
          )}
          {showKeyBox ? (
            <ChevronUp className="w-3 h-3 text-[#E8C99A]" />
          ) : (
            <ChevronDown className="w-3 h-3 text-neutral-400" />
          )}
        </button>
      </div>

      {/* Optional Gemini API Key Drawer (Incised into Steel) */}
      {showKeyBox && (
        <div className="mb-3.5 p-3 rounded-md bg-[#141618] border border-[#3D434B] shadow-inner pl-3 pr-3">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-2">
            <div>
              <span className="text-[11px] font-mono font-bold text-incised-amber flex items-center gap-1.5">
                <Key className="w-3.5 h-3.5 text-[#C87547]" />
                <span>GOOGLE GEMINI API KEY SELECTOR</span>
              </span>
              <p className="text-[10px] text-neutral-400 font-mono mt-0.5">
                {customApiKey
                  ? 'SERIAL: CUSTOM USER KEY REGISTERED'
                  : 'SERIAL: SYSTEM DEFAULT AI ENGINE ACTIVE'}
              </p>
            </div>

            {/* Direct URL to AI Studio API Key */}
            <a
              href="https://aistudio.google.com/app/apikey"
              target="_blank"
              rel="noopener noreferrer"
              className="keycap-steel inline-flex items-center gap-1.5 px-2.5 py-1 rounded text-[10px] font-mono font-bold text-[#E8C99A] hover:text-white transition-all shrink-0 cursor-pointer"
            >
              <span>GET API KEY</span>
              <ExternalLink className="w-3 h-3 text-[#C87547]" />
            </a>
          </div>

          <div className="flex gap-2">
            <input
              type="password"
              value={apiKeyInput}
              onChange={(e) => setApiKeyInput(e.target.value)}
              placeholder="Paste Gemini API key (AIzaSy...)"
              className="flex-1 bg-[#1A1C1F] border border-[#3D434B] focus:border-[#C87547] rounded px-3 py-1.5 text-xs font-mono text-neutral-200 placeholder-neutral-500 outline-none transition-colors shadow-inner"
            />
            <button
              type="button"
              onClick={handleSaveApiKey}
              className="keycap-steel keycap-copper-active px-3 py-1.5 rounded text-xs font-mono font-bold text-[#E8C99A] flex items-center gap-1 cursor-pointer active:scale-95"
            >
              {keySavedToast ? (
                <>
                  <Check className="w-3.5 h-3.5 text-[#E8B274]" />
                  <span>SAVED</span>
                </>
              ) : (
                <span>APPLY</span>
              )}
            </button>
          </div>
        </div>
      )}

      {/* Platform Input Form (Incised Steel Recess) */}
      <form onSubmit={handleSubmit} className="mb-3 pl-1 pr-1">
        <div className="relative flex items-center">
          <input
            type="text"
            value={purposeInput}
            onChange={(e) => setPurposeInput(e.target.value)}
            placeholder={
              lang === 'EN'
                ? 'Enter platform (e.g. PUBG, Facebook, streaming) or paste vmess/vless key...'
                : 'အသုံးပြုမည့် App/Platform ရိုက်ထည့်ပါ (ဥပမာ- PUBG, Facebook) သို့မဟုတ် key ထည့်ပါ...'
            }
            className="w-full bg-[#141618] border border-[#3D434B] focus:border-[#C87547] rounded-md py-2.5 pl-3.5 pr-24 text-xs font-mono text-neutral-200 placeholder-neutral-500 outline-none shadow-inner transition-colors"
          />

          {/* Submit Key-Cap with Copper Glow */}
          <button
            type="submit"
            disabled={isSearching || !purposeInput.trim()}
            className="absolute right-1 keycap-steel keycap-copper-active px-3 py-1.5 rounded text-[11px] font-mono font-bold text-[#E8C99A] disabled:opacity-50 flex items-center gap-1 cursor-pointer active:scale-95"
          >
            {isSearching ? (
              <span className="animate-spin text-[#E8B274]">⚙</span>
            ) : (
              <Search className="w-3 h-3 text-[#C87547]" />
            )}
            <span>{isSearching ? 'DIGGING...' : 'OPTIMIZE'}</span>
          </button>
        </div>
      </form>

      {/* Import Status Alert */}
      {importStatus && (
        <div className="mb-3 mx-1 p-2 rounded bg-[#2A2118] border border-[#C87547]/60 text-[#E8C99A] text-[11px] font-mono flex items-center gap-1.5 shadow-inner animate-in fade-in">
          <Check className="w-3.5 h-3.5 text-[#E8B274]" />
          <span>{importStatus}</span>
        </div>
      )}

      {/* QUICK CHIPS (Incised Brushed Steel Key-Caps) */}
      <div className="pl-1 pr-1">
        <div className="flex items-center gap-1.5 mb-1.5">
          <span className="text-[9px] font-mono font-bold tracking-wider text-incised uppercase">
            QUICK CHIPS // PRESET PROFILES:
          </span>
        </div>
        <div className="flex flex-wrap gap-1.5">
          {quickChips.map((chip) => (
            <button
              key={chip.query}
              type="button"
              onClick={() => {
                setPurposeInput(chip.query);
                onSearchPurpose(chip.query);
              }}
              className="keycap-steel px-2.5 py-1 rounded text-[10px] font-mono font-bold tracking-wider text-incised hover:text-[#E8C99A] hover:border-[#C87547] transition-all cursor-pointer active:scale-95"
            >
              {chip.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

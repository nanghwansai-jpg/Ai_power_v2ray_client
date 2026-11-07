import React, { useState, useRef, useEffect } from 'react';
import { TargetPlatform, Language } from '../types';
import { translations, targetDetails } from '../translations';
import { Target, ChevronDown, Check, Sparkles } from 'lucide-react';

interface TargetPlatformSelectorProps {
  lang: Language;
  selectedTarget: TargetPlatform;
  onSelectTarget: (target: TargetPlatform) => void;
  isOptimizing: boolean;
}

export const TargetPlatformSelector: React.FC<TargetPlatformSelectorProps> = ({
  lang,
  selectedTarget,
  onSelectTarget,
  isOptimizing,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const t = translations[lang];

  const platforms: TargetPlatform[] = [
    'general',
    'facebook',
    'telegram',
    'pubg',
    'mlbb',
    'hok',
    'youtube',
    'tiktok',
  ];

  const currentInfo = targetDetails[selectedTarget];

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (platform: TargetPlatform) => {
    onSelectTarget(platform);
    setIsOpen(false);
  };

  return (
    <div className="relative mb-3.5 z-30" ref={dropdownRef}>
      {/* Compact Dropdown Trigger Button */}
      <div className="bg-[#1e2227] border border-[#2e343b] hover:border-[#3b434d] rounded-2xl p-2.5 sm:p-3 shadow-md transition-all">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          {/* Label & Description */}
          <div className="flex items-center gap-2 min-w-0">
            <div className="w-7 h-7 rounded-xl bg-[#C87547]/15 border border-[#C87547]/40 flex items-center justify-center text-[#C87547] shrink-0">
              <Target className="w-4 h-4" />
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <span className="text-xs font-bold font-mono text-white truncate">
                  {lang === 'EN' ? 'Target App & Routing Mode:' : 'အသုံးပြုမည့် App / Game ရွေးချယ်မှု:'}
                </span>
                {isOptimizing && (
                  <span className="inline-flex items-center gap-1 text-[10px] text-[#f59e0b] font-mono animate-pulse font-bold">
                    <Sparkles className="w-2.5 h-2.5" />
                    <span>AI Calibrating...</span>
                  </span>
                )}
              </div>
              <p className="text-[10px] text-neutral-400 font-mono truncate hidden sm:block">
                {lang === 'EN'
                  ? 'AI auto-routes CDN clusters with latency < 100ms'
                  : 'CDN cluster များသို့ စမ်းသပ်ကာ ping အနည်းဆုံး စီစဉ်ပေးပါသည်'}
              </p>
            </div>
          </div>

          {/* Interactive Dropdown Box */}
          <button
            type="button"
            onClick={() => setIsOpen(!isOpen)}
            disabled={isOptimizing}
            className={`flex items-center justify-between gap-2.5 px-3 py-2 rounded-xl border text-left transition-all w-full sm:w-auto min-w-[210px] sm:min-w-[240px] ${
              isOpen
                ? 'bg-[#252a30] border-[#C87547] text-white ring-1 ring-[#C87547]/30'
                : 'bg-[#252a30] border-[#2e343b] hover:border-[#3b434d] text-neutral-200'
            } disabled:opacity-50 active:scale-98`}
          >
            <div className="flex items-center gap-2 truncate">
              <span className="text-base shrink-0">{currentInfo.icon}</span>
              <div className="truncate">
                <span className="text-xs font-bold font-mono text-white block leading-tight truncate">
                  {lang === 'EN' ? currentInfo.labelEn : currentInfo.labelMm}
                </span>
                <span className="text-[9px] text-[#f59e0b] font-mono block leading-tight truncate">
                  {currentInfo.cdn}
                </span>
              </div>
            </div>

            <ChevronDown
              className={`w-4 h-4 text-[#C87547] shrink-0 transition-transform duration-200 ${
                isOpen ? 'rotate-180' : ''
              }`}
            />
          </button>
        </div>
      </div>

      {/* Dropdown Options Menu Overlay */}
      {isOpen && (
        <div className="absolute top-full left-0 right-0 sm:right-0 sm:left-auto sm:w-[320px] mt-1.5 p-1.5 bg-[#16181b] border border-[#2e343b] rounded-2xl shadow-2xl backdrop-blur-xl z-50 animate-in fade-in slide-in-from-top-2 duration-150 max-h-72 overflow-y-auto">
          <div className="px-2.5 py-1 text-[10px] font-mono font-semibold text-neutral-400 uppercase tracking-wider border-b border-[#2e343b] mb-1">
            {lang === 'EN' ? 'Select Platform for Low-Ping Routing' : 'အကောင်းဆုံး Routing အတွက် ရွေးချယ်ပါ'}
          </div>

          <div className="space-y-1">
            {platforms.map((key) => {
              const info = targetDetails[key];
              const isSelected = selectedTarget === key;

              return (
                <button
                  key={key}
                  type="button"
                  onClick={() => handleSelect(key)}
                  className={`w-full flex items-center justify-between gap-2.5 px-3 py-2 rounded-xl text-left transition-all ${
                    isSelected
                      ? 'bg-[#252a30] text-[#C87547] font-bold border border-[#C87547]/40 shadow-sm'
                      : 'text-neutral-300 hover:bg-[#252a30]/60 hover:text-white border border-transparent'
                  }`}
                >
                  <div className="flex items-center gap-2.5 truncate">
                    <span className="text-base shrink-0">{info.icon}</span>
                    <div className="truncate">
                      <span className="text-xs font-mono font-bold block leading-tight truncate">
                        {lang === 'EN' ? info.labelEn : info.labelMm}
                      </span>
                      <span className="text-[9px] text-neutral-400 font-mono block leading-tight truncate">
                        {info.cdn}
                      </span>
                    </div>
                  </div>

                  {isSelected && (
                    <span className="w-4 h-4 rounded-full bg-[#C87547]/20 border border-[#C87547] flex items-center justify-center text-[#C87547] shrink-0">
                      <Check className="w-2.5 h-2.5" />
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};

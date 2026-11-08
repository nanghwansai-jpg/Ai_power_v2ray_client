import React from 'react';
import { TargetPlatform, Language } from '../types';
import { targetDetails } from '../translations';
import { Bot, Sparkles } from 'lucide-react';

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
  const platforms: TargetPlatform[] = [
    'pubg',
    'mlbb',
    'facebook',
    'telegram',
    'youtube',
    'tiktok',
    'hok',
    'general',
  ];

  return (
    <div className="mb-4 bg-[#1F2226] border border-[#3D434B] rounded-2xl p-4 shadow-[inset_0_1px_0_rgba(255,255,255,0.05),0_6px_16px_rgba(0,0,0,0.5)]">
      {/* AI Prompt Header */}
      <div className="flex items-center justify-between pb-3 mb-3 border-b border-[#2C3035]">
        <div className="flex items-center space-x-2.5">
          <div className="w-8 h-8 rounded-xl bg-[#C87547]/20 border border-[#C87547] flex items-center justify-center text-[#E8C99A] shadow-[0_0_10px_rgba(200,117,71,0.3)]">
            <Bot size={18} />
          </div>
          <div>
            <h3 className="text-xs sm:text-sm font-bold text-[#E8C99A] tracking-wide">
              {lang === 'EN'
                ? 'AI Assistant: "What will you use this VPN for?"'
                : 'AI စနစ်: "VPN ကို ဘာအတွက် အဓိက အသုံးပြုမည်နည်း?"'}
            </h3>
            <p className="text-[10px] text-[#A6ADB8] font-mono">
              {lang === 'EN'
                ? 'Select a target choice below — AI will search, calibrate, and build the best sub-100ms tunnel'
                : 'အောက်ပါ ခလုတ်များထဲမှ ရွေးချယ်ပါ — AI မှ အထူးသင့်လျော်သော Ping အနည်းဆုံး Server ကို တည်ဆောက်ပေးပါမည်'}
            </p>
          </div>
        </div>

        {isOptimizing && (
          <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-full bg-[#7A4A1F]/40 border border-[#E8C99A] text-[10px] text-[#E8C99A] font-mono animate-pulse">
            <Sparkles size={12} className="text-[#E8B274]" />
            <span>AI Building...</span>
          </span>
        )}
      </div>

      {/* Choice Buttons Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
        {platforms.map((key) => {
          const info = targetDetails[key];
          const isSelected = selectedTarget === key;

          return (
            <button
              key={key}
              type="button"
              disabled={isOptimizing}
              onClick={() => onSelectTarget(key)}
              className={`group relative flex items-center space-x-2.5 px-3 py-2.5 rounded-xl border text-left transition-all duration-200 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed ${
                isSelected
                  ? 'bg-gradient-to-r from-[#7A4A1F] to-[#4A2C13] border-[#E8C99A] shadow-[0_0_14px_rgba(232,178,116,0.35)] scale-[1.02]'
                  : 'bg-[#16181B] border-[#3D434B] hover:border-[#C87547] hover:bg-[#202429]'
              }`}
            >
              <span className="text-xl shrink-0 group-hover:scale-110 transition-transform">
                {info.icon}
              </span>
              <div className="min-w-0">
                <span
                  className={`text-xs font-bold block truncate tracking-wide ${
                    isSelected ? 'text-[#E8C99A]' : 'text-[#E6EAF0]'
                  }`}
                >
                  {info.labelEn}
                </span>
                <span className="text-[9px] font-mono block truncate text-[#A6ADB8]">
                  {key === 'pubg' || key === 'mlbb' ? 'Gaming < 35ms' : info.cdn.split(' ')[0]}
                </span>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
};

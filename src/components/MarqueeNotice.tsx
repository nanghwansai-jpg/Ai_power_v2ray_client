import React, { useEffect, useState } from 'react';
import { translations } from '../translations';
import { Language } from '../types';
import { X, RotateCw } from 'lucide-react';

interface MarqueeNoticeProps {
  lang: Language;
  customMessage?: string;
}

export const MarqueeNotice: React.FC<MarqueeNoticeProps> = ({ lang, customMessage }) => {
  const [visible, setVisible] = useState(true);
  const [secondsRemaining, setSecondsRemaining] = useState(120); // 2 minutes auto-hide timer
  const t = translations[lang];

  useEffect(() => {
    const interval = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          setVisible(false);
          clearInterval(interval);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, []);

  const resetTimer = () => {
    setVisible(true);
    setSecondsRemaining(120);
  };

  const displayText = customMessage || t.aiMarqueeText;

  if (!visible) {
    return (
      <div className="max-w-4xl mx-auto px-3 sm:px-6 py-1 flex justify-end">
        <button
          onClick={resetTimer}
          className="text-[10px] font-mono text-neutral-400 hover:text-[#C87547] flex items-center gap-1 transition-colors"
        >
          <RotateCw className="w-2.5 h-2.5 text-[#f59e0b]" />
          <span>{t.showMarquee}</span>
        </button>
      </div>
    );
  }

  const minutes = Math.floor(secondsRemaining / 60);
  const seconds = secondsRemaining % 60;
  const timeFormatted = `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;

  return (
    <aside
      aria-label="AI Live Agent Notice"
      className="border-y border-[#2e343b] bg-[#16181b] py-1.5 px-2 overflow-hidden relative shadow-sm"
    >
      <div className="max-w-4xl mx-auto flex items-center gap-2">
        {/* Live Badge in Azure & Golden */}
        <div className="flex items-center gap-1 px-2 py-0.5 rounded-lg bg-[#C87547]/15 border border-[#C87547]/40 text-[9px] font-mono font-bold text-[#C87547] shrink-0 uppercase tracking-wider">
          <span className="w-1.5 h-1.5 rounded-full bg-[#C87547] animate-ping" />
          <span>AI-PROBE</span>
          <span className="text-[8px] text-[#f59e0b] font-bold ml-0.5">({timeFormatted})</span>
        </div>

        {/* Marquee Text - Smallest font, medium speed right-to-left */}
        <div className="overflow-hidden flex-1 relative whitespace-nowrap mask-fade">
          <div className="inline-block animate-marquee whitespace-nowrap text-[10px] sm:text-[11px] font-mono text-neutral-200 font-medium tracking-tight">
            <span>{displayText}</span>
            <span className="mx-6 text-[#f59e0b] font-bold">///</span>
            <span>{displayText}</span>
          </div>
        </div>

        {/* Manual Dismiss */}
        <button
          onClick={() => setVisible(false)}
          title={t.hideMarquee}
          className="text-neutral-400 hover:text-white p-0.5 rounded transition-colors shrink-0"
        >
          <X className="w-3 h-3" />
        </button>
      </div>
    </aside>
  );
};

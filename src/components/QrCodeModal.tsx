import React, { useEffect, useRef, useState } from 'react';
import QRCode from 'qrcode';
import { XrayNode, Language } from '../types';
import { translations } from '../translations';
import { X, Copy, Check, Smartphone } from 'lucide-react';

interface QrCodeModalProps {
  lang: Language;
  node: XrayNode | null;
  onClose: () => void;
}

export const QrCodeModal: React.FC<QrCodeModalProps> = ({ lang, node, onClose }) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [copied, setCopied] = useState(false);
  const t = translations[lang];

  useEffect(() => {
    if (node && canvasRef.current) {
      QRCode.toCanvas(
        canvasRef.current,
        node.rawKey,
        {
          width: 240,
          margin: 1.5,
          color: {
            dark: '#141619',
            light: '#E8EBEF',
          },
        },
        (error) => {
          if (error) console.error('QR code generation error:', error);
        }
      );
    }
  }, [node]);

  if (!node) return null;

  const handleCopy = () => {
    navigator.clipboard.writeText(node.rawKey);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-sm chamfer-tray-9dp p-5 text-white font-mono shadow-2xl text-center">
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
        <div className="flex items-center justify-center gap-2 mb-2">
          <Smartphone className="w-5 h-5 text-[#C87547]" />
          <h3 className="text-sm sm:text-base font-bold text-incised-strong">
            {t.qrTitle}
          </h3>
        </div>
        <p className="text-[11px] text-neutral-400 mb-4 font-sans leading-relaxed">
          {t.qrSubtitle}
        </p>

        {/* QR Code Canvas Tray */}
        <div className="bg-[#141619] p-3 rounded border border-[#3D434B] shadow-inner inline-block mx-auto mb-4">
          <canvas ref={canvasRef} className="rounded shadow-md max-w-full" />
        </div>

        {/* Node Details Readout */}
        <div className="p-2.5 rounded bg-[#16181B] border border-[#343A42] shadow-inner mb-4 text-left text-xs">
          <div className="flex items-center justify-between mb-1">
            <span className="font-bold text-incised-amber">{node.name}</span>
            <span className="px-1.5 py-0.2 rounded bg-[#2A2118] border border-[#C87547] text-[9px] font-bold text-[#E8C99A] uppercase">
              {node.protocol}
            </span>
          </div>
          <p className="text-[10px] text-neutral-400 font-mono truncate">{node.rawKey}</p>
        </div>

        {/* Action Button: Copy URI Key */}
        <button
          onClick={handleCopy}
          type="button"
          className="w-full keycap-steel keycap-copper-active py-2.5 px-4 rounded text-xs font-mono font-bold text-[#E8C99A] flex items-center justify-center gap-2 cursor-pointer active:scale-95"
        >
          {copied ? <Check className="w-4 h-4 text-[#E8B274]" /> : <Copy className="w-4 h-4 text-[#C87547]" />}
          <span>{copied ? t.copied : t.copyRawConfig}</span>
        </button>
      </div>
    </div>
  );
};

import React, { useEffect, useState } from 'react';
import { XrayNode, Language } from '../types';
import { translations } from '../translations';
import { Power, ArrowDown, ArrowUp, Clock, Gauge } from 'lucide-react';

interface OneClickConnectProps {
  lang: Language;
  selectedNode: XrayNode | null;
  isConnected: boolean;
  isConnecting: boolean;
  onToggleConnect: () => void;
  activePing: number;
}

/* Precision Analog Dial Component with Burnished Copper Needle */
interface AnalogDialProps {
  title: string;
  serialNumber: string;
  value: number;
  unit: string;
  max: number;
  isConnected: boolean;
}

const AnalogDial: React.FC<AnalogDialProps> = ({
  title,
  serialNumber,
  value,
  unit,
  max,
  isConnected,
}) => {
  // Sweep angle between -120deg and +120deg (240deg total sweep)
  const normalizedValue = Math.min(Math.max(value, 0), max);
  const angle = -120 + (normalizedValue / max) * 240;

  return (
    <div className="flex flex-col items-center">
      {/* Outer Machined Bezel Frame */}
      <div className="relative w-20 h-20 sm:w-24 sm:h-24 rounded-full bg-[#181A1D] border-2 border-[#3D434B] p-1 shadow-[inset_0_2px_6px_rgba(0,0,0,0.8),0_4px_10px_rgba(0,0,0,0.5)] flex items-center justify-center">
        {/* Tiny Hex Screws on Dial Flange */}
        <div className="absolute top-1 left-2 w-1 h-1 rounded-full bg-[#525B66] shadow-sm" />
        <div className="absolute top-1 right-2 w-1 h-1 rounded-full bg-[#525B66] shadow-sm" />
        <div className="absolute bottom-1 left-2 w-1 h-1 rounded-full bg-[#525B66] shadow-sm" />
        <div className="absolute bottom-1 right-2 w-1 h-1 rounded-full bg-[#525B66] shadow-sm" />

        {/* Dial Face SVG */}
        <svg viewBox="0 0 100 100" className="w-full h-full select-none">
          <defs>
            {/* Dial Face Metallic Radial Gradient */}
            <radialGradient id="dialFaceGrad" cx="50%" cy="50%" r="50%">
              <stop offset="0%" stopColor="#1C1E22" />
              <stop offset="75%" stopColor="#141619" />
              <stop offset="100%" stopColor="#0E1012" />
            </radialGradient>

            {/* Copper Needle Shadow Filter */}
            <filter id="copperNeedleShadow" x="-20%" y="-20%" width="140%" height="140%">
              <feDropShadow dx="1" dy="2" stdDeviation="1.5" floodColor="#0A0604" floodOpacity="0.9" />
            </filter>
          </defs>

          {/* Background Dial Disk */}
          <circle cx="50" cy="50" r="46" fill="url(#dialFaceGrad)" />

          {/* Outer Scale Arc Track */}
          <path
            d="M 22 78 A 38 38 0 1 1 78 78"
            fill="none"
            stroke="#2B3037"
            strokeWidth="3.5"
            strokeLinecap="round"
          />

          {/* Calibrated Active Copper Arc */}
          <path
            d="M 22 78 A 38 38 0 1 1 78 78"
            fill="none"
            stroke="#C87547"
            strokeWidth="2.5"
            strokeDasharray="160"
            strokeDashoffset={160 - (normalizedValue / max) * 160}
            strokeLinecap="round"
            className="transition-all duration-500 ease-out opacity-85"
          />

          {/* Precision Graduation Ticks */}
          {[0, 24, 48, 72, 96, 120, 144, 168, 192, 216, 240].map((deg, i) => {
            const tickAngle = (-120 + deg) * (Math.PI / 180);
            const isMajor = i % 2 === 0;
            const r1 = isMajor ? 32 : 35;
            const r2 = 39;
            const x1 = 50 + r1 * Math.sin(tickAngle);
            const y1 = 50 - r1 * Math.cos(tickAngle);
            const x2 = 50 + r2 * Math.sin(tickAngle);
            const y2 = 50 - r2 * Math.cos(tickAngle);
            return (
              <line
                key={i}
                x1={x1}
                y1={y1}
                x2={x2}
                y2={y2}
                stroke={isMajor ? '#C87547' : '#525B66'}
                strokeWidth={isMajor ? '1.5' : '0.8'}
              />
            );
          })}

          {/* Unit Tag on Dial */}
          <text
            x="50"
            y="68"
            textAnchor="middle"
            fill="#8B949E"
            fontSize="6.5"
            fontFamily="'Roboto Mono', monospace"
            fontWeight="bold"
            letterSpacing="0.5px"
          >
            {unit}
          </text>

          {/* Burnished Copper Needle Hand (#C87547) */}
          <g
            style={{
              transform: `rotate(${isConnected ? angle : -120}deg)`,
              transformOrigin: '50px 50px',
              transition: 'transform 0.45s cubic-bezier(0.2, 0.85, 0.25, 1)',
            }}
            filter="url(#copperNeedleShadow)"
          >
            {/* Tapered Copper Needle Blade */}
            <polygon
              points="48.8,50 51.2,50 50,15"
              fill="#C87547"
            />
            {/* Highlight Ridge along needle center */}
            <line x1="50" y1="16" x2="50" y2="48" stroke="#E8C99A" strokeWidth="0.6" opacity="0.8" />
            {/* Counterweight Spade */}
            <circle cx="50" cy="56" r="2.8" fill="#7A4A1F" />
          </g>

          {/* Center Pivot Hub (Brushed Steel Cap with Copper Center) */}
          <circle cx="50" cy="50" r="4.5" fill="#2E333A" stroke="#525B66" strokeWidth="1" />
          <circle cx="50" cy="50" r="2" fill="#E8C99A" />
        </svg>

        {/* Ambient Amber Glow dot when active */}
        {isConnected && (
          <span className="absolute top-1.5 w-1.5 h-1.5 rounded-full bg-[#E8B274] shadow-[0_0_6px_#E8B274]" />
        )}
      </div>

      {/* Meter Digital Readout & Serial Plate Label */}
      <div className="mt-1.5 text-center">
        <span className="text-[9px] font-mono font-bold uppercase tracking-wider text-incised block">
          {title}
        </span>
        <span className="text-xs sm:text-sm font-mono font-black text-incised-amber">
          {isConnected ? value : '---'}{' '}
          <span className="text-[9px] font-normal text-neutral-400 font-mono">{unit}</span>
        </span>
      </div>
    </div>
  );
};

export const OneClickConnect: React.FC<OneClickConnectProps> = ({
  lang,
  selectedNode,
  isConnected,
  isConnecting,
  onToggleConnect,
  activePing,
}) => {
  const t = translations[lang];

  // Dynamic telemetry metrics
  const [downloadSpeed, setDownloadSpeed] = useState<number>(0);
  const [uploadSpeed, setUploadSpeed] = useState<number>(0);
  const [duration, setDuration] = useState<number>(0);

  useEffect(() => {
    let interval: any = null;
    if (isConnected) {
      interval = setInterval(() => {
        setDuration((prev) => prev + 1);
        const baseDown = selectedNode ? parseInt(selectedNode.speedDown) || 45 : 45;
        const baseUp = selectedNode ? parseInt(selectedNode.speedUp) || 18 : 18;
        setDownloadSpeed(Math.max(12, Math.floor(baseDown + (Math.random() * 14 - 7))));
        setUploadSpeed(Math.max(5, Math.floor(baseUp + (Math.random() * 8 - 4))));
      }, 1000);
    } else {
      setDuration(0);
      setDownloadSpeed(0);
      setUploadSpeed(0);
    }
    return () => clearInterval(interval);
  }, [isConnected, selectedNode]);

  const formatDuration = (sec: number) => {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const downloadMbps = isConnected ? downloadSpeed : 0;

  return (
    <div className="chamfer-tray-9dp p-4 sm:p-5 relative mb-3.5">
      {/* Corner Hex Screws for Factory Serial Plate */}
      <div className="absolute top-2 left-2 hex-screw" />
      <div className="absolute top-2 right-2 hex-screw" />
      <div className="absolute bottom-2 left-2 hex-screw" />
      <div className="absolute bottom-2 right-2 hex-screw" />

      {/* Section Header Serial Tag */}
      <div className="flex items-center justify-between pl-2 pr-2 mb-3">
        <span className="text-[9px] font-mono font-bold tracking-widest text-incised uppercase">
          PANEL-02 // CORE POWER & TELEMETRY CLUSTER
        </span>
        <div className="flex items-center gap-1.5">
          <span className="text-[9px] font-mono text-neutral-400">STATUS:</span>
          <span
            className={`text-[9px] font-mono font-bold px-1.5 py-0.2 rounded border ${
              isConnected
                ? 'bg-[#2A2118] border-[#C87547] text-[#E8C99A]'
                : 'bg-[#221818] border-[#742A2A] text-[#E57373]'
            }`}
          >
            {isConnected ? 'ONLINE' : 'STANDBY'}
          </span>
        </div>
      </div>

      {/* Trio Cluster: Left Analog Dial + Center Power Key-Cap + Right Analog Dial */}
      <div className="flex items-center justify-around gap-2 sm:gap-4 my-2">
        {/* 1. LEFT: PRECISION ANALOG LATENCY DIAL (Copper Needle) */}
        <AnalogDial
          title="LATENCY"
          serialNumber="GAUGE-01"
          value={activePing || 0}
          unit="ms"
          max={150}
          isConnected={isConnected}
        />

        {/* 2. CENTER: BRUSHED-STEEL POWER KEY-CAP (Warm Copper Under-Glow) */}
        <div className="flex flex-col items-center">
          <div className="relative">
            {/* Warm Copper Under-Glow Aura when Active or Connecting */}
            {(isConnected || isConnecting) && (
              <div
                className={`absolute -inset-4 rounded-full transition-all duration-500 pointer-events-none ${
                  isConnecting
                    ? 'border-2 border-[#E8B274]/70 animate-ping'
                    : 'bg-[#C87547]/20 border-2 border-[#C87547]/50 shadow-[0_0_30px_rgba(200,117,71,0.5)] animate-pulse'
                }`}
              />
            )}

            <button
              onClick={onToggleConnect}
              disabled={isConnecting}
              type="button"
              className={`relative flex items-center justify-center w-24 h-24 sm:w-28 sm:h-28 rounded-full transition-all duration-300 transform active:scale-95 cursor-pointer select-none keycap-steel ${
                isConnected
                  ? 'keycap-copper-active scale-105'
                  : isConnecting
                  ? 'border-[#E8B274] animate-pulse'
                  : ''
              }`}
              style={{
                boxShadow: isConnected
                  ? '0 0 28px rgba(200, 117, 71, 0.55), 0 0 10px rgba(232, 178, 116, 0.4), inset 0 2px 0 #E8C99A, 0 8px 16px rgba(0,0,0,0.8)'
                  : '0 6px 14px rgba(0,0,0,0.7), inset 0 1px 0 rgba(255,255,255,0.15)',
              }}
            >
              {/* Concentric knurled ring inside button */}
              <div className="w-18 h-18 sm:w-20 sm:h-20 rounded-full border border-[#444A53] flex items-center justify-center bg-gradient-to-b from-[#2A2E34] to-[#16181B] shadow-inner">
                <Power
                  className={`w-9 h-9 sm:w-11 sm:h-11 transition-all ${
                    isConnected
                      ? 'text-[#E8C99A] drop-shadow-[0_0_12px_rgba(232,178,116,0.8)]'
                      : isConnecting
                      ? 'text-[#E8B274] animate-pulse'
                      : 'text-neutral-400 hover:text-neutral-200'
                  }`}
                />
              </div>
            </button>
          </div>

          {/* Power State Label (Incised Gunmetal) */}
          <div className="mt-2.5 text-center">
            <p className="text-[11px] sm:text-xs font-mono font-bold tracking-widest uppercase text-incised">
              {isConnecting
                ? 'CONNECTING...'
                : isConnected
                ? 'CONNECTED // ONLINE'
                : 'TAP TO CONNECT'}
            </p>
          </div>
        </div>

        {/* 3. RIGHT: PRECISION ANALOG SPEED DIAL (Copper Needle) */}
        <AnalogDial
          title="SPEED RATE"
          serialNumber="GAUGE-02"
          value={downloadMbps || 0}
          unit="Mbps"
          max={100}
          isConnected={isConnected}
        />
      </div>

      {/* Real-time Connection Metrics Drawer (when Connected) */}
      {isConnected && (
        <div className="grid grid-cols-3 gap-2 max-w-md mx-auto mt-3.5 pt-3 border-t border-[#3D434B] font-mono animate-in fade-in">
          <div className="p-2 rounded bg-[#16181B] border border-[#3D434B] text-left shadow-inner">
            <div className="flex items-center gap-1 text-[9px] text-neutral-400 mb-0.5">
              <ArrowDown className="w-3 h-3 text-[#C87547]" />
              <span className="text-incised">DOWN</span>
            </div>
            <p className="text-xs font-bold text-incised-amber">
              {downloadSpeed > 1000
                ? `${(downloadSpeed / 1024).toFixed(1)} MB/s`
                : `${downloadSpeed} KB/s`}
            </p>
          </div>

          <div className="p-2 rounded bg-[#16181B] border border-[#3D434B] text-left shadow-inner">
            <div className="flex items-center gap-1 text-[9px] text-neutral-400 mb-0.5">
              <ArrowUp className="w-3 h-3 text-[#E8B274]" />
              <span className="text-incised">UP</span>
            </div>
            <p className="text-xs font-bold text-incised-amber">
              {uploadSpeed > 1000
                ? `${(uploadSpeed / 1024).toFixed(1)} MB/s`
                : `${uploadSpeed} KB/s`}
            </p>
          </div>

          <div className="p-2 rounded bg-[#16181B] border border-[#3D434B] text-left shadow-inner">
            <div className="flex items-center gap-1 text-[9px] text-neutral-400 mb-0.5">
              <Clock className="w-3 h-3 text-[#E8C99A]" />
              <span className="text-incised">TIME</span>
            </div>
            <p className="text-xs font-bold text-incised-amber">
              {formatDuration(duration)}
            </p>
          </div>
        </div>
      )}

      {/* BOTTOM HEARTBEAT OSCILLOSCOPE WAVE & DEVELOPER SIGNATURE (Burnished Copper / Amber Neon Pulse) */}
      <div className="mt-4 pt-3 border-t border-[#3D434B] flex flex-col items-center">
        {/* Precision Oscilloscope Screen Bezel */}
        <div className="relative w-full max-w-sm h-12 overflow-hidden rounded-[6px] bg-[#121417] border border-[#3D434B] shadow-inner px-2 flex items-center">
          {/* Oscilloscope Grid Reticle */}
          <div
            className="absolute inset-0 opacity-15 pointer-events-none"
            style={{
              backgroundImage:
                'linear-gradient(to right, #C87547 1px, transparent 1px), linear-gradient(to bottom, #C87547 1px, transparent 1px)',
              backgroundSize: '16px 12px',
            }}
          />

          {/* SVG Heartbeat Wave with Passing Warm Copper/Amber Neon Pulse */}
          <svg
            className="relative z-10 w-full h-full"
            viewBox="0 0 600 50"
            preserveAspectRatio="none"
            fill="none"
          >
            <defs>
              {/* Baseline track in faint copper/steel */}
              <linearGradient id="osc-base" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" stopColor="#7A4A1F" stopOpacity="0.3" />
                <stop offset="50%" stopColor="#C87547" stopOpacity="0.4" />
                <stop offset="100%" stopColor="#7A4A1F" stopOpacity="0.3" />
              </linearGradient>

              {/* Passing Neon Pulse in Warm Burnished Copper & Amber */}
              <linearGradient id="osc-pulse" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" stopColor="#7A4A1F" />
                <stop offset="40%" stopColor="#C87547" />
                <stop offset="75%" stopColor="#E8B274" />
                <stop offset="95%" stopColor="#E8C99A" />
                <stop offset="100%" stopColor="#FFFFFF" />
              </linearGradient>

              {/* Copper Glow Filter */}
              <filter id="osc-glow" x="-20%" y="-40%" width="140%" height="180%">
                <feGaussianBlur stdDeviation="2.5" result="blur" />
                <feMerge>
                  <feMergeNode in="blur" />
                  <feMergeNode in="SourceGraphic" />
                </feMerge>
              </filter>
            </defs>

            {/* 1. Ambient ECG Track */}
            <path
              d="M 0 25 L 180 25 C 190 25 196 28 202 28 C 208 28 214 18 220 18 C 226 18 232 25 238 25 L 246 25 L 252 31 L 264 4 L 276 46 L 284 25 L 296 25 C 306 25 316 15 328 15 C 340 15 350 25 360 25 L 600 25"
              stroke="url(#osc-base)"
              strokeWidth="1.6"
              strokeLinecap="round"
              strokeLinejoin="round"
            />

            {/* 2. Passing Copper/Amber Neon Pulse (Sweeping along ECG path) */}
            <path
              d="M 0 25 L 180 25 C 190 25 196 28 202 28 C 208 28 214 18 220 18 C 226 18 232 25 238 25 L 246 25 L 252 31 L 264 4 L 276 46 L 284 25 L 296 25 C 306 25 316 15 328 15 C 340 15 350 25 360 25 L 600 25"
              stroke="url(#osc-pulse)"
              strokeWidth="3.2"
              strokeLinecap="round"
              strokeLinejoin="round"
              filter="url(#osc-glow)"
              className="animate-copper-pulse"
              strokeDasharray="150 600"
            />

            {/* 3. White-Hot Incandescent Core Beam */}
            <path
              d="M 0 25 L 180 25 C 190 25 196 28 202 28 C 208 28 214 18 220 18 C 226 18 232 25 238 25 L 246 25 L 252 31 L 264 4 L 276 46 L 284 25 L 296 25 C 306 25 316 15 328 15 C 340 15 350 25 360 25 L 600 25"
              stroke="#FFFFFF"
              strokeWidth="1.4"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="animate-copper-pulse"
              strokeDasharray="70 680"
            />

            {/* 4. Radiant Laser Flare traversing along the curve */}
            <g>
              <animateMotion
                dur="2.8s"
                repeatCount="indefinite"
                path="M 0 25 L 180 25 C 190 25 196 28 202 28 C 208 28 214 18 220 18 C 226 18 232 25 238 25 L 246 25 L 252 31 L 264 4 L 276 46 L 284 25 L 296 25 C 306 25 316 15 328 15 C 340 15 350 25 360 25 L 600 25"
              />
              <circle r="6" fill="#C87547" opacity="0.9" />
              <circle r="3" fill="#E8C99A" />
              <circle r="1.5" fill="#FFFFFF" />
            </g>
          </svg>
        </div>

        {/* Text incised into factory serial plate: DEVELOPE BY VICTOR GEEK */}
        <div className="mt-2 flex items-center justify-center gap-2">
          <div className="plate-rivet" />
          <span className="text-[11px] font-mono font-black tracking-widest uppercase text-incised-copper select-none">
            DEVELOPE BY VICTOR GEEK
          </span>
          <div className="plate-rivet" />
        </div>
      </div>
    </div>
  );
};

import React, { useState } from 'react';
import wordmarkPng from '../assets/images/ai_v2ray_client_wordmark_3x.png';

interface CopperWordmarkProps {
  className?: string;
  preferPng?: boolean;
}

export const CopperWordmark: React.FC<CopperWordmarkProps> = ({
  className = '',
  preferPng = true,
}) => {
  const [imgFailed, setImgFailed] = useState(false);

  return (
    <div className={`relative inline-flex items-center select-none ${className}`}>
      {/* 3× PNG Asset exported from reference mockup */}
      {preferPng && !imgFailed ? (
        <img
          src={wordmarkPng}
          alt="Ai V2Ray Client"
          className="h-7 sm:h-8 w-auto object-contain rounded-xs shadow-[0_0_12px_rgba(232,178,116,0.35)]"
          onError={() => setImgFailed(true)}
          style={{
            filter: 'drop-shadow(0 0 8px rgba(232, 178, 116, 0.35))',
          }}
        />
      ) : (
        /* Mathematical 3D Extruded Brushed Steel Wordmark Recolored to Burnished Copper */
        <svg
          viewBox="0 0 280 44"
          className="h-7 sm:h-8 w-auto overflow-visible"
          style={{
            filter: 'drop-shadow(0 0 10px rgba(232, 178, 116, 0.35))',
          }}
        >
          <defs>
            {/* Burnished Copper Front Gradient: #C87547 base with highlight cap #E8C99A */}
            <linearGradient id="copperFrontGrad" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#E8C99A" />
              <stop offset="22%" stopColor="#D98A58" />
              <stop offset="65%" stopColor="#C87547" />
              <stop offset="100%" stopColor="#A85C32" />
            </linearGradient>

            {/* Side Bevel Gradient: #7A4A1F with depth */}
            <linearGradient id="copperSideGrad" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#8A5323" />
              <stop offset="50%" stopColor="#7A4A1F" />
              <stop offset="100%" stopColor="#4A2B10" />
            </linearGradient>

            {/* Brushed steel striation pattern */}
            <pattern id="brushedSteelPattern" width="6" height="2" patternUnits="userSpaceOnUse">
              <line x1="0" y1="0" x2="6" y2="0" stroke="rgba(255,255,255,0.08)" strokeWidth="0.5" />
              <line x1="0" y1="1" x2="6" y2="1" stroke="rgba(0,0,0,0.15)" strokeWidth="0.5" />
            </pattern>
          </defs>

          {/* 1. Deepest 3D Cast Shadow */}
          <text
            x="3"
            y="34"
            fontFamily="'Inter', sans-serif"
            fontWeight="900"
            fontSize="24"
            letterSpacing="-0.5px"
            fill="#1A0E06"
            opacity="0.85"
          >
            Ai V2Ray Client
          </text>

          {/* 2. Side-bevel 3D Extrusion Layer 3 (deep #4A2B10) */}
          <text
            x="2"
            y="33"
            fontFamily="'Inter', sans-serif"
            fontWeight="900"
            fontSize="24"
            letterSpacing="-0.5px"
            fill="#4A2B10"
          >
            Ai V2Ray Client
          </text>

          {/* 3. Side-bevel 3D Extrusion Layer 2 (#7A4A1F) */}
          <text
            x="1"
            y="32"
            fontFamily="'Inter', sans-serif"
            fontWeight="900"
            fontSize="24"
            letterSpacing="-0.5px"
            fill="#7A4A1F"
          >
            Ai V2Ray Client
          </text>

          {/* 4. Side-bevel 3D Extrusion Layer 1 (#7A4A1F) */}
          <text
            x="0.5"
            y="31"
            fontFamily="'Inter', sans-serif"
            fontWeight="900"
            fontSize="24"
            letterSpacing="-0.5px"
            fill="#8F5726"
          >
            Ai V2Ray Client
          </text>

          {/* 5. Front Face Burnished Copper (#C87547) */}
          <text
            x="0"
            y="30"
            fontFamily="'Inter', sans-serif"
            fontWeight="900"
            fontSize="24"
            letterSpacing="-0.5px"
            fill="url(#copperFrontGrad)"
            stroke="#7A4A1F"
            strokeWidth="0.6"
          >
            Ai V2Ray Client
          </text>

          {/* 6. Brushed Striation Texture Overlay */}
          <text
            x="0"
            y="30"
            fontFamily="'Inter', sans-serif"
            fontWeight="900"
            fontSize="24"
            letterSpacing="-0.5px"
            fill="url(#brushedSteelPattern)"
            opacity="0.4"
          >
            Ai V2Ray Client
          </text>

          {/* 7. Highlight Cap Rim (#E8C99A) */}
          <text
            x="0"
            y="29.4"
            fontFamily="'Inter', sans-serif"
            fontWeight="900"
            fontSize="24"
            letterSpacing="-0.5px"
            fill="none"
            stroke="#E8C99A"
            strokeWidth="0.45"
            opacity="0.9"
          >
            Ai V2Ray Client
          </text>
        </svg>
      )}
    </div>
  );
};

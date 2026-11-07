import React, { useState } from 'react';
import { X, Copy, Check, Terminal, Download, FileCode } from 'lucide-react';

interface ComposeCodeModalProps {
  onClose: () => void;
}

export const ComposeCodeModal: React.FC<ComposeCodeModalProps> = ({ onClose }) => {
  const [copied, setCopied] = useState(false);

  const kotlinCode = `// AI V2Ray Client - Industrial Precision-Instrument UI in Jetpack Compose
// Engineered by Victor Geek
package com.victorgeek.aiv2rayclient.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

object IndustrialTheme {
    val BrushedSteelBackground = Color(0xFF2C3035)
    val GunmetalChassis = Color(0xFF1F2226)
    val RecessedTrayBackground = Color(0xFF16181B)
    val MachinedBorder = Color(0xFF3D434B)
    val CopperFront = Color(0xFFC87547)
    val CopperSideBevel = Color(0xFF7A4A1F)
    val CopperHighlightCap = Color(0xFFE8C99A)
    val AmberHalo = Color(0x59E8B274) // 35% opacity
    val AmberGlow = Color(0xFFE8B274)
}

// 9dp Chamfered Bevel Shape
val ChamferShape9dp = GenericShape { size, _ ->
    val chamfer = 9.dp.value * 2.75f
    moveTo(chamfer, 0f)
    lineTo(size.width - chamfer, 0f)
    lineTo(size.width, chamfer)
    lineTo(size.width, size.height - chamfer)
    lineTo(size.width - chamfer, size.height)
    lineTo(chamfer, size.height)
    lineTo(0f, size.height - chamfer)
    lineTo(0f, chamfer)
    close()
}

@Composable
fun AiV2RayClientApp() {
    var isConnected by remember { mutableStateOf(false) }
    var activePing by remember { mutableStateOf(28) }
    var speedMbps by remember { mutableStateOf(52f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IndustrialTheme.BrushedSteelBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title: 'Ai V2Ray Client' 3D-extruded Burnished Copper Wordmark
            Text(
                text = "Ai V2Ray Client",
                color = IndustrialTheme.CopperFront,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(
                    shadow = Shadow(
                        color = IndustrialTheme.AmberHalo,
                        offset = Offset(0f, 2f),
                        blurRadius = 12f
                    )
                )
            )

            // Analog Gauges with Copper Needles & Power Switch
            // Complete source in /src/android/AiV2RayClientScreen.kt
        }
    }
}`;

  const handleCopy = () => {
    navigator.clipboard.writeText(kotlinCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownload = () => {
    const blob = new Blob([kotlinCode], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'AiV2RayClientScreen.kt';
    link.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-5 bg-black/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-3xl max-h-[90vh] flex flex-col chamfer-tray-9dp p-4 sm:p-6 text-neutral-100 font-mono shadow-2xl">
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

        {/* Header */}
        <div className="flex items-center gap-2.5 mb-3 pl-2 pr-2">
          <div className="w-8 h-8 rounded-[4px] bg-[#141619] border border-[#3D434B] flex items-center justify-center text-[#C87547] shadow-inner">
            <FileCode className="w-4 h-4 text-[#C87547]" />
          </div>
          <div>
            <span className="text-[9px] font-mono text-neutral-500 uppercase tracking-widest block">
              ANDROID JETPACK COMPOSE SOURCE CODE
            </span>
            <h3 className="text-sm sm:text-base font-black text-incised-strong">
              AiV2RayClientScreen.kt
            </h3>
          </div>
        </div>

        {/* Code Content Window */}
        <div className="flex-1 min-h-[300px] overflow-y-auto bg-[#101214] border border-[#3D434B] rounded p-3 text-[11px] font-mono text-[#D1D5DB] leading-relaxed shadow-inner">
          <pre className="whitespace-pre">{kotlinCode}</pre>
        </div>

        {/* Footer Actions */}
        <div className="flex items-center justify-between gap-3 mt-3 pt-3 border-t border-[#3D434B] pl-2 pr-2">
          <span className="text-[10px] text-neutral-500">
            Source path: /src/android/AiV2RayClientScreen.kt
          </span>
          <div className="flex items-center gap-2">
            <button
              onClick={handleDownload}
              type="button"
              className="keycap-steel flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono font-bold text-neutral-300 hover:text-white cursor-pointer active:scale-95"
            >
              <Download className="w-3.5 h-3.5 text-[#C87547]" />
              <span>DOWNLOAD .KT</span>
            </button>
            <button
              onClick={handleCopy}
              type="button"
              className="keycap-steel keycap-copper-active flex items-center gap-1.5 px-3.5 py-1.5 rounded text-xs font-mono font-bold text-[#E8C99A] cursor-pointer active:scale-95"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-[#E8B274]" /> : <Copy className="w-3.5 h-3.5 text-[#C87547]" />}
              <span>{copied ? 'COPIED TO CLIPBOARD' : 'COPY KOTLIN CODE'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

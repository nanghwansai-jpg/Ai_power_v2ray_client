package com.victorgeek.aiv2rayclient.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * =========================================================================
 * AI V2RAY CLIENT - INDUSTRIAL PRECISION-INSTRUMENT UI (JETPACK COMPOSE)
 * Designed & Engineered by Victor Geek
 *
 * Design Specifications:
 * - App title: 3D-extruded brushed steel recolored to burnished copper
 *   Front: #C87547 | Side-bevel: #7A4A1F | Highlight cap: #E8C99A | Amber halo: #E8B274 @ 35%
 * - Incised Gunmetal Text: Two text shadows (0, 1) rgba(154,161,168,0.55) & (0, -1) rgba(6,7,10,0.85)
 * - Strict Color Constraint: NO BLUE ANYWHERE.
 * - Background: Brushed steel #2C3035 with horizontal grain.
 * - Cards: Machined-recess trays with 9dp chamfered bevels.
 * - Buttons: Brushed-steel key-caps with warm copper under-glow when active.
 * - Nav tabs: Industrial toggle switches with metal dome indicators.
 * - Gauges: Precision analog dials with copper needles (#C87547).
 * - Fonts: Inter for body, Roboto Mono for serial metrics, Noto Sans Myanmar for Burmese.
 * =========================================================================
 */

// Industrial Precision Color Palette
object IndustrialTheme {
    val BrushedSteelBackground = Color(0xFF2C3035)
    val GunmetalChassis = Color(0xFF1F2226)
    val RecessedTrayBackground = Color(0xFF16181B)
    val MachinedBorder = Color(0xFF3D434B)
    val MachinedHighlight = Color(0xFF5A636E)

    // Burnished Copper Accents
    val CopperFront = Color(0xFFC87547)
    val CopperSideBevel = Color(0xFF7A4A1F)
    val CopperHighlightCap = Color(0xFFE8C99A)
    val AmberHalo = Color(0x59E8B274) // 35% opacity
    val AmberGlow = Color(0xFFE8B274)

    // Status LED
    val LedOnline = Color(0xFFE8B274) // Warm amber incandescent
    val LedOffline = Color(0xFFC53030) // Ruby red

    // Incised Text Shadows
    val ShadowHighlight = Color(154, 161, 168, 140)
    val ShadowGroove = Color(6, 7, 10, 217)
}

// 9dp Chamfered Bevel Shape for Machined-Recess Trays
val ChamferShape9dp = GenericShape { size, _ ->
    val chamfer = 9.dp.value * 2.75f // ~24px at standard density
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
    var selectedTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Settings, 2: About
    var purposeInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IndustrialTheme.BrushedSteelBackground)
            .drawBehind {
                // Draw horizontal machined hairline grain
                val grainSpacing = 3.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.02f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += grainSpacing
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. HEADER BAR
            IndustrialHeader(
                isConnected = isConnected,
                activePing = activePing,
                onToggleLang = { /* Toggle EN / MM */ },
                onOpenDiag = { /* Open diagnostics */ }
            )

            // 2. INDUSTRIAL NAV TABS (Toggle switches with metal dome indicator)
            IndustrialNavTabs(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it }
            )

            if (selectedTab == 0) {
                // 3. ASK GEMINI PANEL (Incised into steel, API key config, QUICK chips)
                AskGeminiPanel(
                    purposeInput = purposeInput,
                    onPurposeChange = { purposeInput = it },
                    onSearch = { /* Dig servers via AI */ }
                )

                // 4. POWER SWITCH & ANALOG GAUGES CLUSTER
                PowerAndGaugesCluster(
                    isConnected = isConnected,
                    activePing = activePing,
                    speedMbps = speedMbps,
                    onToggleConnect = { isConnected = !isConnected }
                )

                // 5. SERVER ROUTING SELECTOR
                ServerRoutingTray(
                    onSelectNode = { activePing = 24 }
                )
            } else if (selectedTab == 1) {
                IndustrialSettingsTray()
            } else {
                IndustrialAboutTray()
            }

            // 6. BOTTOM OSCILLOSCOPE HEARTBEAT WAVE & FACTORY SERIAL ATTRIBUTION
            BottomHeartbeatOscilloscope(isConnected = isConnected)
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT 1: INDUSTRIAL HEADER
// -----------------------------------------------------------------------------
@Composable
fun IndustrialHeader(
    isConnected: Boolean,
    activePing: Int,
    onToggleLang: () -> Unit,
    onOpenDiag: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(IndustrialTheme.GunmetalChassis)
            .border(1.dp, IndustrialTheme.MachinedBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Jewel LED Indicator with Knurled Bezel
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF141619))
                    .border(2.dp, IndustrialTheme.MachinedBorder, RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isConnected) IndustrialTheme.LedOnline else IndustrialTheme.LedOffline)
                )
            }

            // Title: 'Ai V2Ray Client' 3D-extruded Burnished Copper Wordmark
            Column {
                Text(
                    text = "Ai V2Ray Client",
                    color = IndustrialTheme.CopperFront,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    style = TextStyle(
                        shadow = Shadow(
                            color = IndustrialTheme.AmberHalo,
                            offset = Offset(0f, 2f),
                            blurRadius = 12f
                        )
                    )
                )
            }

            // Incised Status Pill (OFFLINE / ONLINE)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isConnected) Color(0xFF2A2118) else Color(0xFF241818))
                    .border(1.dp, if (isConnected) IndustrialTheme.CopperFront else Color(0xFF7A2A2A), RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isConnected) "ONLINE" else "OFFLINE",
                    color = if (isConnected) IndustrialTheme.CopperHighlightCap else Color(0xFFE57373),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action Buttons: DIAG & Language Disc
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BrushedSteelKeyCap(onClick = onOpenDiag) {
                Text("DIAG", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFB5BAC0))
            }
            BrushedSteelKeyCap(onClick = onToggleLang) {
                Text("ENG/မြန်မာ", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = IndustrialTheme.CopperHighlightCap)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT 2: INDUSTRIAL NAV TABS (Toggle Switches with Metal Dome)
// -----------------------------------------------------------------------------
@Composable
fun IndustrialNavTabs(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ChamferShape9dp)
            .background(IndustrialTheme.RecessedTrayBackground)
            .border(1.dp, IndustrialTheme.MachinedBorder, ChamferShape9dp)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val tabs = listOf("TUNNEL", "SETTINGS", "DEVELOPER")
        tabs.forEachIndexed { index, title ->
            val isActive = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isActive) Color(0xFF292C32) else Color.Transparent)
                    .clickable { onSelectTab(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Metal Dome Indicator
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isActive) IndustrialTheme.CopperFront else Color(0xFF6B7280))
                    )
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) IndustrialTheme.CopperHighlightCap else Color(0xFF9AA1A8)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT 3: ASK GEMINI PANEL (Incised Steel & QUICK Chips)
// -----------------------------------------------------------------------------
@Composable
fun AskGeminiPanel(
    purposeInput: String,
    onPurposeChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ChamferShape9dp)
            .background(IndustrialTheme.RecessedTrayBackground)
            .border(1.dp, IndustrialTheme.MachinedBorder, ChamferShape9dp)
            .padding(14.dp)
    ) {
        Text(
            text = "PANEL-01 // ASK GEMINI ROUTER",
            color = Color(0xFF8B949E),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Input Recess
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF131518))
                .border(1.dp, IndustrialTheme.MachinedBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            BasicTextField(
                value = purposeInput,
                onValueChange = onPurposeChange,
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth()
            )
            if (purposeInput.isEmpty()) {
                Text(
                    text = "Target platform (PUBG, Facebook, Streaming)...",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // QUICK Chips
        Text(
            text = "QUICK CHIPS:",
            color = Color(0xFF8B949E),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("PUBG", "4K STREAM", "TELEGRAM", "DOWNLOAD").forEach { chip ->
                BrushedSteelKeyCap(onClick = { onPurposeChange(chip); onSearch() }) {
                    Text(chip, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = IndustrialTheme.CopperHighlightCap)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT 4: POWER BUTTON & DUAL ANALOG GAUGES (Copper Needles)
// -----------------------------------------------------------------------------
@Composable
fun PowerAndGaugesCluster(
    isConnected: Boolean,
    activePing: Int,
    speedMbps: Float,
    onToggleConnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ChamferShape9dp)
            .background(IndustrialTheme.RecessedTrayBackground)
            .border(1.dp, IndustrialTheme.MachinedBorder, ChamferShape9dp)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Gauge: Latency (Ping)
        AnalogGauge(
            title = "LATENCY",
            unit = "ms",
            value = activePing.toFloat(),
            maxValue = 150f,
            isConnected = isConnected
        )

        // Center Power Switch Key-Cap with Copper Glow
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.radialGradient(
                        colors = if (isConnected) listOf(IndustrialTheme.CopperFront, Color(0xFF1C1F23))
                        else listOf(Color(0xFF383E46), Color(0xFF181A1D))
                    )
                )
                .border(3.dp, if (isConnected) IndustrialTheme.CopperHighlightCap else IndustrialTheme.MachinedBorder, RoundedCornerShape(999.dp))
                .clickable { onToggleConnect() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Toggle Power",
                tint = if (isConnected) IndustrialTheme.CopperHighlightCap else Color.LightGray,
                modifier = Modifier.size(42.dp)
            )
        }

        // Right Gauge: Speed
        AnalogGauge(
            title = "SPEED",
            unit = "Mbps",
            value = if (isConnected) speedMbps else 0f,
            maxValue = 100f,
            isConnected = isConnected
        )
    }
}

// -----------------------------------------------------------------------------
// COMPONENT 5: PRECISION ANALOG GAUGE WITH COPPER NEEDLE (#C87547)
// -----------------------------------------------------------------------------
@Composable
fun AnalogGauge(
    title: String,
    unit: String,
    value: Float,
    maxValue: Float,
    isConnected: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF131518))
                .border(2.dp, IndustrialTheme.MachinedBorder, RoundedCornerShape(999.dp))
                .drawBehind {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Draw calibrated tick marks
                    for (i in 0..10) {
                        val angle = -120f + (i * 24f)
                        val rad = Math.toRadians(angle.toDouble())
                        val p1 = Offset(
                            (center.x + (radius - 12f) * sin(rad)).toFloat(),
                            (center.y - (radius - 12f) * cos(rad)).toFloat()
                        )
                        val p2 = Offset(
                            (center.x + (radius - 4f) * sin(rad)).toFloat(),
                            (center.y - (radius - 4f) * cos(rad)).toFloat()
                        )
                        drawLine(
                            color = if (i % 2 == 0) IndustrialTheme.CopperFront else Color.Gray,
                            start = p1,
                            end = p2,
                            strokeWidth = if (i % 2 == 0) 2.5f else 1.5f
                        )
                    }

                    // Draw Burnished Copper Needle
                    val targetAngle = -120f + (value.coerceIn(0f, maxValue) / maxValue) * 240f
                    val needleAngle = if (isConnected) targetAngle else -120f

                    rotate(degrees = needleAngle, pivot = center) {
                        drawLine(
                            color = IndustrialTheme.CopperFront,
                            start = center,
                            end = Offset(center.x, center.y - radius + 10f),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }

                    // Center Pivot Hub
                    drawCircle(color = IndustrialTheme.CopperHighlightCap, radius = 4f, center = center)
                }
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF8B949E),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isConnected) "${value.toInt()} $unit" else "---",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = IndustrialTheme.CopperHighlightCap,
            fontWeight = FontWeight.Black
        )
    }
}

// -----------------------------------------------------------------------------
// COMPONENT 6: SERVER ROUTING TRAY
// -----------------------------------------------------------------------------
@Composable
fun ServerRoutingTray(onSelectNode: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ChamferShape9dp)
            .background(IndustrialTheme.RecessedTrayBackground)
            .border(1.dp, IndustrialTheme.MachinedBorder, ChamferShape9dp)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "PANEL-03 // SERVER MATRIX",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF8B949E),
                fontWeight = FontWeight.Bold
            )
            Text(
                "PING < 50ms",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = IndustrialTheme.CopperHighlightCap
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active Node Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1D2024))
                .border(1.dp, IndustrialTheme.CopperFront, RoundedCornerShape(4.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "SG-01 ULTRA-LOW PING [VLESS]",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "HOST: sg1.node-matrix.net : 443",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray
                )
            }
            Text(
                "28 ms",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = IndustrialTheme.AmberGlow
            )
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT 7: BOTTOM OSCILLOSCOPE HEARTBEAT WAVE & FACTORY SERIAL ATTRIBUTION
// -----------------------------------------------------------------------------
@Composable
fun BottomHeartbeatOscilloscope(isConnected: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ChamferShape9dp)
            .background(IndustrialTheme.RecessedTrayBackground)
            .border(1.dp, IndustrialTheme.MachinedBorder, ChamferShape9dp)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Oscilloscope Screen Reticle & Wave
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF101214))
                .border(1.dp, IndustrialTheme.MachinedBorder, RoundedCornerShape(4.dp))
                .drawBehind {
                    // Draw Oscilloscope ECG Path in Warm Copper
                    val path = Path().apply {
                        val h = size.height / 2
                        moveTo(0f, h)
                        lineTo(size.width * 0.35f, h)
                        lineTo(size.width * 0.42f, h - 16f)
                        lineTo(size.width * 0.50f, h + 18f)
                        lineTo(size.width * 0.58f, h - 8f)
                        lineTo(size.width * 0.65f, h)
                        lineTo(size.width, h)
                    }
                    drawPath(
                        path = path,
                        color = IndustrialTheme.CopperFront,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )
                }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "DEVELOPE BY VICTOR GEEK",
            color = IndustrialTheme.CopperFront,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
    }
}

// -----------------------------------------------------------------------------
// HELPERS
// -----------------------------------------------------------------------------
@Composable
fun BrushedSteelKeyCap(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF2E333A))
            .border(1.dp, IndustrialTheme.MachinedBorder, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun IndustrialSettingsTray() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ChamferShape9dp)
            .background(IndustrialTheme.RecessedTrayBackground)
            .border(1.dp, IndustrialTheme.MachinedBorder, ChamferShape9dp)
            .padding(14.dp)
    ) {
        Text("CHASSIS CALIBRATION & ROUTING RULES", color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text("• DNS Tunnel: 1.1.1.1 (Cloudflare DoH)\n• MTU Calibration: 1420 bytes\n• TCP Mux Concurrency: 8 Channels\n• Carrier Handover: Instant Auto-Reconnect", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun IndustrialAboutTray() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ChamferShape9dp)
            .background(IndustrialTheme.RecessedTrayBackground)
            .border(1.dp, IndustrialTheme.MachinedBorder, ChamferShape9dp)
            .padding(14.dp)
    ) {
        Text("VICTOR GEEK // GOD LEVEL HACKER", color = IndustrialTheme.CopperHighlightCap, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Autonomous V2Ray Network Orchestration Engine calibrated for industrial reliability, high throughput, and zero-loss mobile carrier bypass.", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

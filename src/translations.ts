import { TargetPlatform, Language } from './types';

export const translations = {
  EN: {
    appName: 'AI V2Ray Client',
    developedBy: 'Developed by Victor Geek',
    policyNotice: 'Educational Network Utility & Diagnostics Tool. Complies with Google Developer Security Policies.',
    policyFoldToggle: 'Policy Notice',
    aiMarqueeText: '⚡ [AI V2RAY AGENT] Real-time network probe active. DNS auto-locked to Cloudflare DoH (1.1.1.1). IPv4/IPv6 Dual-Stack verified. Auto-routing: Bypass LAN. Latency self-optimizing for lowest ping...',
    hideMarquee: 'Hide',
    showMarquee: 'Show Live AI Log',
    
    // Permission
    permissionTitle: 'AI Web Researcher Permission',
    permissionDesc: 'Grant AI permission to deep-search public xray repositories and self-diagnose current network latency?',
    permissionAccept: 'Grant Access & Start AI Research',
    permissionDecline: 'Use Default Local Configs',

    // Target selector
    targetPromptTitle: 'What app or game are you using today?',
    targetPromptSubtitle: 'AI will calibrate routing, test latency against target CDN/cloud clusters, and sort servers with Ping < 100ms.',
    targetAll: 'General Web & Streaming',
    targetFB: 'Facebook & Meta',
    targetTG: 'Telegram Messenger',
    targetPUBG: 'PUBG Mobile (SEA AWS)',
    targetMLBB: 'Mobile Legends (Moonton SG)',
    targetHOK: 'Honor of Kings (Tencent SEA)',
    targetYT: 'YouTube 4K & Media',
    targetTikTok: 'TikTok Global',

    // One Click Connect
    oneClickTitle: 'TAP TO CONNECT',
    oneClickConnected: 'TUNNEL ACTIVE',
    oneClickConnecting: 'TUNNELING...',
    oneClickDisconnect: 'DISCONNECT',
    liveLatency: 'Ping Latency',
    uploadRate: 'Upload',
    downloadRate: 'Download',
    sessionTime: 'Connected Duration',
    activeProtocol: 'Active Protocol',
    virtualIp: 'Virtual Route IP',

    // Server list
    serverListTitle: 'Active Xray Nodes',
    serverCount: 'Servers (<100ms)',
    sortBadge: 'Sorted: Lowest to Highest Ping (Ascending)',
    autoSelectedBadge: 'AI Recommended #1 Node',
    refreshAiSearch: 'AI Deep Re-Scan',
    scanning: 'AI Web Digger Searching...',
    copyKey: 'Copy Key',
    copied: 'Copied!',
    viewQr: 'QR Code',
    testPing: 'Ping',
    exportAll: 'Export to v2rayNG',
    exportCopied: 'All Nodes Copied to Clipboard!',
    noServersFound: 'No servers below 100ms found. Try AI Re-Scan.',
    filterProtocol: 'Protocol',
    filterRegion: 'Region',
    allProtocols: 'All Protocols',
    allRegions: 'All Regions',

    // Diagnostics modal
    diagnosticsTitle: 'AI Network Self-Check & Auto-Tuning',
    diagnosticsSubtitle: 'AI continuously monitors connection changes, auto-tunes DNS, MTU, IPv4/IPv6, and routing rules.',
    networkStatus: 'Current Network',
    dnsOptimization: 'DNS Resolution Engine',
    routingEngine: 'Intelligent Routing Table',
    simNetworkChange: 'Simulate Network Switch (Wi-Fi ⇄ 4G)',
    close: 'Close',

    // QR Modal
    qrTitle: 'Scan with Android v2rayNG',
    qrSubtitle: 'Open v2rayNG on your Android device, tap "+" and select "Scan QR Code".',
    copyRawConfig: 'Copy Raw Link',

    // Tab Sheets
    tabDashboard: 'Tunnel',
    tabSettings: 'Settings',
    tabAbout: 'About & Dev',

    // About Tab
    aboutTitle: 'About AI V2Ray Client',
    appVersion: 'App Version',
    buildDate: 'Build Architecture',
    engineCore: 'Engine Core',
    devTitle: 'Lead Developer Profile',
    devRole: 'World Top Legend Programmer & Security Architect',
    devBio: 'Specializing in deep protocol obfuscation, low-latency routing, and AI-driven automated packet telemetry.',
    featuresListTitle: 'System Capabilities',
    complianceTitle: 'Compliance & Safety',
    complianceText: 'Complies with Google Play developer distribution policies and international network security standards. Zero log retention policy.',
    systemSpecs: 'System Diagnostics & Runtime',

    // Settings Tab
    settingsTitle: 'Tunnel & Routing Settings',
    settingsDnsTitle: 'DNS Mode & Upstream Provider',
    settingsRoutingTitle: 'Packet Routing & Splitting',
    settingsMtuTitle: 'MTU Packet Clamping',
    settingsIpv6Title: 'IPv6 Leak Prevention',
    settingsAutoReconnect: 'Auto-Reconnect on Network Switch',
    settingsSaved: 'Settings Saved & Applied!',
  },
  MM: {
    appName: 'AI V2Ray Client',
    developedBy: 'Develope by Victor Geek',
    policyNotice: 'သတိပေးချက် - ဤစနစ်သည် ပညာရေးဆိုင်ရာ ကွန်ရက်လေ့လာစစ်ဆေးမှု utility တစ်ခုဖြစ်ပြီး Google Developer Policies များနှင့်အညီ ဖန်တီးထားပါသည်။',
    policyFoldToggle: 'Google မူဝါဒ ကြေငြာချက်',
    aiMarqueeText: '⚡ [AI V2RAY AGENT] လက်ရှိ Network ကို စစ်ဆေးနေပါသည်။ DNS အား Cloudflare DoH (1.1.1.1) သို့ ချိန်ညှိပြီး IPv4/IPv6 dual-stack စစ်ဆေးထားပါသည်။ Route: Bypass LAN & Local။ Latency အနည်းဆုံးဖြစ်အောင် အလိုအလျောက် ပြုပြင်ပေးနေပါသည်...',
    hideMarquee: 'ပိတ်မည်',
    showMarquee: 'AI အသိပေးချက် ဖွင့်မည်',

    // Permission
    permissionTitle: 'AI Web Researcher ခွင့်ပြုချက် တောင်းခံခြင်း',
    permissionDesc: 'အင်တာနက်ပေါ်ရှိ Public Xray key များကို AI မှ သွားရောက်ရှာဖွေ စစ်ဆေးရန်နှင့် လက်ရှိ Network ကို စမ်းသပ်ရန် ခွင့်ပြုချက် ပေးပါရန်။',
    permissionAccept: 'ခွင့်ပြုချက်ပေးပြီး AI ရှာဖွေမှု စတင်မည်',
    permissionDecline: 'နဂိုမူလ အခြေခံ Server များကို သုံးမည်',

    // Target selector
    targetPromptTitle: 'မည်သည့် App သို့မဟုတ် Game ဖြင့် အဓိက သုံးမည်နည်း?',
    targetPromptSubtitle: 'သင်ရွေးချယ်သော Platform ၏ CDN/Cloud များသို့ Latency စမ်းသပ်ကာ Ping 100ms အောက် Server များကို အနည်းမှ အများသို့ AI က စီစဉ်ပေးပါမည်။',
    targetAll: 'သာမာန် သုံးစွဲရန် (General Web)',
    targetFB: 'Facebook နှင့် Messenger',
    targetTG: 'Telegram Messenger',
    targetPUBG: 'PUBG Mobile (ဂိမ်းဆာဗာ)',
    targetMLBB: 'Mobile Legends (MLBB ဆာဗာ)',
    targetHOK: 'Honor of Kings (HOK ဆာဗာ)',
    targetYT: 'YouTube နှင့် ရုပ်သံလိုင်းများ',
    targetTikTok: 'TikTok Video',

    // One Click Connect
    oneClickTitle: 'ချိတ်ဆက်ရန် နှိပ်ပါ',
    oneClickConnected: 'ချိတ်ဆက်ထားသည်',
    oneClickConnecting: 'ချိတ်ဆက်နေသည်...',
    oneClickDisconnect: 'ဖြုတ်မည်',
    liveLatency: 'Ping တုံ့ပြန်မှုနှုန်း',
    uploadRate: 'အထွက်နှုန်း (Upload)',
    downloadRate: 'အဝင်နှုန်း (Download)',
    sessionTime: 'ချိတ်ဆက်ထားသည့် ကြာချိန်',
    activeProtocol: 'လက်ရှိ Protocol',
    virtualIp: 'Virtual IP လမ်းကြောင်း',

    // Server list
    serverListTitle: 'အသုံးပြုနိုင်သော Xray Server များ',
    serverCount: 'Server အရေအတွက် (Ping <100ms)',
    sortBadge: 'Ping နည်းရာမှ များရာသို့ အလိုအလျောက် စီထားပါသည်',
    autoSelectedBadge: 'AI မှ အကောင်းဆုံး ရွေးချယ်ပေးထားသော Server',
    refreshAiSearch: 'AI ဖြင့် အသစ်ပြန်ရှာမည်',
    scanning: 'AI Web Digger ရှာဖွေနေပါသည်...',
    copyKey: 'Key ကူးယူမည်',
    copied: 'ကူးယူပြီးပါပြီ!',
    viewQr: 'QR ကုဒ်ပြမည်',
    testPing: 'Ping စစ်မည်',
    exportAll: 'v2rayNG သို့ Export လုပ်မည်',
    exportCopied: 'Server အားလုံးကို Clipboard သို့ ကူးယူပြီးပါပြီ!',
    noServersFound: 'Ping 100ms အောက် Server ရှာမတွေ့ပါ။ AI ပြန်လည်ရှာဖွေမှု ပြုလုပ်ပါ။',
    filterProtocol: 'Protocol စစ်ထုတ်ရန်',
    filterRegion: 'နိုင်ငံ/ဒေသ စစ်ထုတ်ရန်',
    allProtocols: 'Protocol အားလုံး',
    allRegions: 'နိုင်ငံ/ဒေသ အားလုံး',

    // Diagnostics modal
    diagnosticsTitle: 'AI Network စစ်ဆေးပြင်ဆင်မှုစနစ်',
    diagnosticsSubtitle: 'Network အပြောင်းအလဲတိုင်းကို AI က သိရှိပြီး DNS, MTU, IPv4/IPv6 နှင့် Routing များကို အလိုအလျောက် ပြုပြင်ပေးပါသည်။',
    networkStatus: 'လက်ရှိ ကွန်ရက်အခြေအနေ',
    dnsOptimization: 'DNS စနစ် ချိန်ညှိမှု',
    routingEngine: 'ဉာဏ်ရည်တု လမ်းကြောင်းခွဲစနစ် (Routing)',
    simNetworkChange: 'Network အပြောင်းအလဲ စမ်းသပ်မည် (Wi-Fi ⇄ 4G)',
    close: 'ပိတ်မည်',

    // QR Modal
    qrTitle: 'Android v2rayNG ဖြင့် Scan ဖတ်ပါ',
    qrSubtitle: 'ဖုန်းရှိ v2rayNG အက်ပ်ကို ဖွင့်ပါ၊ အပေါင်း (+) လက္ခဏာကို နှိပ်ပြီး "Scan QR Code" ဖြင့် ချိန်ဖတ်ပါ။',
    copyRawConfig: 'Raw Link ကူးယူမည်',

    // Tab Sheets
    tabDashboard: 'ချိတ်ဆက်မှု',
    tabSettings: 'ဆက်တင်များ',
    tabAbout: 'အချက်အလက်',

    // About Tab
    aboutTitle: 'AI V2Ray Client အကြောင်း',
    appVersion: 'ဆော့ဖ်ဝဲလ် ဗားရှင်း',
    buildDate: 'ထုတ်လုပ်သည့် ဗိသုကာ',
    engineCore: 'အင်ဂျင် ကုတ် (Engine Core)',
    devTitle: 'Developer အချက်အလက်',
    devRole: 'World Top Legend Programmer & Security Architect',
    devBio: 'Network protocol များအား မြန်ဆန်စွာ ဆက်သွယ်နိုင်ရန်နှင့် AI အခြေခံ Latency တိုင်းတာမှုများ ပြုလုပ်နိုင်ရန် ဖန်တီးသူ။',
    featuresListTitle: 'စနစ်၏ စွမ်းဆောင်ရည်များ',
    complianceTitle: 'မူဝါဒနှင့် လုံခြုံရေး အာမခံချက်',
    complianceText: 'Google Play developer မူဝါဒများနှင့် ကိုက်ညီပြီး User အချက်အလက်နှင့် Log များကို သိမ်းဆည်းခြင်း မရှိပါ။',
    systemSpecs: 'စနစ်ဆိုင်ရာ အချက်အလက်များ',

    // Settings Tab
    settingsTitle: 'VPN လမ်းကြောင်းနှင့် ကွန်ရက် ဆက်တင်များ',
    settingsDnsTitle: 'DNS စနစ်နှင့် အသုံးပြုမည့် Provider',
    settingsRoutingTitle: 'Packet လမ်းကြောင်းခွဲခြားခြင်း (Routing)',
    settingsMtuTitle: 'MTU Packet အရွယ်အစား ချိန်ညှိမှု',
    settingsIpv6Title: 'IPv6 Leak မဖြစ်စေရန် ကာကွယ်မှု',
    settingsAutoReconnect: 'Network ပြောင်းလဲပါက အလိုအလျောက် ပြန်ချိတ်မည်',
    settingsSaved: 'ဆက်တင်များကို သိမ်းဆည်းပြီးပါပြီ!',
  },
};

export const targetDetails: Record<
  TargetPlatform,
  { labelEn: string; labelMm: string; icon: string; cdn: string; color: string }
> = {
  general: {
    labelEn: 'General Web',
    labelMm: 'သာမာန် သုံးရန်',
    icon: '⚡',
    cdn: 'Cloudflare / Google Anycast',
    color: 'from-emerald-500/20 to-emerald-950/40 border-emerald-500/30 text-emerald-400',
  },
  facebook: {
    labelEn: 'Facebook & Meta',
    labelMm: 'Facebook / IG',
    icon: '🌐',
    cdn: 'Meta Global Edge Caches',
    color: 'from-[#C87547]/20 to-[#1F2327] border-[#C87547]/40 text-[#E8C99A]',
  },
  telegram: {
    labelEn: 'Telegram',
    labelMm: 'Telegram',
    icon: '✈️',
    cdn: 'Telegram DC4 & DC5 Asia Cluster',
    color: 'from-amber-600/20 to-[#1F2327] border-amber-500/40 text-amber-300',
  },
  pubg: {
    labelEn: 'PUBG Mobile',
    labelMm: 'PUBG Mobile',
    icon: '🎯',
    cdn: 'AWS Singapore / Tencent SEA Direct UDP',
    color: 'from-amber-500/20 to-amber-950/40 border-amber-500/30 text-amber-400',
  },
  mlbb: {
    labelEn: 'Mobile Legends',
    labelMm: 'Mobile Legends',
    icon: '⚔️',
    cdn: 'Moonton Singapore Gaming Gateway',
    color: 'from-purple-500/20 to-purple-950/40 border-purple-500/30 text-purple-400',
  },
  hok: {
    labelEn: 'Honor of Kings',
    labelMm: 'Honor of Kings',
    icon: '👑',
    cdn: 'Tencent SEA Low-Jitter Server',
    color: 'from-yellow-500/20 to-yellow-950/40 border-yellow-500/30 text-yellow-400',
  },
  youtube: {
    labelEn: 'YouTube 4K',
    labelMm: 'YouTube / Media',
    icon: '📺',
    cdn: 'Google Video Global Cache (GVC)',
    color: 'from-red-500/20 to-red-950/40 border-red-500/30 text-red-400',
  },
  tiktok: {
    labelEn: 'TikTok',
    labelMm: 'TikTok Video',
    icon: '🎵',
    cdn: 'ByteDance FastEdge CDN',
    color: 'from-pink-500/20 to-pink-950/40 border-pink-500/30 text-pink-400',
  },
};

package com.rizmi.cleanshield.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizmi.cleanshield.ui.components.CyberCard
import com.rizmi.cleanshield.ui.theme.AmberWarning
import com.rizmi.cleanshield.ui.theme.CyberCard
import com.rizmi.cleanshield.ui.theme.CyberCardBorder
import com.rizmi.cleanshield.ui.theme.CyberCardElevated
import com.rizmi.cleanshield.ui.theme.CyanAccent
import com.rizmi.cleanshield.ui.theme.EmeraldActive
import com.rizmi.cleanshield.ui.theme.RoseBlocked
import com.rizmi.cleanshield.ui.theme.TextMuted
import com.rizmi.cleanshield.ui.theme.TextPrimary
import com.rizmi.cleanshield.ui.theme.TextSecondary

@Composable
fun AboutScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(CyanAccent, EmeraldActive))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "CleanShield v1.0",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Local VPN DNS Filter & Privacy Guard",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Filtering Mechanism
        item {
            CyberCard(borderColor = CyberCardBorder, backgroundColor = CyberCard) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = CyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "How CleanShield Works",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "CleanShield sets up an on-device local loopback VPN using Android VpnService. Only DNS traffic (port 53) is routed to the local filter engine.\n\n" +
                            "When an app attempts to connect to a domain:\n" +
                            "1. If the domain matches your local blocklist, CleanShield immediately synthesizes an RFC 1035 NXDOMAIN response locally on your phone. No network connection is made.\n" +
                            "2. If the domain is allowed or on your allowlist, the query is forwarded to your chosen upstream resolver (e.g. Cloudflare, Google, Quad9) via a protected socket.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }
        }

        // Privacy Policy & Zero Telemetry
        item {
            CyberCard(borderColor = CyberCardBorder, backgroundColor = CyberCardElevated) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldActive)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Privacy & Local Guarantee",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "• 100% Offline & Private: No developer servers, no cloud telemetry, no accounts, and no advertising trackers.\n" +
                            "• No TLS Interception: CleanShield does not install custom CA certificates or inspect decrypted HTTPS payloads.\n" +
                            "• Local Storage: All logs, blocklists, and statistics reside exclusively in your device's Room database and can be deleted anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }
        }

        // Technical Limitations
        item {
            CyberCard(borderColor = AmberWarning.copy(alpha = 0.5f), backgroundColor = CyberCard) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Important Technical Limitations",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AmberWarning
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "CleanShield is a DNS-level blocker and cannot promise 100% ad removal:\n\n" +
                            "1. Encrypted DNS (DoH / DoT): Apps that hardcode DNS-over-HTTPS, DNS-over-TLS, or their own IP addresses will bypass standard DNS filtering.\n" +
                            "2. First-Party Ads: Ads served from the exact same domain as the primary content (such as YouTube videos or social media feeds) cannot be blocked without breaking the app itself.\n" +
                            "3. HTTPS Encryption: A local DNS VPN cannot read, inspect, or modify encrypted webpage HTML or scripts.\n" +
                            "4. Blocklist Currency: Local blocklists only recognize known tracker domains until you import an updated hosts file.\n" +
                            "5. Upstream Resolver Metadata: Permitted queries are forwarded to your chosen resolver (e.g. Cloudflare, Google), where query metadata is handled according to their privacy policy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

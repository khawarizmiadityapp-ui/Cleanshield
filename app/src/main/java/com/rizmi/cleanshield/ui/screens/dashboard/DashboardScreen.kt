package com.rizmi.cleanshield.ui.screens.dashboard

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rizmi.cleanshield.domain.model.DnsQueryEvent
import com.rizmi.cleanshield.domain.model.QueryStatus
import com.rizmi.cleanshield.domain.model.VpnStatus
import com.rizmi.cleanshield.ui.components.CyberCard
import com.rizmi.cleanshield.ui.components.LiquidPowerCore
import com.rizmi.cleanshield.ui.components.MetricCard
import com.rizmi.cleanshield.ui.components.StatusBadge
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToActivity: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val vpnState by viewModel.vpnState.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val blockedRulesCount by viewModel.totalBlockedRules.collectAsState()
    val recentEvents by viewModel.recentEvents.collectAsState()
    val sessionTimer by viewModel.sessionTimer.collectAsState()

    // Activity Result Launcher for VPN Permission Request (VpnService.prepare)
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startProtectionDirectly(context)
        }
    }

    val isRunning = vpnState.status == VpnStatus.RUNNING
    val isStarting = vpnState.status == VpnStatus.STARTING

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CyanAccent, EmeraldActive)
                                )
                            )
                            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "CleanShield Logo",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CleanShield",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Local DNS Ad & Tracker Blocker",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                StatusBadge(
                    text = when (vpnState.status) {
                        VpnStatus.RUNNING -> "PROTECTED"
                        VpnStatus.STARTING -> "STARTING"
                        VpnStatus.STOPPING -> "STOPPING"
                        VpnStatus.ERROR -> "ERROR"
                        VpnStatus.STOPPED -> "UNPROTECTED"
                    },
                    color = when (vpnState.status) {
                        VpnStatus.RUNNING -> EmeraldActive
                        VpnStatus.STARTING -> AmberWarning
                        VpnStatus.STOPPING -> AmberWarning
                        VpnStatus.ERROR -> RoseBlocked
                        VpnStatus.STOPPED -> TextMuted
                    },
                    isPulsing = isRunning
                )
            }
        }

        // Master Protection Control Card
        item {
            CyberCard(
                borderColor = if (isRunning) EmeraldActive.copy(alpha = 0.5f) else CyanAccent.copy(alpha = 0.3f),
                backgroundColor = Color.Transparent
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Liquid Glass Core Button
                    LiquidPowerCore(
                        isRunning = isRunning,
                        isStarting = isStarting,
                        onClick = {
                            val intent = viewModel.toggleProtection(context)
                            intent?.let { vpnPermissionLauncher.launch(it) }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isRunning) "Shield is Active" else if (isStarting) "Activating VPN..." else "Shield is Off",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isRunning) "All queries filtered through local DNS engine" else "Tap button to begin blocking trackers",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Meta status chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sessionTimer,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = vpnState.currentResolver,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Metrics Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Inspected",
                        value = "${stats.totalQueries}",
                        subtitle = "Total DNS queries",
                        icon = Icons.Default.QueryBuilder,
                        accentColor = CyanAccent
                    )

                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Blocked",
                        value = "${stats.blockedQueries}",
                        subtitle = "Trackers stopped",
                        icon = Icons.Default.Block,
                        accentColor = RoseBlocked
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val percentageStr = String.format(Locale.ROOT, "%.1f%%", stats.blockPercentage)
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Block Rate",
                        value = percentageStr,
                        subtitle = "Zero-division safe",
                        icon = Icons.Default.CheckCircle,
                        accentColor = EmeraldActive
                    )

                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Blocklist",
                        value = "$blockedRulesCount",
                        subtitle = "Active rules",
                        icon = Icons.Default.Security,
                        accentColor = CyanAccent
                    )
                }
            }
        }

        // Recent Activity Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = CyanAccent,
                    modifier = Modifier.clickable { onNavigateToActivity() }
                )
            }
        }

        // Recent Activity Items
        if (recentEvents.isEmpty()) {
            item {
                CyberCard(backgroundColor = CyberCardElevated) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No DNS queries recorded yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enable protection to see live network queries",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(recentEvents, key = { it.id }) { event ->
                RecentEventItem(event)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RecentEventItem(event: DnsQueryEvent) {
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormatter.format(Date(event.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
        border = BorderStroke(1.dp, CyberCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.domain,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${event.queryType} • $formattedTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    if (event.responseTimeMs > 0) {
                        Text(
                            text = " • ${event.responseTimeMs}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            val (badgeText, badgeColor) = when (event.status) {
                QueryStatus.BLOCKED -> "BLOCKED" to RoseBlocked
                QueryStatus.FORWARDED -> "ALLOWED" to CyanAccent
                QueryStatus.ALLOWED -> "ALLOWED" to CyanAccent
                QueryStatus.FAILED -> "FAILED" to AmberWarning
            }

            StatusBadge(text = badgeText, color = badgeColor)
        }
    }
}

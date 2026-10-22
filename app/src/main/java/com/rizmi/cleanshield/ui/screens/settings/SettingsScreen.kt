package com.rizmi.cleanshield.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rizmi.cleanshield.domain.model.DnsResolverConfig
import com.rizmi.cleanshield.ui.components.CyberCard
import com.rizmi.cleanshield.ui.components.StatusBadge
import com.rizmi.cleanshield.ui.theme.CyberCard
import com.rizmi.cleanshield.ui.theme.CyberCardBorder
import com.rizmi.cleanshield.ui.theme.CyberCardElevated
import com.rizmi.cleanshield.ui.theme.CyanAccent
import com.rizmi.cleanshield.ui.theme.RoseBlocked
import com.rizmi.cleanshield.ui.theme.TextMuted
import com.rizmi.cleanshield.ui.theme.TextPrimary
import com.rizmi.cleanshield.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val selectedResolver by viewModel.selectedResolver.collectAsState()
    val loggingEnabled by viewModel.loggingEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val customIpInput by viewModel.customIpInput.collectAsState()
    val ipError by viewModel.ipError.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = "Configure upstream resolver and preferences",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Upstream DNS Resolver Card
        item {
            CyberCard(borderColor = CyberCardBorder, backgroundColor = CyberCard) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = CyanAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upstream DNS Resolver",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Allowed queries are forwarded to this resolver via protected local socket",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                DnsResolverConfig.DEFAULT_LIST.forEach { resolver ->
                    ResolverOption(
                        resolver = resolver,
                        isSelected = selectedResolver.id == resolver.id,
                        onSelect = { viewModel.selectResolver(resolver) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Custom Resolver Option
                val isCustomSelected = selectedResolver.isCustom
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val customConfig = DnsResolverConfig("custom", "Custom", customIpInput, 53, "Custom", true)
                            viewModel.selectResolver(customConfig)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
                    border = BorderStroke(
                        1.dp,
                        if (isCustomSelected) CyanAccent else CyberCardBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isCustomSelected,
                                onClick = {
                                    val customConfig = DnsResolverConfig("custom", "Custom", customIpInput, 53, "Custom", true)
                                    viewModel.selectResolver(customConfig)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CyanAccent,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Custom IPv4 Resolver",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Specify your own trusted DNS IP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        if (isCustomSelected) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customIpInput,
                                    onValueChange = { viewModel.onCustomIpChanged(it) },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("e.g. 1.0.0.1", color = TextMuted) },
                                    isError = ipError != null,
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = CyberCard,
                                        unfocusedContainerColor = CyberCard,
                                        focusedBorderColor = CyanAccent,
                                        unfocusedBorderColor = CyberCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val customConfig = DnsResolverConfig("custom", "Custom", customIpInput, 53, "Custom", true)
                                        viewModel.selectResolver(customConfig)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.Black)
                                }
                            }
                            if (ipError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = ipError!!, style = MaterialTheme.typography.bodySmall, color = RoseBlocked)
                            }
                        }
                    }
                }
            }
        }

        // Logging & Privacy Options Card
        item {
            CyberCard(borderColor = CyberCardBorder, backgroundColor = CyberCard) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = CyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Privacy & Logs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Record DNS Queries",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Save queries locally in Room database for activity inspection. Disabling prevents any query history logging.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = loggingEnabled,
                        onCheckedChange = { viewModel.setLoggingEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyanAccent,
                            uncheckedTrackColor = CyberCardBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Foreground Notification",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Displays active shield notification and quick stop toggle",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyanAccent,
                            uncheckedTrackColor = CyberCardBorder
                        )
                    )
                }
            }
        }

        // Maintenance Action
        item {
            CyberCard(borderColor = CyberCardBorder, backgroundColor = CyberCardElevated) {
                Text(
                    text = "Protection Service Control",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Restart protection to immediately apply changes or refresh DNS socket bindings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.restartProtection(context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restart Protection", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ResolverOption(
    resolver: DnsResolverConfig,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardElevated),
        border = BorderStroke(
            1.dp,
            if (isSelected) CyanAccent else CyberCardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = CyanAccent,
                    unselectedColor = TextSecondary
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${resolver.name} (${resolver.primaryIp})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    if (resolver.id == DnsResolverConfig.ADGUARD.id) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(text = "Dual-Shield", color = CyanAccent)
                    }
                }
                Text(
                    text = resolver.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}

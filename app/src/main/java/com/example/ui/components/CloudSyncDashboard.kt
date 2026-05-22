package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CloudSyncDashboard(
    isSyncing: Boolean,
    logs: List<String>,
    userEmail: String,
    onForceSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    
    // Spinning infinite animation for syncing symbol
    val infiniteTransition = rememberInfiniteTransition(label = "sync_spinner")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isSyncing) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isDark) Color(0xFF2F3F3C) else Color(0xFFE4E3DA),
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF172321) else Color(0xFFFFFFFF)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isSyncing) Color(0xFFFFB300) else Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sincronização em Nuvem",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onForceSync,
                    enabled = !isSyncing,
                    modifier = Modifier.size(36.dp).testTag("sync_now_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync now",
                        tint = if (isSyncing) Color.Gray else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.rotate(angle)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // User credentials and active sync nodes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF1E2B29) else Color(0xFFF5F4EC))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Active Sync Session",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "CONTA UNIFICADA",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = userEmail,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("ONLINE", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Connected Platform Terminals Grid (Browser extension, iOS app, Android)
            Text(
                text = "Dispositivos Sincronizados:",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlatformNodeItem(
                    icon = Icons.Default.Home,
                    name = "Extensão Web",
                    descr = "Instalado (Chrome/Edge)",
                    isActive = true,
                    modifier = Modifier.weight(1f)
                )
                PlatformNodeItem(
                    icon = Icons.Default.Person,
                    name = "Unified iOS",
                    descr = "Compatível v1.2",
                    isActive = true,
                    modifier = Modifier.weight(1f)
                )
                PlatformNodeItem(
                    icon = Icons.Default.Star,
                    name = "Este Android",
                    descr = "Ativo • ID Local",
                    isActive = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-time log console
            Text(
                text = "Console de Sincronia:",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF0F1615) else Color(0xFF1C1C17))
                    .border(1.dp, if (isDark) Color(0xFF1E2B29) else Color(0xFF46483C), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    reverseLayout = false // standard log stream
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            color = if (log.contains("Sincronização concluída")) Color(0xFF81C784) else
                                    if (log.contains("ERRO")) Color(0xFFFF8A80) else Color(0xFFA2AD9C),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformNodeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    descr: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            ),
        color = if (isDark) Color(0xFF1E2B29) else Color(0xFFF5F4EC)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = descr,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

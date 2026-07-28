package com.shieldguard.presentation.urlcheck

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shieldguard.domain.model.RiskLevel
import com.shieldguard.domain.model.UrlRecommendation
import com.shieldguard.domain.model.UrlScanResult
import com.shieldguard.presentation.dashboard.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlCheckScreen(
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var urlInput by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("URL / Link Check Karo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---- HEADER INFO ----
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            Icons.Filled.Shield,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Koi bhi suspicious link yahan check karo",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "WhatsApp link, SMS link, email link — paste karo aur turant pata karo safe hai ya nahi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ---- URL INPUT ----
            item {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL / Link yahan paste karo") },
                    placeholder = { Text("https://example.com ya bit.ly/xyz123") },
                    leadingIcon = { Icon(Icons.Filled.Link, null) },
                    trailingIcon = {
                        if (urlInput.isNotEmpty()) {
                            IconButton(onClick = { urlInput = "" }) {
                                Icon(Icons.Filled.Clear, "Clear")
                            }
                        } else {
                            IconButton(onClick = {
                                clipboard.getText()?.text?.let { urlInput = it }
                            }) {
                                Icon(Icons.Filled.ContentPaste, "Paste")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (urlInput.isNotBlank()) {
                                viewModel.checkUrl(urlInput.trim())
                                keyboard?.hide()
                            }
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }

            // ---- CHECK BUTTON ----
            item {
                Button(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            viewModel.checkUrl(urlInput.trim())
                            keyboard?.hide()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = urlInput.isNotBlank() && !uiState.isCheckingUrl,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isCheckingUrl) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Check ho raha hai...")
                    } else {
                        Icon(Icons.Filled.Search, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Check Karo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // ---- RESULT CARD ----
            uiState.urlScanResult?.let { result ->
                item {
                    UrlResultCard(
                        result = result,
                        onDismiss = { viewModel.dismissUrlResult() }
                    )
                }
            }

            // ---- COMMON SCAM EXAMPLES ----
            item {
                Text(
                    "⚠️ Aise links se bachein",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            val scamExamples = listOf(
                "🏦 Fake Bank Links" to "sbi-secure-verify.xyz, hdfc-kyc-update.tk",
                "🎁 Fake Prize Links" to "amazon-winner-claim.ml, jio-free-recharge.ga",
                "🏛️ Fake Govt Links" to "pm-kisan-help.cf, aadhaar-update-online.xyz",
                "💰 Crypto Scam" to "bitcoin-double.top, crypto-profit.xyz",
                "📱 Fake App Download" to "whatsapp-update-new.com, chrome-security-update.net"
            )

            items(scamExamples) { (title, examples) ->
                ScamExampleItem(title = title, examples = examples)
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// =============================================
// URL RESULT CARD
// =============================================
@Composable
fun UrlResultCard(result: UrlScanResult, onDismiss: () -> Unit) {
    val (bgColor, iconColor, borderColor) = when (result.riskLevel) {
        RiskLevel.CRITICAL -> Triple(Color(0xFFF44336).copy(0.1f), Color(0xFFF44336), Color(0xFFF44336))
        RiskLevel.HIGH     -> Triple(Color(0xFFFF5722).copy(0.1f), Color(0xFFFF5722), Color(0xFFFF5722))
        RiskLevel.MEDIUM   -> Triple(Color(0xFFFF9800).copy(0.1f), Color(0xFFFF9800), Color(0xFFFF9800))
        RiskLevel.LOW      -> Triple(Color(0xFF2196F3).copy(0.1f), Color(0xFF2196F3), Color(0xFF2196F3))
        RiskLevel.SAFE     -> Triple(Color(0xFF4CAF50).copy(0.1f), Color(0xFF4CAF50), Color(0xFF4CAF50))
    }

    val (emoji, headline) = when (result.recommendation) {
        UrlRecommendation.BLOCKED_MALWARE   -> "🚨" to "MALWARE! Ye link MAT kholo!"
        UrlRecommendation.BLOCKED_PHISHING  -> "🎣" to "PHISHING! Ye aapki jaankari churaega!"
        UrlRecommendation.BLOCK_AND_WARN    -> "⛔" to "DANGEROUS! Ye link safe nahi hai!"
        UrlRecommendation.PROCEED_WITH_CAUTION -> "⚠️" to "Sochke kholo — thoda suspicious hai"
        UrlRecommendation.SAFE_TO_VISIT     -> "✅" to "Ye link safe lagta hai"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // HEADLINE
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 28.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    headline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Close, null, tint = iconColor)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = borderColor.copy(0.3f))
            Spacer(Modifier.height(12.dp))

            // AI EXPLANATION
            Text(
                result.aiExplanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // RISK REASONS
            if (result.riskReasons.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Kyu dangerous hai:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                result.riskReasons.forEach { reason ->
                    Text(
                        reason,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = borderColor.copy(0.3f))
            Spacer(Modifier.height(8.dp))

            // TECHNICAL DETAILS
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TechBadge(
                    label = if (result.isHttps) "HTTPS ✅" else "HTTP ⚠️",
                    color = if (result.isHttps) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
                if (result.isUrlShortener) TechBadge("Short URL ⚠️", Color(0xFFFF9800))
                if (result.hasHomographAttack) TechBadge("Fake Chars 🔴", Color(0xFFF44336))
                if (result.redirectChain.size > 2) TechBadge("${result.redirectChain.size} Redirects", Color(0xFFFF9800))
            }

            // REDIRECT CHAIN
            if (result.redirectChain.size > 1) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Redirect chain:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                result.redirectChain.forEachIndexed { i, url ->
                    Text(
                        "${i + 1}. ${url.take(60)}${if (url.length > 60) "..." else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TechBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ScamExampleItem(title: String, examples: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(
                examples,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

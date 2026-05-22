package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.MatchError

@Composable
fun SpellCheckEditor(
    text: String,
    onTextChanged: (String) -> Unit,
    errors: List<MatchError>,
    selectedLanguage: String,
    isAnalyzing: Boolean,
    onLanguageSelected: (String) -> Unit,
    onDictationClicked: () -> Unit,
    onSpeakClicked: () -> Unit,
    onApplyCorrection: (MatchError) -> Unit,
    onFormatAllClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var titleText by remember { mutableStateOf("Texto sem título") }

    // Custom visual transformation that underlines errors on the fly
    val spellVisualTransformation = remember(errors, isDark) {
        VisualTransformation { originalText ->
            val builder = AnnotatedString.Builder(originalText.text)
            
            // Draw beautiful styled highlights for every detected error
            errors.forEach { error ->
                if (error.start in 0..originalText.text.length && 
                    error.end in 0..originalText.text.length && 
                    error.start < error.end) {
                    
                    builder.addStyle(
                        style = SpanStyle(
                            background = if (isDark) Color(0x3DFFB74D) else Color(0xFFFEF3C7),
                            textDecoration = TextDecoration.Underline,
                            color = if (isDark) Color(0xFFFFB74D) else Color(0xFFD97706),
                            fontWeight = FontWeight.Bold
                        ),
                        start = error.start,
                        end = error.end
                    )
                }
            }
            TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .border(
                1.dp,
                if (isDark) Color(0xFF2F3F3C) else Color(0xFFE4E3DA),
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Document Toolbar with language selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Language Dropdown Selector
                LanguageSelectorMenu(
                    currentLanguage = selectedLanguage,
                    onLanguageSelected = onLanguageSelected
                )

                // Toolbar Actions (Dictate, TTS, Clear/Magic Check)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onSpeakClicked,
                        modifier = Modifier.testTag("action_speak")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Read aloud",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDictationClicked,
                        modifier = Modifier.testTag("action_dictate")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Voice Dictation",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (errors.isNotEmpty()) {
                        Button(
                            onClick = onFormatAllClicked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("action_fix_all")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Fix All",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Corrigir Tudo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Real-time Text Editor Area
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 300.dp)
                    .testTag("editor_text_field"),
                placeholder = {
                    Text(
                        text = when (selectedLanguage) {
                            "en" -> "Start typing in English..."
                            "es" -> "Comienza a escribir en Español..."
                            "fr" -> "Commencez à écrire en Français..."
                            else -> "Comece a digitar seu texto aqui..."
                        },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                visualTransformation = spellVisualTransformation,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp)
            )

            if (isAnalyzing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            }

            // Dynamic Spell Corrections Carousel below the editor
            if (errors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Sugestões de Correção (${errors.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("corrections_carousel"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(errors) { error ->
                        CorrectionItemCard(
                            error = error,
                            onApply = { onApplyCorrection(error) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectorMenu(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val languages = listOf(
        Pair("pt", "🇧🇷 Português"),
        Pair("en", "🇺🇸 English"),
        Pair("es", "🇪🇸 Español"),
        Pair("fr", "🇫🇷 Français")
    )
    val selectedLabel = languages.firstOrNull { it.first == currentLanguage }?.second ?: "🇧🇷 Português"

    Box {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .testTag("language_selector_trigger"),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Language",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            languages.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name, fontSize = 12.sp) },
                    onClick = {
                        onLanguageSelected(code)
                        expanded = false
                    },
                    modifier = Modifier.testTag("lang_option_$code")
                )
            }
        }
    }
}

@Composable
fun CorrectionItemCard(
    error: MatchError,
    onApply: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    
    Card(
        modifier = Modifier
            .width(220.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF2F3F3C) else Color(0xFFF5F4EC)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Wrong word
                Text(
                    text = error.original,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706),
                    style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Action correction card pill button
                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("apply_correction_btn_${error.original}")
                ) {
                    Text(
                        text = error.replacement,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Explanation reason
            Text(
                text = error.reason,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

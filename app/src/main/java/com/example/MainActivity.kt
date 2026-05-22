package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.database.Document
import com.example.data.repository.NotesRepository
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MascotState
import com.example.ui.viewmodel.SpellCheckViewModel
import com.example.ui.viewmodel.SpellCheckViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private var speechAssistant: SpeechAssistant? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get database and instantiate repository
        val database = AppDatabase.getDatabase(this)
        val repository = NotesRepository(database.documentDao())

        setContent {
            MyApplicationTheme {
                // Initialize Viewmodel
                val viewModel: SpellCheckViewModel = viewModel(
                    factory = SpellCheckViewModelFactory(repository)
                )

                // Initialize Text To Speech voice helper
                val context = LocalContext.current
                DisposableEffect(Unit) {
                    speechAssistant = SpeechAssistant(
                        context = context,
                        onInitComplete = { success ->
                            if (success) {
                                viewModel.sendBubbleMessage("Assistente de Voz ativado! Clique em mim para ler o texto em voz alta ou fale usando o microfone.")
                            }
                        },
                        onSpeechStateChanged = { isSpeaking ->
                            viewModel.setMascotSpeaking(isSpeaking)
                        }
                    )
                    onDispose {
                        speechAssistant?.shutdown()
                    }
                }

                // Standard STT Speech-to-Text intent listener
                val speechLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        val spokenText = matches?.firstOrNull() ?: ""
                        if (spokenText.isNotEmpty()) {
                            val currentText = viewModel.textInput.value
                            val updatedText = if (currentText.trim().isEmpty()) {
                                spokenText
                            } else {
                                "$currentText $spokenText"
                            }
                            viewModel.onTextChanged(updatedText)
                            viewModel.triggerAnalysis()
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize().testTag("app_scaffold"),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SpellBot Ortografia",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                ) { innerPadding ->
                    MainNavigationContent(
                        viewModel = viewModel,
                        speechLauncher = speechLauncher,
                        onSpeakValue = { text, lang ->
                            speechAssistant?.speak(text, lang)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationContent(
    viewModel: SpellCheckViewModel,
    speechLauncher: androidx.activity.compose.ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult>,
    onSpeakValue: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var selectedTab by remember { mutableStateOf(0) }
    
    val textInput by viewModel.textInput.collectAsStateWithLifecycle()
    val docTitle by viewModel.documentTitle.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    val mascotState by viewModel.mascotState.collectAsStateWithLifecycle()
    val bubbleMessage by viewModel.bubbleMessage.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val activeDocId by viewModel.activeDocumentId.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncingCloud.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Tab indicator choosing unified screens
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Corretor & Mascote", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_editor")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Meus Textos (${documents.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_folder")
            )
        }

        if (selectedTab == 0) {
            // Screen 1: Real-time Editor and Mascot Assistant
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                // Robot mascot reacting on top
                RobotMascot(
                    state = mascotState,
                    speechBubbleText = bubbleMessage,
                    onBubbleSpeechRequested = {
                        bubbleMessage?.let { onSpeakValue(it, selectedLanguage) }
                    }
                )

                // Editable properties
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Document title input
                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { viewModel.onTitleChanged(it) },
                        maxLines = 1,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("document_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (activeDocId != null) {
                        IconButton(
                            onClick = { viewModel.deleteCurrentDocument() },
                            modifier = Modifier.size(36.dp).testTag("delete_doc_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Delete Document",
                                tint = Color(0xFFD32F2F)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.createNewDocument() },
                        modifier = Modifier.size(36.dp).testTag("new_doc_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "New Document",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Advanced spelling editor
                SpellCheckEditor(
                    text = textInput,
                    onTextChanged = { viewModel.onTextChanged(it) },
                    errors = errors,
                    selectedLanguage = selectedLanguage,
                    isAnalyzing = isAnalyzing,
                    onLanguageSelected = { viewModel.onLanguageSelected(it) },
                    onDictationClicked = { launchSpeechToText(speechLauncher, selectedLanguage) },
                    onSpeakClicked = { onSpeakValue(textInput, selectedLanguage) },
                    onApplyCorrection = { viewModel.applyCorrection(it) },
                    onFormatAllClicked = { viewModel.formatAndCorrectAll() },
                    modifier = Modifier.padding(16.dp)
                )

                // Synchronisation status card simulating unifications (Chrome ext, iOS, Android)
                CloudSyncDashboard(
                    isSyncing = isSyncing,
                    logs = syncLogs,
                    userEmail = "miguelassis488@gmail.com",
                    onForceSync = { viewModel.triggerCloudSyncSimulation() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            // Screen 2: Saved Documents Grid (From Room Database)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Meus Textos Salvos",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Button(
                        onClick = {
                            viewModel.createNewDocument()
                            selectedTab = 0
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Criar Novo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (documents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Nenhum documento salvo ainda.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("documents_library")
                    ) {
                        items(documents) { doc ->
                            DocumentGridItem(
                                document = doc,
                                onClick = {
                                    viewModel.selectDocument(doc)
                                    selectedTab = 0
                                },
                                isCurrent = doc.id == activeDocId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentGridItem(
    document: Document,
    onClick: () -> Unit,
    isCurrent: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val formattedDate = sdf.format(Date(document.updatedAt))

    val languageLabel = when(document.language) {
        "en" -> "🇺🇸 EN"
        "es" -> "🇪🇸 ES"
        "fr" -> "🇫🇷 FR"
        else -> "🇧🇷 PT"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isCurrent) 6.dp else 2.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(
                1.5.dp,
                if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                if (isCurrent) Color(0xFF2F3F3C) else Color(0xFF1E2B29)
            } else {
                if (isCurrent) Color(0xFFDCE5E0) else Color(0xFFFFFFFF)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDark) Color(0xFF2F3F3C) else Color(0xFFE4E3DA))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(languageLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                
                Text(
                    text = "${document.content.length} carac.",
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = document.title.ifEmpty { "Sem título" },
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = document.content.ifEmpty { "vazio..." },
                fontSize = 10.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formattedDate,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

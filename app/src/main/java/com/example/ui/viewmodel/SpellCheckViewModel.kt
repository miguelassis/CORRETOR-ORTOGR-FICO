package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.MatchError
import com.example.data.database.Document
import com.example.data.repository.NotesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MascotState {
    IDLE,       // Watching type, blinking
    THINKING,   // Analysis in progress (spinning gears, processing eyes)
    ALERT,      // Spelling/grammar errors detected on-screen (attentive, magnifying glass)
    HAPPY,      // Safe check finished, corrections accepted (sparkles background, waving, dancing)
    SPEAKING    // Active Voice Assistant TTS outputting speech (moving lip syncing ripples)
}

class SpellCheckViewModel(private val repository: NotesRepository) : ViewModel() {

    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput.asStateFlow()

    private val _documentTitle = MutableStateFlow("Texto sem título")
    val documentTitle: StateFlow<String> = _documentTitle.asStateFlow()

    private val _activeDocumentId = MutableStateFlow<Int?>(null)
    val activeDocumentId: StateFlow<Int?> = _activeDocumentId.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("pt")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _errors = MutableStateFlow<List<MatchError>>(emptyList())
    val errors: StateFlow<List<MatchError>> = _errors.asStateFlow()

    private val _mascotState = MutableStateFlow(MascotState.IDLE)
    val mascotState: StateFlow<MascotState> = _mascotState.asStateFlow()

    private val _bubbleMessage = MutableStateFlow<String?>("Olá! Sou o seu Mascote Ortográfico. Vamos escrever juntos?")
    val bubbleMessage: StateFlow<String?> = _bubbleMessage.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Database flow of historic documents
    val documents: StateFlow<List<Document>> = repository.allDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Cloud Syncing Simulator State
    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<String>>(
        listOf(
            "[System] Inicializando sincronização de nuvem unificada...",
            "[System] Logado em miguelassis488@gmail.com como conta ativa.",
            "[Chrome] Extensão de navegador detectada: Conectada há 2 minutos.",
            "[iOS] iPhone de Miguel: Pareado com sucesso."
        )
    )
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    private var checkJob: Job? = null
    private var saveJob: Job? = null
    private var mascotIdleJob: Job? = null

    init {
        // Start speech bubble typing hints loop
        viewModelScope.launch {
            delay(1500)
            if (_errors.value.isEmpty()) {
                sendBubbleMessage(getFriendlyGreetingByLanguage(_selectedLanguage.value))
            }
        }
    }

    fun onTextChanged(newText: String) {
        _textInput.value = newText
        
        // Dynamic reaction to user typing
        if (_mascotState.value != MascotState.THINKING) {
            _mascotState.value = MascotState.IDLE
        }

        // Auto-save debounced (3 seconds)
        saveLocalDocumentDebounced()

        // Real-time grammar analysis debounced (1000ms)
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            delay(1200)
            triggerAnalysis()
        }
    }

    fun onTitleChanged(newTitle: String) {
        _documentTitle.value = newTitle
        saveLocalDocumentDebounced()
    }

    fun onLanguageSelected(lang: String) {
        if (_selectedLanguage.value != lang) {
            _selectedLanguage.value = lang
            val greeting = when(lang) {
                "en" -> "Language changed to English! Try typing 'recieve' or 'dont'."
                "es" -> "¡Idioma cambiado a Español! Intenta escribir 'ola' o 'habia'."
                "fr" -> "Langue changée en Français ! Écrivez pour que je puisse corriger."
                else -> "Idioma alterado para Português! Digite algo como 'andass-te' ou 'concerteza'."
            }
            sendBubbleMessage(greeting)
            _errors.value = emptyList() // clear previous errors for old language
            triggerAnalysis()
        }
    }

    fun triggerAnalysis() {
        checkJob?.cancel()
        viewModelScope.launch {
            _isAnalyzing.value = true
            _mascotState.value = MascotState.THINKING
            _bubbleMessage.value = when (_selectedLanguage.value) {
                "en" -> "Analyzing text with Gemini..."
                "es" -> "Analizando texto con Gemini..."
                "fr" -> "Analyse du texte avec Gemini..."
                else -> "Analisando seu texto com o Gemini..."
            }

            val result = repository.checkSpelling(_textInput.value, _selectedLanguage.value)
            _errors.value = result
            _isAnalyzing.value = false

            if (result.isEmpty()) {
                _mascotState.value = MascotState.HAPPY
                _bubbleMessage.value = when (_selectedLanguage.value) {
                    "en" -> "Fantastic! No spelling errors detected."
                    "es" -> "¡Excelente! No he encontrado ningún error."
                    "fr" -> "Parfait ! Aucune erreur détectée."
                    else -> "Excelente! Não encontrei nenhum erro no seu texto."
                }
                scheduleReturnToIdle()
            } else {
                _mascotState.value = MascotState.ALERT
                _bubbleMessage.value = when (_selectedLanguage.value) {
                    "en" -> "Oops! I found ${result.size} corrections. Tap the highlighted words!"
                    "es" -> "¡Atención! Encontré ${result.size} correcciones. ¡Toca las palabras!"
                    "fr" -> "Attention ! J'ai trouvé ${result.size} corrections."
                    else -> "Epa! Encontrei ${result.size} correções. Toque nas marcar de destaque no texto!"
                }
            }

            // After checking spelling, trigger cloud sync animation automatically so they see sync
            triggerCloudSyncSimulation()
        }
    }

    fun applyCorrection(error: MatchError) {
        val originalText = _textInput.value
        if (error.start in 0..originalText.length && error.end in 0..originalText.length) {
            val prefix = originalText.substring(0, error.start)
            val suffix = originalText.substring(error.end)
            val updatedText = prefix + error.replacement + suffix
            _textInput.value = updatedText

            // Visually update the errors list by removing the corrected item
            // and adjusting subsequent offsets
            val offsetDiff = error.replacement.length - (error.end - error.start)
            val updatedErrors = _errors.value.filter { it != error }.map { remaining ->
                if (remaining.start > error.start) {
                    remaining.copy(
                        start = remaining.start + offsetDiff,
                        end = remaining.end + offsetDiff
                    )
                } else {
                    remaining
                }
            }
            _errors.value = updatedErrors

            // Mascot turns happy for correcting a word
            _mascotState.value = MascotState.HAPPY
            _bubbleMessage.value = when (_selectedLanguage.value) {
                "en" -> "Awesome! Word corrected."
                "es" -> "¡Excelente corrección!"
                "fr" -> "Très bien corrigé !"
                else -> "Excelente correção! Palavra alterada com sucesso."
            }
            scheduleReturnToIdle()

            // Save document and trigger sync
            saveLocalDocument()
            triggerCloudSyncSimulation()
        }
    }

    fun selectDocument(document: Document) {
        _activeDocumentId.value = document.id
        _textInput.value = document.content
        _documentTitle.value = document.title
        _selectedLanguage.value = document.language
        _errors.value = emptyList()
        _bubbleMessage.value = "Abrindo documento '${document.title}'... Vamos revisar!"
        triggerAnalysis()
    }

    fun createNewDocument() {
        viewModelScope.launch {
            val title = when (_selectedLanguage.value) {
                "en" -> "New Document"
                "es" -> "Nuevo Documento"
                "fr" -> "Nouveau Document"
                else -> "Novo Documento"
            }
            val newDoc = Document(
                title = title,
                content = "",
                language = _selectedLanguage.value
            )
            val id = repository.saveDocument(newDoc)
            _activeDocumentId.value = id.toInt()
            _textInput.value = ""
            _documentTitle.value = title
            _errors.value = emptyList()
            _bubbleMessage.value = "Novo documento criado! Comece a digitar para me ver trabalhar."
            _mascotState.value = MascotState.HAPPY
            scheduleReturnToIdle()
        }
    }

    fun deleteCurrentDocument() {
        val currentId = _activeDocumentId.value ?: return
        viewModelScope.launch {
            val currentDoc = Document(
                id = currentId,
                title = _documentTitle.value,
                content = _textInput.value,
                language = _selectedLanguage.value
            )
            repository.deleteDocument(currentDoc)
            _activeDocumentId.value = null
            _textInput.value = ""
            _documentTitle.value = "Texto sem título"
            _errors.value = emptyList()
            _bubbleMessage.value = "Documento apagado. Quer começar um novo?"
        }
    }

    private fun saveLocalDocumentDebounced() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1500)
            saveLocalDocument()
        }
    }

    private fun saveLocalDocument() {
        val currentText = _textInput.value
        val title = _documentTitle.value
        val lang = _selectedLanguage.value
        val currentId = _activeDocumentId.value

        viewModelScope.launch {
            val doc = if (currentId != null) {
                Document(id = currentId, title = title, content = currentText, language = lang, updatedAt = System.currentTimeMillis())
            } else {
                Document(title = title, content = currentText, language = lang, updatedAt = System.currentTimeMillis())
            }
            val savedId = repository.saveDocument(doc)
            if (currentId == null) {
                _activeDocumentId.value = savedId.toInt()
            }
        }
    }

    fun triggerCloudSyncSimulation() {
        if (_isSyncingCloud.value) return
        viewModelScope.launch {
            _isSyncingCloud.value = true
            addSyncLog("Sincronizando projeto: '${_documentTitle.value}'...")
            delay(1000)
            addSyncLog("Sincronização em andamento. Atualizando dados entre Android, Browser Extension (Chrome/Edge) e aparelho iOS...")
            delay(700)
            addSyncLog("Backup criptografado enviado à nuvem na conta miguelassis488@gmail.com")
            delay(500)
            _isSyncingCloud.value = false
            addSyncLog("Sincronização concluída com sucesso! (Todos os dispositivos estão unificados em tempo real.)")
        }
    }

    private fun addSyncLog(message: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = sdf.format(Date())
        val formattedLog = "[$time] $message"
        
        val currentList = _syncLogs.value.toMutableList()
        currentList.add(0, formattedLog)
        if (currentList.size > 20) {
            currentList.removeAt(currentList.lastIndex)
        }
        _syncLogs.value = currentList
    }

    fun setMascotSpeaking(isSpeaking: Boolean) {
        _mascotState.value = if (isSpeaking) MascotState.SPEAKING else MascotState.IDLE
    }

    fun sendBubbleMessage(msg: String) {
        _bubbleMessage.value = msg
    }

    private fun scheduleReturnToIdle() {
        mascotIdleJob?.cancel()
        mascotIdleJob = viewModelScope.launch {
            delay(4000)
            _mascotState.value = MascotState.IDLE
        }
    }

    private fun getFriendlyGreetingByLanguage(lang: String): String {
        return when (lang) {
            "en" -> "Write anything here in English! I will highlight typos in real time."
            "es" -> "Escribe cualquier frase en Español. ¡Te ayudaré con la ortografía!"
            "fr" -> "Écrivez en Français. Je vais vérifier votre grammaire !"
            else -> "Olá! Digite seu texto em Português. Vou destacar erros gramaticais e ortográficos!"
        }
    }

    fun formatAndCorrectAll() {
        val activeErrors = _errors.value
        if (activeErrors.isEmpty()) {
            sendBubbleMessage("Seu texto já está impecável! Nada para corrigir.")
            return
        }
        
        viewModelScope.launch {
            _isAnalyzing.value = true
            _mascotState.value = MascotState.THINKING
            _bubbleMessage.value = "Processando auto-correção de todos os erros..."
            delay(800)
            
            // Order errors descending by start index to prevent shifting index problems on update!
            var currentText = _textInput.value
            val sortedErrors = activeErrors.sortedByDescending { it.start }
            for (error in sortedErrors) {
                if (error.start in 0..currentText.length && error.end in 0..currentText.length) {
                    val prefix = currentText.substring(0, error.start)
                    val suffix = currentText.substring(error.end)
                    currentText = prefix + error.replacement + suffix
                }
            }
            _textInput.value = currentText
            _errors.value = emptyList()
            _isAnalyzing.value = false
            _mascotState.value = MascotState.HAPPY
            _bubbleMessage.value = "Feito! Corrigi todos os pequenos problemas ortográficos que encontrei."
            scheduleReturnToIdle()
            saveLocalDocument()
            triggerCloudSyncSimulation()
        }
    }
}

class SpellCheckViewModelFactory(private val repository: NotesRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpellCheckViewModel::class.java)) {
            return SpellCheckViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

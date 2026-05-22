package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import java.util.Locale

class SpeechAssistant(
    private val context: Context,
    private val onInitComplete: (Boolean) -> Unit,
    private val onSpeechStateChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeechStateChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    onSpeechStateChanged(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onSpeechStateChanged(false)
                }
            })
            onInitComplete(true)
        } else {
            onInitComplete(false)
        }
    }

    fun speak(text: String, languageCode: String) {
        if (!isInitialized) return
        val locale = when (languageCode.lowercase()) {
            "en" -> Locale.US
            "es" -> Locale("es", "ES")
            "fr" -> Locale.FRANCE
            else -> Locale("pt", "BR")
        }

        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mascot_utterance_id")
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
            onSpeechStateChanged(false)
        }
    }

    fun shutdown() {
        tts?.shutdown()
    }
}

/**
 * Prepares the Speech-To-Text (Voice Dictation) intent customized to the current language locale.
 */
fun launchSpeechToText(
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    languageCode: String
) {
    val localeString = when (languageCode.lowercase()) {
        "en" -> "en-US"
        "es" -> "es-ES"
        "fr" -> "fr-FR"
        else -> "pt-BR"
    }

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeString)
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale o texto que deseja corrigir...")
    }
    try {
        launcher.launch(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

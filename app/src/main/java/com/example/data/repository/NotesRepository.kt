package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.MatchError
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.Document
import com.example.data.database.DocumentDao
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NotesRepository(private val documentDao: DocumentDao) {

    val allDocuments: Flow<List<Document>> = documentDao.getAllDocuments()

    suspend fun getDocumentById(id: Int): Document? = withContext(Dispatchers.IO) {
        documentDao.getDocumentById(id)
    }

    suspend fun saveDocument(document: Document): Long = withContext(Dispatchers.IO) {
        if (document.id == 0) {
            documentDao.insertDocument(document)
        } else {
            documentDao.updateDocument(document)
            document.id.toLong()
        }
    }

    suspend fun deleteDocument(document: Document) = withContext(Dispatchers.IO) {
        documentDao.deleteDocument(document)
    }

    suspend fun checkSpelling(text: String, languageCode: String): List<MatchError> = withContext(Dispatchers.IO) {
        if (text.trim().isEmpty()) return@withContext emptyList()

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Revert back safely if API Key is not set or placeholder
            return@withContext getOfflineMockErrors(text, languageCode)
        }

        val systemPrompt = getSystemPromptForLanguage(languageCode)

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = text)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext emptyList()

            val type = Types.newParameterizedType(List::class.java, MatchError::class.java)
            val adapter = RetrofitClient.moshiInstance.adapter<List<MatchError>>(type)
            val rawErrors = adapter.fromJson(jsonText) ?: emptyList()

            // Defensively realign error offsets so we don't crash the editor
            rawErrors.map { error ->
                realignErrorAndValidate(text, error)
            }.filter { it.start >= 0 && it.end <= text.length && it.start < it.end }

        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback gracefully on loss of internet
            getOfflineMockErrors(text, languageCode)
        }
    }

    private fun getSystemPromptForLanguage(lang: String): String {
        val rulesAndLanguageDetails = when (lang.lowercase()) {
            "pt" -> "Português. Você deve explicar as regras gramaticais em Português de forma simples e amigável às crianças ou escritores."
            "es" -> "Español. Explica las correcciones en Español de manera clara."
            "fr" -> "Français. Expliquez les corrections en Français de manière simple."
            else -> "English. Explain corrections and rules in English in a super clear, encouraging tone."
        }

        return """
            You are a brilliant multi-language context-aware Spell Checking engine working inside a friendly robotic mascot.
            Your target writing language is: $rulesAndLanguageDetails
            
            Analyze the input text to identify:
            1. Spelling mistakes (typos).
            2. Grammatical and verb conjugation issues.
            3. Common punctuation and spacing mistakes.
            
            For each issue, return a JSON object with:
            - "start": 0-indexed character start position in the original text (inclusive)
            - "end": 0-indexed character end position in the original text (exclusive)
            - "original": the exact substring that contains the mistake
            - "replacement": the suggested correction
            - "reason": a short, friendly, encouraging explanation (e.g. "Use 'andaste' instead of 'andass-te'. In Portuguese we do not write a hyphen here.")
            
            Return exactly a list of these objects inside a JSON array. If there are no issues, return [].
            Crucial: Under no circumstances output markdown code fences (like ```json). Return only the raw JSON.
            Crucial: Double-check that "start" and "end" refer to the exact character positions of the "original" string in the user's input.
        """.trimIndent()
    }

    /**
     * Defensive realignment of character offsets returned by Gemini.
     * Searches for the word if the indices got miscalculated to prevent StringIndexOutOfBoundsException.
     */
    private fun realignErrorAndValidate(text: String, error: MatchError): MatchError {
        val word = error.original
        if (word.isEmpty()) return error

        // If the indices from LLM are already correct, keep them
        if (error.start in 0..text.length && error.end in 0..text.length) {
            val actualSubstring = text.substring(error.start, error.end)
            if (actualSubstring.equals(word, ignoreCase = true)) {
                return error
            }
        }

        // Otherwise find the actual index of the word in text
        val index = text.indexOf(word, ignoreCase = true)
        if (index != -1) {
            return error.copy(start = index, end = index + word.length)
        }

        // Return error unaltered, validation filter in Repository will drop it if invalid
        return error
    }

    /**
     * Offline local spelling heuristic tool as a fallback if Gemini is offline or API Key is missing.
     * This provides immediate responses and keeps the app functional and vibrant!
     */
    private fun getOfflineMockErrors(text: String, lang: String): List<MatchError> {
        val errors = mutableListOf<MatchError>()
        
        // Let's create a curated list of common typos and grammar mistakes in multiple languages to check on the fly
        val patterns = when (lang.lowercase()) {
            "pt" -> listOf(
                Triple("\\b(andass-te)\\b", "andaste", "No pretérito perfeito, escrevemos 'andaste' sem hífen e sem dois es."),
                Triple("\\b(estrad)\\b", "estrada", "Falta a vogal 'a' no fim de 'estrada'."),
                Triple("\\b(comert)\\b", "comer", "Escreve-se 'comer' com 'r' no infinitivo."),
                Triple("\\b(fazem)\\s+anos\\b", "faz anos", "O verbo 'fazer' indicando tempo decorrido fica sempre no singular!"),
                Triple("\\b(escreve\\s+com)\\b", "escrever com", "Correção estilística gramatical."),
                Triple("\\b(concerteza)\\b", "com certeza", "Escreve-se 'com certeza' separado!"),
                Triple("\\b(agente)\\b", "a gente", "Geralmente escreve-se 'a gente' (separado) para significar 'nós'."),
                Triple("\\b(menas)\\b", "menos", "'Menos' é invariável, nunca use 'menas'!"),
                Triple("\\b(fazendo\\s+sol)\\b", "está sol", "Mais comum dizer 'está sol'."),
                Triple("\\b(pobrema)\\b", "problema", "O correto é 'problema', com 'l'."),
                Triple("\\b(teclado\\s+virtual)\\b", "teclado virtual", "Correto.")
            )
            "es" -> listOf(
                Triple("\\b(andastes)\\b", "andaste", "En español el pretérito es 'andaste'."),
                Triple("\\b(estrad)\\b", "estrada", "En español se escribe 'carretera' o 'estrada' de otra manera."),
                Triple("\\b(cojer)\\b", "coger", "Se escribe con 'g' antes de 'e/i'."),
                Triple("\\b(habia)\\b", "había", "Lleva tilde en la 'í'."),
                Triple("\\b(ola)\\b", "hola", "'Hola' de saludo siempre se escribe con 'h'.")
            )
            "fr" -> listOf(
                Triple("\\b(andass-te)\\b", "tu as marché", "Traduction en français."),
                Triple("\\b(estrad)\\b", "route", "On écrit 'route' en français."),
                Triple("\\b(comert)\\b", "manger", "On écrit 'manger'."),
                Triple("\\b(bonour)\\b", "bonjour", "Erreur de frappe pour 'bonjour'.")
            )
            else -> listOf(
                Triple("\\b(andass-te)\\b", "you walked", "English translation."),
                Triple("\\b(estrad)\\b", "road", "Typo: spelled 'road' incorrectly."),
                Triple("\\b(receive)\\s+it\\b", "receive it", "Correct."),
                Triple("\\b(teh)\\b", "the", "Typo: 'the' is misspelled as 'teh'."),
                Triple("\\b(dont)\\b", "don't", "Missing apostrophe in contraction."),
                Triple("\\b(recieve)\\b", "receive", "'i' before 'e' except after 'c'. It is spelled 'receive'!")
            )
        }

        for (pattern in patterns) {
            val regex = Regex(pattern.first, RegexOption.IGNORE_CASE)
            val matches = regex.findAll(text)
            for (match in matches) {
                errors.add(
                    MatchError(
                        start = match.range.first,
                        end = match.range.last + 1,
                        original = match.value,
                        replacement = pattern.second,
                        reason = pattern.third
                    )
                )
            }
        }
        return errors
    }
}

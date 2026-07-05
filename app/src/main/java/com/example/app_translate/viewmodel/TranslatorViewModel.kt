package com.example.app_translate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_translate.data.local.AppDatabase
import com.example.app_translate.data.local.HistoryEntity
import com.example.app_translate.data.model.Language
import com.example.app_translate.data.model.languages
import com.example.app_translate.data.repository.TranslateRepository
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class TranslatorUiState(
    val sourceLang: Language = languages[0],
    val targetLang: Language = languages[9],
    val inputText: String = "",
    val outputText: String = "",
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val detectedLanguage: Language? = null,
    val isFavorited: Boolean = false,
    val historyList: List<HistoryEntity> = emptyList(),
    val manualSource: Boolean = false
)

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TranslateRepository()
    private val db = AppDatabase.getDatabase(application)
    private val historyDao = db.historyDao()

    private val _uiState = MutableStateFlow(TranslatorUiState())
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    private var translateJob: Job? = null
    private val languageIdentifier = LanguageIdentification.getClient()

    init {
        viewModelScope.launch {
            historyDao.getAllHistory().collect { list ->
                _uiState.update { it.copy(historyList = list) }
            }
        }
    }

    // --- Translate Functions ---
    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, manualSource = false) }
        triggerTranslate()
    }

    fun onSourceLangChanged(lang: Language) {
        _uiState.update { it.copy(sourceLang = lang, detectedLanguage = null, manualSource = true) }
        triggerTranslate()
    }

    fun onTargetLangChanged(lang: Language) {
        _uiState.update { it.copy(targetLang = lang) }
        triggerTranslate()
    }

    fun applyDetectedLanguage() {
        val detected = _uiState.value.detectedLanguage
        if (detected != null) {
            _uiState.update { it.copy(sourceLang = detected, detectedLanguage = null, manualSource = true) }
            triggerTranslate()
        }
    }

    fun onSwapLanguages() {
        _uiState.update { state ->
            state.copy(
                sourceLang = state.targetLang,
                targetLang = state.sourceLang,
                detectedLanguage = null,
                isFavorited = false,
                manualSource = true
            )
        }
        triggerTranslate()
    }

    fun enableAutoDetect() {
        _uiState.update { it.copy(manualSource = false) }
        triggerTranslate()
    }

    fun toggleFavorite() {
        val state = _uiState.value
        if (state.outputText.isBlank()) return
        viewModelScope.launch {
            val existing = historyDao.findHistory(
                state.inputText, state.outputText,
                state.sourceLang.name, state.targetLang.name
            )
            if (existing != null) {
                historyDao.setFavorite(existing.id, !state.isFavorited)
                _uiState.update { it.copy(isFavorited = !it.isFavorited) }
            }
        }
    }

    fun toggleFavoriteFromHistory(id: Int) {
        viewModelScope.launch {
            val entry = historyDao.findHistoryById(id)
            if (entry != null) {
                historyDao.setFavorite(id, !entry.isFavorite)
            }
        }
    }

    private fun triggerTranslate() {
        val state = _uiState.value
        if (state.inputText.isBlank()) {
            _uiState.update { it.copy(outputText = "", isLoading = false, detectedLanguage = null) }
            return
        }
        translateJob?.cancel()
        translateJob = viewModelScope.launch {
            delay(600)
            _uiState.update { it.copy(isLoading = true, isError = false) }
            languageIdentifier.identifyLanguage(state.inputText)
                .addOnSuccessListener { code ->
                    if (code != "und") {
                        val detected = languages.find { it.code == code }
                        if (detected != null) {
                            val current = _uiState.value
                            if (!current.manualSource && code != current.sourceLang.code) {
                                _uiState.update { it.copy(sourceLang = detected, detectedLanguage = null) }
                            } else {
                                _uiState.update { it.copy(detectedLanguage = detected) }
                            }
                        }
                    }
                    performTranslation(_uiState.value)
                }
                .addOnFailureListener {
                    fallbackDetectAndTranslate(state.inputText)
                }
        }
    }

    private fun fallbackDetectAndTranslate(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://api.mymemory.translated.net/langdetect?q=" + URLEncoder.encode(text, "UTF-8"))
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val detectedCode = json.getJSONObject("responseData").optString("detectedLanguage", "")
                if (detectedCode.isNotBlank()) {
                    val detected = languages.find { it.code == detectedCode || it.apiCode == detectedCode }
                    if (detected != null) {
                        val current = _uiState.value
                        if (!current.manualSource && detectedCode != current.sourceLang.code && detectedCode != current.sourceLang.apiCode) {
                            _uiState.update { it.copy(sourceLang = detected, detectedLanguage = null) }
                        } else {
                            _uiState.update { it.copy(detectedLanguage = detected) }
                        }
                    }
                }
            } catch (_: Exception) { }
            performTranslation(_uiState.value)
        }
    }

    private fun performTranslation(state: TranslatorUiState) {
        viewModelScope.launch {
            val result = repository.translate(
                state.inputText,
                state.sourceLang.apiCode,
                state.targetLang.apiCode
            )
            result.fold(
                onSuccess = { translated ->
                    _uiState.update { it.copy(outputText = translated, isLoading = false) }
                    addToHistory(
                        state.inputText,
                        translated,
                        state.sourceLang.name,
                        state.targetLang.name
                    )
                    val existing = historyDao.findHistory(
                        state.inputText, translated,
                        state.sourceLang.name, state.targetLang.name
                    )
                    _uiState.update { it.copy(isFavorited = existing?.isFavorite == true) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            outputText = "Translation failed.",
                            isLoading = false,
                            isError = true
                        )
                    }
                }
            )
        }
    }

    // --- History Functions ---
    private fun addToHistory(source: String, target: String, sLang: String, tLang: String) {
        if (source.isBlank() || target.isBlank()) return
        viewModelScope.launch {
            historyDao.insertHistory(
                HistoryEntity(
                    sourceText = source,
                    targetText = target,
                    sourceLang = sLang,
                    targetLang = tLang
                )
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch { historyDao.clearAll() }
    }
}
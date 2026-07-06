package com.example.app_translate.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_translate.BuildConfig
import com.example.app_translate.data.model.Language
import com.example.app_translate.data.model.languages
import com.example.app_translate.ui.theme.PurpleColor
import com.example.app_translate.ui.theme.WhiteColor
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private val languageToolCodes = setOf("en-US", "en", "pt-PT", "pt-BR", "pt", "id", "es", "fr", "de", "ja", "zh", "ar", "ru", "it", "nl")

data class GrammarError(val offset: Int, val length: Int, val message: String, val replacement: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(
    selectedLang: Language,
    onLangClick: () -> Unit,
    onLanguageDetected: ((Language) -> Unit)? = null
) {
    var inputText by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("Check Grammar") }
    var grammarErrors by remember { mutableStateOf(listOf<GrammarError>()) }
    var detectedLang by remember { mutableStateOf<Language?>(null) }
    var manualLang by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val languageIdentifier = remember { LanguageIdentification.getClient() }

    val modes = listOf("Check Grammar", "Formal", "Expand")

    suspend fun processWithGemini(text: String, prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val key = BuildConfig.GEMINI_API_KEY
                if (key.isBlank()) return@withContext "Gemini API key not configured. Add GEMINI_API_KEY to local.properties"
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + key)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                val body = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            })
                        })
                    })
                }
                conn.outputStream.write(body.toString().toByteArray())
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }

    suspend fun grammarViaGemini(text: String, lang: String): Pair<String, List<GrammarError>> {
        val langPrompt = "Check the grammar of this $lang text. Return valid JSON only with 'corrected' (corrected text) and 'errors' (array of {offset, length, message, replacement}). Text:\n\n$text"
        val response = processWithGemini(text, langPrompt)
        val cleaned = response.replace("""```json""".toRegex(), "").replace("""```""".toRegex(), "").trim()
        return try {
            val json = JSONObject(cleaned)
            val corrected = json.getString("corrected")
            val arr = json.getJSONArray("errors")
            val errors = mutableListOf<GrammarError>()
            for (i in 0 until arr.length()) {
                val e = arr.getJSONObject(i)
                errors.add(GrammarError(e.getInt("offset"), e.getInt("length"), e.getString("message"), e.optString("replacement", "")))
            }
            Pair(corrected, errors)
        } catch (e: Exception) {
            Pair(response, emptyList())
        }
    }

    suspend fun checkGrammar(text: String, langCode: String, langName: String): Pair<String, List<GrammarError>> {
        return withContext(Dispatchers.IO) {
            if (langCode !in languageToolCodes) {
                return@withContext grammarViaGemini(text, langName)
            }
            try {
                val url = URL("https://api.languagetool.org/v2/check")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.doOutput = true
                val body = "text=" + java.net.URLEncoder.encode(text, "UTF-8") + "&language=$langCode"
                conn.outputStream.write(body.toByteArray())
                val responseCode = conn.responseCode
                if (responseCode !in 200..299) {
                    return@withContext grammarViaGemini(text, langName)
                }
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val matches = json.getJSONArray("matches")
                val errors = mutableListOf<GrammarError>()
                val sb = StringBuilder(text)
                for (i in matches.length() - 1 downTo 0) {
                    val m = matches.getJSONObject(i)
                    val offset = m.getInt("offset")
                    val length = m.getInt("length")
                    val message = m.getString("message")
                    val repl = m.getJSONArray("replacements")
                    val replacement = if (repl.length() > 0) repl.getJSONObject(0).getString("value") else ""
                    errors.add(GrammarError(offset, length, message, replacement))
                    if (replacement.isNotEmpty()) {
                        sb.replace(offset, offset + length, replacement)
                    }
                }
                errors.reverse()
                Pair(sb.toString(), errors)
            } catch (e: Exception) {
                grammarViaGemini(text, langName)
            }
        }
    }

    suspend fun processText(text: String, mode: String, langName: String): String {
        return when (mode) {
            "Check Grammar" -> text
            else -> {
                val prompt = when (mode) {
                    "Formal"  -> "Rewrite the following $langName text in a professional formal style. Only show the result:\n\n$text"
                    "Expand"  -> "Expand the following $langName text with more complete details. Only show the result:\n\n$text"
                    else      -> text
                }
                processWithGemini(text, prompt)
            }
        }
    }

    LaunchedEffect(inputText) {
        if (inputText.isBlank()) {
            detectedLang = null
            return@LaunchedEffect
        }
        delay(600)
        languageIdentifier.identifyLanguage(inputText)
            .addOnSuccessListener { code ->
                if (code != "und") {
                    val d = languages.find { it.code == code }
                    if (d != null) {
                        detectedLang = d
                        if (!manualLang) {
                            onLanguageDetected?.invoke(d)
                        }
                    }
                }
            }
    }

    LaunchedEffect(inputText, selectedMode) {
        if (inputText.isBlank() || selectedMode != "Check Grammar") return@LaunchedEffect
        delay(800)
        val lang = detectedLang ?: selectedLang
        val (corrected, errors) = checkGrammar(inputText, lang.apiCode, lang.name)
        grammarErrors = errors
        resultText = corrected
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF8F9FA))
            .padding(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFEEEEEE),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Language: ", fontSize = 13.sp, color = Color.Gray)
                TextButton(onClick = { manualLang = true; onLangClick() }, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        selectedLang.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleColor
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = PurpleColor, modifier = Modifier.size(18.dp))
                }
                if (detectedLang != null && detectedLang != selectedLang) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Detected: ${detectedLang?.name}",
                        fontSize = 11.sp,
                        color = PurpleColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Mode",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modes.forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { selectedMode = mode; grammarErrors = emptyList() },
                    label = { Text(mode, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurpleColor,
                        selectedLabelColor = WhiteColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteColor),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "INPUT TEXT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                val annotatedText = if (selectedMode == "Check Grammar" && grammarErrors.isNotEmpty()) {
                    buildAnnotatedString {
                        append(inputText)
                        for (err in grammarErrors) {
                            val start = err.offset.coerceIn(0, inputText.length)
                            val end = (err.offset + err.length).coerceIn(0, inputText.length)
                            if (end > start) {
                                addStyle(
                                    SpanStyle(color = Color.Red, textDecoration = TextDecoration.Underline),
                                    start, end
                                )
                            }
                        }
                    }
                } else null

                if (annotatedText != null) {
                    var textFieldValue by remember { mutableStateOf(TextFieldValue(annotatedText)) }
                    LaunchedEffect(grammarErrors, inputText) {
                        val newTfv = TextFieldValue(annotatedText)
                        val s = textFieldValue.selection
                        textFieldValue = newTfv.copy(selection = androidx.compose.ui.text.TextRange(
                            start = s.start.coerceIn(0, newTfv.text.length),
                            end = s.end.coerceIn(0, newTfv.text.length)
                        ))
                    }
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            if (it.text.length <= 1000) inputText = it.text
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 300.dp),
                        textStyle = TextStyle(fontSize = 16.sp, color = Color.Black, lineHeight = 24.sp),
                        cursorBrush = SolidColor(PurpleColor)
                    )
                } else {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { if (it.length <= 1000) inputText = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 300.dp),
                        placeholder = { Text("Enter the text you want to improve...") },
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(fontSize = 16.sp, color = Color.Black, lineHeight = 24.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleColor,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${inputText.length}/1000",
                        fontSize = 12.sp,
                        color = if (inputText.length > 900) Color.Red else Color.Gray
                    )
                    TextButton(onClick = { inputText = ""; resultText = ""; grammarErrors = emptyList() }) {
                        Text("Clear", color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedMode == "Check Grammar") {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF3E0),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (grammarErrors.isEmpty() && inputText.isNotBlank()) "No grammar issues found"
                        else if (grammarErrors.isNotEmpty()) "${grammarErrors.size} issue${if (grammarErrors.size > 1) "s" else ""} found"
                        else "Grammar check runs automatically as you type",
                        fontSize = 13.sp,
                        color = Color(0xFF4A4A4A)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        isLoading = true
                        resultText = ""
                        grammarErrors = emptyList()
                        scope.launch {
                            resultText = processText(inputText, selectedMode, selectedLang.name)
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleColor),
                enabled = inputText.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = WhiteColor, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    if (isLoading) "Processing..." else "✨ Process with AI",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (selectedMode == "Check Grammar" && grammarErrors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteColor),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ERRORS (${grammarErrors.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    grammarErrors.forEach { err ->
                        val end = minOf(err.offset + err.length, inputText.length)
                        val wrongText = if (end > err.offset) inputText.substring(err.offset, end) else ""
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, null, tint = Color.Red, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("\"$wrongText\"", fontSize = 14.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                Text(err.message, fontSize = 13.sp, color = Color.Gray)
                                if (err.replacement.isNotEmpty()) {
                                    Text("\u2192 ${err.replacement}", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    if (resultText != inputText) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "CORRECTED TEXT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())
                        ) {
                            Text(text = resultText, fontSize = 16.sp, color = Color.Black, lineHeight = 24.sp)
                        }
                        TextButton(onClick = {
                            inputText = resultText
                            resultText = ""
                            grammarErrors = emptyList()
                        }) {
                            Text("Use correction", color = PurpleColor, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = resultText.isNotEmpty() && selectedMode != "Check Grammar",
            enter = fadeIn() + slideInVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteColor),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("RESULT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            TextButton(onClick = { inputText = resultText; resultText = "" }) {
                                Text("Use this text", color = PurpleColor, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())
                        ) {
                            Text(text = resultText, fontSize = 16.sp, color = Color.Black, lineHeight = 24.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

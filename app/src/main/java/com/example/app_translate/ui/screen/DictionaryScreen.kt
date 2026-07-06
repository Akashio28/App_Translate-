package com.example.app_translate.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_translate.BuildConfig
import com.example.app_translate.data.model.Language
import com.example.app_translate.data.model.languages
import com.example.app_translate.ui.components.LanguagePickerDialog
import com.example.app_translate.viewmodel.TranslatorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
data class DictionaryMeaning(
    val partOfSpeech: String,
    val definitions: List<String>,
    val synonyms: List<String>,
    val antonyms: List<String>
)
data class DictionaryResult(
    val word: String,
    val phonetic: String,
    val meanings: List<DictionaryMeaning>,
    val audioUrl: String
)
private val DictBlue     = Color(0xFF1A56DB)
private val DictBlueBg   = Color(0xFFDEEAFF)
private val DictGrayBg   = Color(0xFFF5F5F5)
private val DictTextGray = Color(0xFF888888)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    viewModel: TranslatorViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<DictionaryResult?>(null) }
    var errorMsg by remember { mutableStateOf("") }
    var dictLang by remember { mutableStateOf(languages.first { it.code == "en" }) }
    var showLangPicker by remember { mutableStateOf(false) }

    if (showLangPicker) {
        LanguagePickerDialog(
            title = "Choose Dictionary Language",
            currentLang = dictLang,
            onLanguageSelected = { dictLang = it; showLangPicker = false },
            onDismiss = { showLangPicker = false }
        )
    }

    suspend fun geminiLookup(word: String, lang: Language): DictionaryResult? {
        return withContext(Dispatchers.IO) {
            try {
                val key = BuildConfig.GEMINI_API_KEY
                if (key.isBlank()) return@withContext null
                val langInstruction = if (lang.code == "tet") "IMPORTANT: All output must be in Tetum language, NOT Portuguese. Use proper Tetum words like naran, hanesan, la hanesan." else ""
                val prompt = "Act as a dictionary. Look up the word \"$word\" in ${lang.name}. $langInstruction Return valid JSON only with: word, phonetic (pronunciation), meanings (array of {partOfSpeech (in ${lang.name} language, e.g. noun=substantivo for Portuguese, kata benda for Indonesian, naran for Tetum), definitions (array of string, max 3, ALL definitions must be in ${lang.name} language), synonyms (array of string, max 5, ALL synonyms must be in ${lang.name} language), antonyms (array of string, max 5, ALL antonyms must be in ${lang.name} language)}). No explanation outside JSON. CRITICAL: The word, phonetic, definitions, synonyms, antonyms, and partOfSpeech must ALL be in ${lang.name} language."
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key")
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
                val text = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                val cleaned = text.replace("""```json""".toRegex(), "").replace("""```""".toRegex(), "").trim()
                val data = JSONObject(cleaned)
                val wordStr = data.optString("word", word)
                val phonetic = data.optString("phonetic", "")
                val meaningsArr = data.optJSONArray("meanings") ?: JSONArray()
                val meanings = mutableListOf<DictionaryMeaning>()
                for (i in 0 until meaningsArr.length()) {
                    val m = meaningsArr.getJSONObject(i)
                    val pos = m.optString("partOfSpeech", "")
                    val defsArr = m.optJSONArray("definitions") ?: JSONArray()
                    val defs = (0 until defsArr.length()).map { defsArr.getString(it) }
                    val synsArr = m.optJSONArray("synonyms") ?: JSONArray()
                    val syns = (0 until synsArr.length()).map { synsArr.getString(it) }
                    val antsArr = m.optJSONArray("antonyms") ?: JSONArray()
                    val ants = (0 until antsArr.length()).map { antsArr.getString(it) }
                    meanings.add(DictionaryMeaning(pos, defs, syns, ants))
                }
                DictionaryResult(wordStr, phonetic, meanings, "")
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun lookupWord(word: String, lang: Language) {
        if (word.isBlank()) return
        if (lang.code != "en") {
            val geminiResult = geminiLookup(word, lang)
            if (geminiResult != null) {
                result = geminiResult
                errorMsg = ""
            } else {
                errorMsg = "Failed to look up \"$word\" in ${lang.name}"
                result = null
            }
            isLoading = false
            return
        }
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(word.trim(), "UTF-8")
                val url = URL("https://api.dictionaryapi.dev/api/v2/entries/en/$encoded")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val code = conn.responseCode
                if (code != 200) {
                    withContext(Dispatchers.Main) {
                        errorMsg = "\"$word\" not found"
                        result = null
                        isLoading = false
                    }
                    return@withContext
                }
                val response = conn.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                val entry = jsonArray.getJSONObject(0)

                val wordStr = entry.optString("word", word)
                val phonetic = entry.optString("phonetic", "")

                var audioUrl = ""
                val phoneticsArr = entry.optJSONArray("phonetics")
                if (phoneticsArr != null) {
                    for (i in 0 until phoneticsArr.length()) {
                        val ph = phoneticsArr.getJSONObject(i)
                        val au = ph.optString("audio", "")
                        if (au.isNotBlank()) { audioUrl = au; break }
                    }
                }

                val meaningsArr = entry.optJSONArray("meanings") ?: JSONArray()
                val meanings = mutableListOf<DictionaryMeaning>()
                for (i in 0 until meaningsArr.length()) {
                    val m = meaningsArr.getJSONObject(i)
                    val pos = m.optString("partOfSpeech", "")
                    val defsArr = m.optJSONArray("definitions") ?: JSONArray()
                    val defs = mutableListOf<String>()
                    for (j in 0 until minOf(defsArr.length(), 3)) {
                        val d = defsArr.getJSONObject(j).optString("definition", "")
                        if (d.isNotBlank()) defs.add(d)
                    }
                    val synsArr = m.optJSONArray("synonyms") ?: JSONArray()
                    val syns = (0 until minOf(synsArr.length(), 5)).map { synsArr.getString(it) }
                    val antsArr = m.optJSONArray("antonyms") ?: JSONArray()
                    val ants = (0 until minOf(antsArr.length(), 5)).map { antsArr.getString(it) }
                    meanings.add(DictionaryMeaning(pos, defs, syns, ants))
                }

                withContext(Dispatchers.Main) {
                    result = DictionaryResult(wordStr, phonetic, meanings, audioUrl)
                    errorMsg = ""
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMsg = "Gagal mencari kata: ${e.message}"
                    result = null
                    isLoading = false
                }
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF444444))
            }
            Text(
                "Dictionary",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { showLangPicker = true }) {
                Text(
                    dictLang.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DictBlue
                )
                Icon(
                    Icons.Default.ArrowDropDown, null,
                    tint = DictBlue, modifier = Modifier.size(18.dp)
                )
            }
        }
        // SEARCH BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search for a word in ${dictLang.name}...") },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = DictTextGray)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            result = null
                            errorMsg = ""
                        }) {
                            Icon(Icons.Default.Close, null, tint = DictTextGray)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DictBlue,
                    unfocusedBorderColor = Color.LightGray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (searchQuery.isNotBlank()) {
                        isLoading = true
                        result = null
                        errorMsg = ""
                        scope.launch { lookupWord(searchQuery, dictLang) }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DictBlue),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text("Search")
            }
        }

        // CONTENT
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DictBlue)
                }
            }

            errorMsg.isNotEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(errorMsg, color = Color.Gray, fontSize = 15.sp)
                    }
                }
            }

            result == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📖", fontSize = 56.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Find the meaning of a ${dictLang.name} word",
                            color = Color.Gray,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Type a word to look up its definition",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )

                    }
                }
            }

            else -> {
                val res = result!!
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = DictBlueBg),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        res.word,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DictBlue
                                    )
                                    if (res.phonetic.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            res.phonetic,
                                            fontSize = 16.sp,
                                            fontStyle = FontStyle.Italic,
                                            color = Color(0xFF555555)
                                        )
                                    }
                                }
                            }
                        }

                        itemsIndexed(res.meanings) { index, meaning ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DictGrayBg
                                    ) {
                                        Text(
                                            meaning.partOfSpeech,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DictBlue,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    meaning.definitions.forEachIndexed { i, def ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                "${i + 1}.",
                                                fontSize = 14.sp,
                                                color = DictBlue,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(24.dp)
                                            )
                                            Text(
                                                def,
                                                fontSize = 15.sp,
                                                color = Color(0xFF1A1A1A),
                                                lineHeight = 22.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (i < meaning.definitions.size - 1) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }

                                    if (meaning.synonyms.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = Color(0xFFF0F0F0))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            "Synonyms",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DictTextGray
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            meaning.synonyms.take(4).forEach { syn ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = DictBlueBg
                                                ) {
                                                    Text(
                                                        syn,
                                                        fontSize = 13.sp,
                                                        color = DictBlue,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (meaning.antonyms.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            "Antonyms",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DictTextGray
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            meaning.antonyms.take(4).forEach { ant ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFFFEEEE)
                                                ) {
                                                    Text(
                                                        ant,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFFCC0000),
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}
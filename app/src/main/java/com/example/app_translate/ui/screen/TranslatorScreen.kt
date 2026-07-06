package com.example.app_translate.ui.screen
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app_translate.data.model.languages
import com.example.app_translate.ui.components.LanguagePickerDialog
import com.example.app_translate.viewmodel.TranslatorViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween

private val DeepLBlue      = Color(0xFF1A56DB)
private val DeepLBlueBg    = Color(0xFFDEEAFF)
private val DeepLGrayBg    = Color(0xFFF0F0F0)
private val DeepLDarkBar   = Color(0xFF4A4A4A)
private val DeepLTextBlack = Color(0xFF1A1A1A)
private val DeepLTextGray  = Color(0xFFAAAAAA)
private val DeepLDivider  = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    tts: TextToSpeech?,
    ttsReady: () -> Boolean,
    viewModel: TranslatorViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var currentTab       by remember { mutableStateOf("translate") }
    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }
    var autoDetect by rememberSaveable { mutableStateOf(true) }

    var undoStack by rememberSaveable { mutableStateOf(listOf<String>()) }
    var redoStack by rememberSaveable { mutableStateOf(listOf<String>()) }

    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (spoken != null) {
            undoStack = undoStack + uiState.inputText
            redoStack = emptyList()
            viewModel.onInputChanged(spoken)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val extracted = visionText.text
                    if (extracted.isNotBlank()) {
                        undoStack = undoStack + uiState.inputText
                        redoStack = emptyList()
                        viewModel.onInputChanged(extracted)
                        Toast.makeText(context, "Text extracted successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No text found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to read text from image", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open image", Toast.LENGTH_SHORT).show()
        }
    }

    fun speakText(text: String, langCode: String) {
        if (!ttsReady() || text.isBlank()) return
        val locale = when (langCode) {
            "en" -> Locale.US
            "pt" -> Locale("pt", "PT")
            "id" -> Locale("in", "ID")
            "es" -> Locale("es", "ES")
            "fr" -> Locale("fr", "FR")
            "ja" -> Locale.JAPAN
            "de" -> Locale.GERMANY
            "ar" -> Locale("ar", "SA")
            "zh" -> Locale.CHINA
            "tet" -> Locale("pt", "PT")
            else -> Locale.US
        }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun copyText(text: String) {
        if (text.isBlank()) return
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("text", text))
        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
    }

    fun pasteText() {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val pasted = cb.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        undoStack = undoStack + uiState.inputText
        redoStack = emptyList()
        viewModel.onInputChanged(pasted)
    }

    fun shareText(text: String) {
        if (text.isBlank()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    }

    fun startVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, uiState.sourceLang.code)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        try { voiceLauncher.launch(intent) }
        catch (e: Exception) {
            Toast.makeText(context, "Voice not available", Toast.LENGTH_SHORT).show()
        }
    }

    fun onInputWithHistory(newText: String) {
        undoStack = undoStack + uiState.inputText
        redoStack = emptyList()
        viewModel.onInputChanged(newText)
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack = redoStack + uiState.inputText
            val prev = undoStack.last()
            undoStack = undoStack.dropLast(1)
            viewModel.onInputChanged(prev)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack = undoStack + uiState.inputText
            val next = redoStack.last()
            redoStack = redoStack.dropLast(1)
            viewModel.onInputChanged(next)
        }
    }

    if (showSourcePicker) {
        LanguagePickerDialog(
            title = "Choose Source Language",
            currentLang = uiState.sourceLang,
            onLanguageSelected = { autoDetect = false; viewModel.onSourceLangChanged(it); showSourcePicker = false },
            onDismiss = { showSourcePicker = false },
            showAutoDetect = true,
            onAutoDetectSelected = {
                autoDetect = true
                viewModel.enableAutoDetect()
                showSourcePicker = false
            }
        )
    }
    if (showTargetPicker) {
        LanguagePickerDialog(
            title = "Choose Target Language",
            currentLang = uiState.targetLang,
            onLanguageSelected = { viewModel.onTargetLangChanged(it); showTargetPicker = false },
            onDismiss = { showTargetPicker = false }
        )
    }

    var writeLang by remember { mutableStateOf(languages.first { it.code == "en" }) }
    var showWriteLangPicker by remember { mutableStateOf(false) }
    if (showWriteLangPicker) {
        LanguagePickerDialog(
            title = "Choose Language",
            currentLang = writeLang,
            onLanguageSelected = { writeLang = it; showWriteLangPicker = false },
            onDismiss = { showWriteLangPicker = false }
        )
    }

    AnimatedContent(targetState = currentTab, transitionSpec = {
        (fadeIn(animationSpec = tween(250)) + slideInHorizontally { w -> w / 5 }).togetherWith(
            fadeOut(animationSpec = tween(250)) + slideOutHorizontally { w -> -w / 5 }
        )
    }, label = "tabContent") { tab ->
        when (tab) {

        "camera" -> {
            CameraScreen(
                viewModel = viewModel,
                tts = tts,
                ttsReady = ttsReady,
                onBack = { currentTab = "translate" }
            )
        }

        "history" -> {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { currentTab = "translate" }
            )
        }

        "dictionary" -> {
            DictionaryScreen(
                viewModel = viewModel,
                onBack = { currentTab = "translate" }
            )
        }

        "favorites" -> {
            FavoritesScreen(
                viewModel = viewModel,
                onBack = { currentTab = "translate" }
            )
        }

        "write" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(DeepLGrayBg)
                            .clickable { currentTab = "translate" }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, null, tint = Color(0xFF888888), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Translator", color = Color(0xFF888888), fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.5.dp, Color(0xFF7B61FF), RoundedCornerShape(50))
                            .background(Color.White)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF7B61FF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Write", color = Color(0xFF7B61FF), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                WriteScreen(
                    selectedLang = writeLang,
                    onLangClick = { showWriteLangPicker = true },
                    onLanguageDetected = { lang -> writeLang = lang }
                )
            }
        }

        else -> {
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.5.dp, DeepLBlue, RoundedCornerShape(50))
                            .background(Color.White)
                            .clickable { currentTab = "translate" }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, null, tint = DeepLBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Translator", color = DeepLBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(DeepLGrayBg)
                            .clickable { currentTab = "write" }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF888888), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Write", color = Color(0xFF888888), fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { currentTab = "history" }) {
                        Icon(Icons.Default.History, null, tint = Color(0xFF888888), modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = { currentTab = "dictionary" }) {
                        Icon(Icons.Default.MenuBook, null, tint = Color(0xFF888888), modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = { currentTab = "favorites" }) {
                        Icon(Icons.Default.Bookmark, null, tint = DeepLBlue, modifier = Modifier.size(26.dp))
                    }
                }

                                // KONTEN
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {

                    // INPUT AREA
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 180.dp)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        if (uiState.inputText.isEmpty()) {
                            Text(
                                "Type, paste, or translate from the source below",
                                color = DeepLTextGray, fontSize = 16.sp, lineHeight = 24.sp
                            )
                        }
                        BasicTextField(
                            value = uiState.inputText,
                            onValueChange = { onInputWithHistory(it) },
                            textStyle = TextStyle(fontSize = 20.sp, color = DeepLTextBlack, lineHeight = 28.sp),
                            cursorBrush = SolidColor(DeepLBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (uiState.inputText.isEmpty()) {
                        Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DeepLBlueBg,
                                modifier = Modifier.clickable { pasteText() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ContentPaste, null, tint = DeepLBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Paste", color = DeepLBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (uiState.inputText.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { speakText(uiState.inputText, uiState.sourceLang.code) }) {
                                Icon(Icons.Default.VolumeUp, null, tint = DeepLTextBlack, modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = { undo() }) {
                                Icon(Icons.AutoMirrored.Filled.Undo, null,
                                    tint = if (undoStack.isNotEmpty()) DeepLTextBlack else DeepLTextGray,
                                    modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = { redo() }) {
                                Icon(Icons.AutoMirrored.Filled.Redo, null,
                                    tint = if (redoStack.isNotEmpty()) DeepLTextBlack else DeepLTextGray,
                                    modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                                Icon(Icons.Default.FolderOpen, null, tint = DeepLTextBlack, modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = { currentTab = "camera" }) {
                                Icon(Icons.Default.PhotoCamera, null, tint = DeepLTextBlack, modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = { startVoice() }) {
                                Icon(Icons.Default.Mic, null, tint = DeepLTextBlack, modifier = Modifier.size(22.dp))
                            }
                        }
                    } else {
                        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                            IconButton(onClick = { undo() }) {
                                Icon(Icons.AutoMirrored.Filled.Undo, null, tint = DeepLTextGray, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { redo() }) {
                                Icon(Icons.AutoMirrored.Filled.Redo, null, tint = DeepLTextGray, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(60.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(60.dp).clip(CircleShape)
                                    .background(DeepLGrayBg)
                                    .clickable { galleryLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.FolderOpen, null, tint = DeepLTextBlack, modifier = Modifier.size(26.dp)) }
                            Spacer(modifier = Modifier.width(20.dp))
                            Box(
                                modifier = Modifier.size(68.dp).clip(CircleShape)
                                    .background(DeepLGrayBg)
                                    .clickable { currentTab = "camera" },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.PhotoCamera, null, tint = DeepLTextBlack, modifier = Modifier.size(28.dp)) }
                            Spacer(modifier = Modifier.width(20.dp))
                            Box(
                                modifier = Modifier.size(60.dp).clip(CircleShape)
                                    .background(DeepLGrayBg)
                                    .clickable { startVoice() },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Mic, null, tint = DeepLTextBlack, modifier = Modifier.size(26.dp)) }
                        }
                    }

                    // OUTPUT
                    if (uiState.inputText.isNotEmpty()) {
                        HorizontalDivider(color = DeepLDivider, thickness = 1.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 160.dp)
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(color = DeepLBlue, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = if (uiState.isError) "Translation failed" else uiState.outputText,
                                    fontSize = 20.sp, lineHeight = 28.sp,
                                    color = if (uiState.isError) Color.Red else DeepLBlue
                                )
                            }
                        }

                        if (!uiState.isLoading && uiState.outputText.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { speakText(uiState.outputText, uiState.targetLang.code) }) {
                                    Icon(Icons.Default.VolumeUp, null, tint = DeepLTextBlack, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            if (uiState.isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            null,
                            tint = if (uiState.isFavorited) DeepLBlue else DeepLTextBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                                IconButton(onClick = { shareText(uiState.outputText) }) {
                                    Icon(Icons.Default.Share, null, tint = DeepLTextBlack, modifier = Modifier.size(22.dp))
                                }
                                IconButton(onClick = { copyText(uiState.outputText) }) {
                                    Icon(Icons.Default.ContentCopy, null, tint = DeepLTextBlack, modifier = Modifier.size(22.dp))
                                }
                            }

                        }
                    }
                }

                    // LANGUAGE BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DeepLDarkBar,
                            modifier = Modifier.fillMaxWidth().clickable { showSourcePicker = true }
                        ) {
                            Text(
                                if (autoDetect && uiState.detectedLanguage != null) uiState.detectedLanguage!!.name
                                else uiState.sourceLang.name,
                                color = Color.White, fontSize = 15.sp,
                                modifier = Modifier.padding(vertical = 13.dp).wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                        if (uiState.detectedLanguage != null && uiState.detectedLanguage != uiState.sourceLang) {
                            Text(
                                "Detected: ${uiState.detectedLanguage?.name}",
                                color = DeepLBlue, fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { autoDetect = false; viewModel.onSwapLanguages() },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.CompareArrows, null,
                            tint = Color(0xFF444444), modifier = Modifier.size(24.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DeepLDarkBar,
                        modifier = Modifier.weight(1f).clickable { showTargetPicker = true }
                    ) {
                        Text(
                            uiState.targetLang.name, color = Color.White, fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 13.dp).wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}
}
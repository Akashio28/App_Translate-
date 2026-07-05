# TranslaTecho — Dokumentasi Lengkap

> Aplikasi Translasi Multibahasa Android
> Package: `com.example.app_translate` | Target SDK: 35 | Min SDK: 24

---

## Daftar Isi

1. [Sekilas Tentang App](#1-sekilas-tentang-app)
2. [10 Bahasa yang Didukung](#2-10-bahasa-yang-didukung)
3. [Fitur Lengkap](#3-fitur-lengkap)
   - 3.1 Splash Screen
   - 3.2 Translator (Fitur Utama)
   - 3.3 Write (Grammar & AI Writing)
   - 3.4 Dictionary
   - 3.5 History
   - 3.6 Favorites
   - 3.7 Camera OCR
4. [Arsitektur & Cara Kerja](#4-arsitektur--cara-kerja)
   - 4.1 MVVM Architecture
   - 4.2 StateFlow
   - 4.3 Alur Data Translator
   - 4.4 Alur Data Grammar Check
   - 4.5 Alur Data Dictionary
5. [API & Service](#5-api--service)
6. [Database](#6-database)
7. [Teknologi & Library](#7-teknologi--library)
8. [Struktur Project](#8-struktur-project)
9. [Cara Build & Install](#9-cara-build--install)
10. [Catatan Penting](#10-catatan-penting)

---

## 1. Sekilas Tentang App

**TranslaTecho** adalah aplikasi Android all-in-one untuk:
- **Menerjemahkan** teks antar 10 bahasa
- **Memeriksa grammar** secara otomatis (live)
- **Mencari arti kata** di dictionary
- **Menulis ulang** teks dengan gaya Formal/Casual/Expand

Dibangun dengan **Kotlin + Jetpack Compose**, menggunakan arsitektur **MVVM** dengan **StateFlow**.

Target utama: Pengguna di Timor-Leste, dengan **Tetum sebagai bahasa default**.

---

## 2. 10 Bahasa yang Didukung

| # | Bahasa | Kode ISO | Kode API | Catatan |
|---|--------|----------|----------|---------|
| 1 | English | en | en-US | Default source language |
| 2 | Portuguese | pt | pt-PT | |
| 3 | Indonesian | id | id | |
| 4 | Spanish | es | es | |
| 5 | French | fr | fr | |
| 6 | Japanese | ja | ja | |
| 7 | German | de | de | |
| 8 | Arabic | ar | ar | |
| 9 | Chinese | zh | zh | |
| 10 | **Tetum** | **tet** | **tet** | **Default target language** |

---

## 3. Fitur Lengkap

### 3.1 Splash Screen
- **Animasi**: Fade-in teks "TranslaTecho" + tagline "Translate Anything, Anywhere"
- **Durasi**: ~1 detik (300ms fade + 700ms tampil)
- **Teknologi**: `Animatable` + `LaunchedEffect`
- File: `SplashScreen.kt`

### 3.2 Translator Screen (Fitur Utama)

**Fungsi**: Menerjemahkan teks dari satu bahasa ke bahasa lain.

**Cara pakai:**
1. Ketik teks di kolom input (atau paste/voice/camera)
2. **Auto-detect** langsung mendeteksi bahasa sumber (ML Kit on-device)
3. Hasil terjemahan muncul otomatis (MyMemory API)
4. Tekan **bookmark** untuk simpan ke Favorites
5. Tekan **swap** untuk tukar source ↔ target

**Tombol aksi:**
| Ikon | Fungsi |
|------|--------|
| 🎤 | Voice input (speech-to-text) |
| 📷 | Camera/Gallery (OCR) |
| ↩️ / ↪️ | Undo/Redo |
| 🔊 | Baca teks (TTS) |
| 📋 | Copy teks |
| 📤 | Share teks |
| 🔖 | Bookmark/Favorite |

### 3.3 Write Screen (Grammar & AI Writing)

**Fungsi**: Memeriksa grammar dan menulis ulang teks.

**Mode:**
| Mode | Fungsi | API |
|------|--------|-----|
| **Check Grammar** | Deteksi error grammar **live** (setiap ngetik) | LanguageTool → Gemini fallback |
| **Formal** | Tulis ulang dengan gaya profesional | Gemini |
| **Casual** | Tulis ulang dengan gaya santai | Gemini |
| **Expand** | Kembangkan teks lebih detail | Gemini |

**Cara kerja Grammar Check:**
1. User pilih mode "Check Grammar"
2. Setiap user ngetik → **800ms debounce** → panggil LanguageTool
3. **Error** digarisbawahi **merah** di input teks
4. Di bawah tampil daftar error: kata salah → saran koreksi **hijau**
5. Tombol "Use correction" untuk terapkan koreksi

### 3.4 Dictionary Screen

**Fungsi**: Mencari arti kata.

**Cara kerja:**
1. Pilih bahasa dictionary (dropdown di kanan atas)
2. Ketik kata di search bar
3. **Kalau bahasa Inggris** → Free Dictionary API (phonetic, definitions, synonyms, antonyms)
4. **Kalau bukan Inggris** → Gemini AI (prompt dictionary → return JSON)

### 3.5 History Screen
- Semua terjemahan otomatis tersimpan
- Urutan: terbaru di atas
- Tombol "Clear All" untuk hapus semua
- Data dari **Room Database** (`history_table`)

### 3.6 Favorites Screen
- Filter dari History: hanya `isFavorite = true`
- Tekan bookmark untuk unfavorite
- Data tetap di database, hanya toggle flag

### 3.7 Camera OCR
- Ambil foto langsung atau dari gallery
- **ML Kit Text Recognition** baca teks dari gambar
- Hasil langsung masuk ke input Translator
- Fitur flash on/off

---

## 4. Arsitektur & Cara Kerja

### 4.1 MVVM Architecture

```
┌─────────────────────────────────────────────────┐
│                   UI LAYER                       │
│  (Jetpack Compose + Material 3)                  │
│  SplashScreen                                    │
│  TranslatorScreen                                │
│  WriteScreen                                     │
│  DictionaryScreen                                │
│  HistoryScreen                                   │
│  FavoritesScreen                                 │
│  CameraScreen                                    │
└───────────────────┬─────────────────────────────┘
                    │ collectAsStateWithLifecycle()
                    ▼
┌─────────────────────────────────────────────────┐
│               VIEW MODEL                         │
│  TranslatorViewModel                             │
│  - onInputChanged()                              │
│  - triggerTranslate()                            │
│  - performTranslation()                          │
│  - toggleFavorite()                              │
│  - onSourceLangChanged()                         │
│  - onSwapLanguages()                             │
│  State: _uiState: MutableStateFlow               │
└────────────┬────────────────────┬───────────────┘
             │                    │
             ▼                    ▼
┌────────────────────┐  ┌────────────────────┐
│   REPOSITORY       │  │   DATABASE         │
│  TranslateRepo     │  │  Room (SQLite)     │
│  DictionaryRepo    │  │  HistoryDao        │
└────────┬───────────┘  └────────────────────┘
         │
    ┌────┴─────────────────────┐
    │                          │
    ▼                          ▼
┌──────────┐           ┌──────────────┐
│MyMemory  │           │ Gemini AI    │
│Language- │           │ LanguageTool │
│Tool      │           │ FreeDict API │
│ML Kit    │           │              │
└──────────┘           └──────────────┘
```

### 4.2 StateFlow — Manajemen State

**Konsep kunci**: Semua state aplikasi ada di 1 tempat → `_uiState: MutableStateFlow<TranslatorUiState>`

```kotlin
data class TranslatorUiState(
    val sourceLang: Language,       // Bahasa sumber
    val targetLang: Language,       // Bahasa target (default: Tetum)
    val inputText: String,          // Teks input user
    val outputText: String,         // Hasil terjemahan
    val isLoading: Boolean,         // Loading state
    val isError: Boolean,           // Error state
    val detectedLanguage: Language?, // Bahasa terdeteksi
    val isFavorited: Boolean,       // Status bookmark
    val historyList: List<HistoryEntity>, // Riwayat
    val manualSource: Boolean       // Flag: user pilih manual?
)
```

**Alurnya:**
1. User action → ViewModel method
2. ViewModel update `_uiState` via `_uiState.update { ... }`
3. UI otomatis recompose karena `collectAsStateWithLifecycle()`

### 4.3 Alur Data — Translator

```
User ketik "Bon dia" di input
       │
       ▼
onInputChanged("Bon dia")
       │
       ├── _uiState.update { copy(inputText = "Bon dia", manualSource = false) }
       │
       └── triggerTranslate()
               │
               ├── translateJob?.cancel() ← cancel sebelumnya
               │
               └── viewModelScope.launch {
                       delay(600) ← debounce
                       │
                       ├── ML Kit: languageIdentifier.identifyLanguage("Bon dia")
                       │        │
                       │        ├── addOnSuccessListener { code = "pt" }
                       │        │    │
                       │        │    └── if (!manualSource && code != sourceLang.code)
                       │        │            _uiState.update { copy(sourceLang = Portuguese) }
                       │        │         else
                       │        │            _uiState.update { copy(detectedLanguage = Portuguese) }
                       │        │
                       │        └── addOnFailureListener → fallbackDetectAndTranslate()
                       │                │
                       │                └── MyMemory /langdetect API → parse → set source
                       │
                       └── performTranslation(state)
                               │
                               └── repository.translate("Bon dia", "pt", "tet")
                                       │
                                       └── HTTP GET ke MyMemory:
                                           api.mymemory.translated.net/get
                                           ?q=Bon+dia
                                           &langpair=pt-PT|tet
                                           &de=enzi23dev@gmail.com
                                               │
                                               ├── Success → parse response
                                               │   ├── outputText = "Bondia"
                                               │   └── addToHistory("Bon dia", "Bondia", ...)
                                               │
                                               └── Failure → outputText = "Translation failed."
                                                            isError = true
                   }
```

### 4.4 Alur Data — Grammar Check (Write)

```
User pilih mode "Check Grammar" + ketik "I goes to school"
       │
       ▼
LaunchedEffect(inputText, selectedMode)
       │
       ├── delay(800ms) ← debounce
       │
       └── checkGrammar("I goes to school", "en-US", "English")
               │
               ├── Cek: "en-US" in languageToolCodes? → YES
               │
               └── LanguageTool API:
                   POST api.languagetool.org/v2/check
                   body: text=I+goes+to+school&language=en-US
                       │
                       ├── Success → parse matches[]
                       │   │
                       │   └── grammarErrors = [
                       │       GrammarError(offset=2, length=4,
                       │         message="The verb 'goes' ...",
                       │         replacement="go")
                       │   ]
                       │   resultText = "I go to school" (corrected)
                       │
                       └── Error → grammarViaGemini("I goes to school", "English")
                               │
                               └── Gemini prompt: "Check the grammar..."
                                       │
                                       └── Response: {"corrected": "...", "errors": [...]}
```

**Error ditampilkan:**
- Di **input teks**: kata "goes" digaris bawah **merah**
- Di **daftar error**: "goes" → message → → "go" (hijau)
- **Corrected text**: "I go to school"

### 4.5 Alur Data — Dictionary

```
User pilih bahasa "Portuguese" + cari "casa"
       │
       ▼
Kalau bahasa.code != "en"
       │
       └── geminiLookup("casa", Portuguese)
               │
               └── Gemini prompt: "Act as a dictionary. Look up 'casa' in Portuguese..."
                       │
                       └── Response: JSON → parse → DictionaryResult
                           { word, phonetic, meanings[...] }

────────────────────────────────────────────────────

User pilih bahasa "English" + cari "house"
       │
       ▼
Kalau bahasa.code == "en"
       │
       └── Free Dictionary API:
           GET api.dictionaryapi.dev/api/v2/entries/en/house
               │
               └── Parse → DictionaryResult(word, phonetic, meanings[])
```

---

## 5. API & Service

### 5.1 MyMemory API (Translasi)

| Endpoint | Parameter | Contoh |
|----------|-----------|--------|
| `GET /get` | `q=text`, `langpair=source|target`, `de=email` | `/get?q=Hello&langpair=en-US|tet&de=...` |

- **Rate limit**: 50.000 karakter/hari (gratis)
- **Email**: `enzi23dev@gmail.com` (untuk kuota lebih besar)
- **Response**: JSON → `responseData.translatedText`

### 5.2 Gemini 2.5 Flash Lite (AI)

| Endpoint | Method |
|----------|--------|
| `POST /v1beta/models/gemini-2.5-flash-lite:generateContent?key=API_KEY` | POST |

**Digunakan di:**
- **Write**: Formal, Casual, Expand mode
- **Write**: Grammar fallback (saat LanguageTool gagal)
- **Dictionary**: Lookup untuk bahasa non-Inggris
- **Key**: Dari `BuildConfig.GEMINI_API_KEY` ← `local.properties`

### 5.3 LanguageTool (Grammar)

| Endpoint | Method | Body |
|----------|--------|------|
| `POST /v2/check` | POST | `text=...&language=en-US` |

- **Gratis**, tanpa API key
- **Bahasa didukung**: en-US, pt-PT, id, es, fr, de, ja, zh, ar, ru, it, nl
- **Response**: JSON → `matches[]` → tiap match punya `offset`, `length`, `message`, `replacements`

### 5.4 Free Dictionary API

| Endpoint | Method |
|----------|--------|
| `GET /api/v2/entries/en/{word}` | GET |

- **Gratis**, tanpa API key
- **Hanya untuk bahasa Inggris**
- **Response**: JSON array → `word`, `phonetic`, `meanings[].definitions[].definition`

### 5.5 ML Kit (On-Device)

| Library | Fungsi | Ukuran |
|---------|--------|--------|
| `language-id:17.0.6` | Deteksi bahasa dari teks | ~1MB |
| `text-recognition:16.0.0` | OCR dari gambar | ~5MB |

- **Offline**: Bekerja tanpa internet
- **Model**: Download sekali saat pertama kali digunakan

---

## 6. Database

### Room: `translate_db` → `history_table`

| Kolom | Tipe | Keterangan |
|-------|------|------------|
| `id` | INTEGER | Primary Key, auto-increment |
| `sourceText` | TEXT | Teks asli |
| `targetText` | TEXT | Hasil terjemahan |
| `sourceLang` | TEXT | Nama bahasa sumber |
| `targetLang` | TEXT | Nama bahasa target |
| `timestamp` | INTEGER | Waktu (milliseconds) |
| `isFavorite` | INTEGER | 0 = biasa, 1 = favorit |

**Query utama:**
| Fungsi | Query |
|--------|-------|
| getAllHistory | `SELECT * FROM history_table ORDER BY timestamp DESC` |
| findHistory | `SELECT * WHERE sourceText=? AND targetText=? AND sourceLang=? AND targetLang=?` |
| getFavorites | `SELECT * WHERE isFavorite=1 ORDER BY timestamp DESC` |
| setFavorite | `UPDATE history_table SET isFavorite=? WHERE id=?` |
| clearAll | `DELETE FROM history_table` |

### Migration Note
Database version = 2. Menggunakan `fallbackToDestructiveMigration()` — data akan hilang jika ada perubahan skema.

---

## 7. Teknologi & Library

| Komponen | Library | Versi |
|----------|---------|-------|
| Bahasa | Kotlin | 2.0.21 |
| UI Framework | Jetpack Compose + Material 3 | BOM 2024.11.00 |
| Architecture | MVVM + StateFlow | - |
| Navigation | Navigation Compose | 2.8.3 |
| Icon | Material Icons Extended | 1.7.5 |
| Database | Room | 2.6.1 |
| Camera | CameraX | 1.3.4 |
| ML Kit Language ID | `com.google.mlkit:language-id` | 17.0.6 |
| ML Kit Text Recognition | `com.google.mlkit:text-recognition` | 16.0.0 |
| Lifecycle | lifecycle-runtime-compose | 2.8.7 |
| Build | Gradle + AGP | 8.13 / 8.7.2 |
| Target SDK | Android 15 | 35 |
| Min SDK | Android 7.0 | 24 |

---

## 8. Struktur Project

```
App_Translate/
├── build.gradle.kts                 # Root build config
├── settings.gradle.kts              # Project settings
├── gradle.properties                # Gradle properties
├── local.properties                 # SDK path + API keys (gitignored)
├── gradle/
│   ├── libs.versions.toml           # Version catalog
│   └── wrapper/                     # Gradle wrapper
├── app/
│   ├── build.gradle.kts             # App build config
│   └── src/main/java/com/example/app_translate/
│       ├── MainActivity.kt          # Entry point + lifecycle
│       ├── viewmodel/
│       │   └── TranslatorViewModel.kt  # Semua logic & state
│       ├── data/
│       │   ├── model/
│       │   │   ├── Language.kt         # 10 bahasa + data class
│       │   │   └── DictionaryModel.kt  # DictionaryEntry, Meaning, Definition
│       │   ├── repository/
│       │   │   ├── TranslateRepository.kt    # MyMemory API call
│       │   │   └── DictionaryRepository.kt   # Free Dictionary API call
│       │   └── local/
│       │       ├── AppDatabase.kt     # Room database
│       │       ├── HistoryDao.kt      # Room DAO (query)
│       │       └── HistoryEntity.kt   # Room entity (table)
│       └── ui/
│           ├── screen/
│           │   ├── SplashScreen.kt        # Fade-in splash
│           │   ├── TranslatorScreen.kt    # Translator utama
│           │   ├── CameraScreen.kt        # Kamera + OCR
│           │   ├── HistoryScreen.kt       # Riwayat
│           │   ├── FavoritesScreen.kt     # Bookmark
│           │   ├── DictionaryScreen.kt    # Cari kata
│           │   ├── WriteScreen.kt         # Grammar + AI Write
│           │   └── DialogueScreen.kt      # AI Chat (outdated/dead)
│           ├── components/
│           │   ├── BottomNavigationBar.kt  # (dead code)
│           │   ├── InputSection.kt         # (dead code)
│           │   ├── OutputSection.kt        # (dead code)
│           │   └── LanguagePickerDialog.kt # Picker bahasa
│           └── theme/
│               ├── Color.kt
│               ├── Theme.kt
│               └── Type.kt
```

---

## 9. Cara Build & Install

### Di Android Studio
1. Clone: `git clone https://github.com/Acacio28/App_Translate-.git`
2. Buka di Android Studio
3. Tambah Gemini key di `local.properties`:
   ```properties
   GEMINI_API_KEY=your_key_here
   ```
   (Dapatkan key gratis di https://aistudio.google.com/apikey)
4. Klik **Run** atau `./gradlew installDebug`

### Via Command Line
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### APK Location
`app/build/outputs/apk/debug/app-debug.apk`

---

## 10. Catatan Penting

### Yang Perlu Diketahui

1. **State hilang saat rotate** — ViewModel dibuat manual di `MainActivity.kt`, bukan pakai `ViewModelProvider`/`hilt`. Screen rotation akan menghilangkan state translasi yang sedang berjalan.

2. **Tetum limited support** — MyMemory API mungkin tidak mendukung Tetum (`tet`), TTS menggunakan locale Portugal sebagai fallback.

3. **Gemini key wajib** — Kalau tidak diisi, Dictionary non-Inggris dan grammar fallback tidak akan jalan.

4. **Database migration destructive** — Menggunakan `fallbackToDestructiveMigration()` — update skema database akan menghapus semua data.

5. **Dead code masih ada**:
   - `DialogueScreen.kt` — screen AI chat tidak terpakai
   - `BottomNavigationBar.kt`, `InputSection.kt`, `OutputSection.kt` — components tidak dipakai
   - `DictionaryRepository.kt` — sudah tidak dipanggil (DictionaryScreen punya implementasi sendiri)

6. **MyMemory rate limit** — 50.000 karakter/hari dengan email `enzi23dev@gmail.com`. Untuk produksi, ganti email atau upgrade.

### Developer Notes

- **Package**: `com.example.app_translate`
- **App Name**: `TranslaTecho` (brand), `App Translate` (package name)
- **Version**: 1.0
- **Remote**: `https://github.com/Acacio28/App_Translate-.git`
- **Default source**: English
- **Default target**: Tetum

---

*Dokumentasi ini dibuat pada Juli 2026.*
*Untuk informasi lebih lanjut, hubungi developer.*

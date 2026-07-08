# Source Code TranslaTecho

```
com.example.app_translate/
├── MainActivity.kt              ← Entry point aplikasaun
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt       ← Database Room (SQLite)
│   │   ├── HistoryDao.kt        ← DAO ba riwayat & favorit
│   │   └── HistoryEntity.kt     ← Model tabel riwayat
│   ├── model/
│   │   ├── DictionaryModel.kt   ← Dados disionáriu (FreeDictionaryAPI)
│   │   └── Language.kt          ← 10 lian + kódigu (en, pt, id, tet...)
│   └── repository/
│       ├── DictionaryRepository.kt ← Repository disionáriu (FreeDictionary)
│       └── TranslateRepository.kt  ← MyMemory API (tradusaun)
├── ui/
│   ├── components/
│   │   ├── BottomNavigationBar.kt  ← Navigasaun 5 tab (Translate, Write, Camera, Dictionary, Chat)
│   │   ├── InputSection.kt         ← Input teks tradusaun
│   │   ├── LanguagePickerDialog.kt ← Dialog hili lian
│   │   └── OutputSection.kt        ← Output tradusaun + TTS
│   ├── screen/
│   │   ├── SplashScreen.kt         ← Splash ho imajen (lakar.jpg)
│   │   ├── TranslatorScreen.kt     ← Screen tradusaun prinsipál
│   │   ├── WriteScreen.kt          ← AI Writing + Grammar Check
│   │   ├── CameraScreen.kt         ← OCR husi kámara
│   │   ├── DictionaryScreen.kt     ← Disionáriu (FreeDictionary + Gemini)
│   │   ├── DialogueScreen.kt       ← Chat ho AI Gemini
│   │   ├── HistoryScreen.kt        ← Históriku tradusaun
│   │   └── FavoritesScreen.kt      ← Favoritu tradusaun
│   └── theme/
│       ├── Color.kt                ← Kór aplikasaun
│       ├── Theme.kt                ← Téma Material3
│       └── Type.kt                 ← Tipografia
└── viewmodel/
    └── TranslatorViewModel.kt  ← ViewModel sentrál (hotu screen uza ne'e)
```

## Pakote & Funsaun

### 1. `MainActivity.kt`
- Entry point. Buka app, initialize TTS (TextToSpeech), ViewModel. Kontrola splash → animasaun ba TranslatorScreen.

### 2. `data/local/` — Database
- **AppDatabase.kt**: Kria database Room ho tabela `history`.
- **HistoryDao.kt**: Operasaun CRUD ba riwayat no favorit. Metodu importante: `insertHistory()`, `getAllHistory()`, `getFavorite()`, `checkFavoriteStatus()`.
- **HistoryEntity.kt**: Entidade ba histtóriku. Fields: id, sourceText, translatedText, sourceLang, targetLang, isFavorite, timestamp.

### 3. `data/model/` — Modelos
- **Language.kt**: Lista 10 lian. Kada ida iha `name`, `code`, `apiCode`. Tetum uza kódigu `tet`.
- **DictionaryModel.kt**: Modelu ba resposta FreeDictionaryAPI: `DictionaryEntry`, `Meaning`, `Definition`.

### 4. `data/repository/` — API Calls
- **TranslateRepository.kt**: Husu tradusaun ba MyMemory API (`api.mymemory.translated.net/get`). Hanesan mós deteksaun lian. Uza `HttpURLConnection`.
- **DictionaryRepository.kt**: Husu disionáriu ba FreeDictionaryAPI (`api.dictionaryapi.dev`). **Nota: repository ida-ne'e la uza iha UI** — DictionaryScreen uza `HttpURLConnection` + Gemini direitamente.

### 5. `ui/components/` — Komponente Reutilizável
- **BottomNavigationBar.kt**: Navigasaun ho 5 tab: Tradús, Hakerek, Kámara, Disionáriu, Chat. Uza `AnimatedContent` ba animasaun.
- **InputSection.kt**: Kampu input teks + botão paste, undo, redo, auto detect.
- **LanguagePickerDialog.kt**: Dialog popup hili lian. Hetan parse `showAutoDetect` ba opsaun "Auto Detect".
- **OutputSection.kt**: Hatudu resultadu tradusaun + botão TTS, copy, share, favorite.

### 6. `ui/screen/` — Telas Prinsipál
- **SplashScreen.kt**: Hatudu imajen `lakar.jpg` durante 0.7s (200ms fade-in + 500ms delay).
- **TranslatorScreen.kt**: Tela tradusaun prinsipál. Iha Input (Text A) no Output (Text B). Botão swap lian, auto-detect, TTS, favorites, history. Uza `TranslatorViewModel`.
- **WriteScreen.kt**: AI Writing Assistant. 3 mode: Check Grammar (LanguageTool API), Formal (Gemini), Expand (Gemini). Grammar error highlight ho kór mean + sugestaun.
- **CameraScreen.kt**: Capture testu husi kámara uza CameraX + ML Kit OCR. Resultadu tradusaun automátiku.
- **DictionaryScreen.kt**: Disionáriu. Lian Inglés uza FreeDictionaryAPI. Lian seluk (inklui Tetum) uza Gemini 2.5 Flash. Tetum agora fó definisaun iha Tetum (la'ós Portugés).
- **DialogueScreen.kt**: Chat AI ho Gemini. Resposta tradús automátiku ba lian ne'ebé hili.
- **HistoryScreen.kt**: Lista riwayat tradusaun. Botão favoritu, delete.
- **FavoritesScreen.kt**: Lista riwayat ne'ebé marka favoritu.

### 7. `ui/theme/` — Téma
- **Color.kt**: Paleta kór aplikasaun.
- **Theme.kt**: Téma Material3 ho dark/light mode.
- **Type.kt**: Tipografia.

### 8. `viewmodel/` — ViewModel
- **TranslatorViewModel.kt**: ViewModel sentrál ne'ebé sira seluk uza. Iha state ba: sourceText, translatedText, sourceLang, targetLang, autoDetect, isFavorite, etc. Metodu: `translateText()`, `swapLanguages()`, `enableAutoDetect()`, `addToHistory()`, `toggleFavorite()`.

## Fluxu dadus

```
User hakerek testu
    ↓
InputSection → TranslatorViewModel.translateText()
    ↓
TranslateRepository → MyMemory API (POST)
    ↓
API fó tradusaun
    ↓
ViewModel atualiza state
    ↓
Compose render OutputSection
    ↓
User klik Favorite / Copy / Share / TTS
```

## API ne'ebé uza

| API | Funsaun | Free? | Limit |
|-----|---------|-------|-------|
| MyMemory API | Tradusaun | ✅ | 50K karakter/dia |
| LanguageTool API | Grammar Check | ✅ | Ilimitadu |
| Gemini 2.5 Flash | AI (Formal, Expand, Dictionary, Chat) | ✅ | 1.500 request/dia |
| ML Kit & CameraX | OCR on-device | ✅ | Ilimitadu |
| FreeDictionaryAPI | Disionáriu Inglés | ✅ | Ilimitadu |

## Informasaun Tékniku

- **Linguajen**: Kotlin + Jetpack Compose
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35
- **Architecture**: MVVM (Model-View-ViewModel)
- **Build**: Gradle + kotlin-dsl
- **API Key**: Rai iha `local.properties` → `BuildConfig`

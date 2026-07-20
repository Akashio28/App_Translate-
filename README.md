# TranslaTecho

**App Translate** — Aplikasaun Tradusaun Modernu / Modern Translation App

---

## English

TranslaTecho is a modern Android translation app built with **Kotlin** and **Jetpack Compose**. It provides text translation, camera OCR, dictionary lookup, grammar checking, AI writing assistance, and AI chat in a Material 3 interface.

### Features

#### Splash Screen
- Animated fade-in of app name and tagline "Translate Anything, Anywhere"
- Auto-dismiss after ~1 second

#### Translator Screen (Main)
- **Text translation** via MyMemory API (50K chars/day, free)
- **10 languages:** English, Portuguese, Indonesian, Spanish, French, Japanese, German, Arabic, Chinese, Tetum
- **Auto language detection** via ML Kit LanguageIdentification + MyMemory fallback
- **Swap languages** button
- **Voice input** (speech-to-text via RecognizerIntent)
- **Text-to-speech** for both input and output
- **Image OCR** via ML Kit Text Recognition (gallery or camera)
- **Undo/Redo** stack for input text
- **Copy, Share, Bookmark/Favorite** output actions
- **Alternatives panel** (Word/Sentence suggestions)

#### Camera Screen
- **Live CameraX preview** with viewfinder overlay
- **Flash toggle** (torch on/off)
- **Shutter button** captures photo → runs ML Kit Text Recognition
- **Gallery picker** fallback
- **Language indicator** bar

#### History Screen
- **Room database** — all translations stored locally
- Chronological list (newest first)
- **Clear all** button
- **Empty state** message

#### Favorites Screen
- Filtered view of bookmarked translations
- Un-bookmark toggle
- Empty state with instructions

#### Dictionary Screen
- **Free Dictionary API** for English words (phonetics, definitions, synonyms, antonyms)
- **Gemini 2.5 Flash Lite** for non-English lookups
- Search bar with clear button
- Part-of-speech chips, synonym/antonym tags

#### Write Screen (AI Writing Assistant)
- **Language selector** with auto-detect via ML Kit
- **Check Grammar** mode:
  - **LanguageTool API** (supports: en-US, pt-PT, id, es, fr, de, ja, zh, ar, ru, it, nl)
  - **Gemini fallback** for unsupported languages (e.g., Tetum)
  - **Red underline** on erroneous words
  - Error cards: wrong text, message, suggested replacement
  - Corrected text with "Use correction" button
  - Auto-check on typing (800ms debounce)
- **Formal / Casual / Expand** modes via **Gemini 2.5 Flash Lite**
- 1000-character limit with counter
- Clear button

#### Dialogue Screen (AI Chat)
- **Gemini 2.5 Flash Lite** backend
- Chat bubbles (user = purple right, AI = white left)
- Persistent history via SQLite
- Loading indicator with animated dots
- Auto-scroll to latest message
- Empty state with robot emoji

### Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material 3 | BOM 2024.11.00 |
| Architecture | MVVM with StateFlow | — |
| Navigation | Navigation Compose | 2.8.3 |
| Database (History) | Room | 2.6.1 |
| Database (Chat) | SQLite via SQLiteOpenHelper | — |
| Camera | CameraX | 1.3.4 |
| ML Kit (Language ID) | com.google.mlkit:language-id | 17.0.6 |
| ML Kit (Translate) | com.google.mlkit:translate | 17.0.1 |
| ML Kit (Text Recognition) | com.google.mlkit:text-recognition | 16.0.0 |
| Icons | Material Icons Extended | 1.7.5 |
| Lifecycle | lifecycle-runtime-compose, lifecycle-viewmodel-compose | 2.8.7 |
| Build System | Gradle | 8.13 |
| Android Gradle Plugin | com.android.application | 8.7.2 |

### APIs Used

| API | Endpoint | Purpose | Key? |
|---|---|---|---|
| MyMemory Translated | `api.mymemory.translated.net/get` | Text translation | No (free tier, 50K chars/day) |
| Google Gemini 2.5 Flash Lite | `generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent` | Write (Formal/Casual/Expand), Dictionary, Dialogue, Grammar fallback | Yes (`GEMINI_API_KEY`) |
| LanguageTool | `api.languagetool.org/v2/check` | Grammar checking | No |
| Free Dictionary API | `api.dictionaryapi.dev/api/v2/entries/en/` | English word definitions | No |

### Languages Supported

| Language | Code | API Code |
|---|---|---|
| English | en | en-US |
| Portuguese | pt | pt-PT |
| Indonesian | id | id |
| Spanish | es | es |
| French | fr | fr |
| Japanese | ja | ja |
| German | de | de |
| Arabic | ar | ar |
| Chinese | zh | zh |
| Tetum | tet | tet |

### Database Schema

#### Room: `history_table` (translate_db)

| Column | Type | Notes |
|---|---|---|
| id | INTEGER | PRIMARY KEY, AUTOINCREMENT |
| sourceText | TEXT | NOT NULL |
| targetText | TEXT | NOT NULL |
| sourceLang | TEXT | NOT NULL |
| targetLang | TEXT | NOT NULL |
| timestamp | INTEGER | NOT NULL |
| isFavorite | INTEGER | 0/1 |

#### SQLite: `chat_history` (translate_app.db)

| Column | Type | Notes |
|---|---|---|
| id | INTEGER | PRIMARY KEY, AUTOINCREMENT |
| original_text | TEXT | — |
| translated_text | TEXT | — |
| is_from_me | INTEGER | 0/1 |
| source_lang | TEXT | — |
| target_lang | TEXT | — |

### Project Structure

```
App_Translate/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties          # SDK path + API keys (gitignored)
├── gradle/
│   ├── libs.versions.toml    # Version catalog
│   └── wrapper/              # Gradle 8.13
└── app/
    ├── build.gradle.kts
    └── src/main/java/com/example/app_translate/
        ├── MainActivity.kt
        ├── viewmodel/TranslatorViewModel.kt
        ├── data/
        │   ├── model/          Language.kt, DictionaryModel.kt
        │   ├── repository/     TranslateRepository.kt, DictionaryRepository.kt
        │   ├── local/          AppDatabase.kt, HistoryDao.kt, HistoryEntity.kt, DatabaseHelper.kt
        │   └── helper/         DatabaseHelper.kt
        └── ui/
            ├── screen/         SplashScreen, TranslatorScreen, CameraScreen, HistoryScreen,
            │                   FavoritesScreen, DictionaryScreen, WriteScreen, DialogueScreen
            ├── components/     BottomNavigationBar, InputSection, OutputSection, LanguagePickerDialog
            └── theme/          Color.kt, Theme.kt, Type.kt
```

### Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | API calls (normal, auto-granted) |
| `CAMERA` | CameraX preview + OCR |
| `RECORD_AUDIO` | Voice input / speech-to-text |
| `camera` feature | `required="false"` — app works without camera |

### Build & Setup

1. **Clone:**
   ```bash
   git clone https://github.com/Acacio28/App_Translate-.git
   ```

2. **Open** in Android Studio, let Gradle sync.

3. **Add API key** to `local.properties`:
   ```properties
   GEMINI_API_KEY=your_gemini_key_here
   ```
   Get a free key at https://aistudio.google.com/apikey

4. **Build:**
   ```bash
   ./gradlew assembleDebug
   ```

5. **APK:** `app/build/outputs/apk/debug/app-debug.apk`

6. **Install on device:**
   ```bash
   ./gradlew installDebug
   ```

7. **Run tests:**
   ```bash
   ./gradlew test                 # Unit tests
   ./gradlew connectedAndroidTest # Instrumented tests
   ```

### Requirements

- **Minimum SDK:** 24 (Android 7.0 Nougat)
- **Target SDK:** 35 (Android 15)
- **Compile SDK:** 35
- **JDK:** 17
- **Android Studio** (latest stable)

### Package Info

- **Package:** `com.example.app_translate`
- **App name:** `App Translate`
- **Brand:** `TranslaTecho`
- **Version:** 1.0 (versionCode 1)

---

## Tetum

Lalakar mak aplikasaun tradusaun Android modernu ida ne'ebé dezenvolve ho **Kotlin** no **Jetpack Compose**. Aplikasaun ne'e fornese tradusaun testu, OCR ho kamera, buka liafuan iha diksionáriu, verifika gramática, asisténsia hakerek ho AI, no xat ho AI — hotu ho interface Material 3 ne'ebé moos no fàsil uza.

### Funsaun Sira

#### Splash Screen
- Animasaun fade-in ba naran aplikasaun no lema "Translate Anything, Anywhere"
- Taka automátiku depois ~1 segundu

#### Tradutor Screen (Principal)
- **Tradusaun testu** liu husi MyMemory API (50K karakter loron-loron, gratuitu)
- **Lian 10:** Inglés, Portugés, Indonéziu, Espanhól, Françés, Japonez, Alemaun, Árabe, Xines, Tetum
- **Deteksaun lian automátiku** liu husi ML Kit LanguageIdentification + MyMemory
- **Botão troka lian**
- **Voz ba testu** (speech-to-text liu husi RecognizerIntent)
- **Testu ba lian** (text-to-speech) ba input no output
- **OCR imajen** liu husi ML Kit Text Recognition (galeria ka kamera)
- **Undo/Redo** ba testu input
- **Kopia, Fahe, Bookmark/Favorite** ba output
- **Painél alternativu** (sujestaun liafuan / fraze)

#### Camera Screen
- **Previsualizasaun CameraX** ho moldura viewfinder
- **Kontrolu flash** (toron on/off)
- **Botão shutter** fotos → ML Kit Text Recognition
- **Seleksaun galeria** alternativa
- **Barra indikador lian**

#### Istória Screen
- **Database Room** — rai hotu tradusaun lokal
- Lista tuir ordem (foun ba tuan)
- **Botão hamoos hotu**
- **Mensajen vaziu**

#### Favorites Screen
- Lista tradusaun ne'ebé bookmark ona
- Botão atu bookmarks
- Mensajen vaziu ho instrusaun

#### Diksionáriu Screen
- **Free Dictionary API** ba liafuan Inglés (fonétika, definisaun, sinónimu, antónimu)
- **Gemini 2.5 Flash Lite** ba lian seluk
- Barra buka ho botão limpa
- Chips parte-lingua, etiketa sinónimu/antónimu

#### Write Screen (Asisténsia Hakerek AI)
- **Selektor lian** ho deteksaun automátiku liu husi ML Kit
- **Check Grammar** (Verifika Gramática):
  - **LanguageTool API** (suporta: en-US, pt-PT, id, es, fr, de, ja, zh, ar, ru, it, nl)
  - **Gemini fallback** ba lian ne'ebé la suporta (hanesan Tetum)
  - **Liaña mean** iha liafuan sala
  - Kartu erro: testu sala, mensajen, sugestaun korreksaun
  - Testu korridu ho botão "Use correction"
  - Verifikasaun automátiku wainhira hakerek (800ms debounce)
- **Formal / Kazuál / Expande** via **Gemini 2.5 Flash Lite**
- Limite 1000 karakter ho kontador
- Botão clear

#### Diólogu Screen (Xat AI)
- **Gemini 2.5 Flash Lite** hanesan backend
- Bolha xat (uza-na'in = roxo liman los, AI = mutin liman karuk)
- Istória persistente liu husi SQLite
- Indikador karega ho animasaun dot
- Auto-scroll ba mensajen foun
- Estadu vaziu ho emoji robot

### Teknolojia ne'ebé Uza

| Kamada | Teknolojia | Versaun |
|---|---|---|
| Lian | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material 3 | BOM 2024.11.00 |
| Arkitetura | MVVM ho StateFlow | — |
| Navegasaun | Navigation Compose | 2.8.3 |
| Database (Istória) | Room | 2.6.1 |
| Database (Xat) | SQLite liu husi SQLiteOpenHelper | — |
| Kamera | CameraX | 1.3.4 |
| ML Kit (Identifika Lian) | com.google.mlkit:language-id | 17.0.6 |
| ML Kit (Tradusaun) | com.google.mlkit:translate | 17.0.1 |
| ML Kit (Reconhese Testu) | com.google.mlkit:text-recognition | 16.0.0 |
| Íkone | Material Icons Extended | 1.7.5 |
| Lifecycle | lifecycle-runtime-compose, lifecycle-viewmodel-compose | 2.8.7 |
| Build System | Gradle | 8.13 |
| Android Gradle Plugin | com.android.application | 8.7.2 |

### API ne'ebé Uza

| API | Endpoint | Objetivu | Xave? |
|---|---|---|---|
| MyMemory Translated | `api.mymemory.translated.net/get` | Tradusaun testu | La (gratuitu, 50K karakter/dia) |
| Google Gemini 2.5 Flash Lite | `generativelanguage.googleapis.com/.../gemini-2.5-flash-lite:generateContent` | Write (Formal/Kazuál/Expande), Diksionáriu, Diólogu, Gramátika fallback | Sim (`GEMINI_API_KEY`) |
| LanguageTool | `api.languagetool.org/v2/check` | Verifika gramátika | La |
| Free Dictionary API | `api.dictionaryapi.dev/api/v2/entries/en/` | Definisaun liafuan Inglés | La |

### Lian ne'ebé Suporta

| Lian | Kódigu | Kódigu API |
|---|---|---|
| Inglés | en | en-US |
| Portugés | pt | pt-PT |
| Indonéziu | id | id |
| Espanhól | es | es |
| Françés | fr | fr |
| Japonez | ja | ja |
| Alemaun | de | de |
| Árabe | ar | ar |
| Xines | zh | zh |
| Tetum | tet | tet |

### Skema Database

#### Room: `history_table` (translate_db)

| Koluna | Tipu | Nota |
|---|---|---|
| id | INTEGER | PRIMARY KEY, AUTOINCREMENT |
| sourceText | TEXT | NOT NULL |
| targetText | TEXT | NOT NULL |
| sourceLang | TEXT | NOT NULL |
| targetLang | TEXT | NOT NULL |
| timestamp | INTEGER | NOT NULL |
| isFavorite | INTEGER | 0/1 |

#### SQLite: `chat_history` (translate_app.db)

| Koluna | Tipu | Nota |
|---|---|---|
| id | INTEGER | PRIMARY KEY, AUTOINCREMENT |
| original_text | TEXT | — |
| translated_text | TEXT | — |
| is_from_me | INTEGER | 0/1 |
| source_lang | TEXT | — |
| target_lang | TEXT | — |

### Estrutura Projetu

```
App_Translate/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties          # Dalán SDK + xave API (gitignored)
├── gradle/
│   ├── libs.versions.toml    # Katalogu versaun
│   └── wrapper/              # Gradle 8.13
└── app/
    ├── build.gradle.kts
    └── src/main/java/com/example/app_translate/
        ├── MainActivity.kt
        ├── viewmodel/TranslatorViewModel.kt
        ├── data/
        │   ├── model/          Language.kt, DictionaryModel.kt
        │   ├── repository/     TranslateRepository.kt, DictionaryRepository.kt
        │   ├── local/          AppDatabase.kt, HistoryDao.kt, HistoryEntity.kt, DatabaseHelper.kt
        │   └── helper/         DatabaseHelper.kt
        └── ui/
            ├── screen/         SplashScreen, TranslatorScreen, CameraScreen, HistoryScreen,
            │                   FavoritesScreen, DictionaryScreen, WriteScreen, DialogueScreen
            ├── components/     BottomNavigationBar, InputSection, OutputSection, LanguagePickerDialog
            └── theme/          Color.kt, Theme.kt, Type.kt
```

### Permisaun

| Permisaun | Objetivu |
|---|---|
| `INTERNET` | Bolu API (normal, auto-granted) |
| `CAMERA` | Previsualizasaun CameraX + OCR |
| `RECORD_AUDIO` | Input voz / speech-to-text |
| `camera` feature | `required="false"` — aplikasaun serbisu la ho kamera |

### Oinsá atu Hahú

1. **Klona:**
   ```bash
   git clone https://github.com/Acacio28/App_Translate-.git
   ```

2. **Loke** iha Android Studio, husik Gradle sincroniza.

3. **Tau xave API** iha `local.properties`:
   ```properties
   GEMINI_API_KEY=your_gemini_key_here
   ```
   Hetan xave gratuitu iha https://aistudio.google.com/apikey

4. **Build:**
   ```bash
   ./gradlew assembleDebug
   ```

5. **APK:** `app/build/outputs/apk/debug/app-debug.apk`

6. **Instala iha dispositivu:**
   ```bash
   ./gradlew installDebug
   ```

7. **Hala'o teste:**
   ```bash
   ./gradlew test                 # Teste unitáriu
   ./gradlew connectedAndroidTest # Teste instrumentadu
   ```

### Rekizitu

- **Minimum SDK:** 24 (Android 7.0 Nougat)
- **Target SDK:** 35 (Android 15)
- **Compile SDK:** 35
- **JDK:** 17
- **Android Studio** (estável foun)

### Informasaun Pakote

- **Pakote:** `com.example.app_translate`
- **Naran aplikasaun:** `App Translate`
- **Marka:** `TranslaTecho`
- **Versaun:** 1.0 (versionCode 1)

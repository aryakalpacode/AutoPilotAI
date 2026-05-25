# AutoPilot AI 🤖

**Your Free Autonomous AI Agent for Android**

AutoPilot AI is a production-ready Android application that functions as an autonomous AI agent (similar to Manus AI). It connects to OpenRouter's API to leverage free open-source AI models like DeepSeek V3, Llama 3, Gemma 2, Mistral, and Qwen.

## ✨ Features

### 🧠 Autonomous Agent Loop (ReAct Pattern)
- Perceive → Think → Act → Observe → Loop
- Automatic task decomposition
- Human-in-the-loop confirmation for sensitive actions
- Dynamic re-planning on failures

### 🔧 10 Built-in Tools
1. **WEB_SEARCH** - Search the internet via DuckDuckGo (no API key needed)
2. **WEB_SCRAPE** - Read and extract text from web pages
3. **FILE_MANAGER** - Create, read, update, delete files in workspace
4. **CODE_EXECUTOR** - Run JavaScript in a sandboxed WebView
5. **CALCULATOR** - Safe mathematical expression evaluation
6. **CLIPBOARD** - Read/write system clipboard
7. **DEVICE_INFO** - Battery, network, storage, date/time info
8. **NOTES_DATABASE** - CRUD operations on local notes database
9. **REMINDER** - Schedule local notification reminders
10. **TEXT_PROCESSOR** - Summarize, translate, extract keys, format text

### 🤖 Multi-Model Support
- Connects to OpenRouter's API (free tier)
- Auto-discovers available free models
- Supported models include:
  - DeepSeek V3 (`deepseek/deepseek-chat`)
  - Llama 3.1 8B (`meta-llama/llama-3.1-8b-instruct:free`)
  - Gemma 2 9B (`google/gemma-2-9b-it:free`)
  - Mistral 7B (`mistralai/mistral-7b-instruct:free`)
  - Qwen 2 7B (`qwen/qwen-2-7b-instruct:free`)

### 📋 Task Templates
- Research a Topic
- Daily News Briefing
- Code Generator
- Content Writer
- Data Analyzer
- Study Assistant
- Travel Planner
- Custom Workflow

### 🎨 Modern UI
- Jetpack Compose + Material 3
- Light / Dark / System themes
- Real-time agent status indicators
- Task progress visualization
- Debug view for raw JSON exchanges

### 🔒 Security
- API keys encrypted with AES-256-GCM via Android KeyStore
- Sandboxed JavaScript execution
- Human-in-the-loop confirmation for sensitive actions

## 📋 Requirements

- Android 8.0+ (API 26+)
- OpenRouter API key (free at [openrouter.ai](https://openrouter.ai))
- Internet connection (for AI models and web tools)

## 🚀 Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK 34

### Build Steps

1. **Clone the repository:**
   ```bash
   git clone <repo-url>
   cd AutoPilotAI
   ```

2. **Open in Android Studio:**
   - File → Open → Select the `AutoPilotAI` directory
   - Wait for Gradle sync to complete

3. **Build and Run:**
   - Connect an Android device or start an emulator
   - Click Run (▶) or press `Shift+F10`

4. **First Launch Setup:**
   - Get a free API key at [openrouter.ai](https://openrouter.ai)
   - Enter your API key in the setup screen
   - Test the connection
   - Select a default model
   - Start using AutoPilot AI!

## 🏗️ Architecture

```
MVVM + Clean Architecture
├── data/           # Data layer (Room DB, Retrofit API, Repositories)
├── domain/         # Domain layer (Models, Use Cases)
├── agent/          # Agent core (Orchestrator, Tools, Prompt Builder)
├── ui/             # Presentation layer (Compose Screens, ViewModels)
├── di/             # Dependency Injection (Hilt modules)
├── service/        # Background services
├── security/       # Encryption (KeyStore manager)
└── util/           # Utilities and extensions
```

### Key Components:
- **AgentOrchestrator** - Implements the autonomous ReAct loop
- **ResponseParser** - Parses LLM JSON responses with multiple fallback strategies
- **ContextManager** - Manages token limits and conversation summarization
- **PromptBuilder** - Constructs system prompts and message formatting

## 📱 Screens

1. **Splash** - Animated logo, routes to Setup or Home
2. **Setup** - API key configuration and model selection
3. **Home** - Quick action templates and recent conversations
4. **Chat** - Main interaction with the AI agent
5. **Tasks** - View all task executions with status filtering
6. **Notes** - Browse, search, and manage saved notes
7. **Settings** - API key, model, agent, appearance configuration

## 🛠️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt (Dagger) |
| Database | Room |
| Networking | Retrofit + OkHttp |
| HTML Parsing | Jsoup |
| Background | WorkManager + Foreground Service |
| Preferences | DataStore |
| Security | Android KeyStore + AES-256-GCM |

## ⚠️ Error Handling

- **Network errors**: Exponential backoff retry (1s → 60s, 5 retries)
- **API errors**: Specific handling for 401, 429, 500
- **Model unavailable**: Automatic fallback to next free model
- **Malformed LLM response**: Multiple parsing strategies with fallback
- **App crash recovery**: State saved to Room DB every step
- **3 consecutive errors**: Stops and asks user for guidance

## 📄 License

This project is open-source. See LICENSE file for details.

## 🤝 Contributing

Contributions are welcome! Please open an issue first to discuss proposed changes.

---

Built with ❤️ using Kotlin, Jetpack Compose, and free open-source AI models.

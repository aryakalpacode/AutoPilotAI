package com.autopilot.agent.util

/**
 * Application-wide constants used across different modules.
 */
object Constants {
    const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/"
    const val DUCKDUCKGO_BASE_URL = "https://html.duckduckgo.com/"
    const val OPENROUTER_REFERER = "https://autopilot-ai.app"
    const val OPENROUTER_TITLE = "AutoPilot AI"

    const val DATABASE_NAME = "autopilot_db"
    const val DATABASE_VERSION = 1

    const val KEYSTORE_ALIAS = "autopilot_api_key_alias"
    const val KEYSTORE_PROVIDER = "AndroidKeyStore"

    const val DEFAULT_MAX_ITERATIONS = 20
    const val MIN_ITERATIONS = 5
    const val MAX_ITERATIONS = 50

    const val DEFAULT_TEMPERATURE = 0.7
    const val DEFAULT_MAX_TOKENS = 4096

    const val TOKEN_CHAR_RATIO = 4 // ~1 token per 4 chars
    const val CONTEXT_WINDOW_THRESHOLD = 0.75 // Summarize at 75%
    const val PRESERVE_LAST_MESSAGES = 5

    const val MAX_SCRAPE_CHARS = 4000
    const val WEB_SCRAPE_TIMEOUT_SECONDS = 15L
    const val CODE_EXECUTION_TIMEOUT_MS = 10_000L

    const val INITIAL_BACKOFF_MS = 1000L
    const val MAX_BACKOFF_MS = 60_000L
    const val MAX_RETRIES = 5

    const val MAX_CONSECUTIVE_ERRORS = 3
    const val CONFIDENCE_THRESHOLD = 0.5

    const val NOTIFICATION_CHANNEL_ID = "agent_execution"
    const val FOREGROUND_SERVICE_ID = 1001

    const val WORKSPACE_DIR = "workspace"

    const val PREFS_DATASTORE = "autopilot_preferences"
    const val PREF_THEME = "theme"
    const val PREF_FONT_SIZE = "font_size"
    const val PREF_DEFAULT_MODEL = "default_model"
    const val PREF_MAX_ITERATIONS = "max_iterations"
    const val PREF_TEMPERATURE = "temperature"
    const val PREF_PERSONALITY = "personality"
    const val PREF_CUSTOM_PROMPT = "custom_system_prompt"
    const val PREF_AUTO_CONFIRM = "auto_confirm"
    const val PREF_SETUP_COMPLETE = "setup_complete"
}

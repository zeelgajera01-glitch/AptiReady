package com.example.aptiready.data.local

import android.content.Context
import android.content.SharedPreferences

interface AdPreferencesRepository {
    fun readState(): String? = null
    fun writeState(state: String) {}

    fun isFirstSession(): Boolean
    fun setFirstSession(isFirst: Boolean)

    fun getSessionCount(): Int
    fun setSessionCount(count: Int)

    fun getDailyCount(): Int
    fun setDailyCount(count: Int)

    fun getDailyDateKey(): String?
    fun setDailyDateKey(dateKey: String)

    fun getLastBackgroundWallTimeMs(): Long
    fun setLastBackgroundWallTimeMs(timeMs: Long)

    fun getLastAdShowWallTimeMs(): Long
    fun setLastAdShowWallTimeMs(timeMs: Long)

    fun getConsumedAttempts(): Set<String>
    fun isAttemptConsumed(qualifiedAttemptId: String): Boolean
    fun markAttemptConsumed(qualifiedAttemptId: String)
}

class AdPreferencesRepositoryImpl(context: Context) : AdPreferencesRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("ad_preferences", Context.MODE_PRIVATE)

    override fun readState(): String? = prefs.getString("session_state_v2", null)
    override fun writeState(state: String) {
        check(prefs.edit().putString("session_state_v2", state).commit()) { "Could not persist ad policy" }
    }

    override fun isFirstSession(): Boolean {
        return prefs.getBoolean(KEY_FIRST_SESSION, true)
    }

    override fun setFirstSession(isFirst: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_SESSION, isFirst).apply()
    }

    override fun getSessionCount(): Int {
        return prefs.getInt(KEY_SESSION_COUNT, 0)
    }

    override fun setSessionCount(count: Int) {
        prefs.edit().putInt(KEY_SESSION_COUNT, count).apply()
    }

    override fun getDailyCount(): Int {
        return prefs.getInt(KEY_DAILY_COUNT, 0)
    }

    override fun setDailyCount(count: Int) {
        prefs.edit().putInt(KEY_DAILY_COUNT, count).apply()
    }

    override fun getDailyDateKey(): String? {
        return prefs.getString(KEY_DAILY_DATE_KEY, null)
    }

    override fun setDailyDateKey(dateKey: String) {
        prefs.edit().putString(KEY_DAILY_DATE_KEY, dateKey).apply()
    }

    override fun getLastBackgroundWallTimeMs(): Long {
        return prefs.getLong(KEY_LAST_BG_WALL_TIME, 0L)
    }

    override fun setLastBackgroundWallTimeMs(timeMs: Long) {
        prefs.edit().putLong(KEY_LAST_BG_WALL_TIME, timeMs).apply()
    }

    override fun getLastAdShowWallTimeMs(): Long {
        return prefs.getLong(KEY_LAST_AD_SHOW_WALL_TIME, 0L)
    }

    override fun setLastAdShowWallTimeMs(timeMs: Long) {
        prefs.edit().putLong(KEY_LAST_AD_SHOW_WALL_TIME, timeMs).apply()
    }

    override fun getConsumedAttempts(): Set<String> {
        return prefs.getStringSet(KEY_CONSUMED_ATTEMPTS, emptySet()) ?: emptySet()
    }

    override fun isAttemptConsumed(qualifiedAttemptId: String): Boolean {
        if (qualifiedAttemptId.isBlank()) return false
        return getConsumedAttempts().contains(qualifiedAttemptId)
    }

    override fun markAttemptConsumed(qualifiedAttemptId: String) {
        if (qualifiedAttemptId.isBlank()) return
        val current = getConsumedAttempts().toMutableSet()
        current.add(qualifiedAttemptId)
        // Never evict consumed attempts: forgetting one would make old results eligible again.
        check(prefs.edit().putStringSet(KEY_CONSUMED_ATTEMPTS, current).commit())
    }

    companion object {
        private const val KEY_FIRST_SESSION = "is_first_session"
        private const val KEY_SESSION_COUNT = "session_count"
        private const val KEY_DAILY_COUNT = "daily_count"
        private const val KEY_DAILY_DATE_KEY = "daily_date_key"
        private const val KEY_LAST_BG_WALL_TIME = "last_bg_wall_time"
        private const val KEY_LAST_AD_SHOW_WALL_TIME = "last_ad_show_wall_time"
        private const val KEY_CONSUMED_ATTEMPTS = "consumed_attempts"
    }
}

class InMemoryAdPreferencesRepository : AdPreferencesRepository {
    private var serializedState: String? = null
    override fun readState() = serializedState
    override fun writeState(state: String) { serializedState = state }

    private var firstSession = true
    private var sessionCount = 0
    private var dailyCount = 0
    private var dailyDateKey: String? = null
    private var lastBackgroundWallTimeMs = 0L
    private var lastAdShowWallTimeMs = 0L
    private val consumedAttempts = mutableSetOf<String>()

    override fun isFirstSession(): Boolean = firstSession
    override fun setFirstSession(isFirst: Boolean) { firstSession = isFirst }

    override fun getSessionCount(): Int = sessionCount
    override fun setSessionCount(count: Int) { sessionCount = count }

    override fun getDailyCount(): Int = dailyCount
    override fun setDailyCount(count: Int) { dailyCount = count }

    override fun getDailyDateKey(): String? = dailyDateKey
    override fun setDailyDateKey(dateKey: String) { dailyDateKey = dateKey }

    override fun getLastBackgroundWallTimeMs(): Long = lastBackgroundWallTimeMs
    override fun setLastBackgroundWallTimeMs(timeMs: Long) { lastBackgroundWallTimeMs = timeMs }

    override fun getLastAdShowWallTimeMs(): Long = lastAdShowWallTimeMs
    override fun setLastAdShowWallTimeMs(timeMs: Long) { lastAdShowWallTimeMs = timeMs }

    override fun getConsumedAttempts(): Set<String> = consumedAttempts.toSet()
    override fun isAttemptConsumed(qualifiedAttemptId: String): Boolean = consumedAttempts.contains(qualifiedAttemptId)
    override fun markAttemptConsumed(qualifiedAttemptId: String) {
        if (qualifiedAttemptId.isNotBlank()) {
            consumedAttempts.add(qualifiedAttemptId)
        }
    }
}

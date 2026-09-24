package com.example.aptiready.ui.ads

import android.content.Context
import com.example.aptiready.data.repository.PracticeRepository
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/** The queue stores earned callbacks only, never an unearned promise. Local persistence, not server verification. */
class RewardDelivery(context: Context, private val repository: PracticeRepository) {
    private val prefs = context.applicationContext.getSharedPreferences("earned_hint_queue", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = ConcurrentHashMap.newKeySet<String>()
    private val inMemory = ConcurrentHashMap<String, RewardContext>()
    private fun encode(c: RewardContext) = JSONObject().put("owner", c.ownerId)
        .put("session", c.sessionId).put("question", c.questionId).put("position", c.position)
        .put("operation", c.operationId).toString()
    private fun decode(s: String): RewardContext {
        val j = JSONObject(s)
        return RewardContext(j.getString("owner"), j.getString("session"), j.getString("question"),
            j.getInt("position"), j.getString("operation"))
    }
    fun resumePending() {
        prefs.all.values.filterIsInstance<String>().forEach { raw ->
            runCatching { decode(raw) }.getOrNull()?.let { deliver(it) }
        }
        inMemory.values.toList().forEach { deliver(it) }
    }
    fun deliver(c: RewardContext) {
        inMemory[c.operationId] = c
        if (!running.add(c.operationId)) return
        scope.launch {
            try {
                repeat(5) { attempt ->
                    try {
                        check(prefs.edit().putString(c.operationId, encode(c)).commit())
                        repository.unlockExtraHint(c.sessionId, c.position, c.questionId, c.ownerId)
                        check(prefs.edit().remove(c.operationId).commit())
                        inMemory.remove(c.operationId)
                        return@launch
                    } catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) { delay(1_000L shl attempt) }
                }
                AdLogger.w("RewardDelivery", "Hint delivery pending; will retry on app resume")
            } finally { running.remove(c.operationId) }
        }
    }
}

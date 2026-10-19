package com.aptirise.app

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.aptirise.app.data.local.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Uses Room's generated DAOs against SQLite, including its actual schema and SQL. */
class SessionWriteRegressionTest {
    private lateinit var database: AppDatabase

    @Before fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()
    }

    @After fun closeDatabase() = database.close()

    private fun insertDefaultRow(table: String, overrides: Map<String, Any>) {
        val db = database.openHelper.writableDatabase
        val fields = mutableListOf<String>()
        val values = mutableListOf<Any?>()
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1)
                val type = cursor.getString(2)
                fields.add("`$name`")
                values.add(overrides[name] ?: if (type == "TEXT") "" else 0)
            }
        }
        db.execSQL("INSERT INTO `$table` (${fields.joinToString()}) VALUES (${fields.joinToString { "?" }})", values.toTypedArray())
    }

    @Test fun answerSubmitAndBookmarkPreserveEarnedHint() = runBlocking {
        insertDefaultRow("practice_sessions", mapOf("id" to "s", "status" to "IN_PROGRESS"))
        insertDefaultRow("session_question_snapshots", mapOf("id" to "s_0", "sessionId" to "s", "isExtraHintUnlocked" to 1))
        val dao = database.practiceSessionDao()
        dao.saveSelection("s", 0, "a")
        dao.saveBookmarkState("s", 0, true)
        dao.saveSelection("s", 0, "c")
        assertEquals(1, dao.submitAnswer("s", 0))
        assertEquals(0, dao.submitAnswer("s", 0))
        assertEquals(0, dao.saveSelection("s", 0, "b"))
        val row = dao.getSnapshotsForSession("s").single()
        assertEquals("c", row.selectedOptionId)
        assertTrue(row.isSubmitted && row.isBookmarked && row.isExtraHintUnlocked)
        assertEquals(0, dao.savePosition("s", 999))
    }

    @Test fun timerCannotOverwriteCompletedAttemptOrItsAnswer() = runBlocking {
        insertDefaultRow("mock_test_attempts", mapOf("id" to "m", "status" to "IN_PROGRESS", "remainingSeconds" to 900))
        insertDefaultRow("mock_question_snapshots", mapOf("id" to "m_0", "attemptId" to "m"))
        val dao = database.mockTestDao()
        dao.saveSelection("m", 0, "d")
        dao.toggleReview("m", 0)
        dao.saveTimer("m", 899, 1, 100, 100)
        database.openHelper.writableDatabase.execSQL("UPDATE mock_test_attempts SET status = 'COMPLETED', remainingSeconds = 0 WHERE id = 'm'")
        assertEquals(0, dao.saveTimer("m", 898, 2, 200, 200))
        assertEquals(0, dao.saveSelection("m", 0, "a"))
        assertEquals(0, dao.toggleReview("m", 0))
        val row = dao.getSnapshotsForAttempt("m").single()
        assertEquals("d", row.selectedOptionId)
        assertTrue(row.isMarkedForReview)
        assertEquals("COMPLETED", dao.getAttemptById("m")?.status)
    }
}

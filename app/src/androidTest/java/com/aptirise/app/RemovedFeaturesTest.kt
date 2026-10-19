package com.aptirise.app

import android.content.pm.PackageManager
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.aptirise.app.data.local.db.AppDatabase
import com.aptirise.app.ui.navigation.NamespaceFragmentFactory
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RemovedFeaturesTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun upgradingVersionSevenPreservesLearningDataAndDropsOnlyRetiredTables() {
        val name = "upgrade-${UUID.randomUUID()}.db"
        val schema = JSONArray(instrumentation.context.assets.open("schema-v7.json").bufferedReader().use { it.readText() })
        val old = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        for (i in 0 until schema.length()) db.execSQL(schema.getString(i))
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build())
        var upgraded: AppDatabase? = null
        try {
            val db = old.writableDatabase
            insertRow(db, "practice_sessions", mapOf("id" to "practice", "ownerId" to "owner", "status" to "COMPLETED", "score" to 1))
            insertRow(db, "session_question_snapshots", mapOf("id" to "snapshot", "sessionId" to "practice", "selectedOptionId" to "b", "isExtraHintUnlocked" to 1))
            insertRow(db, "bookmarks", mapOf("id" to "saved", "ownerId" to "owner", "questionText" to "Keep this bookmark"))
            insertRow(db, "mock_test_attempts", mapOf("id" to "mock", "ownerId" to "owner", "status" to "COMPLETED", "earnedMarks" to 4))
            insertRow(db, "sync_outbox", mapOf("id" to "retired"))
            insertRow(db, "synced_attempts", mapOf("attemptId" to "retired"))
            insertRow(db, "bookmarks", mapOf("id" to "saved-newer", "ownerId" to "another-owner", "questionId" to "", "questionText" to "Keep this bookmark", "createdAt" to 100))
            old.close()

            upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9).build()
            val current = upgraded.openHelper.writableDatabase // Room validates the complete migrated schema.
            assertEquals(9, current.version)
            assertEquals("guest", value(current, "SELECT ownerId FROM practice_sessions WHERE id='practice'"))
            assertEquals("guest", value(current, "SELECT ownerId FROM mock_test_attempts WHERE id='mock'"))
            assertEquals("1", value(current, "SELECT COUNT(*) FROM bookmarks"))
            assertEquals("COMPLETED", value(current, "SELECT status FROM practice_sessions WHERE id='practice'"))
            assertEquals("b", value(current, "SELECT selectedOptionId FROM session_question_snapshots WHERE id='snapshot'"))
            assertEquals("1", value(current, "SELECT isExtraHintUnlocked FROM session_question_snapshots WHERE id='snapshot'"))
            assertEquals("Keep this bookmark", value(current, "SELECT questionText FROM bookmarks WHERE ownerId='guest'"))
            assertEquals("4", value(current, "SELECT earnedMarks FROM mock_test_attempts WHERE id='mock'"))
            assertEquals("0", value(current, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN ('sync_outbox','synced_attempts')"))
        } finally {
            upgraded?.close()
            old.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun appContainsNeitherAdvertisingSdkNorSyncImplementation() {
        for (name in listOf("com.google.android.gms.ads.MobileAds", "com.google.android.ump.UserMessagingPlatform",
            "com.aptirise.app.data.repository.CloudSyncRepository", "androidx.work.WorkManager")) {
            try {
                Class.forName(name, false, context.classLoader)
                fail("Removed implementation is still packaged: $name")
            } catch (_: ClassNotFoundException) { }
        }
        val permissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty()
        assertFalse(permissions.any { it.endsWith("AD_ID") || it.contains("AD_SERVICES") })
    }

    @Test
    fun retiredDiagnosticScreenRestoresToHome() {
        val factory = NamespaceFragmentFactory()
        for (name in listOf("com.example.aptiready.ui.ads.AdDiagnosticsFragment", "com.aptirise.app.ui.ads.AdDiagnosticsFragment")) {
            assertEquals("com.aptirise.app.ui.home.HomeFragment", factory.instantiate(context.classLoader, name).javaClass.name)
        }
    }

    private fun value(db: SupportSQLiteDatabase, sql: String): String = db.query(sql).use {
        assertTrue(it.moveToFirst())
        it.getString(0)
    }

    private fun insertRow(db: SupportSQLiteDatabase, table: String, overrides: Map<String, Any>) {
        val columns = mutableListOf<String>()
        val values = mutableListOf<Any>()
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1)
                columns.add("`$name`")
                values.add(overrides[name] ?: if (cursor.getString(2) == "TEXT") "" else 0)
            }
        }
        db.execSQL("INSERT INTO `$table` (${columns.joinToString()}) VALUES (${columns.joinToString { "?" }})", values.toTypedArray())
    }
}

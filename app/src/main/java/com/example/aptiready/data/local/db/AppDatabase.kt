package com.example.aptiready.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        TopicEntity::class,
        QuestionEntity::class,
        PracticeSessionEntity::class,
        SessionQuestionSnapshotEntity::class,
        BookmarkEntity::class,
        MockTestEntity::class,
        MockTestAttemptEntity::class,
        MockQuestionSnapshotEntity::class,
        SyncOutboxEntity::class,
        SyncedAttemptEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contentDao(): ContentDao
    abstract fun practiceSessionDao(): PracticeSessionDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun mockTestDao(): MockTestDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun syncedAttemptDao(): SyncedAttemptDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2: Migration = AppDatabaseMigrations.MIGRATION_1_2
        val MIGRATION_2_3: Migration = AppDatabaseMigrations.MIGRATION_2_3
        val MIGRATION_3_4: Migration = AppDatabaseMigrations.MIGRATION_3_4
        val MIGRATION_4_5: Migration = AppDatabaseMigrations.MIGRATION_4_5
        val MIGRATION_5_6: Migration = AppDatabaseMigrations.MIGRATION_5_6

        val MIGRATION_6_7: Migration = AppDatabaseMigrations.MIGRATION_6_7

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aptirise_database.db"
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object AppDatabaseMigrations {
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val invalid = "Shortcut Formula: Net Change = x + y + (xy / 100). Apply direct percentage multiplier to solve quickly."
            for (table in listOf("questions", "session_question_snapshots", "bookmarks")) {
                db.execSQL("UPDATE `$table` SET `extraHint` = '' WHERE `extraHint` = ?", arrayOf(invalid))
            }
        }
    }

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `mock_tests` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `questionIdsJson` TEXT NOT NULL,
                    `durationSeconds` INTEGER NOT NULL,
                    `marksPerCorrect` INTEGER NOT NULL,
                    `published` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `mock_test_attempts` (
                    `id` TEXT NOT NULL,
                    `ownerId` TEXT NOT NULL,
                    `testId` TEXT NOT NULL,
                    `testTitle` TEXT NOT NULL,
                    `testDescription` TEXT NOT NULL,
                    `testVersion` INTEGER NOT NULL,
                    `durationSeconds` INTEGER NOT NULL,
                    `marksPerCorrect` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `currentQuestionIndex` INTEGER NOT NULL,
                    `elapsedSeconds` INTEGER NOT NULL,
                    `startTimeMillis` INTEGER NOT NULL,
                    `bootTimeMarker` INTEGER NOT NULL,
                    `lastSavedTimeMillis` INTEGER NOT NULL,
                    `remainingSeconds` INTEGER NOT NULL,
                    `earnedMarks` INTEGER NOT NULL,
                    `maxMarks` INTEGER NOT NULL,
                    `scorePercentage` REAL NOT NULL,
                    `accuracyPercentage` REAL NOT NULL,
                    `finishReason` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `completedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `mock_question_snapshots` (
                    `id` TEXT NOT NULL,
                    `attemptId` TEXT NOT NULL,
                    `questionId` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    `questionText` TEXT NOT NULL,
                    `optionsJson` TEXT NOT NULL,
                    `correctOptionId` TEXT NOT NULL,
                    `explanation` TEXT NOT NULL,
                    `hint` TEXT NOT NULL,
                    `selectedOptionId` TEXT,
                    `isMarkedForReview` INTEGER NOT NULL,
                    `isVisited` INTEGER NOT NULL,
                    `isBookmarked` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sync_outbox` (
                    `id` TEXT NOT NULL,
                    `ownerId` TEXT NOT NULL,
                    `recordId` TEXT NOT NULL,
                    `recordType` TEXT NOT NULL,
                    `action` TEXT NOT NULL,
                    `payloadJson` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `retryCount` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `lastAttemptedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `synced_attempts` (
                    `attemptId` TEXT NOT NULL,
                    `ownerId` TEXT NOT NULL,
                    `syncedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`attemptId`)
                )
                """.trimIndent()
            )
            db.execSQL("ALTER TABLE `sync_outbox` ADD COLUMN `workerId` TEXT")
            db.execSQL("ALTER TABLE `sync_outbox` ADD COLUMN `claimedAt` INTEGER")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `sync_outbox` ADD COLUMN `revision` INTEGER NOT NULL DEFAULT 1")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `questions` ADD COLUMN `extraHint` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `session_question_snapshots` ADD COLUMN `extraHint` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `session_question_snapshots` ADD COLUMN `isExtraHintUnlocked` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `bookmarks` ADD COLUMN `extraHint` TEXT NOT NULL DEFAULT ''")
        }
    }
}

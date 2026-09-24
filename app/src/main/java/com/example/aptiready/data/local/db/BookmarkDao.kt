package com.example.aptiready.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE ownerId = :ownerId AND questionId = :questionId")
    suspend fun deleteBookmark(ownerId: String, questionId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE ownerId = :ownerId AND questionId = :questionId)")
    suspend fun isBookmarked(ownerId: String, questionId: String): Boolean

    @Query("SELECT * FROM bookmarks WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun getBookmarksForOwner(ownerId: String): Flow<List<BookmarkEntity>>
}
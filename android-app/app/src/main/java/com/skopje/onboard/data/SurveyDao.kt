package com.skopje.onboard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(survey: Survey): Long

    @Update
    suspend fun update(survey: Survey)

    @Query("SELECT * FROM surveys WHERE isSubmitted = 0 ORDER BY id DESC LIMIT 1")
    suspend fun getPendingSurvey(): Survey?

    /** Keeps only the newest in-progress row; avoids duplicate drafts if the app raced before resume handling. */
    @Query(
        "DELETE FROM surveys WHERE isSubmitted = 0 AND id < (" +
            "SELECT IFNULL(MAX(id), 0) FROM surveys WHERE isSubmitted = 0" +
            ")",
    )
    suspend fun deleteStalePendingSurveys()

    @Query("SELECT * FROM surveys WHERE isSubmitted = 1 AND uploadedStatus = 0 ORDER BY id ASC")
    suspend fun getUnuploadedSubmittedSurveys(): List<Survey>

    @Query("SELECT * FROM surveys WHERE isSubmitted = 1 ORDER BY id DESC")
    fun observeSubmittedSurveys(): Flow<List<Survey>>

    @Query("UPDATE surveys SET uploadedStatus = 1 WHERE id = :id")
    suspend fun markUploaded(id: Long)

    @Query("SELECT * FROM surveys WHERE id = :id")
    suspend fun getById(id: Long): Survey?

    @Query("DELETE FROM surveys WHERE id = :id")
    suspend fun delete(id: Long)
}

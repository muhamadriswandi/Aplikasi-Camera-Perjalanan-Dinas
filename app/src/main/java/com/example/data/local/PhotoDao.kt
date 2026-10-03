package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE projectName = :projectName ORDER BY timestamp DESC")
    fun getPhotosByProject(projectName: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE projectName = :projectName")
    suspend fun getPhotosByProjectSync(projectName: String): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE dateFormatted = :dateFormatted ORDER BY timestamp DESC")
    fun getPhotosByDate(dateFormatted: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE year = :year AND month = :month ORDER BY timestamp DESC")
    fun getPhotosByMonth(year: Int, month: Int): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE year = :year ORDER BY timestamp DESC")
    fun getPhotosByYear(year: Int): Flow<List<PhotoEntity>>

    @Query("""
        SELECT * FROM photos 
        WHERE fileName LIKE '%' || :query || '%' 
           OR projectName LIKE '%' || :query || '%' 
           OR village LIKE '%' || :query || '%' 
           OR district LIKE '%' || :query || '%' 
           OR regency LIKE '%' || :query || '%' 
           OR address LIKE '%' || :query || '%' 
           OR note LIKE '%' || :query || '%'
           OR dateFormatted LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchPhotos(query: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    suspend fun getPhotoById(id: Long): PhotoEntity?

    @Query("SELECT DISTINCT projectName FROM photos WHERE projectName != ''")
    fun getDistinctProjectNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id IN (:ids)")
    suspend fun deletePhotosByIds(ids: List<Long>)

    @Query("UPDATE photos SET projectName = :newProjectName WHERE id IN (:ids)")
    suspend fun movePhotosToProject(ids: List<Long>, newProjectName: String)

    @Query("SELECT COUNT(*) FROM photos")
    fun getPhotoCount(): Flow<Int>
}

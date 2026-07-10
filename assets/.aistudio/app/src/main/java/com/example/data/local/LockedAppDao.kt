package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LockedApp
import com.example.data.model.SecurityQuestion
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedAppDao {
    @Query("SELECT * FROM locked_apps")
    fun getAllLockedApps(): Flow<List<LockedApp>>

    @Query("SELECT * FROM locked_apps WHERE isLocked = 1")
    fun getLockedAppsOnly(): Flow<List<LockedApp>>

    @Query("SELECT * FROM locked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getLockedAppByPackageName(packageName: String): LockedApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLockedApp(lockedApp: LockedApp)

    @Delete
    suspend fun deleteLockedApp(lockedApp: LockedApp)

    // Security Question operations
    @Query("SELECT * FROM security_questions WHERE id = 1 LIMIT 1")
    fun getSecurityQuestion(): Flow<SecurityQuestion?>

    @Query("SELECT * FROM security_questions WHERE id = 1 LIMIT 1")
    suspend fun getSecurityQuestionSync(): SecurityQuestion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecurityQuestion(securityQuestion: SecurityQuestion)

    @Query("DELETE FROM security_questions WHERE id = 1")
    suspend fun deleteSecurityQuestion()
}

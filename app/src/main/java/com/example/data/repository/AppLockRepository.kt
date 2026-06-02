package com.example.data.repository

import com.example.data.local.LockedAppDao
import com.example.data.model.LockedApp
import com.example.data.model.SecurityQuestion
import kotlinx.coroutines.flow.Flow

class AppLockRepository(
    private val lockedAppDao: LockedAppDao
) {
    fun getAllLockedApps(): Flow<List<LockedApp>> = lockedAppDao.getAllLockedApps()

    fun getLockedAppsOnly(): Flow<List<LockedApp>> = lockedAppDao.getLockedAppsOnly()

    suspend fun getLockedAppByPackageName(packageName: String): LockedApp? {
        return lockedAppDao.getLockedAppByPackageName(packageName)
    }

    suspend fun insertLockedApp(lockedApp: LockedApp) {
        lockedAppDao.insertLockedApp(lockedApp)
    }

    suspend fun deleteLockedApp(lockedApp: LockedApp) {
        lockedAppDao.deleteLockedApp(lockedApp)
    }

    fun getSecurityQuestion(): Flow<SecurityQuestion?> = lockedAppDao.getSecurityQuestion()

    suspend fun getSecurityQuestionSync(): SecurityQuestion? {
        return lockedAppDao.getSecurityQuestionSync()
    }

    suspend fun insertSecurityQuestion(securityQuestion: SecurityQuestion) {
        lockedAppDao.insertSecurityQuestion(securityQuestion)
    }

    suspend fun deleteSecurityQuestion() {
        lockedAppDao.deleteSecurityQuestion()
    }
}

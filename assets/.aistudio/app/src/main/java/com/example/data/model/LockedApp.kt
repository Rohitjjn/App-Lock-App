package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locked_apps")
data class LockedApp(
    @PrimaryKey val packageName: String,
    val isLocked: Boolean = false,
    val lockMethod: LockMethod = LockMethod.PIN
)

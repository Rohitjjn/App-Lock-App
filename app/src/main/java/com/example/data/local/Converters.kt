package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.LockMethod

class Converters {
    @TypeConverter
    fun fromLockMethod(value: LockMethod): String {
        return value.name
    }

    @TypeConverter
    fun toLockMethod(value: String): LockMethod {
        return try {
            LockMethod.valueOf(value)
        } catch (e: Exception) {
            LockMethod.PIN
        }
    }
}

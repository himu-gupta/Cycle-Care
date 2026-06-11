package com.himu.cyclecare.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PeriodEntity::class, DailyLogEntity::class], version = 1, exportSchema = true)
abstract class CycleDatabase : RoomDatabase() {
    abstract fun cycleDao(): CycleDao

    companion object {
        fun create(context: Context): CycleDatabase = Room.databaseBuilder(
            context,
            CycleDatabase::class.java,
            "cycle-care.db",
        ).build()
    }
}

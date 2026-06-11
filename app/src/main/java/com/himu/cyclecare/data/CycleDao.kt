package com.himu.cyclecare.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {
    @Query("SELECT * FROM period_entries ORDER BY startEpochDay")
    fun observePeriods(): Flow<List<PeriodEntity>>

    @Query("SELECT * FROM period_entries ORDER BY startEpochDay")
    suspend fun getPeriods(): List<PeriodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriod(entry: PeriodEntity): Long

    @Delete
    suspend fun deletePeriod(entry: PeriodEntity)

    @Query("SELECT * FROM daily_logs ORDER BY epochDay DESC")
    fun observeLogs(): Flow<List<DailyLogEntity>>

    @Upsert
    suspend fun upsertLog(log: DailyLogEntity)
}

package com.levelup.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isActive = 1 ORDER BY tier, sortOrder, id")
    fun observeActive(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isActive = 1 ORDER BY tier, sortOrder, id")
    suspend fun getActive(): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY tier, sortOrder, id")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM tasks WHERE tier = :tier")
    suspend fun maxOrder(tier: Int): Int

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun count(): Int

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TaskLogDao {
    @Query("SELECT * FROM task_logs WHERE date = :date")
    fun observe(date: String): Flow<List<TaskLogEntity>>

    @Query("SELECT * FROM task_logs WHERE date = :date")
    suspend fun getForDate(date: String): List<TaskLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: TaskLogEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(logs: List<TaskLogEntity>)

    @Query("DELETE FROM task_logs WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: Long)
}

@Dao
interface PhaseDao {
    @Transaction
    @Query("SELECT * FROM phases ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<PhaseWithLessons>>

    @Transaction
    @Query("SELECT * FROM phases ORDER BY sortOrder, id")
    suspend fun getAll(): List<PhaseWithLessons>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM phases")
    suspend fun maxOrder(): Int

    @Query("SELECT COUNT(*) FROM phases")
    suspend fun count(): Int

    @Insert
    suspend fun insert(phase: PhaseEntity): Long

    @Update
    suspend fun update(phase: PhaseEntity)

    @Query("DELETE FROM phases WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons ORDER BY phaseId, sortOrder, id")
    fun observeAll(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE phaseId = :phaseId ORDER BY sortOrder, id")
    suspend fun getForPhase(phaseId: Long): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun get(id: Long): LessonEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM lessons WHERE phaseId = :phaseId")
    suspend fun maxOrder(phaseId: Long): Int

    @Insert
    suspend fun insert(lesson: LessonEntity): Long

    @Update
    suspend fun update(lesson: LessonEntity)

    @Query("UPDATE lessons SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("DELETE FROM lessons WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface DeadlineDao {
    @Query("SELECT * FROM deadlines ORDER BY date, id")
    fun observeAll(): Flow<List<DeadlineEntity>>

    @Query("SELECT * FROM deadlines ORDER BY date, id")
    suspend fun getAll(): List<DeadlineEntity>

    @Query("SELECT * FROM deadlines WHERE id = :id")
    suspend fun get(id: Long): DeadlineEntity?

    @Insert
    suspend fun insert(deadline: DeadlineEntity): Long

    @Update
    suspend fun update(deadline: DeadlineEntity)

    @Query("DELETE FROM deadlines WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface DayRecordDao {
    @Query("SELECT * FROM day_records ORDER BY date")
    fun observeAll(): Flow<List<DayRecordEntity>>

    @Query("SELECT * FROM day_records ORDER BY date")
    suspend fun getAll(): List<DayRecordEntity>

    @Query("SELECT * FROM day_records WHERE date = :date")
    suspend fun get(date: String): DayRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: DayRecordEntity)
}

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips ORDER BY createdAt DESC")
    suspend fun getAll(): List<ClipEntity>

    @Query("SELECT * FROM clips WHERE id = :id")
    suspend fun get(id: Long): ClipEntity?

    @Insert
    suspend fun insert(clip: ClipEntity): Long

    @Query("UPDATE clips SET uploadState = :state, driveFileId = :driveFileId WHERE id = :id")
    suspend fun updateState(id: Long, state: String, driveFileId: String?)

    @Query("UPDATE clips SET uploadState = :toState WHERE uploadState IN (:fromStates)")
    suspend fun updateStateWhere(fromStates: List<String>, toState: String)

    @Query("DELETE FROM clips WHERE id = :id")
    suspend fun delete(id: Long)
}

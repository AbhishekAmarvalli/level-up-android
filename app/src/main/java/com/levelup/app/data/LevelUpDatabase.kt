package com.levelup.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TaskEntity::class,
        TaskLogEntity::class,
        PhaseEntity::class,
        LessonEntity::class,
        DeadlineEntity::class,
        DayRecordEntity::class,
        ClipEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class LevelUpDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskLogDao(): TaskLogDao
    abstract fun phaseDao(): PhaseDao
    abstract fun lessonDao(): LessonDao
    abstract fun deadlineDao(): DeadlineDao
    abstract fun dayRecordDao(): DayRecordDao
    abstract fun clipDao(): ClipDao

    companion object {
        fun build(context: Context): LevelUpDatabase =
            Room.databaseBuilder(context, LevelUpDatabase::class.java, "levelup.db")
                .build()
    }
}

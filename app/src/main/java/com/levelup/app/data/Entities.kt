package com.levelup.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Room schema. Dates are stored as ISO-8601 strings (yyyy-MM-dd) so they sort and
 * compare correctly in SQL and survive timezone changes without shifting the day.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val tier: Int,
    val isAiTagged: Boolean = false,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "task_logs",
    primaryKeys = ["date", "taskId"],
    indices = [Index("taskId")],
)
data class TaskLogEntity(
    val date: String,
    val taskId: Long,
    val done: Boolean,
    /** Swiped away for this day only. Not part of the visible task list. */
    val skipped: Boolean = false,
)

@Entity(tableName = "phases")
data class PhaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = PhaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["phaseId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("phaseId")],
)
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phaseId: Long,
    val title: String,
    val done: Boolean = false,
    val sortOrder: Int = 0,
    val url: String? = null,
)

/** Room relation used by the Roadmap and by the widget snapshot. */
data class PhaseWithLessons(
    @Embedded val phase: PhaseEntity,
    @Relation(parentColumn = "id", entityColumn = "phaseId")
    val lessons: List<LessonEntity>,
)

@Entity(tableName = "deadlines")
data class DeadlineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: String,
    /** EXAM / ASSIGNMENT / PROJECT / OTHER */
    val type: String,
    val done: Boolean = false,
    val doneDate: String? = null,
)

@Entity(tableName = "day_records")
data class DayRecordEntity(
    @PrimaryKey val date: String,
    val minimumCompleted: Boolean,
    val tasksDone: Int,
    val tasksTotal: Int,
)

@Entity(tableName = "clips")
data class ClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val createdAt: Long,
    val durationSeconds: Int,
    /** LOCAL_ONLY / QUEUED / WAITING_FOR_WIFI / UPLOADING / UPLOADED / PAUSED_STORAGE / FAILED */
    val uploadState: String,
    val driveFileId: String? = null,
)

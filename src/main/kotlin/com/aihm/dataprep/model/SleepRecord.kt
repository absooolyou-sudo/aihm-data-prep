package com.aihm.dataprep.model

/** Raw row as read from CSV (all fields as strings, empty = null). */
data class RawSleepRow(
    val lineNumber: Int,
    val userId: String?,
    val date: String?,
    val go2bed: String?,
    val asleep: String?,
    val wakeup: String?,
    val wakeupAtNight: String?,
    val waso: String?,
    val sleepDuration: String?,
    val inBedDuration: String?,
    val sleepLatency: String?,
    val sleepEfficiency: String?
)

/** Parsed, validated sleep record. [issues] collects every quality problem. */
data class SleepRecord(
    val lineNumber: Int,
    val userId: String,
    val date: String,
    val go2bed: String?,
    val asleep: String?,
    val wakeup: String?,
    val wakeupAtNight: Int?,
    val waso: Double?,
    val sleepDuration: Double?,
    val inBedDuration: Double?,
    val sleepLatency: Double?,
    val sleepEfficiency: Double?,
    val issues: MutableList<String> = mutableListOf()
) {
    fun bedtimeMinutesSinceNoon(): Double? = go2bed?.let { hhmmToMinutesSinceNoon(it) }
    fun wakeupMinutes(): Double? = wakeup?.let { hhmmToMinutes(it) }

    companion object {
        fun hhmmToMinutes(s: String): Double? {
            val parts = s.split(":")
            if (parts.size < 2) return null
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val sec = if (parts.size >= 3) parts[2].toIntOrNull() ?: 0 else 0
            return h * 60 + m + sec / 60.0
        }
        fun hhmmToMinutesSinceNoon(s: String): Double? {
            val mins = hhmmToMinutes(s) ?: return null
            return if (mins < 12 * 60) mins + 24 * 60 else mins
        }
    }
}

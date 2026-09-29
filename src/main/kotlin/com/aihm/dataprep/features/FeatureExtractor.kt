package com.aihm.dataprep.features

import com.aihm.dataprep.cleaning.Cleaner
import com.aihm.dataprep.eda.Stats
import com.aihm.dataprep.model.SleepRecord
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Feature extraction (T8: model-ready features).
 * Same pipeline shape reused later for heart-rate windows
 * (mean HR, SD, baseline deviation, steps, time-of-day).
 */
object FeatureExtractor {

    data class RowFeatures(
        val userId: String, val date: String,
        val dayOfWeek: String, val isWeekend: Int,
        val bedtimeHour: Double?, val wakeupHour: Double?,
        val sleepDuration: Double?, val inBedDuration: Double?,
        val sleepLatency: Double?, val waso: Double?,
        val sleepEfficiency: Double?, val wakeupAtNight: Int?,
        val sleepDebtVs8h: Double?, val midSleepHour: Double?
    )

    data class UserFeatures(
        val userId: String, val nights: Int,
        val meanSleepDuration: Double?, val stdSleepDuration: Double?,
        val medianSleepDuration: Double?, val meanEfficiency: Double?,
        val meanLatency: Double?, val meanWaso: Double?,
        val pctNightWakeups: Double?, val pctLowEfficiency: Double?,
        val weekendWeekdaySleepDiff: Double?, val bedtimeConsistencyStd: Double?
    )

    private fun dateOf(r: SleepRecord): LocalDate? = runCatching { LocalDate.parse(r.date) }.getOrNull()

    fun perRow(records: List<SleepRecord>): List<RowFeatures> = records.map { raw ->
        val r = Cleaner.effective(raw)
        val dow = dateOf(r)?.dayOfWeek
        val bedtimeMin = r.bedtimeMinutesSinceNoon()
        val wakeupMin = r.wakeupMinutes()
        val midSleep = if (bedtimeMin != null && wakeupMin != null) {
            val w = wakeupMin + 24 * 60
            (bedtimeMin + w) / 2.0 - 12 * 60
        } else null
        RowFeatures(
            userId = r.userId, date = r.date,
            dayOfWeek = dow?.name ?: "UNKNOWN",
            isWeekend = if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) 1 else 0,
            bedtimeHour = bedtimeMin?.let { it / 60.0 },
            wakeupHour = wakeupMin?.let { it / 60.0 },
            sleepDuration = r.sleepDuration, inBedDuration = r.inBedDuration,
            sleepLatency = r.sleepLatency, waso = r.waso,
            sleepEfficiency = r.sleepEfficiency, wakeupAtNight = r.wakeupAtNight,
            sleepDebtVs8h = r.sleepDuration?.let { it - 8.0 },
            midSleepHour = midSleep?.let { it / 60.0 }
        )
    }

    fun perUser(rows: List<RowFeatures>): List<UserFeatures> {
        val users = rows.map { it.userId }.distinct().sorted()
        return users.map { u ->
            val rs = rows.filter { it.userId == u }
            val dur = rs.mapNotNull { it.sleepDuration }
            val eff = rs.mapNotNull { it.sleepEfficiency }
            val lat = rs.mapNotNull { it.sleepLatency }
            val waso = rs.mapNotNull { it.waso }
            val wknd = rs.filter { it.isWeekend == 1 }.mapNotNull { it.sleepDuration }
            val wday = rs.filter { it.isWeekend == 0 }.mapNotNull { it.sleepDuration }
            val bt = rs.mapNotNull { it.bedtimeHour }
            UserFeatures(
                userId = u, nights = rs.size,
                meanSleepDuration = Stats.mean(dur), stdSleepDuration = Stats.std(dur),
                medianSleepDuration = Stats.median(dur), meanEfficiency = Stats.mean(eff),
                meanLatency = Stats.mean(lat), meanWaso = Stats.mean(waso),
                pctNightWakeups = if (rs.isEmpty()) null else
                    rs.count { (it.wakeupAtNight ?: 0) == 1 } * 100.0 / rs.size,
                pctLowEfficiency = if (eff.isEmpty()) null else
                    eff.count { it < 0.85 } * 100.0 / eff.size,
                weekendWeekdaySleepDiff = if (wknd.isNotEmpty() && wday.isNotEmpty())
                    Stats.mean(wknd)!! - Stats.mean(wday)!! else null,
                bedtimeConsistencyStd = Stats.std(bt)
            )
        }
    }
}

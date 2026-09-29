package com.aihm.dataprep.io

import com.aihm.dataprep.model.RawSleepRow
import com.aihm.dataprep.model.SleepRecord
import java.io.File

/** Minimal CSV reader: comma-separated, trims fields, empty string -> null. */
object CsvReader {
    fun readRows(file: File): List<RawSleepRow> {
        val lines = file.readLines().filter { it.isNotBlank() }
        return lines.drop(1).mapIndexedNotNull { i, line ->
            val cols = splitCsvLine(line)
            if (cols.size < 11) null else RawSleepRow(
                lineNumber = i + 2,
                userId = cols[0].ifBlank { null },
                date = cols[1].ifBlank { null },
                go2bed = cols[2].ifBlank { null },
                asleep = cols[3].ifBlank { null },
                wakeup = cols[4].ifBlank { null },
                wakeupAtNight = cols[5].ifBlank { null },
                waso = cols[6].ifBlank { null },
                sleepDuration = cols[7].ifBlank { null },
                inBedDuration = cols[8].ifBlank { null },
                sleepLatency = cols[9].ifBlank { null },
                sleepEfficiency = cols[10].ifBlank { null }
            )
        }
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { result.add(cur.toString().trim()); cur.clear() }
                else -> cur.append(c)
            }
        }
        result.add(cur.toString().trim())
        while (result.size < 11) result.add("")
        return result
    }
}

object RecordParser {
    private val DATE_RE = Regex("""^\d{4}-\d{2}-\d{2}$""")
    private val TIME_RE = Regex("""^\d{1,2}:\d{2}(:\d{2})?$""")

    fun parse(row: RawSleepRow): SleepRecord? {
        if (row.userId == null || row.date == null) return null
        val rec = SleepRecord(
            lineNumber = row.lineNumber, userId = row.userId, date = row.date,
            go2bed = row.go2bed, asleep = row.asleep, wakeup = row.wakeup,
            wakeupAtNight = row.wakeupAtNight?.toIntOrNull(),
            waso = row.waso?.toDoubleOrNull(),
            sleepDuration = row.sleepDuration?.toDoubleOrNull(),
            inBedDuration = row.inBedDuration?.toDoubleOrNull(),
            sleepLatency = row.sleepLatency?.toDoubleOrNull(),
            sleepEfficiency = row.sleepEfficiency?.toDoubleOrNull()
        )
        if (!DATE_RE.matches(rec.date)) rec.issues.add("bad_date_format")
        for ((name, v) in listOf("go2bed" to rec.go2bed, "asleep" to rec.asleep, "wakeup" to rec.wakeup)) {
            if (v != null && !TIME_RE.matches(v)) rec.issues.add("bad_time_$name")
        }
        if (rec.wakeupAtNight != null && rec.wakeupAtNight !in 0..1) rec.issues.add("wakeup@night_not_binary")
        if (rec.waso != null && rec.waso < 0) rec.issues.add("waso_negative")
        if (rec.sleepDuration != null && rec.sleepDuration <= 0) rec.issues.add("sleep_duration_nonpositive")
        if (rec.inBedDuration != null && rec.inBedDuration <= 0) rec.issues.add("in_bed_nonpositive")
        if (rec.sleepLatency != null && rec.sleepLatency < 0) rec.issues.add("latency_negative")
        if (rec.sleepEfficiency != null && (rec.sleepEfficiency <= 0 || rec.sleepEfficiency > 1.0001))
            rec.issues.add("efficiency_out_of_range")
        if (rec.sleepDuration != null && rec.inBedDuration != null && rec.sleepDuration > rec.inBedDuration + 1e-9)
            rec.issues.add("sleep_gt_in_bed")
        if (rec.sleepDuration != null && rec.inBedDuration != null && rec.inBedDuration > 0) {
            val computed = rec.sleepDuration / rec.inBedDuration
            if (rec.sleepEfficiency != null && kotlin.math.abs(computed - rec.sleepEfficiency) > 0.02)
                rec.issues.add("efficiency_mismatch_computed=%.3f".format(computed))
        }
        if (rec.sleepDuration != null && rec.sleepDuration > 16) rec.issues.add("extreme_sleep_duration_gt16h")
        if (rec.sleepDuration != null && rec.sleepDuration < 2) rec.issues.add("extreme_sleep_duration_lt2h")
        return rec
    }
}

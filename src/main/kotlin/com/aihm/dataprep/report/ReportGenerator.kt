package com.aihm.dataprep.report

import com.aihm.dataprep.cleaning.Cleaner
import com.aihm.dataprep.eda.Profiler
import com.aihm.dataprep.eda.Stats
import com.aihm.dataprep.features.FeatureExtractor
import com.aihm.dataprep.model.SleepRecord
import java.io.File

/** Writes the EDA markdown report + cleaned/feature CSVs into output/. */
object ReportGenerator {

    fun writeAll(
        outDir: File,
        rawProfile: Profiler.Profile,
        cleanProfile: Profiler.Profile,
        cleaningLog: List<String>,
        cleanedRecords: List<SleepRecord>,
        rowFeatures: List<FeatureExtractor.RowFeatures>,
        userFeatures: List<FeatureExtractor.UserFeatures>
    ) {
        outDir.mkdirs()

        val sb = StringBuilder()
        sb.appendLine("# EDA & Data Preparation Report — AIHM Data Prep Pipeline")
        sb.appendLine()
        sb.appendLine("## 1. Dataset Overview (RAW)")
        sb.appendLine("- Total rows: **${rawProfile.totalRows}**")
        sb.appendLine("- Participants (userId): **${rawProfile.users.size}**")
        sb.appendLine("- Date range: ${rawProfile.dateRange?.let { "${it.first} → ${it.second}" } ?: "—"}")
        sb.appendLine("- Rows per participant (first 10): ${rawProfile.rowsPerUser.toList().sortedBy { it.first }.take(10).joinToString { (u, c) -> "$u=$c" }} …")
        sb.appendLine()
        sb.appendLine("## 2. Missing Values (RAW)")
        sb.appendLine("| Column | Missing | % |")
        sb.appendLine("|---|---:|---:|")
        rawProfile.missingPerColumn.forEach { (k, v) ->
            val pct = if (rawProfile.totalRows > 0) "%.2f".format(100.0 * v / rawProfile.totalRows) else "0"
            sb.appendLine("| $k | $v | $pct% |")
        }
        sb.appendLine()
        sb.appendLine("## 3. Duplicates (RAW)")
        sb.appendLine("- Exact duplicate rows: **${rawProfile.exactDuplicates}**")
        sb.appendLine("- Duplicate (userId, date) pairs: **${rawProfile.userDateDuplicates}**")
        sb.appendLine()
        sb.appendLine("## 4. Numeric Descriptive Statistics (RAW)")
        sb.appendLine("| Column | N | Mean | Std | Min | Q1 | Median | Q3 | Max | IQR-outliers |")
        sb.appendLine("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
        rawProfile.numericSummaries.forEach { (name, s) ->
            sb.appendLine("| $name | ${s.count} | ${Stats.fmt(s.mean)} | ${Stats.fmt(s.std)} | ${Stats.fmt(s.min)} | ${Stats.fmt(s.q1)} | ${Stats.fmt(s.median)} | ${Stats.fmt(s.q3)} | ${Stats.fmt(s.max)} | ${s.iqrOutliers.size} |")
        }
        sb.appendLine()
        sb.appendLine("### Outlier counts (IQR method)")
        rawProfile.numericSummaries.forEach { (name, s) ->
            if (s.iqrOutliers.isNotEmpty()) {
                sb.appendLine("- **$name**: ${s.iqrOutliers.size} outliers (e.g. ${s.iqrOutliers.take(5).joinToString { "%.2f".format(it.second) }})")
            }
        }
        sb.appendLine()
        sb.appendLine("## 5. Data Quality Issues (RAW, validation flags)")
        sb.appendLine("- Rows with ≥1 issue: **${rawProfile.rowsWithAnyIssue}** / ${rawProfile.totalRows}")
        if (rawProfile.issueCounts.isEmpty()) sb.appendLine("- No validation issues found.")
        else {
            sb.appendLine("| Issue | Count |")
            sb.appendLine("|---|---:|")
            rawProfile.issueCounts.forEach { (k, v) -> sb.appendLine("| `$k` | $v |") }
        }
        sb.appendLine()
        sb.appendLine("## 6. Cleaning Log")
        cleaningLog.forEach { sb.appendLine("- $it") }
        sb.appendLine()
        sb.appendLine("## 7. Cleaned Dataset Overview")
        sb.appendLine("- Rows after cleaning: **${cleanProfile.totalRows}** (was ${rawProfile.totalRows})")
        sb.appendLine("- Rows with ≥1 issue remaining: **${cleanProfile.rowsWithAnyIssue}**")
        sb.appendLine("| Column | N | Mean | Std | Min | Median | Max |")
        sb.appendLine("|---|---:|---:|---:|---:|---:|---:|")
        cleanProfile.numericSummaries.forEach { (name, s) ->
            sb.appendLine("| $name | ${s.count} | ${Stats.fmt(s.mean)} | ${Stats.fmt(s.std)} | ${Stats.fmt(s.min)} | ${Stats.fmt(s.median)} | ${Stats.fmt(s.max)} |")
        }
        sb.appendLine()
        sb.appendLine("## 8. Output Files")
        sb.appendLine("- `cleaned_sleep_diary.csv` — cleaned, imputed, validated rows")
        sb.appendLine("- `features_per_night.csv` — per-night engineered features (model-ready)")
        sb.appendLine("- `features_per_user.csv` — per-participant aggregated features")
        sb.appendLine()
        sb.appendLine("> Pipeline is generic: swap the loader + record model for heart-rate CSVs (Galaxy Watch Active2 / GalaxyPPG / self-collected Galaxy Watch7) and the same profiling/cleaning/feature-extract stages apply.")

        File(outDir, "eda_report.md").writeText(sb.toString())

        val cleanCsv = StringBuilder()
        cleanCsv.appendLine("userId,date,go2bed,asleep,wakeup,wakeup@night,waso,sleep_duration,in_bed_duration,sleep_latency,sleep_efficiency,flags")
        cleanedRecords.forEach { raw ->
            val r = Cleaner.effective(raw)
            cleanCsv.appendLine(listOf(
                r.userId, r.date, r.go2bed ?: "", r.asleep ?: "", r.wakeup ?: "",
                r.wakeupAtNight?.toString() ?: "", fmt(r.waso), fmt(r.sleepDuration),
                fmt(r.inBedDuration), fmt(r.sleepLatency), fmt(r.sleepEfficiency),
                r.issues.joinToString(";")
            ).joinToString(","))
        }
        File(outDir, "cleaned_sleep_diary.csv").writeText(cleanCsv.toString())

        val rowCsv = StringBuilder()
        rowCsv.appendLine("userId,date,dayOfWeek,isWeekend,bedtimeHour,wakeupHour,sleepDuration,inBedDuration,sleepLatency,waso,sleepEfficiency,wakeupAtNight,sleepDebtVs8h,midSleepHour")
        rowFeatures.forEach { f ->
            rowCsv.appendLine(listOf(
                f.userId, f.date, f.dayOfWeek, f.isWeekend, fmt(f.bedtimeHour), fmt(f.wakeupHour),
                fmt(f.sleepDuration), fmt(f.inBedDuration), fmt(f.sleepLatency), fmt(f.waso),
                fmt(f.sleepEfficiency), f.wakeupAtNight?.toString() ?: "", fmt(f.sleepDebtVs8h), fmt(f.midSleepHour)
            ).joinToString(","))
        }
        File(outDir, "features_per_night.csv").writeText(rowCsv.toString())

        val userCsv = StringBuilder()
        userCsv.appendLine("userId,nights,meanSleepDuration,stdSleepDuration,medianSleepDuration,meanEfficiency,meanLatency,meanWaso,pctNightWakeups,pctLowEfficiency,weekendWeekdaySleepDiff,bedtimeConsistencyStd")
        userFeatures.forEach { f ->
            userCsv.appendLine(listOf(
                f.userId, f.nights, fmt(f.meanSleepDuration), fmt(f.stdSleepDuration),
                fmt(f.medianSleepDuration), fmt(f.meanEfficiency), fmt(f.meanLatency), fmt(f.meanWaso),
                fmt(f.pctNightWakeups), fmt(f.pctLowEfficiency), fmt(f.weekendWeekdaySleepDiff), fmt(f.bedtimeConsistencyStd)
            ).joinToString(","))
        }
        File(outDir, "features_per_user.csv").writeText(userCsv.toString())
    }

    private fun fmt(d: Double?): String = if (d == null) "" else "%.4f".format(d)
}

package com.aihm.dataprep

import com.aihm.dataprep.cleaning.Cleaner
import com.aihm.dataprep.eda.Profiler
import com.aihm.dataprep.features.FeatureExtractor
import com.aihm.dataprep.io.CsvReader
import com.aihm.dataprep.io.RecordParser
import com.aihm.dataprep.report.ReportGenerator
import java.io.File

/**
 * Entry point:  RAW data → EDA profile → cleaning → features → outputs.
 * Usage:  java -jar aihm-dataprep.jar [path/to/raw.csv] [path/to/output_dir]
 * Defaults: data/raw/sleep_diary.csv → output/
 */
fun main(args: Array<String>) {
    val input = args.getOrNull(0) ?: "data/raw/sleep_diary.csv"
    val outDir = File(args.getOrNull(1) ?: "output")

    println("=== AIHM Data Preparation Pipeline ===")
    println("Input : $input")
    println("Output: ${outDir.absolutePath}")

    val rawRows = CsvReader.readRows(File(input))
    val parsed = rawRows.mapNotNull { RecordParser.parse(it) }
    println("Loaded ${rawRows.size} rows -> ${parsed.size} parseable records.")

    val rawProfile = Profiler.profile(parsed)
    println("RAW: ${rawProfile.totalRows} rows, ${rawProfile.users.size} users, " +
            "${rawProfile.rowsWithAnyIssue} rows with quality issues.")

    val cleaned = Cleaner.clean(parsed)
    val effectiveRecords = cleaned.records.map { Cleaner.effective(it) }
    val cleanProfile = Profiler.profile(effectiveRecords)
    println("CLEANING: ${cleaned.log.size} actions applied; final rows = ${cleanProfile.totalRows}")

    val rowFeatures = FeatureExtractor.perRow(cleaned.records)
    val userFeatures = FeatureExtractor.perUser(rowFeatures)
    println("FEATURES: ${rowFeatures.size} per-night rows, ${userFeatures.size} per-user summaries.")

    ReportGenerator.writeAll(
        outDir, rawProfile, cleanProfile, cleaned.log,
        cleaned.records, rowFeatures, userFeatures
    )
    println("DONE. Files written to: ${outDir.absolutePath}")
    println("  - eda_report.md")
    println("  - cleaned_sleep_diary.csv")
    println("  - features_per_night.csv")
    println("  - features_per_user.csv")
}

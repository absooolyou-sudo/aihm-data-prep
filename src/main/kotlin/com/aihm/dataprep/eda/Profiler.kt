package com.aihm.dataprep.eda

import com.aihm.dataprep.model.SleepRecord

/** Exploratory Data Analysis: produces a full profile of a dataset. */
object Profiler {

    data class Profile(
        val totalRows: Int,
        val users: List<String>,
        val rowsPerUser: Map<String, Int>,
        val dateRange: Pair<String, String>?,
        val missingPerColumn: Map<String, Int>,
        val exactDuplicates: Int,
        val userDateDuplicates: Int,
        val numericSummaries: Map<String, Stats.NumericSummary>,
        val issueCounts: Map<String, Int>,
        val rowsWithAnyIssue: Int,
        val perUserSleepMean: Map<String, Double?>
    )

    fun profile(records: List<SleepRecord>): Profile {
        val users = records.map { it.userId }.distinct().sorted()
        val rowsPerUser = records.groupingBy { it.userId }.eachCount()
        val dates = records.map { it.date }.sorted()
        val dateRange = if (dates.isEmpty()) null else dates.first() to dates.last()

        val missingPerColumn = mapOf(
            "go2bed" to records.count { it.go2bed == null },
            "asleep" to records.count { it.asleep == null },
            "wakeup" to records.count { it.wakeup == null },
            "wakeup@night" to records.count { it.wakeupAtNight == null },
            "waso" to records.count { it.waso == null },
            "sleep_duration" to records.count { it.sleepDuration == null },
            "in_bed_duration" to records.count { it.inBedDuration == null },
            "sleep_latency" to records.count { it.sleepLatency == null },
            "sleep_efficiency" to records.count { it.sleepEfficiency == null }
        )

        val exactDuplicates = records.size - records.map { it.copy(issues = mutableListOf()) }.distinct().count()
        val userDateKeys = records.map { it.userId to it.date }
        val userDateDuplicates = userDateKeys.size - userDateKeys.distinct().count()

        val numericSummaries = linkedMapOf(
            "waso (min)" to Stats.summarize(records.map { it.waso }),
            "sleep_duration (h)" to Stats.summarize(records.map { it.sleepDuration }),
            "in_bed_duration (h)" to Stats.summarize(records.map { it.inBedDuration }),
            "sleep_latency (h)" to Stats.summarize(records.map { it.sleepLatency }),
            "sleep_efficiency" to Stats.summarize(records.map { it.sleepEfficiency })
        )

        val issueCounts = records.flatMap { it.issues }.groupingBy { it }.eachCount()
            .toList().sortedByDescending { it.second }.toMap()
        val rowsWithAnyIssue = records.count { it.issues.isNotEmpty() }

        val perUserSleepMean = users.associateWith { u ->
            Stats.mean(records.filter { it.userId == u }.mapNotNull { it.sleepDuration })
        }

        return Profile(
            totalRows = records.size, users = users, rowsPerUser = rowsPerUser,
            dateRange = dateRange, missingPerColumn = missingPerColumn,
            exactDuplicates = exactDuplicates, userDateDuplicates = userDateDuplicates,
            numericSummaries = numericSummaries, issueCounts = issueCounts,
            rowsWithAnyIssue = rowsWithAnyIssue, perUserSleepMean = perUserSleepMean
        )
    }
}

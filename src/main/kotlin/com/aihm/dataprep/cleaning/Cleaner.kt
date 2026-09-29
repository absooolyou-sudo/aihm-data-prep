package com.aihm.dataprep.cleaning

import com.aihm.dataprep.eda.Stats
import com.aihm.dataprep.model.SleepRecord

/** Cleaning pipeline. Every action is logged (reproducibility for T8). */
object Cleaner {

    data class CleaningResult(
        val records: List<SleepRecord>,
        val log: List<String>,
        val imputedFlags: Set<Int>
    )

    /** lineNumber -> (field -> value) overrides applied during cleaning. */
    val overrides = mutableMapOf<Int, MutableMap<String, Double>>()

    fun clean(raw: List<SleepRecord>): CleaningResult {
        val log = mutableListOf<String>()
        var data = raw.toList()
        val imputed = mutableSetOf<Int>()
        overrides.clear()

        // 1. Drop exact duplicates
        val before = data.size
        data = data.distinctBy { it.copy(issues = mutableListOf()) }
        log.add("Dropped ${before - data.size} exact-duplicate rows.")

        // 2. Drop duplicate (userId, date) — keep first
        val seen = hashSetOf<Pair<String, String>>()
        val deduped = mutableListOf<SleepRecord>()
        var udDup = 0
        for (r in data) {
            val key = r.userId to r.date
            if (seen.add(key)) deduped.add(r) else udDup++
        }
        data = deduped
        log.add("Dropped $udDup duplicate (userId, date) rows (kept first occurrence).")

        // 3. Recompute sleep_efficiency when missing
        var recomputed = 0
        for (r in data) {
            if (r.sleepEfficiency == null && r.sleepDuration != null && r.inBedDuration != null && r.inBedDuration > 0) {
                val eff = (r.sleepDuration / r.inBedDuration).coerceIn(0.0, 1.0)
                r.issues.add("efficiency_recomputed")
                setOverride(r, "sleepEfficiency", eff)
                recomputed++
            }
        }
        log.add("Recomputed $recomputed missing sleep_efficiency values from durations.")

        // 4. Impute remaining numeric missing: per-user median, fallback global
        val numericCols = listOf(
            "waso" to { r: SleepRecord -> r.waso },
            "sleepDuration" to { r: SleepRecord -> r.sleepDuration },
            "inBedDuration" to { r: SleepRecord -> r.inBedDuration },
            "sleepLatency" to { r: SleepRecord -> r.sleepLatency },
            "sleepEfficiency" to { r: SleepRecord -> effective(r).sleepEfficiency }
        )
        val users = data.map { it.userId }.distinct()
        for ((name, getter) in numericCols) {
            val globalMedian = Stats.median(data.mapNotNull { getter(it) })
            for (u in users) {
                val userMedian = Stats.median(data.filter { it.userId == u }.mapNotNull { getter(it) }) ?: globalMedian ?: continue
                for (r in data.filter { it.userId == u && getter(it) == null }) {
                    setOverride(r, name, userMedian)
                    r.issues.add("imputed_$name")
                    imputed.add(r.lineNumber)
                }
            }
        }
        log.add("Imputed remaining missing numerics with per-user median (fallback: global median). Rows touched: ${imputed.size}.")

        // 5. Clamp efficiency to [0,1]
        var clamped = 0
        for (r in data) {
            val e = effective(r).sleepEfficiency
            if (e != null && (e < 0 || e > 1)) { setOverride(r, "sleepEfficiency", e.coerceIn(0.0, 1.0)); clamped++ }
        }
        if (clamped > 0) log.add("Clamped $clamped sleep_efficiency values into [0,1].")

        // 6. Drop rows still missing core fields
        val beforeDrop = data.size
        data = data.filter {
            val e = effective(it)
            e.sleepDuration != null && e.inBedDuration != null && e.sleepEfficiency != null
        }
        log.add("Dropped ${beforeDrop - data.size} rows still missing core fields after imputation.")

        return CleaningResult(data, log, imputed)
    }

    private fun setOverride(r: SleepRecord, field: String, v: Double) {
        overrides.getOrPut(r.lineNumber) { mutableMapOf() }[field] = v
    }

    fun effective(r: SleepRecord): SleepRecord {
        val o = overrides[r.lineNumber] ?: return r
        return r.copy(
            waso = o["waso"] ?: r.waso,
            sleepDuration = o["sleepDuration"] ?: r.sleepDuration,
            inBedDuration = o["inBedDuration"] ?: r.inBedDuration,
            sleepLatency = o["sleepLatency"] ?: r.sleepLatency,
            sleepEfficiency = o["sleepEfficiency"] ?: r.sleepEfficiency
        )
    }
}

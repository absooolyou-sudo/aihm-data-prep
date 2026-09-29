package com.aihm.dataprep.eda

/** Zero-dependency descriptive statistics helpers. */
object Stats {
    fun mean(xs: List<Double>): Double? = if (xs.isEmpty()) null else xs.sum() / xs.size

    fun variance(xs: List<Double>): Double? {
        val m = mean(xs) ?: return null
        if (xs.size < 2) return 0.0
        return xs.sumOf { (it - m) * (it - m) } / (xs.size - 1)
    }

    fun std(xs: List<Double>): Double? = variance(xs)?.let { kotlin.math.sqrt(it) }

    fun min(xs: List<Double>): Double? = xs.minOrNull()
    fun max(xs: List<Double>): Double? = xs.maxOrNull()

    /** Linear-interpolation quantile (same convention as numpy default). */
    fun quantile(xs: List<Double>, q: Double): Double? {
        if (xs.isEmpty()) return null
        val s = xs.sorted()
        val pos = q * (s.size - 1)
        val lo = pos.toInt()
        val hi = (lo + 1).coerceAtMost(s.size - 1)
        val frac = pos - lo
        return s[lo] + (s[hi] - s[lo]) * frac
    }

    fun median(xs: List<Double>): Double? = quantile(xs, 0.5)

    data class NumericSummary(
        val count: Int, val missing: Int,
        val mean: Double?, val std: Double?,
        val min: Double?, val q1: Double?, val median: Double?, val q3: Double?, val max: Double?,
        val iqrOutliers: List<Pair<Int, Double>>  // (index in original list, value)
    )

    fun summarize(values: List<Double?>): NumericSummary {
        val present = values.withIndex().filter { it.value != null }.map { iv -> iv.index to iv.value!! }
        val xs = present.map { it.second }
        val q1 = quantile(xs, 0.25)
        val q3 = quantile(xs, 0.75)
        val outliers = mutableListOf<Pair<Int, Double>>()
        if (q1 != null && q3 != null) {
            val iqr = q3 - q1
            val loFence = q1 - 1.5 * iqr
            val hiFence = q3 + 1.5 * iqr
            for ((idx, v) in present) if (v < loFence || v > hiFence) outliers.add(idx to v)
        }
        return NumericSummary(
            count = xs.size, missing = values.size - xs.size,
            mean = mean(xs), std = std(xs), min = min(xs),
            q1 = q1, median = median(xs), q3 = q3, max = max(xs),
            iqrOutliers = outliers
        )
    }

    fun fmt(d: Double?): String = if (d == null) "—" else "%.4f".format(d)
}

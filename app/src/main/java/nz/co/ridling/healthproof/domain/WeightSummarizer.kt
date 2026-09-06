package nz.co.ridling.healthproof.domain

/**
 * Pure, testable transformation from a list of weight samples (any order, any number of
 * sources) into the summary the diagnostic screen displays. Callers are expected to have
 * already filtered the list down to a single preferred source if the user picked one.
 */
fun summarizeWeightSamples(samples: List<WeightSample>): WeightSummary {
    val sorted = samples.sortedByDescending { it.time }
    val latest = sorted.getOrNull(0)
    val previous = sorted.getOrNull(1)
    val change = if (latest != null && previous != null) latest.kilograms - previous.kilograms else null

    return WeightSummary(
        latest = latest,
        previous = previous,
        changeKilograms = change,
        recordsInWindow = sorted.size,
    )
}

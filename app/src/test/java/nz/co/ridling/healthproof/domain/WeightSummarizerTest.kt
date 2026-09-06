package nz.co.ridling.healthproof.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class WeightSummarizerTest {

    private fun sample(kg: Double, secondsAgo: Long, pkg: String = "com.garmin.android.apps.connectmobile") =
        WeightSample(
            kilograms = kg,
            time = Instant.now().minusSeconds(secondsAgo),
            sourcePackageName = pkg,
            sourceAppLabel = "Garmin Connect",
        )

    @Test
    fun `empty list produces empty summary`() {
        val result = summarizeWeightSamples(emptyList())
        assertNull(result.latest)
        assertNull(result.previous)
        assertNull(result.changeKilograms)
        assertEquals(0, result.recordsInWindow)
    }

    @Test
    fun `single sample has no previous or change`() {
        val result = summarizeWeightSamples(listOf(sample(82.5, secondsAgo = 60)))
        assertEquals(82.5, result.latest?.kilograms)
        assertNull(result.previous)
        assertNull(result.changeKilograms)
        assertEquals(1, result.recordsInWindow)
    }

    @Test
    fun `latest is the most recent sample regardless of input order`() {
        val oldest = sample(83.0, secondsAgo = 3000)
        val middle = sample(82.0, secondsAgo = 2000)
        val newest = sample(81.0, secondsAgo = 100)

        val result = summarizeWeightSamples(listOf(middle, oldest, newest))

        assertEquals(81.0, result.latest?.kilograms)
        assertEquals(82.0, result.previous?.kilograms)
        assertEquals(3, result.recordsInWindow)
    }

    @Test
    fun `change is latest minus previous and can be negative`() {
        val result = summarizeWeightSamples(
            listOf(sample(80.0, secondsAgo = 1000), sample(82.0, secondsAgo = 10)),
        )
        assertEquals(2.0, result.changeKilograms!!, 0.0001)
    }
}

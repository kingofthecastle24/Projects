package nz.co.ridling.healthproof.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {

    @Test
    fun `formatKg rounds to one decimal`() {
        assertEquals("82.5 kg", 82.54.formatKg())
    }

    @Test
    fun `formatKgChange adds explicit sign for positive values`() {
        assertEquals("+1.5 kg", 1.5.formatKgChange())
    }

    @Test
    fun `formatKgChange adds minus sign for negative values`() {
        assertEquals("-1.5 kg", (-1.5).formatKgChange())
    }

    @Test
    fun `formatKgChange treats zero as positive`() {
        assertEquals("+0.0 kg", 0.0.formatKgChange())
    }

    @Test
    fun `formatKm converts meters to kilometres`() {
        assertEquals("5.00 km", 5000.0.formatKm())
    }

    @Test
    fun `formatKcal rounds to whole number`() {
        assertEquals("450 kcal", 450.4.formatKcal())
    }

    @Test
    fun `formatSpeedAsPaceMinPerKm converts metres per second to pace`() {
        // 1000m in 300s = 3.333 m/s -> 5:00 per km
        assertEquals("5:00 /km", (1000.0 / 300.0).formatSpeedAsPaceMinPerKm())
    }

    @Test
    fun `formatSpeedAsPaceMinPerKm handles zero speed`() {
        assertEquals("--", 0.0.formatSpeedAsPaceMinPerKm())
    }

    @Test
    fun `formatMeters rounds to whole number`() {
        assertEquals("120 m", 120.4.formatMeters())
    }
}

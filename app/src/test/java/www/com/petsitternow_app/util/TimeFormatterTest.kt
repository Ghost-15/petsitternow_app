package www.com.petsitternow_app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for TimeFormatter.
 * Verifies format matches web implementation: Xh Xm Xs, Xm Xs, or Xs.
 */
class TimeFormatterTest {

    @Test
    fun `formatDurationSeconds returns seconds only for less than minute`() {
        assertEquals("0s", TimeFormatter.formatDurationSeconds(0))
        assertEquals("1s", TimeFormatter.formatDurationSeconds(1))
        assertEquals("30s", TimeFormatter.formatDurationSeconds(30))
        assertEquals("59s", TimeFormatter.formatDurationSeconds(59))
    }

    @Test
    fun `formatDurationSeconds returns minutes and seconds for less than hour`() {
        assertEquals("1m 0s", TimeFormatter.formatDurationSeconds(60))
        assertEquals("1m 30s", TimeFormatter.formatDurationSeconds(90))
        assertEquals("5m 0s", TimeFormatter.formatDurationSeconds(300))
        assertEquals("59m 59s", TimeFormatter.formatDurationSeconds(3599))
    }

    @Test
    fun `formatDurationSeconds returns hours minutes and seconds`() {
        assertEquals("1h 0m 0s", TimeFormatter.formatDurationSeconds(3600))
        assertEquals("1h 0m 1s", TimeFormatter.formatDurationSeconds(3601))
        assertEquals("1h 30m 45s", TimeFormatter.formatDurationSeconds(5445))
        assertEquals("2h 15m 30s", TimeFormatter.formatDurationSeconds(8130))
    }

    @Test
    fun `formatDurationSeconds returns empty string for negative values`() {
        assertEquals("", TimeFormatter.formatDurationSeconds(-1))
        assertEquals("", TimeFormatter.formatDurationSeconds(-100))
    }

    @Test
    fun `formatDurationMs converts milliseconds correctly`() {
        assertEquals("5s", TimeFormatter.formatDurationMs(5000))
        assertEquals("1m 30s", TimeFormatter.formatDurationMs(90000))
        assertEquals("1h 0m 0s", TimeFormatter.formatDurationMs(3600000))
    }

    @Test
    fun `formatDurationMs returns empty for negative values`() {
        assertEquals("", TimeFormatter.formatDurationMs(-1))
    }

    @Test
    fun `formatElapsedTime returns empty for null timestamp`() {
        assertEquals("", TimeFormatter.formatElapsedTime(null))
    }

    @Test
    fun `formatElapsedTime returns empty for zero timestamp`() {
        assertEquals("", TimeFormatter.formatElapsedTime(0L))
    }

    @Test
    fun `formatDurationOption formats 30 minutes correctly`() {
        assertEquals("30 min", TimeFormatter.formatDurationOption(30))
    }

    @Test
    fun `formatDurationOption formats 45 minutes correctly`() {
        assertEquals("45 min", TimeFormatter.formatDurationOption(45))
    }

    @Test
    fun `formatDurationOption formats 60 minutes as 1h`() {
        assertEquals("1h", TimeFormatter.formatDurationOption(60))
    }

    @Test
    fun `formatDurationValue handles string values`() {
        assertEquals("30 min", TimeFormatter.formatDurationValue("30"))
        assertEquals("45 min", TimeFormatter.formatDurationValue("45"))
        assertEquals("1h", TimeFormatter.formatDurationValue("60"))
    }

    @Test
    fun `formatDurationValue defaults to 30 min for null`() {
        assertEquals("30 min", TimeFormatter.formatDurationValue(null))
    }

    @Test
    fun `formatDurationValue defaults to 30 min for invalid value`() {
        assertEquals("30 min", TimeFormatter.formatDurationValue("invalid"))
    }
}

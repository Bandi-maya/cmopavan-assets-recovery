package com.logybyte.assetsrecovery

import com.logybyte.assetsrecovery.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun testFormatFileSize_zeroAndNegative() {
        assertEquals("0 B", FormatUtils.formatFileSize(0))
        assertEquals("0 B", FormatUtils.formatFileSize(-100))
    }

    @Test
    fun testFormatFileSize_bytes() {
        assertEquals("512 B", FormatUtils.formatFileSize(512))
    }

    @Test
    fun testFormatFileSize_kilobytes() {
        val result = FormatUtils.formatFileSize(2048)
        assertTrue(result.contains("KB"))
    }

    @Test
    fun testFormatFileSize_megabytes() {
        val result = FormatUtils.formatFileSize(5 * 1024 * 1024L)
        assertTrue(result.contains("MB"))
    }

    @Test
    fun testFormatFileSize_gigabytes() {
        val result = FormatUtils.formatFileSize(3L * 1024 * 1024 * 1024)
        assertTrue(result.contains("GB"))
    }

    @Test
    fun testFormatDuration_zeroAndNegative() {
        assertEquals("00:00", FormatUtils.formatDuration(0))
        assertEquals("00:00", FormatUtils.formatDuration(-5000))
    }

    @Test
    fun testFormatDuration_secondsAndMinutes() {
        assertEquals("01:05", FormatUtils.formatDuration(65000))
        assertEquals("10:30", FormatUtils.formatDuration((10 * 60 + 30) * 1000L))
    }

    @Test
    fun testFormatDuration_hours() {
        assertEquals("1:15:30", FormatUtils.formatDuration((3600 + 15 * 60 + 30) * 1000L))
    }

    @Test
    fun testFormatDate_validTimestamp() {
        val formatted = FormatUtils.formatDate(1729440000L) // in seconds
        assertTrue(formatted.isNotEmpty())
        assertTrue(formatted != "Unknown")
    }

    @Test
    fun testFormatDate_zeroOrNegative() {
        assertEquals("Unknown", FormatUtils.formatDate(0))
        assertEquals("Unknown", FormatUtils.formatDate(-1))
    }
}

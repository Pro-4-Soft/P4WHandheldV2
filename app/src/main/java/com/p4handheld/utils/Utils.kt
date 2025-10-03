package com.p4handheld.utils

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun formatDateTime(isoString: String): String {
    // Expected format like 2025-09-21T18:29:20.120349-06:00
    return try {
        val odt = OffsetDateTime.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
        odt.format(formatter)
    } catch (e: DateTimeParseException) {
        ""
    } catch (e: Exception) {
        ""
    }
}

fun formatChatTimestamp(isoString: String): String {
    // Expected format like 2025-09-21T18:29:20.120349-06:00
    return try {
        val odt = OffsetDateTime.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("h:mm a")
        odt.format(formatter)
    } catch (e: DateTimeParseException) {
        ""
    } catch (e: Exception) {
        ""
    }
}
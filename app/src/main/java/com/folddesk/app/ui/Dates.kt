package com.folddesk.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil

val RU: Locale = Locale.forLanguageTag("ru")

private val dayMonth = DateTimeFormatter.ofPattern("d MMMM", RU)
private val dayMonthShort = DateTimeFormatter.ofPattern("d MMM", RU)
private val weekday = DateTimeFormatter.ofPattern("EEEE", RU)
private val fullDate = DateTimeFormatter.ofPattern("EEEE, d MMMM", RU)
private val dateTime = DateTimeFormatter.ofPattern("d MMMM, HH:mm", RU)

fun todayEpoch(): Long = LocalDate.now().toEpochDay()

fun isToday(millis: Long?): Boolean =
    millis != null && Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()

fun String.capitalized(): String = replaceFirstChar { it.titlecase(RU) }

fun dueLabel(epochDay: Long): String {
    val diff = epochDay - todayEpoch()
    val date = LocalDate.ofEpochDay(epochDay)
    return when {
        diff == 0L -> "Сегодня"
        diff == 1L -> "Завтра"
        diff == -1L -> "Вчера"
        diff in 2..6 -> date.format(weekday).capitalized()
        date.year == LocalDate.now().year -> date.format(dayMonth)
        else -> date.format(dayMonthShort) + " " + date.year
    }
}

fun todayTitle(): String = LocalDate.now().format(fullDate).capitalized()

fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Доброе утро"
    in 12..17 -> "Добрый день"
    in 18..22 -> "Добрый вечер"
    else -> "Доброй ночи"
}

fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateTime)

fun relativeTime(millis: Long): String {
    val minutes = (System.currentTimeMillis() - millis) / 60_000
    return when {
        minutes < 1 -> "только что"
        minutes < 60 -> "$minutes мин назад"
        minutes < 24 * 60 -> "${minutes / 60} ч назад"
        else -> formatDateTime(millis)
    }
}

fun formatTimer(ms: Long): String {
    val totalSeconds = ceil(ms / 1000.0).toLong().coerceAtLeast(0)
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

fun weekdayShort(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, RU).lowercase(RU)

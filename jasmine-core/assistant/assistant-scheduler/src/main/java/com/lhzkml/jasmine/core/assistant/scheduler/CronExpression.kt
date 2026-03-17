package com.lhzkml.jasmine.core.assistant.scheduler

import java.util.Calendar
import java.util.TimeZone

/**
 * Cron 表达式解析与计算核心（移植自 Kai）
 * 支持 5 段式：minute hour day-of-month month day-of-week
 */
class CronExpression(expression: String) {

    private val minutes: Set<Int>
    private val hours: Set<Int>
    private val daysOfMonth: Set<Int>
    private val months: Set<Int>
    private val daysOfWeek: Set<Int>

    init {
        val parts = expression.trim().split(Regex("\\s+"))
        require(parts.size == 5) { "Cron expression must have 5 fields" }
        minutes = parseField(parts[0], 0, 59)
        hours = parseField(parts[1], 0, 23)
        daysOfMonth = parseField(parts[2], 1, 31)
        months = parseField(parts[3], 1, 12)
        daysOfWeek = parseField(parts[4], 0, 6)
    }

    /**
     * 计算 nextAfter 的逻辑（本地 Java Calendar 版本适配）
     */
    fun nextAfter(afterMs: Long): Long? {
        val cal = Calendar.getInstance()
        cal.timeInMillis = afterMs
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MINUTE, 1)

        val maxIterations = 525960 // ~2 years
        var iterations = 0

        while (iterations < maxIterations) {
            iterations++

            if (cal.get(Calendar.MONTH) + 1 !in months) {
                cal.add(Calendar.MONTH, 1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                continue
            }

            if (cal.get(Calendar.DAY_OF_MONTH) !in daysOfMonth || toCronDayOfWeek(cal) !in daysOfWeek) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                continue
            }

            if (cal.get(Calendar.HOUR_OF_DAY) !in hours) {
                cal.add(Calendar.HOUR_OF_DAY, 1)
                cal.set(Calendar.MINUTE, 0)
                continue
            }

            if (cal.get(Calendar.MINUTE) !in minutes) {
                cal.add(Calendar.MINUTE, 1)
                continue
            }

            return cal.timeInMillis
        }
        return null
    }

    private fun toCronDayOfWeek(cal: Calendar): Int {
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> 0
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 0
        }
    }

    companion object {
        private fun parseField(field: String, min: Int, max: Int): Set<Int> {
            val result = mutableSetOf<Int>()
            for (part in field.split(",")) {
                when {
                    part == "*" -> result.addAll(min..max)
                    part.startsWith("*/") -> {
                        val step = part.substringAfter("*/").toInt()
                        var i = min
                        while (i <= max) {
                            result.add(i)
                            i += step
                        }
                    }
                    part.contains("-") -> {
                        val (start, end) = part.split("-").map { it.toInt() }
                        result.addAll(start..end)
                    }
                    else -> {
                        val value = part.toInt()
                        if (value in min..max) result.add(value)
                    }
                }
            }
            return result
        }
    }
}

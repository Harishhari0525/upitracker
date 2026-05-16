package com.example.upitracker.util

import com.example.upitracker.data.BudgetPeriod
import java.util.Calendar

object DateUtils {

    // Helper to get the start/end timestamps for a specific BudgetPeriod
    fun getRangeForPeriod(period: BudgetPeriod): Pair<Long, Long> {
        return when (period) {
            BudgetPeriod.WEEKLY -> getCurrentWeekRange()
            BudgetPeriod.MONTHLY -> getCurrentMonthRange()
            BudgetPeriod.YEARLY -> getCurrentYearRange()
        }
    }

    // Returns (Start of Monday, End of Sunday)
    fun getCurrentWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        setStartOfDay(calendar)
        val start = calendar.timeInMillis

        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis
        return Pair(start, end)
    }

    // Returns (Start of 1st, End of Last Day)
    fun getCurrentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        setStartOfDay(calendar)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis
        return Pair(start, end)
    }

    // Returns (Start of Jan 1st, End of Dec 31st)
    fun getCurrentYearRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        setStartOfDay(calendar)
        val start = calendar.timeInMillis

        calendar.add(Calendar.YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis
        return Pair(start, end)
    }

    // Helper to reset time to 00:00:00.000
    private fun setStartOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }
}
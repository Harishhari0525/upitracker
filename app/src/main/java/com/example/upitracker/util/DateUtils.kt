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

    // Helper to get the previous period's start/end timestamps
    fun getPreviousRangeForPeriod(period: BudgetPeriod): Pair<Long, Long> {
        return when (period) {
            BudgetPeriod.WEEKLY -> getPreviousWeekRange()
            BudgetPeriod.MONTHLY -> getPreviousMonthRange()
            BudgetPeriod.YEARLY -> getPreviousYearRange()
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

    // Returns previous week's range
    fun getPreviousWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        setStartOfDay(calendar)
        val start = calendar.timeInMillis

        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis
        return Pair(start, end)
    }

    // Returns previous month's range
    fun getPreviousMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        setStartOfDay(calendar)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis
        return Pair(start, end)
    }

    // Returns previous year's range
    fun getPreviousYearRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -1)
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        setStartOfDay(calendar)
        val start = calendar.timeInMillis

        calendar.add(Calendar.YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis
        return Pair(start, end)
    }

    // Returns daily trend date range (daysToShow before today to end of today)
    fun getDailyTrendDateRange(daysToShow: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.DAY_OF_YEAR, -(daysToShow - 1))
        setStartOfDay(calendar)
        val start = calendar.timeInMillis
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
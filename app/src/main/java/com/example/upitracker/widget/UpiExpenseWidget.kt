package com.example.upitracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.unit.ColorProvider
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.upitracker.MainActivity
import com.example.upitracker.data.AppDatabase
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class UpiExpenseWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // ✨ 1. Fetch State & Data OUTSIDE provideContent (This runs in a coroutine)
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val viewMode = prefs[widgetViewModeKey] ?: "MONTH"

        val (startTime, endTime) = if (viewMode == "TODAY") getTodayRange() else getCurrentMonthRange()
        val db = AppDatabase.getDatabase(context)

        // ✨ 2. Now you can safely call your DAO (Blocking or Suspend both work here)
        val spentAmount = db.transactionDao().getSpentAmountInRangeSync(startTime, endTime, "Refund") ?: 0.0

        provideContent {
            GlanceTheme {
                // ✨ 3. Pass the pre-loaded data to the UI
                WidgetContent(spentAmount = spentAmount, viewMode = viewMode)
            }
        }
    }

    @Composable
    private fun WidgetContent(spentAmount: Double, viewMode: String) {
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Header: Toggle Tabs ---
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.surfaceVariant)
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TabItem(
                    text = "Today",
                    isSelected = viewMode == "TODAY",
                    onClick = actionRunCallback<ToggleViewModeAction>(
                        actionParametersOf(ActionParameters.Key<String>("mode") to "TODAY")
                    )
                )
                Spacer(GlanceModifier.width(8.dp))
                TabItem(
                    text = "Month",
                    isSelected = viewMode == "MONTH",
                    onClick = actionRunCallback<ToggleViewModeAction>(
                        actionParametersOf(ActionParameters.Key<String>("mode") to "MONTH")
                    )
                )
            }

            Spacer(GlanceModifier.height(12.dp))

            Text(
                text = currencyFormatter.format(spentAmount),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(GlanceModifier.height(4.dp))

            Text(
                text = if (viewMode == "TODAY") "Spent Today" else "Spent This Month",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
            )

            Spacer(GlanceModifier.defaultWeight())

            // --- Footer: Add Button ---
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.primaryContainer)
                    .padding(vertical = 8.dp)
                    .clickable(
                        // ✨ FIX: Explicit class reference to avoid ambiguity
                        actionStartActivity(
                            activity = MainActivity::class.java,
                            parameters = actionParametersOf(
                                ActionParameters.Key<Boolean>("SHOW_ADD_DIALOG") to true
                            )
                        )
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+ Add Expense",
                    style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontWeight = FontWeight.Medium)
                )
            }
        }
    }

    @Composable
    private fun TabItem(text: String, isSelected: Boolean, onClick: androidx.glance.action.Action) {
        Text(
            text = text,
            style = TextStyle(
                color = if (isSelected) GlanceTheme.colors.onSecondary else GlanceTheme.colors.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            modifier = GlanceModifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .background(
                    if (isSelected) GlanceTheme.colors.secondary
                    else ColorProvider(Color.Transparent)
                )
                .clickable(onClick)
        )
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
        val start = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59)
        val end = calendar.timeInMillis
        return start to end
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis
        return start to end
    }
}
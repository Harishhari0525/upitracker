package com.example.upitracker.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState

// Define the key for storing the view mode
val widgetViewModeKey = stringPreferencesKey("widget_view_mode")

class ToggleViewModeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Get the requested mode ("TODAY" or "MONTH")
        val newMode = parameters[ActionParameters.Key<String>("mode")] ?: "MONTH"

        // Save it to the widget's state
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[widgetViewModeKey] = newMode
        }

        // Refresh the widget
        UpiExpenseWidget().update(context, glanceId)
    }
}
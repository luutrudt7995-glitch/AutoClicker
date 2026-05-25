package com.autoclicker.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.autoclicker.app.model.AppSettings
import com.autoclicker.app.model.ClickMode
import com.autoclicker.app.model.SwipePoint
import org.json.JSONArray
import org.json.JSONObject

object PreferenceManager {

    private const val PREF_NAME = "auto_clicker_prefs"
    private const val KEY_CLICK_MODE = "click_mode"
    private const val KEY_FIXED_DELAY = "fixed_delay"
    private const val KEY_RANDOM_MIN = "random_min"
    private const val KEY_RANDOM_MAX = "random_max"
    private const val KEY_CLICK_X = "click_x"
    private const val KEY_CLICK_Y = "click_y"
    private const val KEY_SWIPE_POINTS = "swipe_points"
    private const val KEY_IS_SWIPE_MODE = "is_swipe_mode"
    private const val KEY_SHOW_INDICATOR = "show_indicator"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveSettings(context: Context, settings: AppSettings) {
        prefs(context).edit().apply {
            putString(KEY_CLICK_MODE, settings.clickMode.name)
            putLong(KEY_FIXED_DELAY, settings.fixedDelay)
            putLong(KEY_RANDOM_MIN, settings.randomDelayMin)
            putLong(KEY_RANDOM_MAX, settings.randomDelayMax)
            putFloat(KEY_CLICK_X, settings.clickX)
            putFloat(KEY_CLICK_Y, settings.clickY)
            putString(KEY_SWIPE_POINTS, serializeSwipePoints(settings.swipePoints))
            putBoolean(KEY_IS_SWIPE_MODE, settings.isSwipeMode)
            putBoolean(KEY_SHOW_INDICATOR, settings.showTouchIndicator)
            apply()
        }
    }

    fun loadSettings(context: Context): AppSettings {
        val p = prefs(context)
        val modeName = p.getString(KEY_CLICK_MODE, ClickMode.FIXED.name) ?: ClickMode.FIXED.name
        return AppSettings(
            clickMode = try { ClickMode.valueOf(modeName) } catch (e: Exception) { ClickMode.FIXED },
            fixedDelay = p.getLong(KEY_FIXED_DELAY, 1000L),
            randomDelayMin = p.getLong(KEY_RANDOM_MIN, 100L),
            randomDelayMax = p.getLong(KEY_RANDOM_MAX, 1000L),
            clickX = p.getFloat(KEY_CLICK_X, 500f),
            clickY = p.getFloat(KEY_CLICK_Y, 1000f),
            swipePoints = deserializeSwipePoints(p.getString(KEY_SWIPE_POINTS, "[]") ?: "[]"),
            isSwipeMode = p.getBoolean(KEY_IS_SWIPE_MODE, false),
            showTouchIndicator = p.getBoolean(KEY_SHOW_INDICATOR, true)
        )
    }

    private fun serializeSwipePoints(points: List<SwipePoint>): String {
        val arr = JSONArray()
        points.forEach { pt ->
            val obj = JSONObject()
            obj.put("id", pt.id)
            obj.put("startX", pt.startX)
            obj.put("startY", pt.startY)
            obj.put("endX", pt.endX)
            obj.put("endY", pt.endY)
            obj.put("duration", pt.duration)
            obj.put("delayBefore", pt.delayBefore)
            obj.put("label", pt.label)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeSwipePoints(json: String): List<SwipePoint> {
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<SwipePoint>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SwipePoint(
                        id = obj.getLong("id"),
                        startX = obj.getDouble("startX").toFloat(),
                        startY = obj.getDouble("startY").toFloat(),
                        endX = obj.getDouble("endX").toFloat(),
                        endY = obj.getDouble("endY").toFloat(),
                        duration = obj.getLong("duration"),
                        delayBefore = obj.getLong("delayBefore"),
                        label = obj.optString("label", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}

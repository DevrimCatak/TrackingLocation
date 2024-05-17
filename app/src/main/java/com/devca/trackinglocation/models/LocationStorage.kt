package com.devca.trackinglocation.models

import android.content.Context

/**
 * Created by @devrimcatak on 17.05.2024.
 */
object LocationStorage {
    private const val PREFERENCES_NAME = "LocationPreferences"
    private const val KEY_LOCATION_LIST = "LocationList"
    private const val KEY_ROUTE_TRACKING_ENABLED = "route_tracking_enabled"


    fun saveLocation(context: Context, latitude: Double, longitude: Double) {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val locationList = getLocationList(context)
        locationList.add("$latitude,$longitude")
        sharedPreferences.edit().putStringSet(KEY_LOCATION_LIST, locationList.toSet()).apply()
    }

    fun getLocationList(context: Context): MutableList<String> {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet(KEY_LOCATION_LIST, mutableSetOf())?.toMutableList() ?: mutableListOf()
    }

    fun clearLocationList(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().remove(KEY_LOCATION_LIST).apply()
    }

    fun isRouteTrackingEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ROUTE_TRACKING_ENABLED, false)
    }

    fun setRouteTrackingEnabled(context: Context, isEnabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ROUTE_TRACKING_ENABLED, isEnabled).apply()
    }
}
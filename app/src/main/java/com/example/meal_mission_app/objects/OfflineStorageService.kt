package com.example.meal_mission_app.objects

import android.content.Context
import android.content.SharedPreferences

object OfflineStorageService {

    private const val PREFERENCES_NAME = "meals_mission_prefs"
    private const val TOKEN_KEY = "token"
    private const val REFRESH_TOKEN_KEY = "refresh_token"
    private const val USER_ID_KEY = "user_id"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    fun saveCredentials(context: Context, token: String?, refreshToken: String?, userId: String?) {
        val editor = getPreferences(context).edit()
        editor.putString(TOKEN_KEY, token)
        editor.putString(REFRESH_TOKEN_KEY, refreshToken)
        editor.putString(USER_ID_KEY, userId)
        editor.apply()
    }

    fun getToken(context: Context): String? {
        return getPreferences(context).getString(TOKEN_KEY, null)
    }

    fun getRefreshToken(context: Context): String? {
        return getPreferences(context).getString(REFRESH_TOKEN_KEY, null)
    }

    fun getUserId(context: Context): String? {
        return getPreferences(context).getString(USER_ID_KEY, null)
    }

    fun clearCredentials(context: Context) {
        val editor = getPreferences(context).edit()
        editor.clear()
        editor.apply()
    }
}

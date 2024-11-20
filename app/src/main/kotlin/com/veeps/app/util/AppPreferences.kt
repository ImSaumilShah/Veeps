package com.veeps.app.util

import android.content.Context
import android.content.SharedPreferences
import com.veeps.app.application.Veeps

object AppPreferences {
	private val sharedPreference: SharedPreferences by lazy {
		Veeps.appContext.getSharedPreferences(AppConstants.PREFS_FILENAME, Context.MODE_PRIVATE)
	}

	fun set(key: String?, value: String?) {
		sharedPreference.edit().putString(key, value).apply()
	}

	fun set(key: String?, value: Int) {
		sharedPreference.edit().putInt(key, value).apply()
	}

	fun set(key: String?, value: Boolean) {
		sharedPreference.edit().putBoolean(key, value).apply()
	}

	fun get(key: String?, defaultValue: String?): String? {
		return sharedPreference.getString(key, defaultValue)
	}

	fun get(key: String?, defaultValue: Int): Int {
		return sharedPreference.getInt(key, defaultValue)
	}

	fun get(key: String?, defaultValue: Boolean): Boolean {
		return sharedPreference.getBoolean(key, defaultValue)
	}

	fun remove(key: String?) {
		sharedPreference.edit().remove(key).apply()
	}

	fun removeAuthenticatedUser() {
		remove(AppConstants.isUserAuthenticated)
		remove(AppConstants.userID)
		remove(AppConstants.userEmailAddress)
		remove(AppConstants.userFullName)
		remove(AppConstants.userDisplayName)
		remove(AppConstants.userAvatar)
		remove(AppConstants.userSubscriptionStatus)
		remove(AppConstants.userCurrency)
		remove(AppConstants.userTimeZone)
		remove(AppConstants.userTimeZoneAbbr)
		remove(AppConstants.userBeaconBaseURL)
		remove(AppConstants.authenticatedUserToken)
		remove(AppConstants.generatedJWT)
		remove(AppConstants.reservedId)
		remove(AppConstants.orderId)
		remove(AppConstants.requestId)
		remove(AppConstants.SKUId)
		remove(AppConstants.receiptId)
		Logger.print("Authenticated User Details Are Removed.")
	}
}
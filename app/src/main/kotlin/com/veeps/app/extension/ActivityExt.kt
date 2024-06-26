package com.veeps.app.extension

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.widget.Button
import android.widget.Toast
import com.veeps.app.R
import java.io.Serializable

fun Activity.showToast(message: CharSequence) =
	Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

inline fun <reified T : Activity> Activity.goToScreen(
	requiredSingleTop: Boolean, vararg params: Pair<String, Any>,
) {
	val intent = Intent(this, T::class.java)
	intent.putExtras(*params)
	if (requiredSingleTop) {
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
	}
	this.startActivity(intent)
	if (Build.VERSION.SDK_INT >= 34) {
		overrideActivityTransition(
			Activity.OVERRIDE_TRANSITION_OPEN,
			R.anim.fade_in,
			R.anim.fade_out
		)
	} else {
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
	}
}

fun Intent.putExtras(vararg params: Pair<String, Any>): Intent {
	if (params.isEmpty()) return this
	params.forEach { (key, value) ->
		when (value) {
			is Int -> putExtra(key, value)
			is Byte -> putExtra(key, value)
			is Char -> putExtra(key, value)
			is Long -> putExtra(key, value)
			is Float -> putExtra(key, value)
			is Short -> putExtra(key, value)
			is Double -> putExtra(key, value)
			is Boolean -> putExtra(key, value)
			is Bundle -> putExtra(key, value)
			is String -> putExtra(key, value)
			is IntArray -> putExtra(key, value)
			is ByteArray -> putExtra(key, value)
			is CharArray -> putExtra(key, value)
			is LongArray -> putExtra(key, value)
			is FloatArray -> putExtra(key, value)
			is Parcelable -> putExtra(key, value)
			is ShortArray -> putExtra(key, value)
			is DoubleArray -> putExtra(key, value)
			is BooleanArray -> putExtra(key, value)
			is CharSequence -> putExtra(key, value)
			is Array<*> -> {
				when {
					value.isArrayOf<String>() -> putExtra(key, value as Array<String?>)
					value.isArrayOf<Parcelable>() -> putExtra(key, value as Array<Parcelable?>)
					value.isArrayOf<CharSequence>() -> putExtra(key, value as Array<CharSequence?>)
					else -> putExtra(key, value)
				}
			}

			is Serializable -> putExtra(key, value)
		}
	}
	return this
}

fun Button.makeDisabled() {/*background = ContextCompat.getDrawable(context, R.drawable.disabled_button)*/
	isEnabled = false
}
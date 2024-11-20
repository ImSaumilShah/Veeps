package com.veeps.app.util

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import kotlin.math.min

object Logger {

	fun doNothing() {}

	fun print(message: Any) {
		Log.wtf(AppConstants.TAG, message.toString())
	}

	fun printWithTag(tag: String, message: Any) {
		Log.wtf(tag, message.toString())
	}

	fun printMessage(message: Any) {
		Log.wtf("AppConstants.TAG", message.toString())
	}

	fun printError(message: Any) {
		Log.e(AppConstants.TAG, message.toString())
	}

	fun printAPILogs(
		chain: Interceptor.Chain, response: Response, bodyString: String, tookMs: Long, tookS: Long,
	) {
		printWithTag(
			"VeepsAPI",
			"Request URL :: ${chain.request().method} ${chain.request().url} \n\n"
		)
		printWithTag("VeepsAPI", "Request Time for ${chain.request().url.encodedPath} :: $tookS Seconds ( $tookMs ms ) \n\n")
		for (i in 0 until chain.request().headers.size) {
			printWithTag(
				"VeepsAPI", "Request Headers for ${chain.request().url.encodedPath} :: ${chain.request().headers.name(i)} : ${
					chain.request().headers.value(
						i
					)
				} \n\n"
			)
		}
		try {
			val buffer = Buffer()
			val body = chain.request().body
			body?.writeTo(buffer)
			printWithTag(
				"VeepsAPI",
				"Request Body for ${chain.request().url.encodedPath} :: ${if (buffer.size != 0L) buffer.readUtf8() else "null"} \n\n"
			)
		} catch (e: IOException) {
			e.printStackTrace()
			printWithTag(
				"VeepsAPI",
				"EXCEPTION Occurred while printing API Logs for ${chain.request().url.encodedPath} -- " + e.message + " \n\n"
			)
		}
		printWithTag("VeepsAPI", "Response Code for ${chain.request().url.encodedPath} :: ${response.code} \n\n")
		val maxLogSize = 4000
		for (i in 0..bodyString.length / maxLogSize) {
			val start = i * maxLogSize
			var end = (i + 1) * maxLogSize
			end = min(end, bodyString.length)
			printWithTag(
				"VeepsAPI",
				(if (i == 0) "Response for ${chain.request().url.encodedPath} :: " else DEFAULT.EMPTY_STRING) + bodyString.substring(
					start,
					end
				)
			)
		}
	}
}
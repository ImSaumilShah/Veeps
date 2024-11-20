package com.veeps.app.application

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.veeps.app.BuildConfig
import com.veeps.app.BuildConfig.isProduction
import com.veeps.app.util.CrashHandler
import com.veeps.app.util.Logger
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid

class Veeps : Application() {

	companion object {
		lateinit var appContext: Context
	}

	override fun onCreate() {
		super.onCreate()
		appContext = applicationContext
		if (isProduction && !BuildConfig.DEBUG) {
			SentryAndroid.init(appContext) { options ->
				options.isReportHistoricalAnrs = true
				options.isAttachAnrThreadDump = true
				options.tracesSampleRate = 1.0
				options.tracesSampler = SentryOptions.TracesSamplerCallback {
					null
				}
			}
		}
		Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
		ViewPump.init(
			ViewPump.builder().addInterceptor(
				CalligraphyInterceptor(
					CalligraphyConfig.Builder().setDefaultFontPath("fonts/SaansRegular.ttf").build()
				)
			).build()
		)

		Logger.print("Application Created")
	}

	override fun onTerminate() {
		Logger.print("Application Terminated")
		super.onTerminate()
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		Logger.print("Configuration Changed to $newConfig.orientation")
	}

	override fun onLowMemory() {
		super.onLowMemory()
		Logger.print("Device is on low memory")
	}

	override fun onTrimMemory(level: Int) {
		super.onTrimMemory(level)
		Logger.print("Device is on trim memory with level $level")
	}
}
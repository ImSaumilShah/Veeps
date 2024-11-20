package com.veeps.app.feature.routing.ui

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.veeps.app.databinding.ActivityRoutingScreenBinding
import com.veeps.app.extension.goToScreen
import com.veeps.app.feature.home.ui.HomeScreen
import com.veeps.app.feature.intro.ui.IntroScreen
import com.veeps.app.util.AppConstants
import com.veeps.app.util.AppPreferences
import com.veeps.app.util.Screens
import io.github.inflationx.viewpump.ViewPumpContextWrapper


class RoutingScreen : Activity() {

	lateinit var binding: ActivityRoutingScreenBinding
	private lateinit var handler: Handler
	private lateinit var runnable: Runnable

	override fun attachBaseContext(newBase: Context?) {
		super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase!!))
	}

	override fun onCreate(savedInstanceState: Bundle?) {
//		installSplashScreen()
		super.onCreate(savedInstanceState)
		binding = ActivityRoutingScreenBinding.inflate(layoutInflater)
		setContentView(binding.root)
		if (AppPreferences.get(
				AppConstants.isUserAuthenticated, false
			) && !AppPreferences.get(
				AppConstants.authenticatedUserToken, ""
			).isNullOrEmpty()
		) {
			goToScreen()
		} else {

			binding.logo.addValueCallback(
				KeyPath("**"), LottieProperty.COLOR_FILTER
			) { _ ->
				PorterDuffColorFilter(
					Color.WHITE, PorterDuff.Mode.SRC_ATOP
				)
			}
			binding.logo.repeatCount = 0
			binding.logo.addLottieOnCompositionLoadedListener {
				if (this::handler.isInitialized && this::runnable.isInitialized) handler.postDelayed(
					runnable, 2000
				)
			}

			binding.logo.setFailureListener {
				goToScreen()
			}
			binding.logo.addAnimatorListener(object : AnimatorListener {
				override fun onAnimationStart(animator: Animator) {}

				override fun onAnimationEnd(animator: Animator) {
					goToScreen()
				}

				override fun onAnimationCancel(animator: Animator) {}

				override fun onAnimationRepeat(animator: Animator) {}

			})
			handler = Handler(Looper.getMainLooper())
			runnable = Runnable {

			}
			binding.logo.postDelayed({ binding.logo.playAnimation() }, 100)
		}
	}

	private fun goToScreen() {
		if (AppPreferences.get(
				AppConstants.isUserAuthenticated, false
			) && !AppPreferences.get(
				AppConstants.authenticatedUserToken, ""
			).isNullOrEmpty()
		) {
			goToScreen<HomeScreen>(true, AppConstants.TAG to Screens.BROWSE)
		} else {
			goToScreen<IntroScreen>(
				true, AppConstants.TAG to Screens.INTRO
			)
		}
	}

	/*private fun setupSplashScreenConfiguration() {
		*//* Comment: This keeps splash open till manually moved to next activity
		splashScreen.setKeepOnScreenCondition { true } *//*

		val content: View = findViewById(android.R.id.content)
		content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
			override fun onPreDraw(): Boolean {
				content.viewTreeObserver.removeOnPreDrawListener(this)
				return true
			}
		})
	}*/

}

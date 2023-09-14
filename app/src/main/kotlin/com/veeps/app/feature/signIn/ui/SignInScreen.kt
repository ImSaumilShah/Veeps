package com.veeps.app.feature.signIn.ui

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import com.veeps.app.R
import com.veeps.app.core.BaseActivity
import com.veeps.app.core.BaseDataSource.Resource.CallStatus.*
import com.veeps.app.databinding.ActivitySignInScreenBinding
import com.veeps.app.extension.convertToMilli
import com.veeps.app.extension.isGreaterThan
import com.veeps.app.extension.loadImage
import com.veeps.app.feature.signIn.viewModel.SignInViewModel
import com.veeps.app.util.APIConstants
import com.veeps.app.util.AppConstants
import com.veeps.app.util.AppPreferences
import com.veeps.app.util.ImageTags
import com.veeps.app.util.IntValue
import com.veeps.app.util.Logger
import com.veeps.app.util.PollingStatus
import kotlin.math.max


class SignInScreen : BaseActivity<SignInViewModel, ActivitySignInScreenBinding>() {

	private lateinit var polling: Handler
	private lateinit var pollTask: Runnable
	private lateinit var tokenExpiry: Handler
	private lateinit var expiryTask: Runnable

	override fun isSplashScreenRequired(): Boolean {
		return false
	}

	override fun getBackCallback(): OnBackPressedCallback {
		val backPressedCallback = object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				Logger.print(
					"Back Pressed on ${
						this@SignInScreen.localClassName.substringAfterLast(
							"."
						)
					} Finishing Activity"
				)
				finish()
			}
		}
		return backPressedCallback
	}

	override fun getViewBinding(): ActivitySignInScreenBinding {
		return ActivitySignInScreenBinding.inflate(layoutInflater)
	}

	override fun showError(tag: String, message: String) {
		viewModel.contentHasLoaded.postValue(true)
		viewModel.errorMessage.postValue(message)
		when (tag) {
			else -> {
				viewModel.errorPositiveLabel.postValue(getString(R.string.ok_label))
				viewModel.errorNegativeLabel.postValue(getString(R.string.cancel_label))
				viewModel.isErrorPositiveApplicable.postValue(true)
				viewModel.isErrorNegativeApplicable.postValue(false)
			}
		}
		binding.positive.tag = tag
		binding.negative.tag = tag

		binding.positive.nextFocusUpId = binding.positive.id
		binding.positive.nextFocusDownId = binding.positive.id
		binding.positive.nextFocusLeftId = binding.positive.id
		binding.positive.nextFocusRightId =
			if (binding.negative.isEnabled) binding.negative.id else binding.positive.id
		binding.positive.nextFocusForwardId =
			if (binding.negative.isEnabled) binding.negative.id else binding.positive.id

		binding.negative.nextFocusUpId = binding.negative.id
		binding.negative.nextFocusDownId = binding.negative.id
		binding.negative.nextFocusLeftId =
			if (binding.positive.isEnabled) binding.positive.id else binding.negative.id
		binding.negative.nextFocusRightId = binding.negative.id
		binding.negative.nextFocusForwardId =
			if (binding.positive.isEnabled) binding.positive.id else binding.negative.id

		binding.errorContainer.visibility = View.VISIBLE
		binding.errorContainer.apply {
			alpha = 0f
			visibility = View.VISIBLE
			animate().alpha(1f)
				.setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
				.setListener(null)
		}
		binding.positive.postDelayed({
			binding.positive.requestFocus()
		}, AppConstants.keyPressShortDelayTime)
	}

	fun onErrorPositive(tag: Any?) {
		backPressedCallback.handleOnBackPressed()
	}

	fun onErrorNegative(tag: Any?) {
		backPressedCallback.handleOnBackPressed()
	}

	override fun onRendered(viewModel: SignInViewModel, binding: ActivitySignInScreenBinding) {
		binding.apply {
			signIn = viewModel
			signInScreen = this@SignInScreen
			lifecycleOwner = this@SignInScreen
			errorContainer.visibility = View.GONE
			loader.visibility = View.GONE
		}
		notifyAppEvents()
		loadAppContent()
	}

	private fun loadAppContent() {
		fetchAuthenticationDetails(isRefreshing = false)
	}

	private fun fetchAuthenticationDetails(isRefreshing: Boolean) {
		viewModel.fetchAuthenticationDetails().observe(this@SignInScreen) { authenticationDetails ->
			fetch(
				authenticationDetails,
				isLoaderEnabled = !isRefreshing,
				canUserAccessScreen = true,
				shouldBeInBackground = false
			) {
				/* TODO: Handle 401 Unauthorized 422 Unprocessable Entity */
				authenticationDetails.response?.let { signInData ->
					viewModel.authenticationQRCode.postValue(APIConstants.QR_CODE_BASE_URL + signInData.authenticationCode)
					viewModel.authenticationCode.postValue(signInData.authenticationCode)
					viewModel.authenticationLoginURL.postValue(signInData.authenticationLoginURL)
					viewModel.pollingInterval.postValue(signInData.pollingInterval.convertToMilli())
					viewModel.tokenExpiryTime.postValue(signInData.tokenExpiryTime.convertToMilli())
					viewModel.deviceCode.postValue(signInData.deviceCode)
					viewModel.errorMessage.postValue(signInData.errorMessage)
					viewModel.contentHasLoaded.postValue(true)

				} ?: showError(authenticationDetails.tag, getString(R.string.unknown_error))
			}
		}
	}

	private fun authenticationPolling() {
		viewModel.authenticationPolling().observe(this@SignInScreen) { pollingDetails ->
			fetch(
				pollingDetails,
				isLoaderEnabled = false,
				canUserAccessScreen = true,
				shouldBeInBackground = true
			) {
				when (pollingDetails.callStatus) {
					SUCCESS -> {
						Logger.printMessage("POLL SUCCESS")
						pollingDetails.response?.let { pollingData ->
							if (this::polling.isInitialized && this::pollTask.isInitialized) {
								polling.removeCallbacks(pollTask)
								polling.removeCallbacksAndMessages(pollTask)
							}
							if (this::tokenExpiry.isInitialized && this::expiryTask.isInitialized) {
								tokenExpiry.removeCallbacks(expiryTask)
								tokenExpiry.removeCallbacksAndMessages(expiryTask)
							}
							AppPreferences.set(
								AppConstants.authenticatedUserToken, pollingData.accessToken
							)
							viewModel.errorMessage.postValue(pollingData.errorMessage)
							viewModel.contentHasLoaded.postValue(true)
							fetchUserDetails()

						} ?: showError(pollingDetails.tag, getString(R.string.unknown_error))
					}

					ERROR -> {
						when (pollingDetails.message) {
							PollingStatus.PENDING -> {
								Logger.printMessage("POLL ERROR - TOKEN PENDING")
							}

							PollingStatus.SLOW_DOWN -> {
								Logger.printMessage("POLL ERROR - SLOWING DOWN")
								val interval = viewModel.pollingInterval.value
								viewModel.pollingInterval.value =
									max(IntValue.SIZE_10.convertToMilli(), interval!!)
							}

							PollingStatus.EXPIRED_TOKEN -> {
								viewModel.tokenExpiryTime.value = IntValue.SIZE_100
								Logger.printMessage("POLL ERROR - TOKEN Expired")
							}
						}
					}

					LOADING -> {
						Logger.printMessage("POLL LOADING")
					}
				}                /* TODO: Handle 401 Unauthorized 422 Unprocessable Entity */
			}
		}
	}

	private fun fetchUserDetails() {
		viewModel.fetchUserDetails().observe(this@SignInScreen) { userDetails ->
			fetch(
				userDetails,
				isLoaderEnabled = false,
				canUserAccessScreen = true,
				shouldBeInBackground = false
			) {
				/* TODO: Handle 401 Unauthorized 422 Unprocessable Entity */
				userDetails.response?.let { userData ->
					userData.data?.let { user ->
						AppPreferences.set(AppConstants.userID, user.id)
						AppPreferences.set(
							AppConstants.userSubscriptionStatus, user.subscriptionStatus
						)
						AppPreferences.set(AppConstants.userCurrency, user.currency)
						AppPreferences.set(AppConstants.userTimeZone, user.timeZone)
						AppPreferences.set(AppConstants.userBeaconBaseURL, user.beaconBaseURL)
						AppPreferences.set(AppConstants.userEmailAddress, user.email)
						AppPreferences.set(AppConstants.userFullName, user.fullName)
						AppPreferences.set(AppConstants.userDisplayName, user.displayName)
						AppPreferences.set(AppConstants.userAvatar, user.avatarURL)
						AppPreferences.set(AppConstants.userTimeZoneAbbr, user.timeZoneAbbr)
						AppPreferences.set(AppConstants.isUserAuthenticated, value = true)
					} ?: showError(userDetails.tag, getString(R.string.unknown_error))
				} ?: showError(userDetails.tag, getString(R.string.unknown_error))
			}
		}
	}

	private fun notifyAppEvents() {
		polling = Handler(Looper.getMainLooper())
		tokenExpiry = Handler(Looper.getMainLooper())
		setupBlurView()
		viewModel.authenticationQRCode.observe(this@SignInScreen) { qrCode ->
			binding.qrCode.loadImage(qrCode, ImageTags.ROUNDED)
		}
		viewModel.pollingInterval.observe(this@SignInScreen) { pollingInterval ->
			Logger.printMessage("Interval - $pollingInterval")
			if (pollingInterval.isGreaterThan(0)) {
				if (this::polling.isInitialized && this::pollTask.isInitialized) {
					polling.removeCallbacks(pollTask)
					polling.removeCallbacksAndMessages(pollTask)
				}
				pollTask = Runnable {
					authenticationPolling()
					Logger.printMessage("Inner Interval - $pollingInterval")
					polling.postDelayed(pollTask, pollingInterval?.toLong()!!)
				}
				polling.postDelayed(pollTask, pollingInterval.toLong())
			}
		}
		viewModel.tokenExpiryTime.observe(this@SignInScreen) { tokenExpiryTime ->
			Logger.printMessage("Expiry - $tokenExpiryTime")
			if (tokenExpiryTime.isGreaterThan(0)) {
				if (this::tokenExpiry.isInitialized && this::expiryTask.isInitialized) {
					tokenExpiry.removeCallbacks(expiryTask)
					tokenExpiry.removeCallbacksAndMessages(expiryTask)
				}
				expiryTask = Runnable {
					Logger.printMessage("Internal Expiry - $tokenExpiryTime")
					fetchAuthenticationDetails(isRefreshing = true)
					tokenExpiry.postDelayed(expiryTask, tokenExpiryTime?.toLong()!!)
				}
				tokenExpiry.postDelayed(expiryTask, tokenExpiryTime.toLong())
			}
		}
	}

	private fun setupBlurView() {
		binding.errorContainer.setupWith(binding.container).setBlurRadius(12.5f)
	}

	override fun onPause() {
		super.onPause()
		if (this::polling.isInitialized && this::pollTask.isInitialized) {
			polling.removeCallbacks(pollTask)
			polling.removeCallbacksAndMessages(pollTask)
		}
	}

	override fun onResume() {
		super.onResume()
		if (this::polling.isInitialized && this::pollTask.isInitialized) polling.post(pollTask)
	}

}
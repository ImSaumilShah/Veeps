package com.veeps.app.feature.waitingRoom.ui

import android.os.CountDownTimer
import android.view.View
import androidx.activity.OnBackPressedCallback
import com.veeps.app.R
import com.veeps.app.core.BaseActivity
import com.veeps.app.databinding.ActivityWaitingRoomScreenBinding
import com.veeps.app.extension.goToScreen
import com.veeps.app.extension.loadImage
import com.veeps.app.feature.video.ui.VideoPlayerScreen
import com.veeps.app.feature.waitingRoom.viewModel.WaitingRoomViewModel
import com.veeps.app.util.AppConstants
import com.veeps.app.util.DEFAULT
import com.veeps.app.util.ImageTags
import com.veeps.app.util.Logger
import com.veeps.app.util.Screens
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Duration
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.PeriodFormatterBuilder
import kotlin.math.abs

class WaitingRoomScreen : BaseActivity<WaitingRoomViewModel, ActivityWaitingRoomScreenBinding>() {

	private fun getBackCallback(): OnBackPressedCallback {
		val backPressedCallback = object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				Logger.print(
					"Back Pressed on ${
						this@WaitingRoomScreen.localClassName.substringAfterLast(
							"."
						)
					} Finishing Activity"
				)
				finish()
			}
		}
		return backPressedCallback
	}

	override fun getViewBinding(): ActivityWaitingRoomScreenBinding =
		ActivityWaitingRoomScreenBinding.inflate(layoutInflater)

	override fun onRendered(
		viewModel: WaitingRoomViewModel, binding: ActivityWaitingRoomScreenBinding
	) {
		backPressedCallback = getBackCallback()
		onBackPressedDispatcher.addCallback(this@WaitingRoomScreen, backPressedCallback)
		binding.apply {
			waitingRoom = viewModel
			waitingRoomScreen = this@WaitingRoomScreen
			lifecycleOwner = this@WaitingRoomScreen
			loader.visibility = View.GONE
		}
		notifyAppEvents()
		loadAppContent()
	}

	private fun notifyAppEvents() {
		viewModel.eventTimer.observe(this@WaitingRoomScreen) { timer ->
			if (timer.isNullOrBlank()) {
				binding.showStartsIn.visibility = View.INVISIBLE
				binding.timer.visibility = View.INVISIBLE
			} else {
				viewModel.eventTimerDescription.value?.let {
					binding.showStartsIn.text = it
				}
				binding.showStartsIn.visibility = View.VISIBLE
				binding.timer.visibility = View.VISIBLE
			}
		}

		viewModel.eventLogo.observe(this@WaitingRoomScreen) { logo ->
			if (!logo.isNullOrBlank()) {
				binding.logo.loadImage(logo, ImageTags.LOGO)
			}
		}
	}

	private fun loadAppContent() {
		if (intent != null && intent.hasExtra("eventId")) {
			viewModel.eventId.postValue(intent.getStringExtra("eventId"))
			viewModel.eventLogo.postValue(intent.getStringExtra("eventLogo"))
			viewModel.eventTitle.postValue(intent.getStringExtra("eventTitle"))
			val currentDate = DateTime.now()
			val eventStreamStartsAt =
				intent.getStringExtra("eventStreamStartsAt") ?: currentDate.toString()
			val eventDoorOpensAt =
				intent.getStringExtra("eventDoorOpensAt") ?: currentDate.toString()
			val eventDate: DateTime
			if (eventDoorOpensAt.isBlank() || eventDoorOpensAt == currentDate.toString()) {
				eventDate = DateTime(
					eventStreamStartsAt, DateTimeZone.UTC
				).withZone(DateTimeZone.getDefault()).toDateTime()
				viewModel.eventTimerDescription.postValue(getString(R.string.show_starts_in_label))
			} else {
				eventDate =
					DateTime(eventDoorOpensAt, DateTimeZone.UTC).withZone(DateTimeZone.getDefault())
						.toDateTime()
				viewModel.eventTimerDescription.postValue(getString(R.string.door_opens_at_label))
			}

			val timer =
				object : CountDownTimer(abs(Duration(currentDate, eventDate).millis), 1000) {
					override fun onTick(millisUntilFinished: Long) {
						viewModel.eventTimer.postValue(
							PeriodFormatterBuilder()
								.printZeroNever().minimumPrintedDigits(2).appendDays()
								.appendSeparator(":")
								.printZeroAlways().minimumPrintedDigits(2).appendHours()
								.appendSeparator(":")
								.printZeroAlways().minimumPrintedDigits(2).appendMinutes()
								.appendSeparator(":")
								.printZeroAlways().minimumPrintedDigits(2).appendSeconds()
								.toFormatter().print(
									Period.millis(millisUntilFinished.toInt()).normalizedStandard(
										PeriodType.yearMonthDayTime()
									)
								)
						)
					}

					override fun onFinish() {
						finish()
						goToScreen<VideoPlayerScreen>(
							false,
							AppConstants.TAG to Screens.VIDEO,
							"eventId" to (viewModel.eventId.value ?: DEFAULT.EMPTY_STRING)
						)
					}
				}
			timer.start()
		} else {
			onExit()
		}
	}

	fun onExit() {
		onBackPressedDispatcher.onBackPressed()
	}

}
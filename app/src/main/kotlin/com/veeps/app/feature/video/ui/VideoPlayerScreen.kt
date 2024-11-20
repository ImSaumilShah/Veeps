package com.veeps.app.feature.video.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewOutlineProvider
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.TimeBar
import androidx.recyclerview.widget.PagerSnapHelper
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerBuilder
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.advertising.AdItem
import com.bitmovin.player.api.advertising.AdSource
import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.api.advertising.AdvertisingConfig
import com.bitmovin.player.api.deficiency.ErrorEvent
import com.bitmovin.player.api.deficiency.PlayerErrorCode
import com.bitmovin.player.api.deficiency.SourceErrorCode
import com.bitmovin.player.api.drm.WidevineConfig
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.source.SourceBuilder
import com.bitmovin.player.api.source.SourceConfig
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.UserId
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNHeartbeatNotificationOptions
import com.pubnub.api.enums.PNLogVerbosity
import com.pubnub.api.enums.PNStatusCategory
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.retry.RetryConfiguration
import com.veeps.app.BuildConfig.bitmovinAnalyticsKey
import com.veeps.app.BuildConfig.pubnubPublishKey
import com.veeps.app.BuildConfig.pubnubSubscribeKey
import com.veeps.app.R
import com.veeps.app.core.BaseActivity
import com.veeps.app.databinding.ActivityVideoPlayerScreenBinding
import com.veeps.app.extension.loadImage
import com.veeps.app.extension.round
import com.veeps.app.extension.setHorizontalBias
import com.veeps.app.feature.contentRail.model.ChatMessageItem
import com.veeps.app.feature.contentRail.model.Entities
import com.veeps.app.feature.video.adapter.ChatMessagesAdapter
import com.veeps.app.feature.video.viewModel.VideoPlayerViewModel
import com.veeps.app.util.APIConstants
import com.veeps.app.util.AppConstants
import com.veeps.app.util.AppConstants.drmLicenseURL
import com.veeps.app.util.AppPreferences
import com.veeps.app.util.AppUtil
import com.veeps.app.util.DEFAULT
import com.veeps.app.util.EntityTypes
import com.veeps.app.util.EventTypes
import com.veeps.app.util.ImageTags
import com.veeps.app.util.IntValue
import com.veeps.app.util.LastSignalTypes
import com.veeps.app.util.Logger
import com.veeps.app.util.Screens
import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
class VideoPlayerScreen : BaseActivity<VideoPlayerViewModel, ActivityVideoPlayerScreenBinding>() {

	private lateinit var getCurrentTimer: Handler
	private lateinit var statsManagement: Handler
	private lateinit var timerTask: Runnable
	private lateinit var addStatsTask: Runnable
	private lateinit var player: Player
	private var timeout = Handler(Looper.getMainLooper())
	private val inactivitySeconds = 10
	private var scrubbedPosition = 0L
	private var playbackStream = DEFAULT.EMPTY_STRING
	private var isScrubVisible = false
	var trickPlayVisible = MutableLiveData<Boolean>()
	private val trickPlayRunnable = Runnable { trickPlayVisible.value = false }
	private lateinit var pubnub: PubNub
	private lateinit var pubNubListener: SubscribeCallback
	private var artistChannel = DEFAULT.EMPTY_STRING
	private var subscribeChannel = DEFAULT.EMPTY_STRING
	private var publishChannel = DEFAULT.EMPTY_STRING
	private var signalChannel = DEFAULT.EMPTY_STRING
	private var playingPosition = 0L
	private var isChatEnabled = false
	lateinit var chatAdapter: ChatMessagesAdapter
	private val adItems: MutableList<AdItem> = ArrayList()
	private var isDrmAvailable = false
	private var isAdAvailable = false
	private var isAdVisible = false
	private var currentTime: String = DEFAULT.EMPTY_STRING
	private var duration: String = DEFAULT.EMPTY_STRING
	private var ticketId: String = DEFAULT.EMPTY_STRING
	private var eventName: String? = DEFAULT.EMPTY_STRING
	private var userType: String = DEFAULT.EMPTY_STRING

	private fun getBackCallback(): OnBackPressedCallback {
		val backPressedCallback = object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				handleBack()
			}
		}
		return backPressedCallback
	}

	private fun handleBack() {
		if (binding.seekDuration.visibility == View.VISIBLE) {
			timeout.removeCallbacks(trickPlayRunnable)
			timeout.postDelayed(trickPlayRunnable, (500).toLong())
		} else {
			if (binding.errorContainer.visibility != View.VISIBLE && binding.chatFromPhoneContainer.visibility != View.VISIBLE) {
				showError(Screens.EXIT_APP, "Are you sure you want to exit?", "")
			}
		}
	}

	override fun getViewBinding(): ActivityVideoPlayerScreenBinding =
		ActivityVideoPlayerScreenBinding.inflate(layoutInflater)

	override fun onRendered(
		viewModel: VideoPlayerViewModel, binding: ActivityVideoPlayerScreenBinding
	) {
		backPressedCallback = getBackCallback()
		onBackPressedDispatcher.addCallback(this, backPressedCallback)
		binding.apply {
			video = viewModel
			videoPlayerScreen = this@VideoPlayerScreen
			lifecycleOwner = this@VideoPlayerScreen
			errorContainer.visibility = View.GONE
			chatFromPhoneContainer.visibility = View.GONE
			loader.visibility = View.GONE
			progress.hideScrubber(500)
			binding.progress.setKeyTimeIncrement(10)
			imagePreview.visibility = View.VISIBLE
			seekDuration.visibility = View.GONE
			vodControls.visibility = View.GONE
			standBy.visibility = View.GONE
			liveControls.visibility = View.GONE
			topControls.visibility = View.GONE
			playPause.requestFocus()
			if (isChatEnabled) {
				listing.apply {
					setNumColumns(1)
					chatAdapter = ChatMessagesAdapter(arrayListOf())
					adapter = chatAdapter
					onFlingListener = PagerSnapHelper()
					isFocusable = false
					isFocusableInTouchMode = false
					smoothScrollToPosition(chatAdapter.itemCount - 1)
				}
				chatToggle.visibility = View.VISIBLE
				chatFromPhone.nextFocusUpId = chatFromPhone.id
				chatFromPhone.nextFocusDownId = chatFromPhone.id
				chatFromPhone.nextFocusLeftId = chatFromPhone.id
				chatFromPhone.nextFocusRightId = chatToggle.id

				chatToggle.nextFocusUpId = chatToggle.id
				chatToggle.nextFocusDownId = chatToggle.id
				chatToggle.nextFocusRightId = chatToggle.id
				chatToggle.nextFocusLeftId = chatFromPhone.id
			} else {
				chatToggle.visibility = View.GONE
				chatFromPhone.nextFocusUpId = chatFromPhone.id
				chatFromPhone.nextFocusDownId = chatFromPhone.id
				chatFromPhone.nextFocusLeftId = chatFromPhone.id
				chatFromPhone.nextFocusRightId = chatFromPhone.id
			}
		}
		notifyAppEvents()
		loadAppContent()
	}

	private fun loadAppContent() {
		if (intent != null && intent.hasExtra("eventId")) {
			setupVideoPlayer()
			viewModel.eventId.postValue(intent.getStringExtra("eventId"))
		}
	}

	private fun notifyAppEvents() {
		getCurrentTimer = Handler(Looper.getMainLooper())
		statsManagement = Handler(Looper.getMainLooper())

		binding.chatFromPhone.setOnFocusChangeListener { _, hasFocus ->
			binding.chatFromPhoneLabel.setTextColor(
				ContextCompat.getColor(
					this@VideoPlayerScreen, if (hasFocus) R.color.dark_black else R.color.white
				)
			)
		}

		binding.chatToggle.setOnFocusChangeListener { _, hasFocus ->
			binding.chatToggleLabel.setTextColor(
				ContextCompat.getColor(
					this@VideoPlayerScreen, if (hasFocus) R.color.dark_black else R.color.white
				)
			)
		}

		setupBlur()

		binding.playPause.setOnClickListener {
			if (binding.playPause.isSelected) {
				player.pause().also { binding.playPause.isSelected = false }
			} else {
				player.play().also { binding.playPause.isSelected = true }
			}
		}

		// Hiding the trickBar to disable the keys when the hiding animation is complete
		binding.controls.animate().setListener(object : AnimatorListenerAdapter() {
			override fun onAnimationEnd(animation: Animator) {
				super.onAnimationEnd(animation)
				if (trickPlayVisible.value != null && !trickPlayVisible.value!!) {
					binding.controls.visibility = View.GONE
				}
			}
		})

		viewModel.chatMessages.observe(this@VideoPlayerScreen) { chatMessages ->
			if (!chatMessages.isNullOrEmpty()) {
				Logger.doNothing()
			}
		}

		trickPlayVisible.observe(this@VideoPlayerScreen) { visible: Boolean ->
			if (visible) {
				binding.controls.visibility = View.VISIBLE
				binding.playPause.requestFocus()
				binding.controls.animate().translationY(0F).alpha(1F)
			} else {
				binding.controls.animate().translationY(1F).alpha(0F)
			}
		}

		viewModel.eventId.observe(this@VideoPlayerScreen) { eventId ->
			if (!eventId.isNullOrBlank()) {
				fetchAllPurchasedEvents(eventId)
			}
		}

		viewModel.playbackURL.observe(this@VideoPlayerScreen) { playbackURL ->
			if (!playbackURL.isNullOrBlank()) {
				if (this::player.isInitialized) {
					// Create a new source config
					//https://mtoczko.github.io/hls-test-streams/test-vtt/playlist.m3u8 // VOD 10 mins
					//https://cdn.bitmovin.com/content/assets/sintel/hls/playlist.m3u8 // VOD Full
					//https://cdn.bitmovin.com/content/assets/sintel/sintel.mpd // VOD DASH
					//https://mtoczko.github.io/hls-test-streams/test-gap/playlist.m3u8 // VOD 4 min Gap video
					//https://mtoczko.github.io/hls-test-streams/test-group/playlist.m3u8 // VOD 1 min Quality changes
					//https://mtoczko.github.io/hls-test-streams/test-vtt-ts-segments/playlist.m3u8 // vod 20 seconds timer
					//https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8 // Live HLS
					//https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8 //HLS Bit-Bop Simple
					//https://devstreaming-cdn.apple.com/videos/streaming/examples/historic_planet_content_2023-10-26-3d-video/main.m3u8 //basic HLS
					//https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8 //HLS Bip-Bop - Language and Subtitles
					//https://devstreaming-cdn.apple.com/videos/streaming/examples/adv_dv_atmos/main.m3u8 //HLS main VOD 4K


					Logger.print("Saumil Playback URL - $playbackURL")
					Logger.print("Saumil DRM Applicable - $isDrmAvailable. ${if (isDrmAvailable) "DRM License URL - $drmLicenseURL" else ""}")

					val sourceConfig = SourceConfig.fromUrl(playbackURL)

					// Attach DRM handling to the source config
					if (isDrmAvailable) {
						sourceConfig.apply {
							this@apply.drmConfig = WidevineConfig(drmLicenseURL)
						}
					}

					val customData: CustomData = CustomData.Builder().setCustomData1(userType)
						.setCustomData2(Player.sdkVersion).setCustomData3(
							AppPreferences.get(
								AppConstants.userID,
								DEFAULT.EMPTY_STRING
							)
						).setCustomData4(EntityTypes.EVENT).setCustomData5(eventName).build()
					// create a source with a sourceMetadata for custom analytics tracking
					val sourceMetadata: SourceMetadata =
						SourceMetadata.Builder().setTitle(eventName).setIsLive(player.isLive)
							.setCustomData(customData)
							.setVideoId(viewModel.eventId.value ?: DEFAULT.EMPTY_STRING).build()
					val source =
						SourceBuilder(sourceConfig).configureAnalytics(sourceMetadata).build()
					// Load the source
					player.load(source)
					binding.videoPlayer.isUiVisible = false
				}
			}
		}
	}

	private fun fetchAllPurchasedEvents(eventId: String) {
		viewModel.fetchAllPurchasedEvents().observe(this@VideoPlayerScreen) { allPurchasedEvents ->
			fetch(
				allPurchasedEvents,
				isLoaderEnabled = false,
				canUserAccessScreen = true,
				shouldBeInBackground = true,
			) {
				allPurchasedEvents.response?.let { railData ->
					if (railData.data.isNotEmpty()) {
						val eventData = railData.data.filter { it.eventId == eventId }
						if (eventData.size == 1) {
							ticketId = eventData.get(0).id.toString()
						}
					}
				}
				fetchUserStats(eventId)
			}
		}
	}

	private fun setupBlur() {
		binding.errorLayoutContainer.setupWith(binding.errorContainer).setBlurRadius(12.5f)
		binding.chatFromPhoneLayoutContainer.setupWith(binding.chatFromPhoneContainer)
			.setBlurRadius(12.5f)
		binding.liveLabel.setupWith(binding.liveControls).setBlurRadius(12.5f)
		binding.chatFromPhone.setupWith(binding.liveControls).setBlurRadius(12.5f)
		binding.chatFromPhone.outlineProvider = ViewOutlineProvider.BACKGROUND
		binding.chatFromPhone.clipToOutline = true
		binding.chatToggle.setupWith(binding.liveControls).setBlurRadius(12.5f)
		binding.chatToggle.outlineProvider = ViewOutlineProvider.BACKGROUND
		binding.chatToggle.clipToOutline = true
		binding.chatToggle.isSelected = true
	}

	private fun setupVideoPlayer() {
		binding.videoPlayer.isUiVisible = false

		player = PlayerBuilder(this).setPlayerConfig(createPlayerConfig()).configureAnalytics(AnalyticsConfig(bitmovinAnalyticsKey)).build()
		binding.videoPlayer.player = player
		binding.videoPlayer.keepScreenOn = true

		player.on<PlayerEvent.Error> { errorEvent: ErrorEvent ->
			Logger.print("Saumil Error Event - $errorEvent")
			binding.playPause.isSelected = false
			when (errorEvent.code) {
				PlayerErrorCode.General, PlayerErrorCode.DecoderGeneral, PlayerErrorCode.DecodingFailed, PlayerErrorCode.LicenseKeyNotFound, PlayerErrorCode.DecoderInitialization, PlayerErrorCode.DecodingExceedsCapabilities, PlayerErrorCode.DecodingUnsupported, PlayerErrorCode.LicenseAuthenticationFailed -> {
					showError(
						Screens.PLAYER_ERROR,
						getString(R.string.video_error_title),
						getString(R.string.video_error_description)
					)
				}

				SourceErrorCode.General, SourceErrorCode.HttpStatusCode, SourceErrorCode.DrmGeneral, SourceErrorCode.Io, SourceErrorCode.ClearTextConnection, SourceErrorCode.ConnectionFailed, SourceErrorCode.ConnectionTimeout, SourceErrorCode.DrmKeyExpired, SourceErrorCode.DrmRequestFailed, SourceErrorCode.DrmUnsupported -> {
					showError(
						Screens.PLAYER_ERROR,
						getString(R.string.video_error_title),
						getString(R.string.video_error_description)
					)
				}

				else -> {
					showError(
						Screens.PLAYER_ERROR,
						getString(R.string.video_error_title),
						getString(R.string.video_error_description)
					)
				}
			}
		}

		player.on<PlayerEvent.Playing> {
			resumePlayer(playingPosition)
			binding.videoPlayer.isUiVisible = false
			if (this@VideoPlayerScreen::statsManagement.isInitialized && this@VideoPlayerScreen::addStatsTask.isInitialized) {
				statsManagement.removeCallbacks(addStatsTask)
				statsManagement.removeCallbacksAndMessages(addStatsTask)
				statsManagement.post(addStatsTask)
			}
			binding.loader.visibility = View.GONE
			binding.playPause.isSelected = true
			binding.videoPlayer.postDelayed(
				this@VideoPlayerScreen::getPlayerProgress,
				if (isAdAvailable && isAdVisible) IntValue.NUMBER_500.toLong() else IntValue.NUMBER_1000.toLong()
			)
			if (player.isLive) {
				binding.playPause.isFocusable = false
				binding.playPause.isFocusableInTouchMode = false
				binding.progress.isFocusable = false
				binding.progress.isFocusableInTouchMode = false
				binding.chatFromPhone.isFocusable = true
				if (isChatEnabled) binding.chatToggle.isFocusable = true
				if (!binding.chatFromPhone.isFocused && !binding.chatToggle.isFocused) binding.chatFromPhone.requestFocus()
			} else {
				binding.chatToggle.isFocusable = false
				binding.chatToggle.isFocusableInTouchMode = false
				binding.chatFromPhone.isFocusable = false
				binding.chatFromPhone.isFocusableInTouchMode = false
				binding.playPause.isFocusable = true
				binding.progress.isFocusable = true
				if (!binding.playPause.isFocused && !binding.progress.isFocused) binding.playPause.requestFocus()
			}
		}

		player.on<PlayerEvent.Paused> {
			binding.videoPlayer.isUiVisible = false
			if (this@VideoPlayerScreen::statsManagement.isInitialized && this@VideoPlayerScreen::addStatsTask.isInitialized) {
				statsManagement.removeCallbacks(addStatsTask)
				statsManagement.removeCallbacksAndMessages(addStatsTask)
			}

			binding.playPause.isSelected = false
		}

		player.on<PlayerEvent.PlaybackFinished> {
			binding.playPause.isSelected = false
		}

		player.on<PlayerEvent.StallStarted> {
			binding.loader.visibility = View.VISIBLE
		}

		player.on<PlayerEvent.StallEnded> {
			binding.loader.visibility = View.GONE
		}

		player.on<PlayerEvent.TimeChanged> {
			if (player.isLive) {
				binding.topControls.visibility = View.VISIBLE
				binding.liveControls.visibility = View.VISIBLE
				binding.standBy.visibility = View.GONE
				binding.vodControls.visibility = View.GONE
			} else {
				binding.topControls.visibility = View.VISIBLE
				binding.vodControls.visibility = View.VISIBLE
				binding.liveControls.visibility = View.GONE
				binding.standBy.visibility = View.GONE
				if (isAdAvailable && player.isAd) {
					binding.videoPlayer.isUiVisible = false
					trickPlayVisible.value = true
					binding.chatToggle.isFocusable = false
					binding.chatToggle.isFocusableInTouchMode = false
					binding.chatFromPhone.isFocusable = false
					binding.chatFromPhone.isFocusableInTouchMode = false
					binding.playPause.isFocusable = true
					binding.progress.isFocusable = true
					if (!binding.playPause.isFocused && !binding.progress.isFocused) binding.playPause.requestFocus()
					isAdVisible = true
					binding.topControls.visibility = View.GONE
					binding.playPause.requestFocus()
					binding.playPause.isSelected = true
				}
			}
		}

		player.on<PlayerEvent.AdStarted> {
			binding.videoPlayer.isUiVisible = false
			getPlayerProgress()
		}

		player.on<PlayerEvent.AdFinished> {
			binding.videoPlayer.isUiVisible = false
			isAdVisible = false
		}

		player.on<PlayerEvent.AdError> {
			binding.videoPlayer.isUiVisible = false
			isAdVisible = false
		}

		binding.progress.setOnFocusChangeListener { _, hasFocus ->
			if (hasFocus) {
				scrubbedPosition = player.currentTime.toLong()
				isScrubVisible = true
				setImagePreview()
				binding.progress.showScrubber(500)
			} else {
				scrubbedPosition = player.currentTime.toLong()
				isScrubVisible = false
				setImagePreview()
				getPlayerProgress()
				binding.progress.hideScrubber(500)
			}
		}

		binding.progress.addListener(object : TimeBar.OnScrubListener {
			override fun onScrubStart(timeBar: TimeBar, position: Long) {
				scrubbedPosition = position
				isScrubVisible = true
				setImagePreview()
				player.pause()
			}

			override fun onScrubMove(timeBar: TimeBar, position: Long) {
				scrubbedPosition = position
				isScrubVisible = true
				setImagePreview()
			}

			override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
				binding.progress.setPosition(position)
				scrubbedPosition = position
				isScrubVisible = true
				setImagePreview()
			}
		})

		// In INACTIVITY_SECONDS seconds of inactivity hide the trickBar
		timeout.postDelayed(trickPlayRunnable, (inactivitySeconds * 1000).toLong())
	}

	private fun resumePlayer(playingPosition: Long) {
		player.seek(playingPosition.toDouble())
		player.play()
		binding.playPause.requestFocus()
	}

	private fun initPubNub(eventDetails: Entities) {
		if (this::pubnub.isInitialized && this::pubNubListener.isInitialized) pubnub.removeListener(
			pubNubListener
		)
		pubNubListener = object : SubscribeCallback() {
			override fun status(pubnub: PubNub, pnStatus: PNStatus) {
				if (pnStatus.category == PNStatusCategory.PNUnexpectedDisconnectCategory || pnStatus.category == PNStatusCategory.PNTimeoutCategory) {
					pubnub.reconnect()
				} else if (pnStatus.category === PNStatusCategory.PNConnectedCategory) {
					Logger.doNothing()
				}
			}

			override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
				if (isChatEnabled) {
					if (pnMessageResult.channel == subscribeChannel) {
						chatAdapter.addMessage(
							ChatMessageItem(
								name = pnMessageResult.message.asJsonObject.get("author").asString,
								message = pnMessageResult.message.asJsonObject.get("body").asString,
								isArtist = pnMessageResult.message.asJsonObject.get("type").asString != "fan"
							)
						)
						binding.listing.smoothScrollToPosition(chatAdapter.itemCount - 1)
					}
				}
			}

			override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {
				when (pnSignalResult.message.asJsonObject.get("ls").asString) {
					LastSignalTypes.DISCONNECTED -> {
						binding.videoPlayer.player?.pause()
						binding.standBy.visibility = View.VISIBLE
						binding.topControls.visibility = View.VISIBLE
						binding.liveControls.visibility = View.VISIBLE
						binding.vodControls.visibility = View.GONE
						//on demand call
					}

					LastSignalTypes.CONNECTED -> {
						player.pause()
						binding.standBy.visibility = View.VISIBLE
						binding.topControls.visibility = View.VISIBLE
						binding.liveControls.visibility = View.VISIBLE
						binding.vodControls.visibility = View.GONE
					}

					LastSignalTypes.ACTIVE, LastSignalTypes.RECORDING, LastSignalTypes.NO_SIGNAL -> {
						binding.standBy.visibility = View.GONE
						viewModel.playbackURL.postValue(playbackStream.ifBlank { DEFAULT.EMPTY_STRING })
					}

					LastSignalTypes.IDLE -> {
						player.pause()
						binding.standBy.visibility = View.VISIBLE
						binding.topControls.visibility = View.VISIBLE
						binding.liveControls.visibility = View.VISIBLE
						binding.vodControls.visibility = View.GONE
					}

					LastSignalTypes.STREAM_RESTARTED -> {
						binding.standBy.visibility = View.GONE
						viewModel.playbackURL.postValue(playbackStream.ifBlank { DEFAULT.EMPTY_STRING })
					}

					LastSignalTypes.ON_DEMAND_READY -> {
						showError(
							LastSignalTypes.ON_DEMAND_READY,
							"Thanks for watching the show!",
							"Hang tight, rewatch available soon"
						)
					}

					LastSignalTypes.VOD_READY -> {
						binding.standBy.visibility = View.GONE
						releaseVideoPlayer()
						loadAppContent()
						timeout.removeCallbacks(trickPlayRunnable)
						timeout.postDelayed(trickPlayRunnable, (inactivitySeconds * 1000).toLong())
						if (trickPlayVisible.value != null && !trickPlayVisible.value!!) {
							trickPlayVisible.value = true
							if (player.isLive) binding.chatFromPhone.requestFocus() else binding.playPause.requestFocus()
						}
						binding.errorContainer.visibility = View.GONE
					}

					LastSignalTypes.CHAT_MESSAGE_DELETED -> {

					}

					LastSignalTypes.STREAM_ENDED -> {
						binding.standBy.visibility = View.GONE

						if (eventDetails.eventReWatchDuration.isNullOrBlank()) {
							binding.reWatch.text = getString(R.string.re_watch_not_available)
						} else {
							val reWatchDuration =
								eventDetails.eventReWatchDuration!!.ifBlank { "0" }.toInt()
							if (reWatchDuration == 0) {
								binding.reWatch.text = getString(R.string.re_watch_not_available)
							} else {
								val reWatchDurationString =
									AppUtil.calculateReWatchTime(reWatchDuration)
								binding.reWatch.text = getString(
									R.string.re_watch_available_for_time, reWatchDurationString
								)
							}
						}
						showError(
							Screens.STREAM_END,
							"Thanks for watching the show!",
							"Hang tight, rewatch available soon"
						)
					}
				}
			}
		}

		/*val config = PNConfiguration.builder(
			userId = UserId(
				AppPreferences.get(
					AppConstants.userID,
					getString(R.string.app_platform)
				) ?: getString(R.string.app_platform)
			), subscribeKey = pubnubSubscribeKey
		).apply {
			publishKey = pubnubPublishKey
			secure = true
			logVerbosity = PNLogVerbosity.BODY
			heartbeatNotificationOptions = PNHeartbeatNotificationOptions.ALL
			retryConfiguration = RetryConfiguration.Exponential(
				minDelayInSec = 3, maxDelayInSec = 10, maxRetryNumber = 5
			)
		}
		pubnub = PubNub.create(config.build())
		pubnub.addListener(pubNubListener)

		pubnub.subscriptionSetOf(channels = setOf(signalChannel, artistChannel, subscribeChannel),).subscribe()*/

		val config = PNConfiguration(
			UserId(
				AppPreferences.get(
					AppConstants.userID, getString(R.string.app_platform)
				) ?: getString(R.string.app_platform)
			)
		).apply {
			subscribeKey = pubnubSubscribeKey
			publishKey = pubnubPublishKey
			secure = true
			logVerbosity = PNLogVerbosity.BODY
			heartbeatNotificationOptions = PNHeartbeatNotificationOptions.ALL
		}
		pubnub = PubNub(config)
		pubnub.addListener(pubNubListener)
		pubnub.configuration.retryConfiguration = RetryConfiguration.Exponential(
			minDelayInSec = 3, maxDelayInSec = 10, maxRetryNumber = 5
		)

		pubnub.subscribe(channels = listOf(signalChannel, artistChannel, subscribeChannel))

		if (isChatEnabled) {
			pubnub.history(channel = subscribeChannel, includeMeta = true, includeTimetoken = true)
				.async { result, status ->
					if (!status.error) {
						result?.let { subscriberHistory ->
							subscriberHistory.messages.forEach { messages ->
								chatAdapter.addMessage(
									ChatMessageItem(
										name = messages.entry.asJsonObject.get("author").asString,
										message = messages.entry.asJsonObject.get("body").asString,
										isArtist = messages.entry.asJsonObject.get("type").asString != "fan",
									)
								)
								binding.listing.smoothScrollToPosition(chatAdapter.itemCount - 1)
							}
						}
					} else {
						status.exception?.printStackTrace()
					}
				}

			pubnub.history(channel = artistChannel, includeMeta = true, includeTimetoken = true)
				.async { _, status ->
					if (status.error) {
						status.exception?.printStackTrace()
					}
				}
		}
	}

	private fun setImagePreview() {
		val aligner = 160L
		val positionInPercentage =
			maxOf(scrubbedPosition - aligner, aligner) / player.duration.round(2).toFloat()
		binding.vodControls.setHorizontalBias(R.id.image_preview, positionInPercentage.toFloat())
//		if (!viewModel.tiles.value.isNullOrEmpty() && isScrubVisible && binding.progress.hasFocus()) {
//			binding.imagePreview.clipToOutline = true
//			runOnUiThread {
//				Glide.with(binding.imagePreview.context).asBitmap().load(viewModel.storyBoard ?: "")
//					.transform(
//						MultiTransformation(
//							GlideThumbnailTransformation(
//								scrubbedPosition,
//								viewModel.tileWidth.value ?: 0,
//								viewModel.tileHeight.value ?: 0,
//								viewModel.tiles.value ?: arrayListOf(),
//							), CenterInside(), RoundedCorners(IntValue.NUMBER_5)
//						)
//					).placeholder(binding.imagePreview.drawable).error(R.drawable.card_background_black)
//					.into(binding.imagePreview)
//
//				val tiles = viewModel.tiles.value ?: arrayListOf()
//				var x = 0
//				var y = 0
//				for (position in tiles.indices) {
//					try {
//						if (scrubbedPosition >= tiles[position].start && scrubbedPosition <= tiles[position + 1].start) {
//							x = tiles[position].x
//							y = tiles[position].y
//							break
//						}
//					} catch (e: Exception) {
//						if (scrubbedPosition >= tiles[position].start) {
//							x = tiles[position].x
//							y = tiles[position].y
//							break
//						}
//					}
//				}
//				viewModel.storyBoard?.let {
//					val frameBitmap = Bitmap.createBitmap(it, x, y, viewModel.tileWidth.value ?: 0, viewModel.tileHeight.value ?: 0)
//					binding.imagePreview.setImageBitmap(frameBitmap)
//				} ?: run {
//					binding.imagePreview.setImageDrawable(AppCompatResources.getDrawable(this@VideoPlayerScreen, R.drawable.card_background_black))
//				}
//
//			}
//			binding.imagePreview.visibility = View.VISIBLE
//		} else {
//			binding.imagePreview.visibility = View.INVISIBLE
//		}
		val seekDuration =
			PeriodFormatterBuilder().printZeroAlways().minimumPrintedDigits(2).appendHours()
				.appendSeparator(":").printZeroAlways().minimumPrintedDigits(2).appendMinutes()
				.appendSeparator(":").printZeroAlways().minimumPrintedDigits(2).appendSeconds()
				.toFormatter().print(
					Period.seconds(scrubbedPosition.toInt()).normalizedStandard()
				)
		binding.seekDuration.text = seekDuration ?: "00:00:00"
		binding.seekDuration.visibility =
			if (isScrubVisible && binding.progress.hasFocus()) View.VISIBLE else View.INVISIBLE
	}

	private fun releaseVideoPlayer() {
		if (this::player.isInitialized) {
			binding.videoPlayer.onPause()
			binding.videoPlayer.onDestroy()
		}
	}

	private fun fetchUserStats(eventId: String) {
		val userStatsAPIURL = AppPreferences.get(
			AppConstants.userBeaconBaseURL, DEFAULT.EMPTY_STRING
		) + APIConstants.FETCH_USER_STATS
		AppPreferences.set(AppConstants.generatedJWT, AppUtil.generateJWT(eventIds = eventId))
		viewModel.fetchUserStats(userStatsAPIURL, eventIds = eventId)
			.observe(this@VideoPlayerScreen) { userStatsDetails ->
				fetch(
					userStatsDetails,
					isLoaderEnabled = false,
					canUserAccessScreen = true,
					shouldBeInBackground = true,
				) {
					userStatsDetails.response?.let { userStatsResponse ->
						playingPosition = if (userStatsResponse.userStats.isNotEmpty()) {
							val stats = userStatsResponse.userStats.filter { it.eventId == eventId }
							if (stats.size == 1) {
								val currentStat = (stats[0].cursor / stats[0].duration)
								if (currentStat < 95) {
									stats[0].cursor.roundToInt().toLong()
								} else {
									0
								}
							} else {
								0
							}
						} else {
							0
						}
					} ?: run {
						playingPosition = 0
					}
					fetchEventPlaybackDetails(eventId)
				}
			}
	}

	private fun fetchEventPlaybackDetails(eventId: String) {
		viewModel.fetchEventStreamDetails(eventId)
			.observe(this@VideoPlayerScreen) { eventStreamResponse ->
				fetch(
					eventStreamResponse,
					isLoaderEnabled = true,
					canUserAccessScreen = false,
					shouldBeInBackground = false
				) {
					eventStreamResponse.response?.let { eventStreamData ->
						eventStreamData.data?.let { eventDetails ->
							val organizationName =
								eventDetails.organizationName?.ifBlank { DEFAULT.EMPTY_STRING }
									?: DEFAULT.EMPTY_STRING
							val ticketId = eventDetails.id?.ifBlank { DEFAULT.EMPTY_STRING }
								?: DEFAULT.EMPTY_STRING
							val posterImage =
								eventDetails.presentation.posterUrl?.ifBlank { DEFAULT.EMPTY_STRING }
									?: DEFAULT.EMPTY_STRING
							binding.heroImage.loadImage(posterImage, ImageTags.HERO)
							binding.chatFromPhoneBackground.loadImage(posterImage, ImageTags.HERO)
							fetchCompanions(eventId, organizationName, ticketId)
//							fetchStoryBoard(eventDetails.storyboards.json ?: "")
							eventName = eventDetails.eventName ?: DEFAULT.EMPTY_STRING
							binding.title.text =
								if (!eventDetails.presentation.subtitle.isNullOrBlank()) "${eventDetails.presentation.subtitle} - ${eventDetails.eventName}" else "${eventDetails.eventName}"
							binding.errorTitle.text =
								if (!eventDetails.presentation.subtitle.isNullOrBlank()) "${eventDetails.presentation.subtitle} - ${eventDetails.eventName}" else "${eventDetails.eventName}"
//							viewModel.playbackURL.postValue(eventDetails.playback.streamUrl?.ifBlank { DEFAULT.EMPTY_STRING }
//								?: DEFAULT.EMPTY_STRING)
							setEventData(eventDetails)
						} ?: onErrorPositive(Screens.EXIT_APP)
					} ?: onErrorPositive(Screens.EXIT_APP)
				}
			}
	}

	private fun setEventData(eventDetails: Entities) {
		val status = eventDetails.status?.lowercase() ?: DEFAULT.EMPTY_STRING
		val isChatEnabled = eventDetails.chat.enabled
		artistChannel = eventDetails.chat.chatChannels.artist ?: DEFAULT.EMPTY_STRING
		publishChannel = eventDetails.chat.chatChannels.mainPublish ?: DEFAULT.EMPTY_STRING
		subscribeChannel = eventDetails.chat.chatChannels.mainSubscribe ?: DEFAULT.EMPTY_STRING
		signalChannel = eventDetails.chat.chatChannels.signals ?: DEFAULT.EMPTY_STRING

		isDrmAvailable = false//!eventDetails.playback.widevineUrl.isNullOrBlank()
		val streamURL = eventDetails.playback.streamUrl?.ifBlank { DEFAULT.EMPTY_STRING } ?: DEFAULT.EMPTY_STRING
		val widevineURL = eventDetails.playback.widevineUrl?.ifBlank { streamURL } ?: DEFAULT.EMPTY_STRING
		playbackStream = if (isDrmAvailable) widevineURL else streamURL

		if (isAdAvailable) {
			eventDetails.playback.ads.let { ads ->
				for (ad in ads) {
					val adSource = ad.adUrl?.ifBlank { DEFAULT.EMPTY_STRING }
						?.let { AdSource(AdSourceType.Bitmovin, it) }
					val adItem = adSource?.let { AdItem(ad.adPosition ?: "pre", it) }
					adItem?.let { adItems.add(it) }
				}
			}
		}

		userType = AppUtil.getUserType(eventDetails)
		addStatsTask = Runnable {
			if (player.isPlaying) {
				currentTime = player.currentTime.toString()
				duration = player.duration.toString()
				val playerVersion =
					"ntv"//"${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
				val deviceModel: String = Build.MODEL
				val deviceVendor: String = Build.MANUFACTURER
				val playbackStreamType: String =
					if (player.isLive) EventTypes.LIVE else EventTypes.ON_DEMAND
				val platform: String = getString(R.string.app_platform)

				viewModel.eventId.value?.let { eventId ->
					if (eventId.isNotBlank()) {
						AppPreferences.set(AppConstants.generatedJWT, AppUtil.generateJWT(eventId))
						val addStatsAPIURL = AppPreferences.get(
							AppConstants.userBeaconBaseURL, DEFAULT.EMPTY_STRING
						) + APIConstants.ADD_STATS
						addStats(
							addStatsAPIURL.trim(),
							currentTime,
							duration,
							playerVersion,
							deviceModel,
							deviceVendor,
							playbackStreamType,
							platform,
							userType
						)
					}
				}
			}
			if (this::statsManagement.isInitialized && this::addStatsTask.isInitialized) {
				statsManagement.removeCallbacks(addStatsTask)
				statsManagement.removeCallbacksAndMessages(addStatsTask)
				statsManagement.postDelayed(addStatsTask, 30000)
			}
		}

		if (isChatEnabled.and(
				status.contains(EventTypes.LIVE, true)
					.or(status.contains(EventTypes.UPCOMING, true))
					.or(status.contains(EventTypes.ENDED, true))
			)
		) {
			initPubNub(eventDetails)
			binding.chat.visibility =
				if (isChatEnabled && binding.chatToggle.isSelected) View.VISIBLE else View.GONE
		}

		if (status.contains(EventTypes.ON_DEMAND, true)) {
			binding.standBy.visibility = View.GONE
			viewModel.playbackURL.postValue(playbackStream.ifBlank { DEFAULT.EMPTY_STRING })
		} else {
			when (eventDetails.lastSignal?.lowercase() ?: DEFAULT.EMPTY_STRING) {
				LastSignalTypes.DISCONNECTED, LastSignalTypes.CONNECTING, LastSignalTypes.CONNECTED, LastSignalTypes.IDLE -> {
					binding.standBy.visibility = View.VISIBLE
					binding.topControls.visibility = View.VISIBLE
					binding.liveControls.visibility = View.VISIBLE
					binding.vodControls.visibility = View.GONE
				}

				LastSignalTypes.NO_SIGNAL, LastSignalTypes.ACTIVE, LastSignalTypes.RECORDING -> {
					binding.standBy.visibility = View.GONE
					viewModel.playbackURL.postValue(playbackStream.ifBlank { DEFAULT.EMPTY_STRING })
				}

				LastSignalTypes.STREAM_ENDED -> {
					binding.standBy.visibility = View.GONE
					if (eventDetails.eventReWatchDuration.isNullOrBlank()) {
						binding.reWatch.text = getString(R.string.re_watch_not_available)
					} else {
						val reWatchDuration =
							eventDetails.eventReWatchDuration!!.ifBlank { "0" }.toInt()
						if (reWatchDuration == 0) {
							binding.reWatch.text = getString(R.string.re_watch_not_available)
						} else {
							val reWatchDurationString =
								AppUtil.calculateReWatchTime(reWatchDuration)
							binding.reWatch.text = getString(
								R.string.re_watch_available_for_time, reWatchDurationString
							)
						}
					}
					showError(
						Screens.STREAM_END,
						"Thanks for watching the show!",
						"Hang tight, rewatch available soon"
					)
				}
			}
		}
	}

	private fun fetchStoryBoard(storyBoardURL: String) {
		viewModel.fetchStoryBoard(storyBoardURL)
			.observe(this@VideoPlayerScreen) { storyBoardResponse ->
				fetch(
					storyBoardResponse,
					isLoaderEnabled = true,
					canUserAccessScreen = true,
					shouldBeInBackground = true
				) {
					storyBoardResponse.response?.let { storyBoardImages ->
						if (storyBoardImages.tiles.isNotEmpty()) {
							viewModel.storyBoardURL.postValue(storyBoardImages.url)
//							runOnUiThread {
//								Glide.with(binding.imagePreview.context).asBitmap()
//									.load(storyBoardImages.url)
//									.placeholder(R.drawable.card_background_black)
//									.error(R.drawable.card_background_black)
//									.into(object : CustomTarget<Bitmap>() {
//										override fun onResourceReady(
//											resource: Bitmap, transition: Transition<in Bitmap>?
//										) {
//											viewModel.storyBoard = resource
//										}
//
//										override fun onLoadCleared(placeholder: Drawable?) {
//
//										}
//									})
//							}
//							viewModel.tileWidth.postValue(storyBoardImages.tileWidth)
//							viewModel.tileHeight.postValue(storyBoardImages.tileHeight)
//							viewModel.tiles.postValue(storyBoardImages.tiles)
						}
					}
				}
			}
	}

	private fun fetchCompanions(eventId: String, organizationName: String, ticketId: String) {
		viewModel.fetchCompanions(
			hashMapOf(
				"event_id" to eventId,
				"organization_name" to organizationName,
				"ticket_id" to ticketId
			)
		).observe(this@VideoPlayerScreen) { companionResponse ->
			fetch(
				companionResponse,
				isLoaderEnabled = true,
				canUserAccessScreen = true,
				shouldBeInBackground = true
			) {
				companionResponse.response?.let { companion ->
					companion.data?.let { companionData ->
						binding.qrCode.loadImage(
							APIConstants.QR_CODE_BASE_URL.plus(
								companionData.url.replace(" ", "").trim().lowercase()
							), ImageTags.QR
						)
					}
				}
			}
		}
	}

	private fun addStats(
		addStatsAPIURL: String,
		currentTime: String,
		duration: String,
		playerVersion: String,
		deviceModel: String,
		deviceVendor: String,
		playbackStreamType: String,
		platform: String,
		userType: String,
	) {
		viewModel.addStats(
			addStatsAPIURL,
			currentTime,
			duration,
			playerVersion,
			deviceModel,
			deviceVendor,
			playbackStreamType,
			platform,
			userType,
		).observe(this@VideoPlayerScreen) { addStatsResponse ->
			fetch(
				addStatsResponse,
				isLoaderEnabled = false,
				canUserAccessScreen = true,
				shouldBeInBackground = true
			) {
				addStatsResponse.response?.let {
					Logger.doNothing()
				}
			}
		}
	}

	override fun showError(tag: String, message: String, description: String) {
		viewModel.contentHasLoaded.postValue(true)
		viewModel.errorMessage.postValue(message)
		when (tag) {
			Screens.EXIT_APP -> {
				binding.heroImage.visibility = View.VISIBLE
				binding.blackImage.visibility = View.GONE
				binding.errorDescription.visibility = View.INVISIBLE
				binding.errorDescription.text = description
				binding.errorCode.visibility = View.GONE
				binding.reWatchContainer.visibility = View.GONE
				viewModel.errorPositiveLabel.postValue(getString(R.string.yes_label))
				viewModel.errorNegativeLabel.postValue(getString(R.string.cancel_label))
				viewModel.isErrorPositiveApplicable.postValue(true)
				viewModel.isErrorNegativeApplicable.postValue(true)
			}

			Screens.PLAYER_ERROR -> {
				binding.heroImage.visibility = View.GONE
				binding.blackImage.visibility = View.VISIBLE
				binding.errorDescription.visibility = View.VISIBLE
				binding.errorCode.visibility = View.VISIBLE
				binding.errorDescription.text = description
				binding.reWatchContainer.visibility = View.GONE
				binding.errorCode.text = getString(R.string.error_label)
				viewModel.errorPositiveLabel.postValue(getString(R.string.exit_player_label))
				viewModel.errorNegativeLabel.postValue(getString(R.string.try_again_label))
				viewModel.isErrorPositiveApplicable.postValue(true)
				viewModel.isErrorNegativeApplicable.postValue(true)
			}

			Screens.STREAM_END -> {
				binding.heroImage.visibility = View.GONE
				binding.blackImage.visibility = View.VISIBLE
				binding.errorDescription.visibility = View.VISIBLE
				binding.errorCode.visibility = View.GONE
				binding.errorDescription.text = description
				binding.reWatchContainer.visibility = View.VISIBLE
				viewModel.errorPositiveLabel.postValue(getString(R.string.exit_player_label))
				viewModel.errorNegativeLabel.postValue(getString(R.string.exit_player_label))
				viewModel.isErrorPositiveApplicable.postValue(false)
				viewModel.isErrorNegativeApplicable.postValue(true)
			}

			LastSignalTypes.ON_DEMAND_READY -> {
				binding.heroImage.visibility = View.GONE
				binding.blackImage.visibility = View.VISIBLE
				binding.errorDescription.visibility = View.VISIBLE
				binding.errorCode.visibility = View.GONE
				binding.errorDescription.text = description
				binding.reWatchContainer.visibility = View.VISIBLE
				viewModel.errorPositiveLabel.postValue(getString(R.string.re_watch_now_label))
				viewModel.errorNegativeLabel.postValue(getString(R.string.exit_player_label))
				viewModel.isErrorPositiveApplicable.postValue(true)
				viewModel.isErrorNegativeApplicable.postValue(true)
			}

			else -> {
				binding.heroImage.visibility = View.VISIBLE
				binding.blackImage.visibility = View.GONE
				binding.errorDescription.visibility = View.INVISIBLE
				binding.errorCode.visibility = View.GONE
				binding.reWatchContainer.visibility = View.GONE
				viewModel.errorPositiveLabel.postValue(getString(R.string.ok_label))
				viewModel.errorNegativeLabel.postValue(getString(R.string.cancel_label))
				viewModel.isErrorPositiveApplicable.postValue(true)
				viewModel.isErrorNegativeApplicable.postValue(true)
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

		player.pause()
		binding.errorContainer.visibility = View.VISIBLE
		binding.errorContainer.apply {
			alpha = 0f
			visibility = View.VISIBLE
			animate().alpha(1f)
				.setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
				.setListener(object : Animator.AnimatorListener {
					override fun onAnimationStart(animation: Animator) {
					}

					override fun onAnimationEnd(animation: Animator) {
						binding.negative.requestFocus()
						if (binding.negative.isVisible) {
							binding.negative.postDelayed({
								binding.negative.requestFocus()
							}, AppConstants.keyPressShortDelayTime)
						}
					}

					override fun onAnimationCancel(animation: Animator) {
					}

					override fun onAnimationRepeat(animation: Animator) {
					}
				})
		}
		if (binding.negative.isVisible) {
			binding.negative.postDelayed({
				binding.negative.requestFocus()
			}, AppConstants.keyPressShortDelayTime)
		}
	}

	fun onChatFromPhoneClicked() {
		player.pause()
		binding.chatFromPhoneContainer.visibility = View.VISIBLE
		binding.chatFromPhoneContainer.apply {
			alpha = 0f
			visibility = View.VISIBLE
			animate().alpha(1f)
				.setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
				.setListener(null)
		}
		binding.allDoneAction.postDelayed({
			binding.allDoneAction.requestFocus()
		}, AppConstants.keyPressShortDelayTime)
	}

	fun onAllDoneClicked() {
		player.play()
		binding.chatFromPhoneContainer.visibility = View.GONE
	}

	fun onCancelClicked() {
		player.play()
		binding.chatFromPhoneContainer.visibility = View.GONE
	}

	fun onChatToggleClicked() {
		if (isChatEnabled) {
			binding.chatToggle.isSelected = !binding.chatToggle.isSelected
			binding.chatToggleLabel.text =
				if (binding.chatToggle.isSelected) getString(R.string.turn_chat_off) else getString(
					R.string.turn_chat_on
				)
			binding.chat.visibility =
				if (isChatEnabled && binding.chatToggle.isSelected) View.VISIBLE else View.GONE
		}
	}

	fun onErrorPositive(tag: Any?) {
		when (tag) {
			Screens.EXIT_APP -> {
				Logger.print(
					"Back Pressed on ${
						this@VideoPlayerScreen.localClassName.substringAfterLast(
							"."
						)
					} Finishing Activity"
				)
				if (this::statsManagement.isInitialized && this::addStatsTask.isInitialized) {
					Handler(Looper.getMainLooper()).post(addStatsTask)
				}
				releaseVideoPlayer()
				finish()
			}

			Screens.PLAYER_ERROR -> {
				Logger.print(
					"Exit Player Pressed on ${
						this@VideoPlayerScreen.localClassName.substringAfterLast(
							"."
						)
					} Finishing Activity"
				)
				releaseVideoPlayer()
				finish()
			}

			LastSignalTypes.ON_DEMAND_READY -> {
				Logger.print(
					"Re watch now Pressed on ${
						this@VideoPlayerScreen.localClassName.substringAfterLast(
							"."
						)
					}"
				)
				releaseVideoPlayer()
				binding.standBy.visibility = View.GONE
				loadAppContent()
				timeout.removeCallbacks(trickPlayRunnable)
				timeout.postDelayed(trickPlayRunnable, (inactivitySeconds * 1000).toLong())
				if (trickPlayVisible.value != null && !trickPlayVisible.value!!) {
					trickPlayVisible.value = true
					if (player.isLive) binding.chatFromPhone.requestFocus() else binding.playPause.requestFocus()
				}
				binding.errorContainer.visibility = View.GONE
			}

			else -> {
				binding.errorContainer.visibility = View.GONE
			}
		}
	}

	fun onErrorNegative(tag: Any?) {
		when (tag) {
			Screens.STREAM_END -> {
				Logger.print(
					"Exit Player Pressed on ${
						this@VideoPlayerScreen.localClassName.substringAfterLast(
							"."
						)
					} Finishing Activity"
				)
				releaseVideoPlayer()
				finish()
			}

			Screens.PLAYER_ERROR -> {
				Logger.print(
					"Try Again Pressed on ${
						this@VideoPlayerScreen.localClassName.substringAfterLast(
							"."
						)
					}"
				)
				if (isChatEnabled) chatAdapter.clearAllMessages()
				releaseVideoPlayer()
				binding.standBy.visibility = View.GONE
				loadAppContent()
				timeout.removeCallbacks(trickPlayRunnable)
				timeout.postDelayed(trickPlayRunnable, (inactivitySeconds * 1000).toLong())
				if (trickPlayVisible.value != null && !trickPlayVisible.value!!) {
					trickPlayVisible.value = true
					if (player.isLive) binding.chatFromPhone.requestFocus() else binding.playPause.requestFocus()
				}
				binding.errorContainer.visibility = View.GONE
			}

			else -> {
				player.play()
				binding.errorContainer.visibility = View.GONE
			}
		}
	}

	override fun onDestroy() {
		if (this::pubnub.isInitialized && this::pubNubListener.isInitialized) {
			pubnub.removeListener(listener = pubNubListener)
		}
		releaseVideoPlayer()
		super.onDestroy()
		System.gc()
	}

	override fun onPause() {
		super.onPause()
		if (this::player.isInitialized && player.isPlaying) {
			player.pause()
		}
		if (this::getCurrentTimer.isInitialized && this::timerTask.isInitialized) {
			getCurrentTimer.removeCallbacks(timerTask)
			getCurrentTimer.removeCallbacksAndMessages(timerTask)
		}
		if (this::statsManagement.isInitialized && this::addStatsTask.isInitialized) {
			statsManagement.removeCallbacks(addStatsTask)
			statsManagement.removeCallbacksAndMessages(addStatsTask)
		}
	}

	override fun onResume() {
		super.onResume()
		if (this::player.isInitialized && !player.isPlaying) {
			player.play()
		}
		if (this::getCurrentTimer.isInitialized && this::timerTask.isInitialized) getCurrentTimer.post(
			timerTask
		)
		if (this::statsManagement.isInitialized && this::addStatsTask.isInitialized) statsManagement.post(
			addStatsTask
		)
	}

	override fun onStop() {
		binding.videoPlayer.onStop()
		super.onStop()
	}

	@SuppressLint("RestrictedApi")
	override fun dispatchKeyEvent(event: KeyEvent): Boolean {
		// This method is called on key down and key up, so avoid being called twice
		if (event.action == KeyEvent.ACTION_DOWN) {
			if (handleUserInput(event.keyCode)) {
				return true
			}
		}
		// Make sure to return super.dispatchKeyEvent(event) so that any key not handled yet will work as expected
		return super.dispatchKeyEvent(event)
	}

	// region Private
	private fun handleUserInput(keycode: Int): Boolean {
		return if (binding.errorContainer.visibility == View.VISIBLE) {
			false
		} else if (binding.chatFromPhoneContainer.visibility == View.VISIBLE) {
			false
		} else {
			when (keycode) {
				KeyEvent.KEYCODE_BACK -> {
					handleBack()
					true
				}

				KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
					if (!player.isLive) binding.playPause.performClick()
					true
				}

				KeyEvent.KEYCODE_MEDIA_PLAY -> {
					if (!player.isLive) player.play()
					true
				}

				KeyEvent.KEYCODE_MEDIA_PAUSE -> {
					if (!player.isLive) player.pause()
					true
				}

				KeyEvent.KEYCODE_MEDIA_STOP -> {
					if (!player.isLive) player.onStop()
					true
				}

				KeyEvent.KEYCODE_DPAD_LEFT -> {
					if (!isAdAvailable && !isAdVisible) {
						timeout.removeCallbacks(trickPlayRunnable)
						timeout.postDelayed(trickPlayRunnable, (inactivitySeconds * 1000).toLong())
						if (trickPlayVisible.value != null && !trickPlayVisible.value!!) {
							trickPlayVisible.value = true
							if (!player.isLive) binding.progress.requestFocus() else binding.chatFromPhone.requestFocus()
							true
						} else return false
					} else true
				}

				KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD, KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
					if (!isAdAvailable && !isAdVisible) {
						timeout.removeCallbacks(trickPlayRunnable)
						timeout.postDelayed(trickPlayRunnable, (inactivitySeconds * 1000).toLong())
						if (trickPlayVisible.value != null && !trickPlayVisible.value!!) {
							trickPlayVisible.value = true
							if (!player.isLive) binding.progress.requestFocus() else binding.chatFromPhone.requestFocus()
							true
						} else {
							if (!player.isLive) {
								if (!binding.progress.isFocused) binding.progress.requestFocus() else {
									if (this::player.isInitialized && player.isPlaying) {
										player.pause()
									}
									isScrubVisible = true
									scrubbedPosition -= 10
									if (scrubbedPosition < 0) scrubbedPosition = 0
									binding.progress.setPosition(scrubbedPosition)
									setImagePreview()
								}
							}
							return false
						}
					} else {
						true
					}
				}

				KeyEvent.KEYCODE_DPAD_RIGHT -> {
					if (!isAdAvailable && !isAdVisible) {
						timeout.removeCallbacks(trickPlayRunnable)
						timeout.postDelayed(trickPlayRunnable, (inactivitySeconds * 1000).toLong())
						if (trickPlayVisible.value != null && !trickPlayVisible.value!!) {
							trickPlayVisible.value = true
							if (!player.isLive) binding.progress.requestFocus() else binding.chatFromPhone.requestFocus()
							true
						} else return false
					} else true
				}

				KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD, KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
					if (!isAdAvailable && !isAdVisible) {
						timeout.removeCallbacks(trickPlayRunnable)
						timeout.postDelayed(trickPlayRunnable, (inactivitySeconds * 1000).toLong())
						if (trickPlayVisible.value != null && !trickPlayVisible.value!!) {
							trickPlayVisible.value = true
							if (!player.isLive) binding.progress.requestFocus() else binding.chatFromPhone.requestFocus()
							true
						} else {
							if (!player.isLive) {
								if (!binding.progress.isFocused) binding.progress.requestFocus() else {
									if (this::player.isInitialized && player.isPlaying) {
										player.pause()
									}
									isScrubVisible = true
									scrubbedPosition += 10
									if (scrubbedPosition > player.duration) scrubbedPosition =
										player.duration.toLong()
									binding.progress.setPosition(scrubbedPosition)
									setImagePreview()
								}
							}
							return false
						}
					} else true

				}

				KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_SPACE -> {
					// When a key is hit, cancel the timeout of hiding the trick bar and set it again
					if (!player.isLive && binding.progress.hasFocus()) {
						isScrubVisible = false
						setImagePreview()
						playingPosition = scrubbedPosition
						resumePlayer(scrubbedPosition)
						true
					} else {
						timeout.removeCallbacks(trickPlayRunnable)
						timeout.postDelayed(trickPlayRunnable, (inactivitySeconds * 1000).toLong())
						if (trickPlayVisible.value != null && !trickPlayVisible.value!!) {
							trickPlayVisible.value = true
							if (player.isLive) binding.chatFromPhone.requestFocus() else binding.playPause.requestFocus()
							true
						} else return false
					}

				}

				else -> {
					if (!isAdAvailable && !isAdVisible) {
						timeout.removeCallbacks(trickPlayRunnable)
						timeout.postDelayed(trickPlayRunnable, (inactivitySeconds * 1000).toLong())
						if (trickPlayVisible.value != null && !trickPlayVisible.value!!) {
							trickPlayVisible.value = true
							if (player.isLive) binding.chatFromPhone.requestFocus() else binding.playPause.requestFocus()
							true
						} else return false
					} else true
				}
			}
		}
	}

	private fun createPlayerConfig(): PlayerConfig {
		val playerConfig = PlayerConfig()
		val playbackConfig = PlaybackConfig()
		playbackConfig.isAutoplayEnabled = true
		playerConfig.playbackConfig = playbackConfig
		if (isAdAvailable) {
			val advertisingConfig = AdvertisingConfig(adItems)
			playerConfig.advertisingConfig = advertisingConfig
		}
		return playerConfig
	}

	private fun getPlayerProgress() {
		val currentPosition = player.currentTime
		val totalDuration = player.duration
		if (player.isPlaying) scrubbedPosition = currentPosition.toLong()
		setImagePreview()
		binding.progress.setDuration(totalDuration.toLong())
		binding.progress.setPosition(currentPosition.toLong())
		val currentDuration =
			PeriodFormatterBuilder().printZeroAlways().minimumPrintedDigits(2).appendHours()
				.appendSeparator(":").printZeroAlways().minimumPrintedDigits(2).appendMinutes()
				.appendSeparator(":").printZeroAlways().minimumPrintedDigits(2).appendSeconds()
				.toFormatter().print(
					Period.seconds((totalDuration - currentPosition).toInt()).normalizedStandard()
				)
		binding.currentDuration.text = currentDuration ?: "00:00:00"
		if (player.isPlaying) {
			binding.videoPlayer.postDelayed(
				{ getPlayerProgress() },
				if (isAdAvailable && isAdVisible) IntValue.NUMBER_500.toLong() else IntValue.NUMBER_1000.toLong()
			)
		}
	}
}
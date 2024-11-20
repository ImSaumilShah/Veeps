package com.veeps.app.feature.event.ui

import android.os.Bundle
import android.text.Html
import android.view.KeyEvent
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.content.ContextCompat
import androidx.leanback.widget.BaseGridView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE
import androidx.media3.common.C.USAGE_MEDIA
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import androidx.recyclerview.widget.PagerSnapHelper
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.veeps.app.R
import com.veeps.app.core.BaseFragment
import com.veeps.app.databinding.FragmentEventDetailsScreenBinding
import com.veeps.app.extension.fadeInNow
import com.veeps.app.extension.fadeOutNow
import com.veeps.app.extension.isFireTV
import com.veeps.app.extension.isGreaterThan
import com.veeps.app.extension.loadImage
import com.veeps.app.extension.showToast
import com.veeps.app.feature.contentRail.adapter.ContentRailsAdapter
import com.veeps.app.feature.contentRail.model.Entities
import com.veeps.app.feature.contentRail.model.Products
import com.veeps.app.feature.contentRail.model.RailData
import com.veeps.app.feature.event.viewModel.EventViewModel
import com.veeps.app.util.APIConstants
import com.veeps.app.util.AppAction
import com.veeps.app.util.AppConstants
import com.veeps.app.util.AppPreferences
import com.veeps.app.util.AppUtil
import com.veeps.app.util.ButtonLabels
import com.veeps.app.util.CardTypes
import com.veeps.app.util.DEFAULT
import com.veeps.app.util.DateTimeCompareDifference
import com.veeps.app.util.EntityTypes
import com.veeps.app.util.ImageTags
import com.veeps.app.util.IntValue
import com.veeps.app.util.Logger
import com.veeps.app.util.PurchaseResponseStatus
import com.veeps.app.util.Screens
import com.veeps.app.widget.navigationMenu.NavigationItems
import io.noties.markwon.Markwon
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.json.JSONObject
import kotlin.math.roundToInt

class EventScreen : BaseFragment<EventViewModel, FragmentEventDetailsScreenBinding>() {

	private lateinit var player: ExoPlayer
	private var entity = ""
	private var entityId = ""
	private var entityScope = ""
	private var posterImage = ""
	private var trailerUrl = ""
	private var product: Products = Products()
	private var eventDetails: Entities = Entities()
	private var isEventPurchased: Boolean = false
	private var rail: ArrayList<RailData> = arrayListOf()
	private var recommendedRailList: ArrayList<RailData> = arrayListOf()
	private var recommendedRailData: ArrayList<RailData> = arrayListOf()
	private val action by lazy {
		object : AppAction {
			override fun onAction() {
				Logger.print(
					"Action performed on ${
						this@EventScreen.javaClass.name.substringAfterLast(".")
					}"
				)
			}
		}
	}

	override fun getViewBinding(): FragmentEventDetailsScreenBinding =
		FragmentEventDetailsScreenBinding.inflate(layoutInflater)

	override fun onDestroyView() {
		viewModelStore.clear()
		releaseVideoPlayer()
		super.onDestroyView()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.apply {
			event = viewModel
			eventScreen = this@EventScreen
			lifecycleOwner = viewLifecycleOwner
			lifecycle.addObserver(viewModel)
			loader.visibility = View.VISIBLE
			logo.requestFocus()
			rail = arrayListOf()
			subscribe.visibility = View.GONE
			myShows.visibility = View.GONE
			carousel.visibility = View.INVISIBLE
			resumeProgress.visibility = View.INVISIBLE
			darkBackground.visibility = View.VISIBLE
			paymentLoader.visibility = View.GONE
		}
		notifyAppEvents()
		loadAppContent()
	}

	private fun setupVideoPlayer() {
		releaseVideoPlayer()
		player = context?.let { context -> ExoPlayer.Builder(context).build() }!!
		player.addListener(object : Player.Listener {
			override fun onIsPlayingChanged(isPlaying: Boolean) {
				if (isPlaying) {
					binding.heroImage.fadeOutNow(IntValue.NUMBER_1000)
				} else {
					binding.heroImage.fadeInNow(IntValue.NUMBER_1000)
				}
				super.onIsPlayingChanged(isPlaying)
			}

			override fun onPlaybackStateChanged(playbackState: Int) {
				if (playbackState == Player.STATE_ENDED) {
					binding.heroImage.fadeInNow(IntValue.NUMBER_1000)
					releaseVideoPlayer()
				}
				super.onPlaybackStateChanged(playbackState)
			}

			override fun onPlayerError(error: PlaybackException) {
				releaseVideoPlayer()
				super.onPlayerError(error)
			}
		})
		player.setAudioAttributes(
			AudioAttributes.Builder().setUsage(USAGE_MEDIA).setContentType(AUDIO_CONTENT_TYPE_MOVIE)
				.build(), true
		)
		player.addAnalyticsListener(EventLogger())
		binding.videoPlayer.player = player
		player.repeatMode = Player.REPEAT_MODE_ONE
		if (trailerUrl.isNotBlank()) {
			player.setMediaItem(MediaItem.fromUri(trailerUrl))
			player.prepare()
			player.play()
			if (homeViewModel.isNavigationMenuVisible.value!! || homeViewModel.isErrorVisible.value!! || binding.darkBackground.visibility == View.VISIBLE) {
				if (this::player.isInitialized && player.isPlaying) {
					player.pause()
				}
			}
		}
	}

	private fun releaseVideoPlayer() {
		if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0)) {
			player.playWhenReady = false
			player.pause()
			player.release()
		}
	}

	private fun setupBlur() {
		binding.primary.setupWith(binding.layoutContainer).setBlurRadius(12.5f)
		binding.primary.outlineProvider = ViewOutlineProvider.BACKGROUND
		binding.primary.clipToOutline = true
		binding.primary.setBlurEnabled(true)

		binding.subscribe.setupWith(binding.layoutContainer).setBlurRadius(12.5f)
		binding.subscribe.outlineProvider = ViewOutlineProvider.BACKGROUND
		binding.subscribe.clipToOutline = true
		binding.subscribe.setBlurEnabled(true)

		binding.myShows.setupWith(binding.layoutContainer).setBlurRadius(12.5f)
		binding.myShows.outlineProvider = ViewOutlineProvider.BACKGROUND
		binding.myShows.clipToOutline = true
		binding.myShows.setBlurEnabled(true)
	}

	private fun loadAppContent() {
		helper.completelyHideNavigationMenu()
		helper.fetchAllWatchListEvents()
		if (arguments != null) {
			entity = requireArguments().getString("entity").toString()
			entityId = requireArguments().getString("entityId").toString()
			viewModel.eventId = entityId
			entityScope = "$entity.$entityId"
			entity = entity.plus("s")
		}
		binding.primaryLabel.isSelected = false
		setupBlur()
		fetchEventStreamDetails()
	}

	private fun notifyAppEvents() {
		binding.primary.setOnFocusChangeListener { _, hasFocus ->
			context?.let { context ->
				if (binding.primary.tag == ButtonLabels.PLAY || binding.primary.tag == ButtonLabels.RESUME) {
					binding.primaryLabel.compoundDrawables.forEach { drawable ->
						drawable?.setTint(
							ContextCompat.getColor(
								context, if (hasFocus) R.color.dark_black else R.color.white
							)
						)
					}
				}
				binding.primaryLabel.setTextColor(
					ContextCompat.getColor(
						context,
						if (hasFocus) R.color.dark_black else if (binding.primaryLabel.isSelected) R.color.white_30 else R.color.white
					)
				)

			}
		}
		binding.subscribe.setOnFocusChangeListener { _, hasFocus ->
			context?.let { context ->
				binding.subscribeLabel.setTextColor(
					ContextCompat.getColor(
						context,
						if (hasFocus) R.color.dark_black else if (binding.subscribeLabel.isSelected) R.color.white_30 else R.color.white
					)
				)
			}
		}
		binding.myShows.setOnFocusChangeListener { _, hasFocus ->
			binding.myShows.tag = hasFocus
			context?.let { context ->
				binding.myShowsLabel.compoundDrawables.forEach { drawable ->
					drawable?.setTint(
						ContextCompat.getColor(
							context,
							if (hasFocus.or(binding.myShows.isFocused)
									.or(binding.myShows.hasFocus())
							) R.color.dark_black else R.color.white
						)
					)
				}
				binding.myShowsLabel.setTextColor(
					ContextCompat.getColor(
						context,
						if (hasFocus.or(binding.myShows.isFocused)
								.or(binding.myShows.hasFocus())
						) R.color.dark_black else R.color.white
					)
				)
			}
		}

		binding.primary.setOnKeyListener { _, keyCode, keyEvent ->
			if (keyEvent.action == KeyEvent.ACTION_DOWN) {
				if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
					if (rail.none { it.entities.isNotEmpty() }) {
						if (!binding.description.text.isNullOrBlank()) {
							if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && player.isPlaying) {
								player.pause()
							}
							binding.description.requestFocus()
							binding.darkBackground.visibility = View.VISIBLE
							binding.carousel.visibility = View.GONE
						}
					}
				}
			}
			return@setOnKeyListener false
		}

		binding.subscribe.setOnKeyListener { _, keyCode, keyEvent ->
			if (keyEvent.action == KeyEvent.ACTION_DOWN) {
				if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
					if (rail.none { it.entities.isNotEmpty() }) {
						if (!binding.description.text.isNullOrBlank()) {
							if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && player.isPlaying) {
								player.pause()
							}
							binding.description.requestFocus()
							binding.darkBackground.visibility = View.VISIBLE
							binding.carousel.visibility = View.GONE
						}
					}
				}
			}
			return@setOnKeyListener false
		}

		binding.myShows.setOnKeyListener { _, keyCode, keyEvent ->
			if (keyEvent.action == KeyEvent.ACTION_DOWN) {
				if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
					if (rail.none { it.entities.isNotEmpty() }) {
						if (!binding.description.text.isNullOrBlank()) {
							if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && player.isPlaying) {
								player.pause()
							}
							binding.description.requestFocus()
							binding.darkBackground.visibility = View.VISIBLE
							binding.carousel.visibility = View.GONE
						}
					}
				}
			}
			return@setOnKeyListener false
		}

		binding.scrollView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
			binding.optionsContainer.visibility = if (scrollY > 0) View.GONE else View.VISIBLE
		}

		binding.description.setOnKeyListener { _, keyCode, keyEvent ->
			if (keyEvent.action == KeyEvent.ACTION_DOWN) {
				if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
					if (binding.scrollView.scrollY == 0) {
						if (!rail.none { it.entities.isNotEmpty() }) {
							binding.listing.visibility = View.VISIBLE
							binding.listing.requestFocus()
						} else {
							binding.carousel.visibility = View.VISIBLE
							if (binding.primary.visibility == View.VISIBLE) binding.primary.requestFocus() else binding.myShows.requestFocus()
							binding.darkBackground.visibility = View.GONE
							if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && !player.isPlaying && homeViewModel.isErrorVisible.value?.equals(
									false
								) == true && binding.darkBackground.visibility == View.GONE
							) {
								player.play()
							}
						}
					}
				} else if (!recommendedRailList.none { it.entities.isNotEmpty() } && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
					binding.listing.visibility = View.GONE
					binding.description.visibility = View.GONE
					binding.scrollView.visibility = View.GONE
					binding.optionsContainer.visibility = View.GONE
					binding.recommendationListing.requestFocus()
				}
			}
			return@setOnKeyListener false
		}

		homeViewModel.focusItem.observe(viewLifecycleOwner) { hasFocus ->
			if (hasFocus && !binding.description.text.isNullOrBlank()) {
				binding.description.visibility = View.VISIBLE
				binding.scrollView.visibility = View.VISIBLE
				binding.optionsContainer.visibility = View.VISIBLE
				binding.description.requestFocus()
				if (!rail.none { it.entities.isNotEmpty() }) {
					binding.listing.visibility = View.GONE
				} else {
					if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && player.isPlaying) {
						player.pause()
					}
					binding.darkBackground.visibility = View.VISIBLE
					binding.carousel.visibility = View.GONE
				}
			}
		}
		homeViewModel.isNavigationMenuVisible.observe(viewLifecycleOwner) { isNavigationMenuVisible ->
			if (isNavigationMenuVisible) {
				if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && player.isPlaying) {
					player.pause()
				}
			} else {
				if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && !player.isPlaying && homeViewModel.isErrorVisible.value?.equals(
						false
					) == true && binding.darkBackground.visibility == View.GONE
				) {
					player.play()
				}
			}
		}
		homeViewModel.playerShouldRelease.observe(viewLifecycleOwner) { playerShouldRelease ->
			if (playerShouldRelease) {
				releaseVideoPlayer()
			}
		}
		homeViewModel.playerShouldPause.observe(viewLifecycleOwner) { playerShouldPause ->
			if (playerShouldPause) {
				if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && player.isPlaying) {
					player.pause()
				}
			} else {
				if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && !player.isPlaying && homeViewModel.isErrorVisible.value?.equals(
						false
					) == true && binding.darkBackground.visibility == View.GONE
				) {
					player.play()
				}
			}
		}
		homeViewModel.translateCarouselToTop.observe(viewLifecycleOwner) { shouldTranslate ->
			if (shouldTranslate && viewModel.isVisible.value == true) {
				if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && player.isPlaying) {
					player.pause()
				}
				binding.darkBackground.visibility = View.VISIBLE
				binding.carousel.visibility = View.GONE
			}
		}
		homeViewModel.translateCarouselToBottom.observe(viewLifecycleOwner) { shouldTranslate ->
			if (shouldTranslate && viewModel.isVisible.value == true) {
				binding.darkBackground.visibility = View.GONE
				binding.carousel.visibility = View.VISIBLE
				if (binding.primary.visibility == View.VISIBLE) binding.primary.requestFocus() else binding.myShows.requestFocus()
				if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && !player.isPlaying && homeViewModel.isErrorVisible.value?.equals(
						false
					) == true && binding.darkBackground.visibility == View.GONE
				) {
					player.play()
				}
			}
		}

		homeViewModel.purchaseAction.observe(viewLifecycleOwner) {
			if (it.isNullOrBlank()) {
				binding.paymentLoader.visibility = View.GONE
			} else if (it == PurchaseResponseStatus.SUCCESS) {
				if (!homeViewModel.isSubscription) createOrder()
			} else if (it == PurchaseResponseStatus.SUCCESS_WITH_PENDING_PURCHASE) {
				fetchEventStreamDetails()
			} else {
				var errorMessage = getString(R.string.unknown_error)
				when (it) {
					PurchaseResponseStatus.FAILED -> {
						errorMessage = getString(R.string.payment_failed)
					}

					PurchaseResponseStatus.INVALID_SKU -> {
						errorMessage = getString(R.string.product_not_available)
					}

					PurchaseResponseStatus.NOT_SUPPORTED -> {
						errorMessage = getString(R.string.iap_not_supported)
					}

					PurchaseResponseStatus.PENDING -> {
						errorMessage = getString(R.string.payment_pending)
					}

					PurchaseResponseStatus.ALREADY_PURCHASED -> {
						errorMessage = getString(R.string.ticket_already_purchased)
					}
				}

				if (it != PurchaseResponseStatus.NONE) {
					helper.showErrorOnScreen(APIConstants.GENERATE_NEW_ORDER, errorMessage)
					homeViewModel.purchaseAction.postValue(PurchaseResponseStatus.NONE)
				}

				AppPreferences.remove(AppConstants.reservedId)
				AppPreferences.remove(AppConstants.receiptId)
				AppPreferences.remove(AppConstants.orderId)
				AppPreferences.remove(AppConstants.requestId)
				AppPreferences.remove(AppConstants.SKUId)
				homeViewModel.isPaymentInProgress = false
				binding.paymentLoader.visibility = View.GONE
			}
		}

		homeViewModel.updateUserStat.observe(viewLifecycleOwner) { doesUpdateRequired ->
			if (doesUpdateRequired && binding.primary.tag == ButtonLabels.PLAY) {
				fetchUserStats()
				homeViewModel.updateUserStat.postValue(false)
			}
		}

		viewModel.isVisible.observeForever { isVisible ->
			Logger.print(
				"Visibility Changed to $isVisible On ${
					this@EventScreen.javaClass.name.substringAfterLast(".")
				}"
			)
			if (isVisible) {
				helper.selectNavigationMenu(NavigationItems.NO_MENU)
				helper.completelyHideNavigationMenu()
				if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && !player.isPlaying && homeViewModel.isErrorVisible.value?.equals(
						false
					) == true && binding.darkBackground.visibility == View.GONE
				) {
					player.play()
				}
			} else {
				if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && player.isPlaying) {
					player.pause()
				}
			}
		}
	}

	private fun fetchEventProductDetails() {
		viewModel.fetchEventProductDetails().observe(viewLifecycleOwner) { eventProductResponse ->
			fetch(
				eventProductResponse,
				isLoaderEnabled = true,
				canUserAccessScreen = true,
				shouldBeInBackground = true
			) {
				eventProductResponse.response?.let { productsResponse ->
					if (productsResponse.products.isNotEmpty()) {
						productsResponse.products.forEach { product ->
							this.product = product
							binding.primaryLabel.text =
								ButtonLabels.BUY_TICKET.plus(product.displayPrice)
							if (context?.isFireTV == true) {
								PurchasingService.getProductData(hashSetOf(product.productCode))
							} else {
								homeViewModel.purchaseAction.postValue(PurchaseResponseStatus.NOT_SUPPORTED)
							}
						}
					} else {
						product = Products()
						binding.primaryLabel.text =
							ButtonLabels.UNAVAILABLE.also { binding.primaryLabel.isSelected }
					}
				} ?: run {
					binding.primaryLabel.text =
						ButtonLabels.UNAVAILABLE.also { binding.primaryLabel.isSelected }
				}
			}
		}
	}

	private fun fetchEventStreamDetails() {
		viewModel.fetchEventStreamDetails().observe(viewLifecycleOwner) { eventStreamResponse ->
			fetch(
				eventStreamResponse,
				isLoaderEnabled = true,
				canUserAccessScreen = true,
				shouldBeInBackground = true
			) {
				eventStreamResponse.response?.let { eventStreamData ->
					eventStreamData.data?.let { eventDetails ->
						isEventPurchased = true
						fetchRecommendedContent(eventDetails)
					} ?: fetchEventDetails()
				} ?: fetchEventDetails()
			}
		}
	}

	private fun fetchEventDetails() {
		viewModel.fetchEventDetails().observe(viewLifecycleOwner) { eventResponse ->
			fetch(
				eventResponse,
				isLoaderEnabled = true,
				canUserAccessScreen = true,
				shouldBeInBackground = true
			) {
				eventResponse.response?.let { eventStreamData ->
					eventStreamData.data?.let { eventDetails ->
						fetchRecommendedContent(eventDetails)
					} ?: helper.goBack()
				} ?: helper.goBack()
			}
		}
	}

	private fun fetchRecommendedContent(eventDetails: Entities) {
		recommendedRailData = arrayListOf()
		recommendedRailList = arrayListOf()
		viewModel.fetchRecommendedContent().observe(viewLifecycleOwner) { recommendedContent ->
			fetch(
				recommendedContent,
				isLoaderEnabled = false,
				canUserAccessScreen = true,
				shouldBeInBackground = true,
			) {
				recommendedContent.response?.let { recommendedRailResponse ->
					if (recommendedRailResponse.railData.isNotEmpty()) {
						recommendedRailData = recommendedRailResponse.railData
					}
				}
				setEventDetails(eventDetails)
			}
		}
	}

	private fun setEventDetails(eventDetails: Entities) {
		binding.logo.requestFocus()
		this.eventDetails = eventDetails
		binding.primary.alpha = 0.1f
		var primaryLabelText: String
		when (AppUtil.getPrimaryLabelText(eventDetails, Screens.EVENT, isEventPurchased).also {
			primaryLabelText = it
			binding.primary.tag = primaryLabelText
		}) {
			ButtonLabels.PLAY -> {
				primaryLabelText = DEFAULT.EMPTY_STRING
				fetchUserStats()
			}

			ButtonLabels.JOIN_LIVE -> {
				binding.primaryLabel.compoundDrawablePadding =
					resources.getDimensionPixelSize(R.dimen.dp8)
				binding.primaryLabel.setCompoundDrawablesRelativeWithIntrinsicBounds(
					R.drawable.rounded_card_background_red, 0, 0, 0
				)
			}

			ButtonLabels.BUY_TICKET -> {
				primaryLabelText = DEFAULT.EMPTY_STRING
				fetchEventProductDetails()
			}
		}
		binding.primaryLabel.text = primaryLabelText.also { label ->
			binding.primaryLabel.isSelected = label == ButtonLabels.UNAVAILABLE
		}
		binding.primary.visibility = if (AppPreferences.get(
				AppConstants.userSubscriptionStatus, "none"
			) == "none" && primaryLabelText == ButtonLabels.UNAVAILABLE && eventDetails.access.containsAll(arrayListOf("veeps_plus"))
		) View.GONE else View.VISIBLE
		binding.primary.alpha = 1.0f
		binding.myShows.visibility = View.VISIBLE
		binding.subscribe.alpha = 1.0f
		binding.subscribeLabel.text = getString(
			R.string.free_for_subscriber
		)
		binding.subscribe.visibility = View.GONE

		setupMyShows(isAdded = homeViewModel.watchlistIds.contains(eventDetails.id))

		val currentDate = DateTime.now()
		val streamStartAtDate =
			DateTime(eventDetails.eventStreamStartsAt?.ifBlank { DateTime.now().toString() }
				?: DateTime.now().toString(), DateTimeZone.UTC).withZone(
				DateTimeZone.getDefault()
			).toDateTime()
		binding.date.text = streamStartAtDate.toString("MMM dd, yyyy${DEFAULT.SEPARATOR}ha")
		if (AppUtil.compare(
				streamStartAtDate, currentDate
			) == DateTimeCompareDifference.GREATER_THAN
		) {
			binding.date.visibility = View.VISIBLE
			binding.liveNow.visibility = View.GONE
		} else {
			binding.date.visibility = View.VISIBLE
			binding.liveNow.visibility = View.GONE
		}
		val title = eventDetails.eventName ?: DEFAULT.EMPTY_STRING
		binding.title.text = title
		var description =
			eventDetails.eventDescription?.ifBlank { DEFAULT.EMPTY_STRING } ?: DEFAULT.EMPTY_STRING
		description += "\n\n\n\n\n"
		context?.let {
			Markwon.create(it).setMarkdown(binding.description, description)
		} ?: run {
			binding.description.text = Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY)
				.ifBlank { DEFAULT.EMPTY_STRING }
		}

		posterImage = eventDetails.presentation.posterUrl?.ifBlank { DEFAULT.EMPTY_STRING }
			?: DEFAULT.EMPTY_STRING
		val logoImage = eventDetails.presentation.logoUrl?.ifBlank { DEFAULT.EMPTY_STRING }
			?: DEFAULT.EMPTY_STRING
		binding.contentBadge.text = AppUtil.getContentBadge(eventDetails.presentation.contentBadges)

		when (val reWatchLabel = AppUtil.getRewatchLabelText(entity = eventDetails)) {
			DEFAULT.EMPTY_STRING -> {
				binding.reWatch.visibility = View.GONE
				binding.reWatchImage.visibility = View.GONE
			}

			else -> {
				binding.reWatch.text = reWatchLabel
			}
		}

		setupRails()

		val videoPreviewTreeMap = eventDetails.videoPreviews ?: false
		trailerUrl = DEFAULT.EMPTY_STRING
		if (videoPreviewTreeMap is LinkedTreeMap<*, *>) {
			if (videoPreviewTreeMap.isNotEmpty()) {
				val jsonObject = Gson().toJsonTree(videoPreviewTreeMap).asJsonObject
				if (jsonObject != null && !jsonObject.isJsonNull && !jsonObject.isEmpty) {
					val videoPreviewString: String = Gson().toJson(jsonObject)
					val videoPreview = JSONObject(videoPreviewString)
					if (videoPreview.has("high")) {
						trailerUrl = videoPreview.getString("high")
					}
				}
			}
		}
		binding.carouselLogo.loadImage(logoImage, ImageTags.LOGO)
		binding.heroImage.loadImage(posterImage, ImageTags.HERO)
		if (trailerUrl.isBlank()) {
			releaseVideoPlayer()
		} else {
			setupVideoPlayer()
		}
		binding.darkBackground.visibility = View.GONE
		binding.carousel.visibility = View.VISIBLE
		if (binding.primary.visibility == View.VISIBLE) binding.primary.requestFocus() else binding.myShows.requestFocus()
		binding.logo.isFocusable = false
		binding.logo.isFocusableInTouchMode = false
		homeViewModel.purchaseAction.postValue(PurchaseResponseStatus.NONE)
	}

	private fun setupRails() {
		rail = arrayListOf()
		val artistEntities: ArrayList<Entities> = arrayListOf()
		eventDetails.lineup.forEach { lineup ->
			artistEntities.add(Entities(id = lineup.id?.ifBlank { DEFAULT.EMPTY_STRING }
				?: DEFAULT.EMPTY_STRING,
				name = lineup.name?.ifBlank { DEFAULT.EMPTY_STRING } ?: DEFAULT.EMPTY_STRING,
				landscapeUrl = lineup.landscapeUrl?.ifBlank { DEFAULT.EMPTY_STRING }
					?: DEFAULT.EMPTY_STRING,
				portraitUrl = lineup.portraitUrl?.ifBlank { DEFAULT.EMPTY_STRING }
					?: DEFAULT.EMPTY_STRING,
				logoUrl = lineup.logoUrl?.ifBlank { DEFAULT.EMPTY_STRING } ?: DEFAULT.EMPTY_STRING))
		}
		val artistRail = RailData(
			name = getString(
				R.string.featuring_artist
			),
			entities = artistEntities,
			cardType = CardTypes.CIRCLE,
			entitiesType = EntityTypes.ARTIST
		)
		rail.add(artistRail)

		val venueEntities: ArrayList<Entities> = arrayListOf()
		if (eventDetails.venueId != null) {
			venueEntities.add(Entities(id = eventDetails.venueId,
				name = eventDetails.venueName,
				landscapeUrl = eventDetails.venueLandscapeUrl,
				portraitUrl = eventDetails.venuePortraitUrl?.ifBlank { DEFAULT.EMPTY_STRING }
					?: DEFAULT.EMPTY_STRING,
				logoUrl = eventDetails.venueLogoUrl?.ifBlank { DEFAULT.EMPTY_STRING }
					?: DEFAULT.EMPTY_STRING))
			val venueRail = RailData(
				name = getString(
					R.string.featuring_venue
				),
				entities = venueEntities,
				cardType = CardTypes.CIRCLE,
				entitiesType = EntityTypes.VENUE
			)
			rail.add(venueRail)
		}

		if (recommendedRailData.isNotEmpty()) {
			recommendedRailData.let { recommendedData ->
				val railData = recommendedData.first()
				val recommendedRail = RailData(
					name = railData.name,
					entities = railData.entities,
					cardType = CardTypes.PORTRAIT,
					entitiesType = EntityTypes.EVENT
				)
				recommendedRailList.add(recommendedRail)
			}
		}
		if (rail.none { it.entities.isNotEmpty() }) {
			binding.listing.visibility = View.GONE
		} else {
			binding.listing.apply {
				layoutParams.height =
					if (rail.size == 2) context.resources.getDimensionPixelSize(R.dimen.row_height_rail_circle_without_follow) else context.resources.getDimensionPixelSize(
						R.dimen.row_height_default
					)
				itemAnimator = null
				setNumColumns(1)
				setHasFixedSize(true)
				windowAlignment = BaseGridView.WINDOW_ALIGN_HIGH_EDGE
				windowAlignmentOffsetPercent = 0f
				isItemAlignmentOffsetWithPadding = true
				itemAlignmentOffsetPercent = 0f
				adapter = ContentRailsAdapter(rails = rail, helper, Screens.EVENT, action)
				onFlingListener = PagerSnapHelper()
			}
			binding.listing.visibility = View.VISIBLE
		}
		if (recommendedRailList.none { it.entities.isNotEmpty() }) {
			binding.recommendationListing.visibility = View.GONE
		} else {
			binding.recommendationListing.apply {
				layoutParams.height = context.resources.getDimensionPixelSize(R.dimen.row_height_rail)
				itemAnimator = null
				setNumColumns(1)
				setHasFixedSize(true)
				windowAlignment = BaseGridView.WINDOW_ALIGN_HIGH_EDGE
				windowAlignmentOffsetPercent = 0f
				isItemAlignmentOffsetWithPadding = true
				itemAlignmentOffsetPercent = 0f
				adapter = ContentRailsAdapter(rails = recommendedRailList, helper, Screens.EVENT, action, true)
				onFlingListener = PagerSnapHelper()
			}
			binding.recommendationListing.visibility = View.VISIBLE
		}
	}

	private fun fetchUserStats() {
		val userStatsAPIURL = AppPreferences.get(
			AppConstants.userBeaconBaseURL, DEFAULT.EMPTY_STRING
		) + APIConstants.FETCH_USER_STATS
		AppPreferences.set(
			AppConstants.generatedJWT, AppUtil.generateJWT(eventIds = viewModel.eventId)
		)
		viewModel.fetchUserStats(userStatsAPIURL, eventIds = viewModel.eventId)
			.observe(viewLifecycleOwner) { userStatsDetails ->
				fetch(
					userStatsDetails,
					isLoaderEnabled = false,
					canUserAccessScreen = true,
					shouldBeInBackground = true,
				) {
					userStatsDetails.response?.let { userStatsResponse ->
						if (userStatsResponse.userStats.isNotEmpty()) {
							val stats =
								userStatsResponse.userStats.filter { it.eventId == viewModel.eventId }
							if (stats.size == 1) {
								val currentStat = (stats[0].cursor / stats[0].duration)
								if (currentStat < 95 && currentStat > 0) {
									binding.primaryLabel.text = ButtonLabels.RESUME
									binding.resumeProgress.visibility = View.VISIBLE
									binding.resumeProgress.max = stats[0].duration.roundToInt()
									binding.resumeProgress.progress = stats[0].cursor.roundToInt()
								} else {
									binding.primaryLabel.text = ButtonLabels.PLAY
									binding.resumeProgress.visibility = View.INVISIBLE
								}
							} else {
								binding.primaryLabel.text = ButtonLabels.PLAY
								binding.resumeProgress.visibility = View.INVISIBLE
							}
						} else {
							binding.primaryLabel.text = ButtonLabels.PLAY
							binding.resumeProgress.visibility = View.INVISIBLE
						}
					} ?: run {
						binding.primaryLabel.text = ButtonLabels.PLAY
						binding.resumeProgress.visibility = View.INVISIBLE
					}

					binding.primaryLabel.compoundDrawablePadding =
						resources.getDimensionPixelSize(R.dimen.dp4)
					binding.primaryLabel.setCompoundDrawablesRelativeWithIntrinsicBounds(
						if (binding.primary.hasFocus() || binding.primary.isFocused) R.drawable.play_black else R.drawable.play_white,
						0,
						0,
						0
					)
					context?.let { context ->
						binding.primaryLabel.compoundDrawables.forEach { drawable ->
							drawable?.setTint(
								ContextCompat.getColor(
									context,
									if ((binding.primary.isFocused).or(binding.primary.hasFocus())) R.color.dark_black else R.color.white
								)
							)
						}
					}
					context?.let { context ->
						binding.subscribeLabel.compoundDrawables.forEach { drawable ->
							drawable?.setTint(
								ContextCompat.getColor(
									context,
									if ((binding.subscribe.isFocused).or(binding.subscribe.hasFocus())) R.color.dark_black else R.color.white
								)
							)
						}
					}
				}
			}
	}

	fun addRemoveWatchListEvent(eventId: String) {
		if (eventId.isNotBlank()) {
			viewModel.addRemoveWatchListEvent(
				hashMapOf("event_id" to eventId), binding.myShows.isSelected
			).observe(viewLifecycleOwner) { addWatchList ->
				fetch(
					addWatchList,
					isLoaderEnabled = false,
					canUserAccessScreen = true,
					shouldBeInBackground = true,
				) {
					setupMyShows(!binding.myShows.isSelected)
				}
			}
		}
	}

	private fun claimFreeTicketForEvent() {
		viewModel.claimFreeTicketForEvent().observe(viewLifecycleOwner) { claimResponse ->
			fetch(
				claimResponse,
				isLoaderEnabled = true,
				canUserAccessScreen = true,
				shouldBeInBackground = true,
			) {
				claimResponse.response?.let {
					fetchEventStreamDetails()
				}
			}
		}
	}

	private fun setupMyShows(isAdded: Boolean) {
		binding.myShows.isSelected = isAdded
		binding.myShowsLabel.setCompoundDrawablesRelativeWithIntrinsicBounds(
			if (binding.myShows.isSelected) {
				if (binding.myShows.hasFocus()) R.drawable.check_black else R.drawable.check_white
			} else {
				if (binding.myShows.hasFocus()) R.drawable.add_black else R.drawable.add_white
			}, 0, 0, 0
		)
		context?.let { context ->
			binding.myShowsLabel.compoundDrawables.forEach { drawable ->
				drawable?.setTint(
					ContextCompat.getColor(
						context,
						if (binding.myShows.isFocused.or(binding.myShows.hasFocus())) R.color.dark_black else R.color.white
					)
				)
			}
			binding.myShowsLabel.setTextColor(
				ContextCompat.getColor(
					context,
					if (binding.myShows.isFocused.or(binding.myShows.hasFocus())) R.color.dark_black else R.color.white
				)
			)
		}
	}

	private fun clearAllReservations() {
		viewModel.clearAllReservations().observe(viewLifecycleOwner) { clearAllReservations ->
			fetch(
				clearAllReservations,
				isLoaderEnabled = false,
				canUserAccessScreen = true,
				shouldBeInBackground = true
			) {
				setNewReservation()
			}
		}
	}

	private fun setNewReservation() {
		viewModel.setNewReservation(hashMapOf("item_id" to (product.id ?: "")))
			.observe(viewLifecycleOwner) { setNewReservation ->
				fetch(
					setNewReservation,
					isLoaderEnabled = false,
					canUserAccessScreen = true,
					shouldBeInBackground = true
				) {
					setNewReservation.response?.let {
						it.data?.let { reservation ->
							AppPreferences.set(AppConstants.reservedId, reservation.id)
							generateNewOrder()
						} ?: run {
							homeViewModel.purchaseAction.postValue(PurchaseResponseStatus.FAILED)
						}
					} ?: run {
						homeViewModel.purchaseAction.postValue(PurchaseResponseStatus.FAILED)
					}
				}
			}
	}

	private fun generateNewOrder() {
		viewModel.generateNewOrder().observe(viewLifecycleOwner) { generateNewOrder ->
			fetch(
				generateNewOrder,
				isLoaderEnabled = false,
				canUserAccessScreen = true,
				shouldBeInBackground = true
			) {
				generateNewOrder.response?.let {
					it.data?.let { order ->
						AppPreferences.set(AppConstants.orderId, order.id)
						if (context?.isFireTV == true) {
							val requestId = PurchasingService.purchase(product.productCode)
							AppPreferences.set(AppConstants.SKUId, product.productCode)
							AppPreferences.set(AppConstants.requestId, requestId.toString())
						} else {
							homeViewModel.purchaseAction.postValue(PurchaseResponseStatus.NOT_SUPPORTED)
						}
					} ?: run { homeViewModel.purchaseAction.postValue(PurchaseResponseStatus.FAILED) }
				} ?: run { homeViewModel.purchaseAction.postValue(PurchaseResponseStatus.FAILED) }
			}
		}
	}

	private fun createOrder() {
		viewModel.createOrder(
			hashMapOf(
				"order_id" to AppPreferences.get(AppConstants.orderId, DEFAULT.EMPTY_STRING).toString(),
				"payment_id" to AppPreferences.get(AppConstants.receiptId, DEFAULT.EMPTY_STRING).toString(),
				"vendor" to "fire_tv",
			)
		).observe(viewLifecycleOwner) { createOrder ->
			fetch(
				createOrder,
				isLoaderEnabled = false,
				canUserAccessScreen = true,
				shouldBeInBackground = true
			) {
				createOrder.response?.let {
					it.data?.let {
						fetchEventStreamDetails()
						PurchasingService.notifyFulfillment(AppPreferences.get(AppConstants.receiptId, DEFAULT.EMPTY_STRING).toString(), FulfillmentResult.FULFILLED)
						if (homeViewModel.purchaseAction.value == PurchaseResponseStatus.SUCCESS) showToast(getString(R.string.payment_success))
					} ?: run {
						PurchasingService.notifyFulfillment(AppPreferences.get(AppConstants.receiptId, DEFAULT.EMPTY_STRING).toString(), FulfillmentResult.UNAVAILABLE)
						homeViewModel.purchaseAction.postValue(if (homeViewModel.purchaseAction.value == PurchaseResponseStatus.SUCCESS_WITH_PENDING_PURCHASE) PurchaseResponseStatus.NONE else PurchaseResponseStatus.FAILED)
					}
				} ?: run {
					PurchasingService.notifyFulfillment(AppPreferences.get(AppConstants.receiptId, DEFAULT.EMPTY_STRING).toString(), FulfillmentResult.UNAVAILABLE)
					homeViewModel.purchaseAction.postValue(if (homeViewModel.purchaseAction.value == PurchaseResponseStatus.SUCCESS_WITH_PENDING_PURCHASE) PurchaseResponseStatus.NONE else PurchaseResponseStatus.FAILED)
				}
			}
		}
	}

	fun onPrimaryClicked(tag: Any?) {
		when (tag) {
			ButtonLabels.BUY_TICKET -> {
				if (context?.isFireTV == true) {
					if (!homeViewModel.isPaymentInProgress) {
						homeViewModel.isPaymentInProgress = true
						homeViewModel.isSubscription = false
						binding.paymentLoader.visibility = View.VISIBLE
						clearAllReservations()
					}
				} else {
					homeViewModel.purchaseAction.postValue(PurchaseResponseStatus.NOT_SUPPORTED)
				}
			}

			ButtonLabels.PLAY -> {
				releaseVideoPlayer()
				helper.goToVideoPlayer(viewModel.eventId)
			}

			ButtonLabels.JOIN_LIVE -> {
				helper.goToVideoPlayer(viewModel.eventId)
			}

			ButtonLabels.JOIN -> {
				val eventLogo = eventDetails.presentation.logoUrl?.ifBlank { DEFAULT.EMPTY_STRING }
					?: DEFAULT.EMPTY_STRING
				val eventId = eventDetails.eventId ?: eventDetails.id ?: DEFAULT.EMPTY_STRING
				val eventTitle =
					eventDetails.eventName?.ifBlank { DEFAULT.EMPTY_STRING } ?: DEFAULT.EMPTY_STRING
				val streamStartsAt = eventDetails.eventStreamStartsAt ?: ""
				val doorOpensAt = eventDetails.eventDoorsAt ?: ""
				if (AppUtil.isEventStarted(streamStartsAt)) {
					helper.goToVideoPlayer(viewModel.eventId)
				} else {
					helper.goToWaitingRoom(eventId, eventLogo, eventTitle, doorOpensAt, streamStartsAt)
				}
			}

			ButtonLabels.CLAIM_FREE_TICKET -> {
				claimFreeTicketForEvent()
			}
		}
	}
}
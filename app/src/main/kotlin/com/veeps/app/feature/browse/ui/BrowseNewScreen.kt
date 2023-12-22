package com.veeps.app.feature.browse.ui

import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AlphaAnimation
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE
import androidx.media3.common.C.USAGE_MEDIA
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.rubensousa.dpadrecyclerview.ChildAlignment
import com.rubensousa.dpadrecyclerview.ParentAlignment
import com.veeps.app.R
import com.veeps.app.core.BaseFragment
import com.veeps.app.databinding.FragmentBrowseScreenNewBinding
import com.veeps.app.extension.isGreaterThan
import com.veeps.app.feature.artist.ui.ArtistScreen
import com.veeps.app.feature.browse.viewModel.BrowseViewModel
import com.veeps.app.feature.contentRail.adapter.ContentRailsNewAdapter
import com.veeps.app.feature.contentRail.model.Entities
import com.veeps.app.feature.contentRail.model.RailData
import com.veeps.app.feature.contentRail.model.UserStats
import com.veeps.app.feature.event.ui.EventScreen
import com.veeps.app.feature.venue.ui.VenueScreen
import com.veeps.app.util.APIConstants
import com.veeps.app.util.AppAction
import com.veeps.app.util.AppConstants
import com.veeps.app.util.AppPreferences
import com.veeps.app.util.AppUtil
import com.veeps.app.util.CardTypes
import com.veeps.app.util.DEFAULT
import com.veeps.app.util.DateTimeCompareDifference
import com.veeps.app.util.EntityTypes
import com.veeps.app.util.Logger
import com.veeps.app.util.Screens
import com.veeps.app.widget.navigationMenu.NavigationItems


class BrowseNewScreen : BaseFragment<BrowseViewModel, FragmentBrowseScreenNewBinding>() {

	private lateinit var player: ExoPlayer
	private var posterImage: String = DEFAULT.EMPTY_STRING
	private var playbackURL: String = DEFAULT.EMPTY_STRING
	private val action by lazy {
		object : AppAction {
			override fun onEvent(entity: Entities) {
				Logger.printWithTag(
					"BrowseNew", "Event ${
						(entity.eventId ?: entity.id ?: DEFAULT.EMPTY_STRING)
					} Clicked on ${
						this@BrowseNewScreen.javaClass.name.substringAfterLast(".")
					}"
				)
				helper.setupPageChange(
					true, EventScreen::class.java, bundleOf(
						AppConstants.TAG to Screens.EVENT,
						"entityId" to (entity.eventId ?: entity.id ?: DEFAULT.EMPTY_STRING),
						"entity" to EntityTypes.EVENT
					), Screens.EVENT, true
				)
			}

			override fun onArtist(entity: Entities) {
				Logger.printWithTag(
					"BrowseNew", "Artist ${entity.id} Clicked on ${
						this@BrowseNewScreen.javaClass.name.substringAfterLast(".")
					}"
				)
				helper.setupPageChange(
					true, ArtistScreen::class.java, bundleOf(
						AppConstants.TAG to Screens.ARTIST,
						"entityId" to entity.id,
						"entity" to EntityTypes.ARTIST
					), Screens.ARTIST, true
				)
			}

			override fun onVenue(entity: Entities) {
				Logger.printWithTag(
					"BrowseNew", "Venue ${entity.id} Clicked on ${
						this@BrowseNewScreen.javaClass.name.substringAfterLast(".")
					}"
				)
				helper.setupPageChange(
					true, VenueScreen::class.java, bundleOf(
						AppConstants.TAG to Screens.VENUE,
						"entityId" to entity.id,
						"entity" to EntityTypes.VENUE
					), Screens.VENUE, true
				)
			}

			override fun onAction(entity: Entities) {
				Logger.printWithTag(
					"BrowseNew", "Action performed on ${
						this@BrowseNewScreen.javaClass.name.substringAfterLast(".")
					}"
				)
				fetchEventDetails(entity)
			}
		}
	}
	private val railAdapter by lazy {
		ContentRailsNewAdapter(rails = arrayListOf(), helper, Screens.BROWSE, action)
	}
	private val playerListener by lazy {
		object : Player.Listener {
			override fun onIsPlayingChanged(isPlaying: Boolean) {
				super.onIsPlayingChanged(isPlaying)
				if (isPlaying) {
					if (homeViewModel.isNavigationMenuVisible.value!! || homeViewModel.isErrorVisible.value!!) {
						player.pause()
					} else {
						val fadeOutAnimation = AlphaAnimation(1.0f, 0.0f)
						fadeOutAnimation.fillAfter = true
						fadeOutAnimation.duration = 333
						binding.heroImage.startAnimation(fadeOutAnimation)
					}
				} else {
					val fadeInAnimation = AlphaAnimation(0.0f, 1.0f)
					fadeInAnimation.fillAfter = true
					fadeInAnimation.duration = 333
					binding.heroImage.startAnimation(fadeInAnimation)
				}
			}

			override fun onPlaybackStateChanged(playbackState: Int) {
				super.onPlaybackStateChanged(playbackState)
				if (playbackState == Player.STATE_ENDED) {
					releaseVideoPlayer()
					val fadeInAnimation = AlphaAnimation(0.0f, 1.0f)
					fadeInAnimation.fillAfter = true
					fadeInAnimation.duration = 333
					binding.heroImage.startAnimation(fadeInAnimation)
				}
			}

			override fun onPlayerError(error: PlaybackException) {
				super.onPlayerError(error)
				releaseVideoPlayer()
				val fadeInAnimation = AlphaAnimation(0.0f, 1.0f)
				fadeInAnimation.fillAfter = true
				fadeInAnimation.duration = 333
				binding.heroImage.startAnimation(fadeInAnimation)
			}
		}
	}

	override fun getViewBinding(): FragmentBrowseScreenNewBinding =
		FragmentBrowseScreenNewBinding.inflate(layoutInflater)

	override fun onDestroyView() {
		Logger.printWithTag("BrowseNew", "browse new view is destroy requested")
		super.onDestroyView()
		releaseVideoPlayer()
		Logger.printWithTag("BrowseNew", "browse new view is destroyed")
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		Logger.printWithTag("BrowseNew", "browse new view created")
		super.onViewCreated(view, savedInstanceState)
		binding.apply {
			browse = viewModel
			browseScreen = this@BrowseNewScreen
			browseLabel.setupWith(container).setBlurRadius(12.5f)
			browseLabel.outlineProvider = ViewOutlineProvider.BACKGROUND
			browseLabel.clipToOutline = true
			onDemandLabel.setupWith(container).setBlurRadius(12.5f)
			onDemandLabel.outlineProvider = ViewOutlineProvider.BACKGROUND
			onDemandLabel.clipToOutline = true
			loader.visibility = View.VISIBLE
			darkBackground.visibility = View.GONE
			watermark.visibility = View.GONE
		}
		loadAppContent()
		notifyAppEvents()
	}

	private fun setupVideoPlayer() {
		releaseVideoPlayer()
		player = context?.let { context -> ExoPlayer.Builder(context).build() }!!
		if (this::player.isInitialized && playbackURL.isNotBlank()) {
			player.removeListener(playerListener)
			player.addListener(playerListener)
			player.setAudioAttributes(
				AudioAttributes.Builder().setUsage(USAGE_MEDIA)
					.setContentType(AUDIO_CONTENT_TYPE_MOVIE).build(), true
			)
			player.repeatMode = Player.REPEAT_MODE_ONE
			binding.videoPlayer.player = player
			player.setMediaItem(MediaItem.fromUri(playbackURL))
			player.prepare()
			player.play()
		}
	}

	private fun releaseVideoPlayer() {
		if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0)) {
			player.playWhenReady = false
			player.pause()
			player.release()
			player.removeListener(playerListener)
		}
	}

	private fun loadAppContent() {
		binding.listing.apply {
//			setSpanSizeLookup(object : DpadSpanSizeLookup() {
//				override fun getSpanSize(position: Int): Int {
//					return if (position == 0) {
//						getSpanCount()
//					} else {
//						1
//					}
//				}
//			})
			setSmoothScrollSpeedFactor(2f)
			setParentAlignment(
				ParentAlignment(
					edge = ParentAlignment.Edge.MAX,
					offset = 0,
					fraction = 0.1f,
					preferKeylineOverEdge = false
				)
			)
			setChildAlignment(
				ChildAlignment(
					offset = 0, fraction = 0f, includePadding = true
				)
			)
//			addOnViewHolderSelectedListener(object : OnViewHolderSelectedListener {
//				override fun onViewHolderSelected(
//					parent: RecyclerView,
//					child: RecyclerView.ViewHolder?,
//					position: Int,
//					subPosition: Int
//				) {
//					Logger.printWithTag(
//						"BrowseNew", "view holder selected - $position -- $subPosition"
//					)
//				}
//
//				override fun onViewHolderSelectedAndAligned(
//					parent: RecyclerView,
//					child: RecyclerView.ViewHolder?,
//					position: Int,
//					subPosition: Int
//				) {
//					Logger.printWithTag(
//						"BrowseNew", "view holder selected and aligned - $position -- $subPosition"
//					)
//				}
//			})
			adapter = railAdapter
		}
		onBrowseLabelClicked()
	}

	private fun notifyAppEvents() {
		binding.onDemandLabel.setOnFocusChangeListener { _, hasFocus ->
			context?.let { context ->
				binding.onDemandText.setTextColor(
					ContextCompat.getColor(
						context, if (hasFocus) R.color.dark_black else R.color.white
					)
				)
			}
		}
		binding.browseLabel.setOnFocusChangeListener { _, hasFocus ->
			context?.let { context ->
				binding.browseText.setTextColor(
					ContextCompat.getColor(
						context, if (hasFocus) R.color.dark_black else R.color.white
					)
				)
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
					) == true && homeViewModel.isNavigationMenuVisible.value?.equals(
						false
					) == true
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
					) == true && homeViewModel.isNavigationMenuVisible.value?.equals(
						false
					) == true
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
				binding.browseLabel.visibility = View.GONE
				binding.onDemandLabel.visibility = View.GONE
				binding.logo.visibility = View.GONE
				binding.watermark.visibility = View.VISIBLE
			}
		}
		homeViewModel.translateCarouselToBottom.observe(viewLifecycleOwner) { shouldTranslate ->
			Logger.printWithTag(
				"saumil", " Here in bottom - $shouldTranslate -- ${viewModel.isVisible.value}"
			)
			if (shouldTranslate && viewModel.isVisible.value == true) {
				if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && !player.isPlaying && homeViewModel.isErrorVisible.value?.equals(
						false
					) == true
				) {
					player.play()
				}
				binding.watermark.visibility = View.GONE
				binding.darkBackground.visibility = View.GONE
				binding.browseLabel.visibility = View.VISIBLE
				binding.onDemandLabel.visibility = View.VISIBLE
				binding.logo.visibility = View.VISIBLE
			}
		}

		viewModel.isVisible.observeForever { isVisible ->
			Logger.print(
				"Visibility Changed to $isVisible On ${
					this@BrowseNewScreen.javaClass.name.substringAfterLast(".")
				}"
			)
			if (isVisible) {
				helper.selectNavigationMenu(NavigationItems.BROWSE_MENU)
				helper.completelyHideNavigationMenu()
				if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && !player.isPlaying && homeViewModel.isErrorVisible.value?.equals(
						false
					) == true
				) {
					player.play()
				}
			} else {
				if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && player.isPlaying) {
					player.pause()
				}
			}
		}
//		viewModel.carouselData.observe(viewLifecycleOwner) { carouselData ->
//			val random = 0//Random.nextInt(carouselData.entities.size)
//			val entity =
//				if (carouselData.entities.isNotEmpty()) carouselData.entities[random] else Entities()
//			binding.ctaContainer.visibility = View.INVISIBLE
//			binding.primary.alpha = 0.1f
//			binding.primaryLabel.text =
//				AppUtil.getPrimaryLabelText(entity, Screens.BROWSE, false).also { label ->
//					binding.primaryLabel.isSelected = label == ButtonLabels.UNAVAILABLE
//				}
//			viewModel.eventId = entity.id ?: DEFAULT.EMPTY_STRING
//			binding.primary.alpha = 1.0f
//			binding.myShows.visibility = if (AppPreferences.get(
//					AppConstants.userSubscriptionStatus, "none"
//				) != "none"
//			) View.VISIBLE else View.GONE
//			setupMyShows(isAdded = homeViewModel.watchlistIds.contains(entity.id?.ifBlank { DEFAULT.EMPTY_STRING }
//				?: DEFAULT.EMPTY_STRING))
//			binding.ctaContainer.visibility =
//				if (carouselData.entities.isNotEmpty()) View.VISIBLE else View.INVISIBLE
//			val currentDate = DateTime.now()
//			val otherDate =
//				DateTime(entity.eventStreamStartsAt?.ifBlank { DateTime.now().toString() }
//					?: DateTime.now().toString(),
//					DateTimeZone.UTC).withZone(DateTimeZone.getDefault()).toDateTime()
//			binding.date.text = otherDate.toString("MMM dd, yyyy${DEFAULT.SEPARATOR}ha")
//			if (AppUtil.compare(otherDate, currentDate) == DateTimeCompareDifference.GREATER_THAN) {
//				binding.date.visibility =
//					if (binding.browseLabel.isSelected) View.VISIBLE else View.GONE
//				binding.liveNow.visibility = View.GONE
//			} else {
//				binding.date.visibility =
//					if (binding.browseLabel.isSelected) View.VISIBLE else View.GONE
//				binding.liveNow.visibility = View.GONE
//			}
//			val title = entity.eventName?.ifBlank { DEFAULT.EMPTY_STRING } ?: DEFAULT.EMPTY_STRING
//			posterImage = entity.presentation.posterUrl?.ifBlank { DEFAULT.EMPTY_STRING }
//				?: DEFAULT.EMPTY_STRING
//			val logoImage = entity.presentation.logoUrl?.ifBlank { DEFAULT.EMPTY_STRING }
//				?: DEFAULT.EMPTY_STRING
//			val videoPreviewTreeMap = entity.videoPreviews ?: false
//			playbackURL = DEFAULT.EMPTY_STRING
//			if (videoPreviewTreeMap is LinkedTreeMap<*, *>) {
//				if (videoPreviewTreeMap.isNotEmpty()) {
//					val jsonObject = Gson().toJsonTree(videoPreviewTreeMap).asJsonObject
//					if (jsonObject != null && !jsonObject.isJsonNull && !jsonObject.isEmpty) {
//						val videoPreviewString: String = Gson().toJson(jsonObject)
//						val videoPreview = JSONObject(videoPreviewString)
//						if (videoPreview.has("high")) {
//							playbackURL = videoPreview.getString("high")
//						}
//					}
//				}
//			}
//			binding.carouselLogo.loadImage(logoImage, ImageTags.LOGO)
//			binding.heroImage.loadImage(posterImage, ImageTags.HERO)
//			binding.carouselTitle.text = title
//			binding.carousel.visibility = View.VISIBLE
//			binding.darkBackground.visibility = View.GONE
//			if (playbackURL.isNotBlank()) {
//				setupVideoPlayer()
//			} else {
//				releaseVideoPlayer()
//			}
//		}
		viewModel.railData.observe(viewLifecycleOwner) { rails ->
			if (rails.isNotEmpty()) {
//				if (requireCarouselRemoval) {
//					var carouselPosition = 0
//					val carousel = rails.filterIndexed { index, railData ->
//						if (railData.cardType == CardTypes.HERO) {
//							carouselPosition = index
//						}
//						return@filterIndexed railData.cardType == CardTypes.HERO
//					}
//					if (carousel.isNotEmpty()) {
//						viewModel.carouselData.postValue(rails[carouselPosition])
//						rails.removeAt(carouselPosition)
//					}
//				} else {
//					requireCarouselRemoval = true
//				}
//				rails.removeIf { rail ->
//					rail.cardType.equals("wide")
//				}
				railAdapter.setRails(rails)
			} else {
				railAdapter.setRails(arrayListOf())
			}
		}
	}

	private fun fetchEventDetails(entity: Entities) {
		viewModel.fetchEventDetails(entity.eventId ?: entity.id ?: DEFAULT.EMPTY_STRING)
			.observe(viewLifecycleOwner) { eventResponse ->
				fetch(
					eventResponse,
					isLoaderEnabled = true,
					canUserAccessScreen = false,
					shouldBeInBackground = false
				) {
					eventResponse.response?.let { eventStreamData ->
						eventStreamData.data?.let {
							val streamStartsAt = it.eventStreamStartsAt ?: DEFAULT.EMPTY_STRING
							val doorOpensAt = it.eventDoorsAt ?: DEFAULT.EMPTY_STRING
							if (doorOpensAt.isBlank()) {
								if (streamStartsAt.isNotBlank() && AppUtil.compare(
										streamStartsAt
									) == DateTimeCompareDifference.GREATER_THAN
								) {
									val eventId = it.eventId ?: it.id ?: DEFAULT.EMPTY_STRING
									val eventLogo =
										entity.presentation.logoUrl?.ifBlank { DEFAULT.EMPTY_STRING }
											?: DEFAULT.EMPTY_STRING
									val eventTitle =
										entity.eventName?.ifBlank { DEFAULT.EMPTY_STRING }
											?: DEFAULT.EMPTY_STRING
									helper.goToWaitingRoom(
										eventId, eventLogo, eventTitle, doorOpensAt, streamStartsAt
									)
								} else {
									helper.goToVideoPlayer(
										it.eventId ?: it.id ?: DEFAULT.EMPTY_STRING
									)
								}
							} else {
								if (AppUtil.compare(doorOpensAt) == DateTimeCompareDifference.LESS_THAN) {
									helper.goToVideoPlayer(
										it.eventId ?: it.id ?: DEFAULT.EMPTY_STRING
									)
								} else if (streamStartsAt.isNotBlank() && AppUtil.compare(
										streamStartsAt
									) == DateTimeCompareDifference.GREATER_THAN
								) {
									val eventId = it.eventId ?: it.id ?: DEFAULT.EMPTY_STRING
									val eventLogo =
										entity.presentation.logoUrl?.ifBlank { DEFAULT.EMPTY_STRING }
											?: DEFAULT.EMPTY_STRING
									val eventTitle =
										entity.eventName?.ifBlank { DEFAULT.EMPTY_STRING }
											?: DEFAULT.EMPTY_STRING
									helper.goToWaitingRoom(
										eventId, eventLogo, eventTitle, doorOpensAt, streamStartsAt
									)
								} else {
									helper.goToVideoPlayer(
										it.eventId ?: it.id ?: DEFAULT.EMPTY_STRING
									)
								}
							}
						}
					}
				}
			}
	}

	private fun fetchBrowseRails() {
		viewModel.fetchBrowseRails().observe(viewLifecycleOwner) { browseRail ->
			fetch(
				browseRail,
				isLoaderEnabled = true,
				canUserAccessScreen = false,
				shouldBeInBackground = false
			) {
				browseRail.response?.let { railResponse ->
					viewModel.railData.postValue(railResponse.railData)
					fetchContinueWatchingRail()
				} ?: helper.showErrorOnScreen(browseRail.tag, getString(R.string.unknown_error))
			}
		}
	}

	private fun fetchOnDemandRails() {
		viewModel.fetchOnDemandRails().observe(viewLifecycleOwner) { onDemand ->
			fetch(
				onDemand,
				isLoaderEnabled = true,
				canUserAccessScreen = true,
				shouldBeInBackground = false
			) {
				onDemand.response?.let { railResponse ->
					viewModel.railData.postValue(railResponse.railData)
				} ?: helper.showErrorOnScreen(onDemand.tag, getString(R.string.unknown_error))
			}
		}
	}

	private fun fetchContinueWatchingRail() {
		viewModel.fetchContinueWatchingRail().observe(viewLifecycleOwner) { continueWatching ->
			fetch(
				continueWatching,
				isLoaderEnabled = false,
				canUserAccessScreen = true,
				shouldBeInBackground = true,
			) {
				continueWatching.response?.let { railData ->
					if (railData.data.isNotEmpty()) {
						fetchUserStats(railData.data)
					}
				}
			}
		}
	}

	fun addRemoveWatchListEvent(eventId: String) {
		if (eventId.isNotBlank()) {
			viewModel.addRemoveWatchListEvent(
				hashMapOf("event_id" to eventId), false
			).observe(viewLifecycleOwner) { addWatchList ->
				fetch(
					addWatchList,
					isLoaderEnabled = false,
					canUserAccessScreen = true,
					shouldBeInBackground = true,
				) {
//					setupMyShows(!binding.myShows.isSelected)
				}
			}
		}
	}

//	private fun setupMyShows(isAdded: Boolean) {
//		binding.myShows.isSelected = isAdded
//		binding.myShowsLabel.setCompoundDrawablesRelativeWithIntrinsicBounds(
//			if (binding.myShows.isSelected) {
//				if (binding.myShows.hasFocus()) R.drawable.check_black else R.drawable.check_white
//			} else {
//				if (binding.myShows.hasFocus()) R.drawable.add_black else R.drawable.add_white
//			}, 0, 0, 0
//		)
//	}

	private fun fetchUserStats(continueWatchingEntities: ArrayList<Entities>) {
		var eventIds = ""
		continueWatchingEntities.forEachIndexed { index, entities ->
			eventIds += "${entities.eventId}${(if (index == continueWatchingEntities.size - 1) "" else ",")}"
		}
		val userStatsAPIURL = AppPreferences.get(
			AppConstants.userBeaconBaseURL, DEFAULT.EMPTY_STRING
		) + APIConstants.fetchUserStats
		AppPreferences.set(AppConstants.generatedJWT, AppUtil.generateJWT(eventIds))
		viewModel.fetchUserStats(userStatsAPIURL, eventIds)
			.observe(viewLifecycleOwner) { userStatsDetails ->
				fetch(
					userStatsDetails,
					isLoaderEnabled = false,
					canUserAccessScreen = true,
					shouldBeInBackground = true,
				) {
					var userStats = arrayListOf<UserStats>()
					userStatsDetails.response?.let { userStatsResponse ->
						userStats = userStatsResponse.userStats
					}
					val continueWatchingRail = RailData(
						name = "Continue Watching",
						entities = continueWatchingEntities,
						cardType = CardTypes.PORTRAIT,
						entitiesType = EntityTypes.EVENT,
						isContinueWatching = true,
						userStats = userStats
					)
					val rails = viewModel.railData.value.orEmpty()

					viewModel.railData.value = ArrayList(rails).apply {
						add(0, continueWatchingRail)
//						requireCarouselRemoval = false
					}
				}
			}
	}

	fun onPrimaryClicked() {
		helper.setupPageChange(
			true, EventScreen::class.java, bundleOf(
				AppConstants.TAG to Screens.EVENT,
				"entityId" to viewModel.eventId,
				"entity" to EntityTypes.EVENT
			), Screens.EVENT, true
		)
	}

	fun onBrowseLabelClicked() {
		if (!binding.browseLabel.isSelected) {
			railAdapter.setRails(arrayListOf())
			binding.darkBackground.visibility = View.VISIBLE
			binding.loader.visibility = View.VISIBLE
			binding.loader.requestFocus()
			binding.browseLabel.isSelected = true
			binding.onDemandLabel.isSelected = false
			binding.browseLabel.requestFocus()
			binding.browseLabel.setBlurEnabled(true)
			binding.onDemandLabel.setBlurEnabled(false)
			helper.fetchAllWatchListEvents()
			fetchBrowseRails()
		}
	}

	fun onOnDemandLabelClicked() {
		if (!binding.onDemandLabel.isSelected) {
			railAdapter.setRails(arrayListOf())
			binding.darkBackground.visibility = View.VISIBLE
			binding.loader.visibility = View.VISIBLE
			binding.loader.requestFocus()
			binding.onDemandLabel.isSelected = true
			binding.browseLabel.isSelected = false
			binding.onDemandLabel.requestFocus()
			binding.onDemandLabel.setBlurEnabled(true)
			binding.browseLabel.setBlurEnabled(false)
			helper.fetchAllWatchListEvents()
			fetchOnDemandRails()
		}
	}
}
package com.veeps.app.feature.shows.ui

import android.os.Bundle
import android.view.View
import androidx.leanback.widget.BaseGridView
import androidx.recyclerview.widget.PagerSnapHelper
import com.veeps.app.core.BaseFragment
import com.veeps.app.databinding.FragmentShowsScreenBinding
import com.veeps.app.extension.filterFor
import com.veeps.app.feature.contentRail.adapter.ContentRailsAdapter
import com.veeps.app.feature.contentRail.model.Entities
import com.veeps.app.feature.contentRail.model.RailData
import com.veeps.app.feature.shows.viewModel.ShowsViewModel
import com.veeps.app.util.AppAction
import com.veeps.app.util.AppConstants
import com.veeps.app.util.AppUtil
import com.veeps.app.util.CardTypes
import com.veeps.app.util.DEFAULT
import com.veeps.app.util.DateTimeCompareDifference
import com.veeps.app.util.EntityTypes
import com.veeps.app.util.EventTypes
import com.veeps.app.util.Logger
import com.veeps.app.util.Screens
import com.veeps.app.widget.navigationMenu.NavigationItems

class ShowsScreen : BaseFragment<ShowsViewModel, FragmentShowsScreenBinding>() {

	private var allExpiredEvents: ArrayList<Entities> = arrayListOf()
	private var allOnDemandWatchlistEvents: ArrayList<Entities> = arrayListOf()
	private var allOnDemandPurchasedEvents: ArrayList<Entities> = arrayListOf()
	private var allUpcomingWatchlistEvents: ArrayList<Entities> = arrayListOf()
	private var allUpcomingPurchasedEvents: ArrayList<Entities> = arrayListOf()
	private var rails: ArrayList<RailData> = arrayListOf()
	private var isLoading = true
	private val action by lazy {
		object : AppAction {
			override fun onAction(entity: Entities) {
				Logger.print(
					"Action performed on ${
						this@ShowsScreen.javaClass.name.substringAfterLast(".")
					}"
				)
				fetchEventDetails(entity)
			}
		}
	}
	private val showsAdapter by lazy {
		ContentRailsAdapter(rails = rails, helper, Screens.SHOWS, action)
	}

	override fun getViewBinding(): FragmentShowsScreenBinding =
		FragmentShowsScreenBinding.inflate(layoutInflater)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.apply {
			shows = viewModel
			showsScreen = this@ShowsScreen
			lifecycleOwner = viewLifecycleOwner
			lifecycle.addObserver(viewModel)
			loader.visibility = View.GONE
			noResultContainer.visibility = View.GONE
			title.visibility = View.VISIBLE
			listing.apply {
				itemAnimator = null
				setNumColumns(1)
//					layoutAnimation = AnimationUtils.loadLayoutAnimation(
//						context, R.anim.layout_animation_fall_down
//					)
				setHasFixedSize(true)
				windowAlignment = BaseGridView.WINDOW_ALIGN_HIGH_EDGE
				windowAlignmentOffsetPercent = 0f
				isItemAlignmentOffsetWithPadding = true
				itemAlignmentOffsetPercent = 0f
				adapter = showsAdapter
				onFlingListener = PagerSnapHelper()
			}
		}
		loadAppContent()
		notifyAppEvents()
	}

	override fun onDestroyView() {
		viewModelStore.clear()
		super.onDestroyView()
	}

	private fun loadAppContent() {
		fetchAllPurchasedEvents()
	}

	private fun setupRails() {
		rails = ArrayList()

		val allUpcomingPurchasedEventsRail = RailData(
			name = "Upcoming events you’ve purchased",
			entities = allUpcomingPurchasedEvents.ifEmpty { arrayListOf() },
			cardType = CardTypes.PORTRAIT,
			entitiesType = EntityTypes.EVENT,
		)
		rails.add(allUpcomingPurchasedEventsRail)

		val allUpcomingWatchlistEventsRail = RailData(
			name = "Upcoming from your Watchlist",
			entities = allUpcomingWatchlistEvents.ifEmpty { arrayListOf() },
			cardType = CardTypes.PORTRAIT,
			entitiesType = EntityTypes.EVENT,
			isWatchList = true,
		)
		rails.add(allUpcomingWatchlistEventsRail)

		val allOnDemandPurchasedEventsRail = RailData(
			name = "On Demand events you’ve purchased",
			entities = allOnDemandPurchasedEvents.ifEmpty { arrayListOf() },
			cardType = CardTypes.PORTRAIT,
			entitiesType = EntityTypes.EVENT,
		)
		rails.add(allOnDemandPurchasedEventsRail)

		val allOnDemandWatchlistEventsRail = RailData(
			name = "On Demand from your Watchlist",
			entities = allOnDemandWatchlistEvents.ifEmpty { arrayListOf() },
			cardType = CardTypes.PORTRAIT,
			entitiesType = EntityTypes.EVENT,
			isWatchList = true,
		)
		rails.add(allOnDemandWatchlistEventsRail)

		val allExpiredEventsRail = RailData(
			name = "Expired",
			entities = allExpiredEvents.ifEmpty { arrayListOf() },
			cardType = CardTypes.PORTRAIT,
			entitiesType = EntityTypes.EVENT,
			isExpired = true
		)
		rails.add(allExpiredEventsRail)

		rails.removeAll { it.entities.isEmpty() }
		viewModel.allRails.postValue(rails)
	}

	private fun fetchEventDetails(entity: Entities) {
		viewModel.fetchEventDetails(entity.eventId ?: entity.id ?: DEFAULT.EMPTY_STRING)
			.observe(viewLifecycleOwner) { eventResponse ->
				fetch(
					eventResponse,
					isLoaderEnabled = true,
					canUserAccessScreen = true,
					shouldBeInBackground = true
				) {
					eventResponse.response?.let { eventStreamData ->
						eventStreamData.data?.let {
							val streamStartsAt = it.eventStreamStartsAt ?: DEFAULT.EMPTY_STRING
							val doorOpensAt = it.eventDoorsAt ?: DEFAULT.EMPTY_STRING
							if (doorOpensAt.isBlank()) {
								if (streamStartsAt.isNotBlank() && AppUtil.compare(streamStartsAt) == DateTimeCompareDifference.GREATER_THAN) {
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

	private fun fetchAllPurchasedEvents() {
		viewModel.fetchAllPurchasedEvents().observe(viewLifecycleOwner) { allPurchasedEvents ->
			isLoading = true
			fetch(
				allPurchasedEvents,
				isLoaderEnabled = false,
				canUserAccessScreen = true,
				shouldBeInBackground = true,
			) {
				allPurchasedEvents.response?.let { railData ->
					if (railData.data.isNotEmpty()) {
						allUpcomingPurchasedEvents = arrayListOf()
						allOnDemandPurchasedEvents = arrayListOf()
						allExpiredEvents = arrayListOf()

						allUpcomingPurchasedEvents.addAll(
							railData.data.filterFor(
								AppUtil.getFilterFor(
									EventTypes.LIVE
								)
							)
						)
						allUpcomingPurchasedEvents.addAll(
							railData.data.filterFor(
								AppUtil.getFilterFor(
									EventTypes.UPCOMING
								)
							)
						)
						allOnDemandPurchasedEvents.addAll(
							railData.data.filterFor(
								AppUtil.getFilterFor(
									EventTypes.ON_DEMAND
								)
							)
						)
						allExpiredEvents.addAll(
							railData.data.filterFor(
								AppUtil.getFilterFor(
									EventTypes.EXPIRED
								)
							)
						)
					}
				}
				isLoading = false
				fetchAllWatchListEvents()
			}
		}
	}

	private fun fetchAllWatchListEvents() {
		viewModel.fetchAllWatchListEvents().observe(viewLifecycleOwner) { allWatchListEvents ->
			isLoading = true
			fetch(
				allWatchListEvents,
				isLoaderEnabled = false,
				canUserAccessScreen = true,
				shouldBeInBackground = true,
			) {
				allWatchListEvents.response?.let { railData ->
					allUpcomingWatchlistEvents = arrayListOf()
					allOnDemandWatchlistEvents = arrayListOf()
					if (railData.data.isNotEmpty()) {
						allUpcomingWatchlistEvents.addAll(
							railData.data.filterFor(
								AppUtil.getFilterFor(
									EventTypes.UPCOMING
								)
							).sortedWith(compareBy { it.eventStreamStartsAt })
						)
						allOnDemandWatchlistEvents.addAll(
							railData.data.filterFor(
								AppUtil.getFilterFor(
									EventTypes.ON_DEMAND
								)
							).sortedWith(compareBy { it.watchUntil })
						)
//						allExpiredEvents.addAll(railData.data.filterFor(AppUtil.getFilterFor(EventTypes.EXPIRED)))
					}
				}
				isLoading = false
				setupRails()
			}
		}
	}

	private fun notifyAppEvents() {
		homeViewModel.shouldRefresh.observe(viewLifecycleOwner) { shouldRefresh ->
			if (shouldRefresh) {
				fetchAllWatchListEvents()
				homeViewModel.shouldRefresh.postValue(false)
			}
		}
		homeViewModel.focusItem.observe(viewLifecycleOwner) { isTrue ->
			if (isTrue) {
				fetchAllWatchListEvents()
				homeViewModel.focusItem.postValue(false)
			}
		}

		viewModel.allRails.observe(viewLifecycleOwner) { rails ->
			if (rails.none { it.entities.isNotEmpty() }) {
				if (isLoading) {
					binding.loader.visibility = View.VISIBLE
				} else {
					binding.title.visibility = View.GONE
					binding.noResultContainer.visibility = View.VISIBLE
					binding.browseShows.requestFocus()
					binding.loader.visibility = View.GONE
					binding.listing.visibility = View.GONE
				}
			} else {
				binding.title.visibility = View.VISIBLE
				binding.noResultContainer.visibility = View.GONE
				binding.listing.visibility = View.VISIBLE
				binding.loader.visibility = View.GONE
				showsAdapter.setRails(rails)
				binding.listing.postDelayed({
					binding.listing.requestFocus()
					binding.title.isFocusable = false
					binding.title.isFocusableInTouchMode = false
				}, AppConstants.keyPressShortDelayTime)
			}
		}

		viewModel.isVisible.observeForever { isVisible ->
			if (isVisible) {
				helper.selectNavigationMenu(NavigationItems.MY_SHOWS_MENU)
				helper.completelyHideNavigationMenu()
			}
			Logger.print(
				"Visibility Changed to $isVisible On ${
					this@ShowsScreen.javaClass.name.substringAfterLast(".")
				}"
			)
		}
	}

	fun onBrowseShows() {
		helper.select(NavigationItems.BROWSE_MENU)
	}
}
package com.veeps.app.feature.search.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.leanback.widget.BaseGridView
import androidx.recyclerview.widget.PagerSnapHelper
import com.veeps.app.R
import com.veeps.app.core.BaseFragment
import com.veeps.app.databinding.FragmentSearchScreenBinding
import com.veeps.app.extension.clear
import com.veeps.app.feature.contentRail.adapter.ContentRailsAdapter
import com.veeps.app.feature.contentRail.adapter.ContentRailsAdapterGrid
import com.veeps.app.feature.contentRail.model.RailData
import com.veeps.app.feature.search.viewModel.SearchViewModel
import com.veeps.app.util.AppAction
import com.veeps.app.util.CardTypes
import com.veeps.app.util.DEFAULT
import com.veeps.app.util.EntityTypes
import com.veeps.app.util.Logger
import com.veeps.app.util.Screens
import com.veeps.app.widget.navigationMenu.NavigationItems

class SearchScreen : BaseFragment<SearchViewModel, FragmentSearchScreenBinding>() {

	private val action by lazy {
		object : AppAction {
			override fun onAction() {
				Logger.print(
					"Action performed on ${
						this@SearchScreen.javaClass.name.substringAfterLast(".")
					}"
				)
			}
		}
	}

	override fun getViewBinding(): FragmentSearchScreenBinding =
		FragmentSearchScreenBinding.inflate(layoutInflater)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.apply {
			search = viewModel
			searchScreen = this@SearchScreen
			lifecycleOwner = viewLifecycleOwner
			lifecycle.addObserver(viewModel)
			loader.visibility = View.GONE
			noResultContainer.visibility = View.GONE
			keyboardA.requestFocus()
			viewModel.search.value?.let { searchString ->
				searchInput.setText(searchString.ifBlank { DEFAULT.EMPTY_STRING })
			}
			keyboardContainer.visibility = View.VISIBLE
		}
		notifyAppEvents()
	}

	fun onKeyboardClick(tag: Any) {
		when (tag) {
			"backspace" -> {
				if (binding.searchInput.text.toString().isNotBlank()) {
					binding.searchInput.setText(
						binding.searchInput.text.toString()
							.substring(0, binding.searchInput.text.length - 1)
					)
				}
			}

			"space" -> {
				binding.searchInput.setText(binding.searchInput.text.toString().plus(" "))
			}

			"delete" -> {
				if (binding.searchInput.text.toString().isNotBlank()) {
					binding.searchInput.clear()
				}
			}

			else -> {
				binding.searchInput.setText(binding.searchInput.text.toString().plus(tag))
			}
		}

	}

	private fun fetchUpcomingEvents() {
		viewModel.fetchUpcomingEvents().observe(viewLifecycleOwner) { upcomingEvents ->
			fetch(
				upcomingEvents,
				isLoaderEnabled = false,
				canUserAccessScreen = true,
				shouldBeInBackground = true,
			) {
				upcomingEvents.response?.let { railData ->
					if (railData.data.isNotEmpty()) {
						val upcomingEventsRail = RailData(
							name = getString(
								R.string.suggested_events_label
							),
							entities = railData.data,
							cardType = CardTypes.PORTRAIT,
							entitiesType = EntityTypes.EVENT
						)
						val rails = ArrayList<RailData>()
						rails.add(upcomingEventsRail)
						viewModel.upcomingRail.postValue(rails)
					}
				}
			}
		}
	}

	private fun fetchFeaturedContent() {
		viewModel.fetchFeaturedContent().observe(viewLifecycleOwner) { featuredContent ->
			fetch(
				featuredContent,
				isLoaderEnabled = false,
				canUserAccessScreen = true,
				shouldBeInBackground = true,
			) {
				featuredContent.response?.let { railResponse ->
					viewModel.noResult.postValue(false)
					viewModel.featuredContentRail.postValue(railResponse.railData)
				}
			}
		}
	}

	private fun fetchSearchedResult() {
		viewModel.fetchSearchResult().observe(viewLifecycleOwner) { searchedEvents ->
			fetch(
				searchedEvents,
				isLoaderEnabled = false,
				canUserAccessScreen = true,
				shouldBeInBackground = true,
			) {
				searchedEvents.response?.let { searchResponse ->
					if (viewModel.search.value.isNullOrEmpty()) {
						fetchFeaturedContent()
					} else {
						if (searchResponse.data != null) {
							searchResponse.data.let {
								val rails = ArrayList<RailData>()
								val eventsRail: RailData
								val artistRail: RailData
								if (searchResponse.data!!.events.isNotEmpty()) {
									eventsRail = RailData(
										name = getString(R.string.events_label),
										entities = searchResponse.data!!.events,
										cardType = CardTypes.PORTRAIT,
										entitiesType = EntityTypes.EVENT
									)
									rails.add(eventsRail)
								}
								if (searchResponse.data!!.artists.isNotEmpty()) {
									artistRail = RailData(
										name = getString(R.string.artists_label),
										entities = searchResponse.data!!.artists,
										cardType = CardTypes.CIRCLE,
										entitiesType = EntityTypes.ARTIST
									)
									rails.add(artistRail)
								}
								if (rails.isNotEmpty()) {
									viewModel.noResult.postValue(false)
									viewModel.searchResult.postValue(rails)
								} else {
									viewModel.noResult.postValue(true)
									fetchUpcomingEvents()
								}
							}
						}
					}
				}
			}
		}
	}

	private fun notifyAppEvents() {
		homeViewModel.focusItem.observe(viewLifecycleOwner) { hasFocus ->
			if (hasFocus) {
				binding.keyboardF.requestFocus()
			}
		}
		viewModel.searchResult.observe(viewLifecycleOwner) { searchedResult ->
			binding.listing.apply {
				itemAnimator = null
				setNumColumns(1)
				windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
				windowAlignmentOffsetPercent = 0f
				isItemAlignmentOffsetWithPadding = true
				itemAlignmentOffsetPercent = 0f
				adapter =
					ContentRailsAdapter(rails = searchedResult, helper, Screens.SEARCH, action)
				onFlingListener = PagerSnapHelper()
			}
			binding.listing.visibility = View.VISIBLE
		}

		viewModel.upcomingRail.observe(viewLifecycleOwner) { searchedResult ->
			binding.listing.apply {
				itemAnimator = null
				setNumColumns(1)
				setHasFixedSize(true)
				windowAlignment = BaseGridView.WINDOW_ALIGN_HIGH_EDGE
				windowAlignmentOffsetPercent = 0f
				isItemAlignmentOffsetWithPadding = true
				itemAlignmentOffsetPercent = 0f
				adapter =
					ContentRailsAdapterGrid(rails = searchedResult, helper, Screens.SEARCH, action)
				onFlingListener = PagerSnapHelper()
			}
			binding.listing.visibility = View.VISIBLE
		}

		viewModel.featuredContentRail.observe(viewLifecycleOwner) { featuredContentRails ->
			if (featuredContentRails.isNotEmpty()) {
				featuredContentRails.removeIf { rail ->
					rail.cardType.equals(CardTypes.WIDE).or(rail.cardType.equals(CardTypes.HERO))
						.or(rail.cardType.equals(CardTypes.GENRE))
				}
				binding.listing.apply {
					itemAnimator = null
					setNumColumns(1)
					windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
					windowAlignmentOffsetPercent = 0f
					isItemAlignmentOffsetWithPadding = true
					itemAlignmentOffsetPercent = 0f
					adapter = ContentRailsAdapter(
						rails = featuredContentRails,
						helper,
						Screens.SEARCH,
						action
					)
					onFlingListener = PagerSnapHelper()
				}
				binding.listing.visibility = View.VISIBLE
				viewModel.featuredContentRail.postValue(arrayListOf())
			}
		}

		viewModel.noResult.observe(viewLifecycleOwner) {
			binding.noResultDescription.text =
				getString(R.string.no_results_description_label, binding.searchInput.text)
			binding.noResultContainer.visibility = if (it) View.VISIBLE else View.GONE
			binding.listing.visibility = if (it) View.GONE else View.VISIBLE
		}

		viewModel.search.observe(viewLifecycleOwner) { searchedText ->
			if (searchedText.isBlank()) {
				fetchFeaturedContent()
			} else {
				fetchSearchedResult()
			}
		}

		binding.searchInput.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: Editable) {
				viewModel.search.postValue(s.toString())
			}
		})

		viewModel.isVisible.observeForever { isVisible ->
			if (isVisible) {
				helper.selectNavigationMenu(NavigationItems.SEARCH_MENU)
				helper.completelyHideNavigationMenu()
			}
			Logger.print(
				"Visibility Changed to $isVisible On ${
					this@SearchScreen.javaClass.name.substringAfterLast(
						"."
					)
				}"
			)
		}
	}
}
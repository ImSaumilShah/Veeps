package com.veeps.app.widget.navigationMenu

import com.veeps.app.R

object NavigationItems {
	const val IS_GENRE_AVAILABLE = true

	const val PROFILE = "Profile"
	const val BROWSE = "Browse"
	const val LIVE_TV = "Live TV"
	const val MY_SHOWS = "My Shows"
	const val SEARCH = "Search"

	const val NO_MENU = -1
	const val PROFILE_MENU = 0
	const val BROWSE_MENU = 1
	const val LIVE_TV_MENU = 2
	const val MY_SHOWS_MENU = 3
	const val SEARCH_MENU = 4

	val defaultResource = R.drawable.logo

	val imageFilledResources = listOf(
		R.drawable.logo,
		R.drawable.home_active,
		R.drawable.live_tv_active,
		R.drawable.my_shows_active,
		R.drawable.search_active,
	)

	val imageNonFilledResources = listOf(
		R.drawable.logo,
		R.drawable.home,
		R.drawable.live_tv,
		R.drawable.my_shows,
		R.drawable.search,
	)
}
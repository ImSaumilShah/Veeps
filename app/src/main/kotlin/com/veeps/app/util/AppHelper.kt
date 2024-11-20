package com.veeps.app.util

import android.os.Bundle
import androidx.fragment.app.Fragment

interface AppHelper {
	fun setCarousel(position: Int)
	fun translateCarouselToTop(needsOnTop: Boolean)
	fun translateCarouselToBottom(needsOnBottom: Boolean)
	fun selectNavigationMenu(selectedItem: Int)
	fun showErrorOnScreen(tag: String, message: String)
	fun showNavigationMenu()
	fun completelyHideNavigationMenu()
	fun focusItem()

	fun showToastMessage(message: String)

	fun setupPageChange(
		shouldReplace: Boolean,
		fragment: Class<out Fragment>?,
		arguments: Bundle,
		tag: String,
		shouldAddToBackStack: Boolean,
	)

	fun goToVideoPlayer(eventId: String)

	fun goToWaitingRoom(
		eventId: String,
		eventLogo: String,
		eventTitle: String,
		eventDoorOpensAt: String,
		eventStreamStartsAt: String
	)

	fun select(selectedItem: Int)
	fun goBack()
	fun removeEventFromWatchList(eventId: String)

	fun fetchAllWatchListEvents()
}
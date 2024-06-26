package com.veeps.app.feature.home.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.veeps.app.data.common.APIRepository
import com.veeps.app.util.DEFAULT

class HomeViewModel : ViewModel() {
	var contentHasLoaded = MutableLiveData(false)
	var errorMessage = MutableLiveData(DEFAULT.EMPTY_STRING)
	var errorPositiveLabel = MutableLiveData(DEFAULT.EMPTY_STRING)
	var errorNegativeLabel = MutableLiveData(DEFAULT.EMPTY_STRING)
	var isErrorPositiveApplicable = MutableLiveData(false)
	var isErrorNegativeApplicable = MutableLiveData(false)
	val isNavigationMenuVisible = MutableLiveData(false)
	val playerShouldRelease = MutableLiveData(false)
	val playerShouldPause = MutableLiveData(false)
	val isErrorVisible = MutableLiveData(false)
	val translateCarouselToTop = MutableLiveData(false)
	val translateCarouselToBottom = MutableLiveData(false)
	val focusItem = MutableLiveData(false)
	val updateUserStat = MutableLiveData(false)
	val purchaseAction = MutableLiveData("")

	var watchlistIds = listOf<String>()
	var reservedId = ""
	var receiptId = ""
	var orderId = ""

	fun fetchAllWatchListEvents() = APIRepository().fetchAllWatchListEvents()
	fun addRemoveWatchListEvent(eventId: HashMap<String, Any>, isRemoveFromWatchList: Boolean) =
		APIRepository().addRemoveWatchListEvent(eventId, isRemoveFromWatchList)
}
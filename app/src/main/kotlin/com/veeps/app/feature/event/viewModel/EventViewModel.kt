package com.veeps.app.feature.event.viewModel

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.veeps.app.data.common.APIRepository

class EventViewModel : ViewModel(), DefaultLifecycleObserver {
	var isVisible = MutableLiveData(false)
	var eventId: String = ""

	override fun onResume(owner: LifecycleOwner) {
		super.onResume(owner)
		isVisible.postValue(true)
	}

	override fun onPause(owner: LifecycleOwner) {
		isVisible.postValue(false)
		super.onPause(owner)
	}

	fun addRemoveWatchListEvent(eventId: HashMap<String, Any>, isRemoveFromWatchList: Boolean) =
		APIRepository().addRemoveWatchListEvent(eventId, isRemoveFromWatchList)

	fun fetchEventStreamDetails() = APIRepository().fetchEventStreamDetails(eventId)
	fun fetchEventDetails() = APIRepository().fetchEventDetails(eventId)
	fun fetchEventProductDetails() = APIRepository().fetchEventProductDetails(eventId)
	fun claimFreeTicketForEvent() = APIRepository().claimFreeTicketForEvent(eventId)
	fun clearAllReservations() = APIRepository().clearAllReservations()
	fun setNewReservation(itemId: HashMap<String, Any>) = APIRepository().setNewReservation(itemId)
	fun generateNewOrder() = APIRepository().generateNewOrder()
	fun createOrder(orderDetails: HashMap<String, Any>) = APIRepository().createOrder(orderDetails)
	fun fetchUserStats(userStatsAPIURL: String, eventIds: String) =
		APIRepository().fetchUserStats(userStatsAPIURL, eventIds)
}
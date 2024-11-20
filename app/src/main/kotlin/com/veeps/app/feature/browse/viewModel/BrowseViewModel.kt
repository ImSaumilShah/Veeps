package com.veeps.app.feature.browse.viewModel

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.veeps.app.data.common.APIRepository
import com.veeps.app.feature.contentRail.model.RailData

class BrowseViewModel : ViewModel(), DefaultLifecycleObserver {
	var isVisible = MutableLiveData(false)
	var appVersionUpdateShouldVisible = false
	var railData = MutableLiveData(ArrayList<RailData>())
	var eventId: String = ""
	var isAppUpdateCall: Boolean = false

	override fun onResume(owner: LifecycleOwner) {
		super.onResume(owner)
		isVisible.postValue(true)
	}

	override fun onPause(owner: LifecycleOwner) {
		isVisible.postValue(false)
		super.onPause(owner)
	}

	fun fetchEventDetails(eventId: String) = APIRepository().fetchEventDetails(eventId)
	fun fetchBrowseRails() = APIRepository().fetchBrowseRails()
	fun fetchOnDemandRails() = APIRepository().fetchOnDemandRails()
	fun fetchContinueWatchingRail() = APIRepository().fetchContinueWatchingRail()
	fun fetchUserStats(userStatsAPIURL: String, eventIds: String) =
		APIRepository().fetchUserStats(userStatsAPIURL, eventIds)

	fun addRemoveWatchListEvent(eventId: HashMap<String, Any>, isRemoveFromWatchList: Boolean) =
		APIRepository().addRemoveWatchListEvent(eventId, isRemoveFromWatchList)

	fun validateAppVersion(appVersion: String) = APIRepository().validateAppVersion(appVersion)
}
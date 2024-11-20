package com.veeps.app.feature.video.viewModel

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.veeps.app.data.common.APIRepository
import com.veeps.app.feature.contentRail.model.ChatMessageItem
import com.veeps.app.feature.video.model.StoryBoardImagePosition
import com.veeps.app.util.DEFAULT

class VideoPlayerViewModel : ViewModel() {
	var contentHasLoaded = MutableLiveData(false)
	var errorMessage = MutableLiveData(DEFAULT.EMPTY_STRING)
	var errorPositiveLabel = MutableLiveData(DEFAULT.EMPTY_STRING)
	var errorNegativeLabel = MutableLiveData(DEFAULT.EMPTY_STRING)
	var isErrorPositiveApplicable = MutableLiveData(false)
	var isErrorNegativeApplicable = MutableLiveData(false)
	var eventId = MutableLiveData(DEFAULT.EMPTY_STRING)
	var playbackURL = MutableLiveData(DEFAULT.EMPTY_STRING)
	var storyBoardURL = MutableLiveData("")
	var storyBoard: Bitmap? = null
	var tileWidth = MutableLiveData(0)
	var tileHeight = MutableLiveData(0)
	var tiles = MutableLiveData(ArrayList<StoryBoardImagePosition>())
	var chatMessages = MutableLiveData(ArrayList<ChatMessageItem>())

	fun fetchUserStats(userStatsAPIURL: String, eventIds: String) =
		APIRepository().fetchUserStats(userStatsAPIURL, eventIds)

	fun fetchAllPurchasedEvents() = APIRepository().fetchAllPurchasedEvents()
	fun fetchEventStreamDetails(eventId: String) = APIRepository().fetchEventStreamDetails(eventId)
	fun fetchStoryBoard(storyBoardURL: String) = APIRepository().fetchStoryBoard(storyBoardURL)
	fun fetchCompanions(eventDetails: HashMap<String, Any>) =
		APIRepository().fetchCompanions(eventDetails)

	fun addStats(
		addStatsAPIURL: String,
		currentTime: String,
		duration: String,
		playerVersion: String,
		deviceModel: String,
		deviceVendor: String,
		playbackStreamType: String,
		platform: String,
		userType: String
	) = APIRepository().addStats(
		addStatsAPIURL,
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
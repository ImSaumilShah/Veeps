package com.veeps.app.feature.contentRail.model

import com.google.gson.annotations.SerializedName
import com.veeps.app.feature.video.model.Ads

data class Playback(
	@SerializedName("channel") var channel: String? = null,
	@SerializedName("content_id") var contentId: String? = null,
	@SerializedName("last_signal") var lastSignal: Any? = null,
	@SerializedName("stream_url") var streamUrl: String? = null,
	@SerializedName("storyboards") var storyboards: Storyboards = Storyboards(),
	@SerializedName("ads") var ads: ArrayList<Ads> = arrayListOf(),
	@SerializedName("widevine_url") var widevineUrl: String? = null,
)

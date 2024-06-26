package com.veeps.app.feature.video.model

import com.google.gson.annotations.SerializedName

data class StoryBoardImagePosition(
	@SerializedName("start") var start: Float = 0F,
	@SerializedName("x") var x: Int = 0,
	@SerializedName("y") var y: Int = 0,
)

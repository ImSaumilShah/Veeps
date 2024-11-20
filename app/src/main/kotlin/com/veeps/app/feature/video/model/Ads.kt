package com.veeps.app.feature.video.model

import com.google.gson.annotations.SerializedName

data class Ads(
    @SerializedName("internal") var internal: Boolean? = null,
    @SerializedName("type") var name: String? = null,
    @SerializedName("url") var adUrl: String? = null,
    @SerializedName("position") var adPosition: String? = null,
)
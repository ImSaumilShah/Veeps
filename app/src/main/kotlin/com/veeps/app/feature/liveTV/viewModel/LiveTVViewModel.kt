package com.veeps.app.feature.liveTV.viewModel

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.analytics.AnalyticsPlayerConfig
import com.veeps.app.BuildConfig.bitmovinAnalyticsKey
import com.veeps.app.application.Veeps.Companion.appContext

class LiveTVViewModel : ViewModel(), DefaultLifecycleObserver {
	var contentHasLoaded = MutableLiveData(false)
	var isVisible = MutableLiveData(false)
	var player: Player = Player(
		appContext,
		PlayerConfig(playbackConfig = PlaybackConfig(isAutoplayEnabled = true)),
		AnalyticsPlayerConfig.Enabled(AnalyticsConfig(bitmovinAnalyticsKey)),
	)

	override fun onResume(owner: LifecycleOwner) {
		super.onResume(owner)
		isVisible.postValue(true)
	}

	override fun onPause(owner: LifecycleOwner) {
		isVisible.postValue(false)
		super.onPause(owner)
	}

	override fun onStart(owner: LifecycleOwner) {
		super.onStart(owner)
		isVisible.postValue(true)
	}

	override fun onStop(owner: LifecycleOwner) {
		isVisible.postValue(false)
		super.onStop(owner)
	}
}
package com.veeps.app.feature.liveTV.ui

import android.os.Bundle
import android.view.View
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.source.SourceConfig
import com.veeps.app.core.BaseFragment
import com.veeps.app.databinding.FragmentLiveTvScreenBinding
import com.veeps.app.feature.liveTV.viewModel.LiveTVViewModel
import com.veeps.app.util.Logger
import com.veeps.app.widget.navigationMenu.NavigationItems

class LiveTVScreen : BaseFragment<LiveTVViewModel, FragmentLiveTvScreenBinding>() {

	private val playbackURL = "https://d1gl5ojlzl1vm6.cloudfront.net/veeps/playlist.m3u8"

	override fun getViewBinding(): FragmentLiveTvScreenBinding =
		FragmentLiveTvScreenBinding.inflate(layoutInflater)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.apply {
			liveTV = viewModel
			liveTVScreen = this@LiveTVScreen
			lifecycleOwner = viewLifecycleOwner
			lifecycle.addObserver(viewModel)
			loader.visibility = View.VISIBLE
			logo.requestFocus()
		}
		loadAppContent()
		notifyAppEvents()
	}

	override fun onDestroyView() {
		viewModelStore.clear()
		super.onDestroyView()
	}

	private fun loadAppContent() {
		binding.playerView.player = viewModel.player
		binding.playerView.isUiVisible = false
		binding.playerView.keepScreenOn = true
		binding.playerView.isFocusable = false
		binding.playerView.isFocusableInTouchMode = false
		binding.playerView.setOnKeyListener(null)

		viewModel.player.load(SourceConfig.fromUrl(playbackURL))

		addEventListener()
	}

	private fun addEventListener() {

		viewModel.player.on<PlayerEvent.Playing> {
			binding.loader.visibility = View.GONE
		}

		viewModel.player.on<PlayerEvent.StallStarted> {
			binding.loader.visibility = View.VISIBLE
		}

		viewModel.player.on<PlayerEvent.StallEnded> {
			binding.loader.visibility = View.GONE
		}

		viewModel.player.on<PlayerEvent.Error> {
			Logger.print("Error occurred: ${it.message}")
			binding.loader.visibility = View.GONE
		}
		viewModel.player.on<SourceEvent.Error> {
			Logger.print("Error occurred: ${it.message}")
			binding.loader.visibility = View.GONE
		}
	}

	private fun removeEventListener() {
		viewModel.player.off<PlayerEvent.Playing> {}
		viewModel.player.off<PlayerEvent.StallStarted> {}
		viewModel.player.off<PlayerEvent.StallEnded> {}
		viewModel.player.off<PlayerEvent.Error> {}
		viewModel.player.off<SourceEvent.Error> {}
	}

	override fun onResume() {
		super.onResume()
		if (!viewModel.player.isPlaying) {
			binding.playerView.onResume()
			addEventListener()
			viewModel.player.play()
		}
	}

	override fun onPause() {
		if (viewModel.player.isPlaying) {
			removeEventListener()
			viewModel.player.pause()
			binding.playerView.onPause()
		}
		super.onPause()
	}

	override fun onStart() {
		super.onStart()
		if (!viewModel.player.isPlaying) {
			binding.playerView.onStart()
		}
	}

	override fun onStop() {
		if (!viewModel.player.isPlaying) {
			viewModel.player.pause()
			binding.playerView.onStop()
		}
		super.onStop()
	}

	override fun onDestroy() {
		if (!viewModel.player.isPlaying) {
			releaseVideoPlayer()
		}
		super.onDestroy()
	}

	private fun releaseVideoPlayer() {
			viewModel.player.pause() // Ensure the player is paused
			removeEventListener() // Remove all event listeners
			viewModel.player.unload() // Unload the source to release resources
			viewModel.player.destroy() // Fully destroy the player instance
			binding.playerView.onDestroy() // Destroy the PlayerView
	}

	private fun notifyAppEvents() {
		homeViewModel.isNavigationMenuVisible.observe(viewLifecycleOwner) { isNavigationMenuVisible ->
			if (!isNavigationMenuVisible) {
				binding.logo.requestFocus()
			}
		}
		viewModel.isVisible.observeForever { isVisible ->
			if (isVisible) {
				helper.selectNavigationMenu(NavigationItems.LIVE_TV_MENU)
				helper.completelyHideNavigationMenu()
				if (!viewModel.player.isPlaying && homeViewModel.isErrorVisible.value?.equals(
						false
					) == true
				) {
					viewModel.player.play()
				}
			} else {
				if (viewModel.player.isPlaying) {
					viewModel.player.pause()
				}
			}
			Logger.print(
				"Visibility Changed to $isVisible On ${
					this@LiveTVScreen.javaClass.name.substringAfterLast(".")
				}"
			)
		}
	}
}
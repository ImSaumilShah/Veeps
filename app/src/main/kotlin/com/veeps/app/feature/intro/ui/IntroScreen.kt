package com.veeps.app.feature.intro.ui

import android.content.Intent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.veeps.app.R
import com.veeps.app.core.BaseActivity
import com.veeps.app.databinding.ActivityIntroScreenBinding
import com.veeps.app.extension.goToScreen
import com.veeps.app.feature.intro.viewModel.IntroViewModel
import com.veeps.app.feature.signIn.ui.SignInScreen
import com.veeps.app.util.AppConstants
import com.veeps.app.util.Screens
import kotlin.system.exitProcess


@UnstableApi
class IntroScreen : BaseActivity<IntroViewModel, ActivityIntroScreenBinding>() {

    private lateinit var player: ExoPlayer

    private fun getBackCallback(): OnBackPressedCallback {
        val backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTaskRoot) {
                    val homeIntent = Intent(Intent.ACTION_MAIN)
                    homeIntent.addCategory(Intent.CATEGORY_HOME)
                    homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(homeIntent)
                } else {
                    finishAffinity()
                    exitProcess(0)
                }
//				moveTaskToBack(true)
//				finish()
            }
        }
        return backPressedCallback
    }

    override fun getViewBinding(): ActivityIntroScreenBinding =
        ActivityIntroScreenBinding.inflate(layoutInflater)

    override fun showError(tag: String, message: String, description: String) {
        viewModel.contentHasLoaded.postValue(true)
    }

    override fun onRendered(viewModel: IntroViewModel, binding: ActivityIntroScreenBinding) {
        backPressedCallback = getBackCallback()
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        binding.apply {
            intro = viewModel
            introScreen = this@IntroScreen
            lifecycleOwner = this@IntroScreen
            signIn.requestFocus()
        }
    }

    private fun setupVideoPlayer() {
        releaseVideoPlayer()
        player = ExoPlayer.Builder(this@IntroScreen).build()
        player.setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true
        )
        binding.videoPlayer.player = player
        player.repeatMode = Player.REPEAT_MODE_ALL
        val playbackUrl = RawResourceDataSource.buildRawResourceUri(R.raw.splash_video)
        player.setMediaItem(MediaItem.fromUri(playbackUrl))
        player.prepare()
        player.play()
    }

    override fun onResume() {
        super.onResume()
        setupVideoPlayer()
    }

    override fun onStop() {
        releaseVideoPlayer()
        super.onStop()
    }

    override fun onDestroy() {
        releaseVideoPlayer()
        super.onDestroy()
    }

    private fun releaseVideoPlayer() {
        if (this::player.isInitialized) {
            player.playWhenReady = false
            player.pause()
            player.release()
        }
    }

    fun onSignIn() {
        goToScreen<SignInScreen>(
            false, Pair(AppConstants.TAG, Screens.SIGN_IN)
        )
    }

    fun onErrorPositive() {
        binding.errorLayoutContainer.visibility = View.GONE
        player.play()
    }
}
package com.veeps.app.feature.genre.ui

import android.os.Bundle
import android.view.View
import androidx.leanback.widget.BaseGridView
import androidx.media3.exoplayer.ExoPlayer
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.veeps.app.R
import com.veeps.app.core.BaseFragment
import com.veeps.app.databinding.FragmentGenreScreenBinding
import com.veeps.app.extension.isGreaterThan
import com.veeps.app.extension.loadImage
import com.veeps.app.feature.genre.viewModel.GenreViewModel
import com.veeps.app.feature.contentRail.adapter.ContentRailsAdapter
import com.veeps.app.feature.contentRail.model.Entities
import com.veeps.app.feature.contentRail.model.RailData
import com.veeps.app.util.AppAction
import com.veeps.app.util.AppUtil
import com.veeps.app.util.CardTypes
import com.veeps.app.util.DEFAULT
import com.veeps.app.util.DateTimeCompareDifference
import com.veeps.app.util.ImageTags
import com.veeps.app.util.Logger
import com.veeps.app.util.Screens
import org.json.JSONObject
import kotlin.random.Random


class GenreScreen : BaseFragment<GenreViewModel, FragmentGenreScreenBinding>() {

    private lateinit var carouselData: RailData
    private lateinit var player: ExoPlayer
    private var posterImage: String = DEFAULT.EMPTY_STRING
    private var playbackURL: String = DEFAULT.EMPTY_STRING
    private var requireCarouselRemoval: Boolean = true
    private var genreName = ""
    private var genreSlug = ""
    private val action by lazy {
        object : AppAction {
            override fun onAction(entity: Entities) {
                Logger.print(
                    "Action performed on ${
                        this@GenreScreen.javaClass.name.substringAfterLast(".")
                    }"
                )
                fetchEventDetails(entity)
            }
        }
    }
    private val railAdapter by lazy {
        ContentRailsAdapter(rails = arrayListOf(), helper, Screens.BROWSE, action)
    }

    override fun getViewBinding(): FragmentGenreScreenBinding =
        FragmentGenreScreenBinding.inflate(layoutInflater)

    override fun onDestroyView() {
        viewModel.railData.postValue(arrayListOf())
        viewModelStore.clear()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            genre = viewModel
            genreScreen = this@GenreScreen
            lifecycleOwner = viewLifecycleOwner
            lifecycle.addObserver(viewModel)
            loader.visibility = View.VISIBLE
            loader.requestFocus()
            carousel.visibility = View.VISIBLE
            watermark.visibility = View.GONE
        }
        loadAppContent()
        notifyAppEvents()
    }

    private fun loadAppContent() {
        if (arguments != null) {
            genreName = requireArguments().getString("genreName").toString()
            genreSlug = requireArguments().getString("genreSlug").toString()
        }
        binding.listing.apply {
            setNumColumns(1)
            setHasFixedSize(true)
            windowAlignment = BaseGridView.WINDOW_ALIGN_HIGH_EDGE
            windowAlignmentOffsetPercent = 0f
            isItemAlignmentOffsetWithPadding = true
            itemAlignmentOffsetPercent = 0f
            adapter = railAdapter
        }

        fetchGenreRails(genreSlug)
    }

    private fun notifyAppEvents() {
        homeViewModel.isNavigationMenuVisible.observe(viewLifecycleOwner) { isNavigationMenuVisible ->

        }
        homeViewModel.translateCarouselToTop.observe(viewLifecycleOwner) { shouldTranslate ->
            if (shouldTranslate && viewModel.isVisible.value == true) {
                binding.carousel.visibility = View.GONE
                binding.logo.visibility = View.GONE
                binding.watermark.visibility = View.VISIBLE
            }
        }
        homeViewModel.translateCarouselToBottom.observe(viewLifecycleOwner) { shouldTranslate ->
            if (shouldTranslate && viewModel.isVisible.value == true) {
                if (this::player.isInitialized && player.mediaItemCount.isGreaterThan(0) && !player.isPlaying && homeViewModel.isErrorVisible.value?.equals(
                        false
                    ) == true && homeViewModel.isNavigationMenuVisible.value?.equals(
                        false
                    ) == true
                ) {
                    player.play()
                }
                binding.watermark.visibility = View.GONE
                binding.carousel.visibility = View.VISIBLE
                binding.logo.visibility = View.VISIBLE
            }
        }

        viewModel.isVisible.observeForever { isVisible ->
            Logger.print(
                "Visibility Changed to $isVisible On ${
                    this@GenreScreen.javaClass.name.substringAfterLast(".")
                }"
            )
        }
        viewModel.railData.observe(viewLifecycleOwner) { rails ->
            if (rails.isNotEmpty()) {
                if (requireCarouselRemoval) {
                    var carouselPosition = 0
                    val carousel = rails.filterIndexed { index, railData ->
                        if (railData.cardType == CardTypes.HERO) {
                            carouselPosition = index
                        }
                        return@filterIndexed railData.cardType == CardTypes.HERO
                    }
                    if (carousel.isNotEmpty()) {
                        carouselData = rails[carouselPosition]
                        setCarousel()
                        rails.removeAt(carouselPosition)
                    }
                } else {
                    requireCarouselRemoval = true
                }
                binding.genreTitle.text = genreName
                rails.removeIf { rail ->
                    rail.cardType.equals(CardTypes.WIDE)
                }
                railAdapter.setRails(rails)
            } else {
                railAdapter.setRails(arrayListOf())
            }
            binding.genreTitle.text = genreName
        }
    }

    private fun setCarousel() {
        val random = Random.nextInt(carouselData.entities.size)
        val entity =
            if (carouselData.entities.isNotEmpty()) carouselData.entities[random] else Entities()

        posterImage =
            entity.presentation.posterUrl?.ifBlank { DEFAULT.EMPTY_STRING } ?: DEFAULT.EMPTY_STRING
        val logoImage =
            entity.presentation.logoUrl?.ifBlank { DEFAULT.EMPTY_STRING } ?: DEFAULT.EMPTY_STRING
        val videoPreviewTreeMap = entity.videoPreviews ?: false
        if (videoPreviewTreeMap is LinkedTreeMap<*, *>) {
            if (videoPreviewTreeMap.isNotEmpty()) {
                val jsonObject = Gson().toJsonTree(videoPreviewTreeMap).asJsonObject
                if (jsonObject != null && !jsonObject.isJsonNull && !jsonObject.isEmpty) {
                    val videoPreviewString: String = Gson().toJson(jsonObject)
                    val videoPreview = JSONObject(videoPreviewString)
                    if (videoPreview.has("high")) {
                        playbackURL = videoPreview.getString("high")
                    }
                }
            }
        }
        binding.carouselLogo.loadImage(logoImage, ImageTags.LOGO)
        binding.heroImage.loadImage(posterImage, ImageTags.HERO)

        binding.genreTitle.text = genreName
        binding.carousel.visibility = View.VISIBLE
    }

    private fun fetchEventDetails(entity: Entities) {
        viewModel.fetchEventDetails(entity.eventId ?: entity.id ?: DEFAULT.EMPTY_STRING)
            .observe(viewLifecycleOwner) { eventResponse ->
                fetch(
                    eventResponse,
                    isLoaderEnabled = true,
                    canUserAccessScreen = true,
                    shouldBeInBackground = false
                ) {
                    eventResponse.response?.let { eventStreamData ->
                        eventStreamData.data?.let {
                            val streamStartsAt = it.eventStreamStartsAt ?: DEFAULT.EMPTY_STRING
                            val doorOpensAt = it.eventDoorsAt ?: DEFAULT.EMPTY_STRING
                            if (doorOpensAt.isBlank()) {
                                if (streamStartsAt.isNotBlank() && AppUtil.compare(
                                        streamStartsAt
                                    ) == DateTimeCompareDifference.GREATER_THAN
                                ) {
                                    val eventId = it.eventId ?: it.id ?: DEFAULT.EMPTY_STRING
                                    val eventLogo =
                                        entity.presentation.logoUrl?.ifBlank { DEFAULT.EMPTY_STRING }
                                            ?: DEFAULT.EMPTY_STRING
                                    val eventTitle =
                                        entity.eventName?.ifBlank { DEFAULT.EMPTY_STRING }
                                            ?: DEFAULT.EMPTY_STRING
                                    helper.goToWaitingRoom(
                                        eventId, eventLogo, eventTitle, doorOpensAt, streamStartsAt
                                    )
                                } else {
                                    helper.goToVideoPlayer(
                                        it.eventId ?: it.id ?: DEFAULT.EMPTY_STRING
                                    )
                                }
                            } else {
                                if (AppUtil.compare(doorOpensAt) == DateTimeCompareDifference.LESS_THAN) {
                                    helper.goToVideoPlayer(
                                        it.eventId ?: it.id ?: DEFAULT.EMPTY_STRING
                                    )
                                } else if (streamStartsAt.isNotBlank() && AppUtil.compare(
                                        streamStartsAt
                                    ) == DateTimeCompareDifference.GREATER_THAN
                                ) {
                                    val eventId = it.eventId ?: it.id ?: DEFAULT.EMPTY_STRING
                                    val eventLogo =
                                        entity.presentation.logoUrl?.ifBlank { DEFAULT.EMPTY_STRING }
                                            ?: DEFAULT.EMPTY_STRING
                                    val eventTitle =
                                        entity.eventName?.ifBlank { DEFAULT.EMPTY_STRING }
                                            ?: DEFAULT.EMPTY_STRING
                                    helper.goToWaitingRoom(
                                        eventId, eventLogo, eventTitle, doorOpensAt, streamStartsAt
                                    )
                                } else {
                                    helper.goToVideoPlayer(
                                        it.eventId ?: it.id ?: DEFAULT.EMPTY_STRING
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun fetchGenreRails(genreScope: String) {
        viewModel.fetchGenreRails(genreScope).observe(viewLifecycleOwner) { genreRail ->
            fetch(
                genreRail,
                isLoaderEnabled = true,
                canUserAccessScreen = false,
                shouldBeInBackground = false
            ) {
                genreRail.response?.let { railResponse ->
                    viewModel.railData.postValue(railResponse.railData)
                } ?: helper.showErrorOnScreen(genreRail.tag, getString(R.string.unknown_error))
            }
        }
    }

}
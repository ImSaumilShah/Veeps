package com.veeps.app.feature.card.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.veeps.app.R
import com.veeps.app.databinding.RowCardBinding
import com.veeps.app.extension.isOfType
import com.veeps.app.feature.artist.ui.ArtistScreen
import com.veeps.app.feature.contentRail.model.Entities
import com.veeps.app.feature.contentRail.model.UserStats
import com.veeps.app.feature.event.ui.EventScreen
import com.veeps.app.feature.genre.ui.GenreScreen
import com.veeps.app.feature.venue.ui.VenueScreen
import com.veeps.app.util.AppAction
import com.veeps.app.util.AppConstants
import com.veeps.app.util.AppHelper
import com.veeps.app.util.AppUtil
import com.veeps.app.util.AppUtil.getGenreCardColor
import com.veeps.app.util.BadgeStatus
import com.veeps.app.util.CardTypes
import com.veeps.app.util.DEFAULT
import com.veeps.app.util.DateTimeCompareDifference
import com.veeps.app.util.EntityTypes
import com.veeps.app.util.EventTypes
import com.veeps.app.util.Image
import com.veeps.app.util.IntValue
import com.veeps.app.util.Screens
import java.util.Locale
import kotlin.math.roundToInt

class CardAdapter(private val action: AppAction) : RecyclerView.Adapter<CardAdapter.ViewHolder>() {

	private lateinit var context: Context
	private var entities: ArrayList<Entities> = arrayListOf()
	private var cardType: String? = CardTypes.PORTRAIT
	private var entitiesType: String? = EntityTypes.EVENT
	private lateinit var helper: AppHelper
	private var adapterPosition: Int = 0
	private var screen: String = Screens.BROWSE
	private var isContinueWatching: Boolean = false
	private var isWatchList: Boolean = false
	private var railCount: Int = 0
	private var isExpired: Boolean = false
	private var isRecommended: Boolean = false
	private var userStats: ArrayList<UserStats> = arrayListOf()
	private var loopingLimit = 7
	override fun onCreateViewHolder(
		parent: ViewGroup, viewType: Int,
	): ViewHolder {
		context = parent.context
		return ViewHolder(
			RowCardBinding.inflate(
				LayoutInflater.from(parent.context), parent, false
			)
		)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		var entityPosition = position
		if (entities.size > loopingLimit) entityPosition %= entities.size
		holder.binding.container.setOnKeyListener { _, keyCode, keyEvent ->
			val currentTime = System.currentTimeMillis()
			if (keyEvent.action == KeyEvent.ACTION_DOWN) {
				if (holder.bindingAdapterPosition == 0 && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
					when (screen) {
						Screens.BROWSE, Screens.SHOWS, Screens.ARTIST, Screens.VENUE, Screens.EVENT -> {
							helper.showNavigationMenu()
							true
						}

						Screens.SEARCH -> {
							helper.focusItem()
							true
						}

						else -> false
					}
				}
//				else if (entityPosition % entities.size == 0 && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
//						when (screen) {
//							Screens.BROWSE, Screens.SHOWS -> {
//								helper.showNavigationMenu()
//								true
//							}
//
//							else -> false
//						}
//				}
				else if (isRecommended && adapterPosition == 0 && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
					helper.focusItem()
					true
				} else if (adapterPosition == 0 && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
					helper.translateCarouselToBottom(true)
					true
				} else if (adapterPosition == 0 && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
					if (!isRecommended && screen == Screens.EVENT && railCount == 1) {
						helper.focusItem()
						true
					} else if (screen == Screens.ARTIST || screen == Screens.VENUE) {
						action.focusDown()
						true
					} else false
				} else if (screen == Screens.EVENT && railCount == 2 && adapterPosition == 1 && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
					helper.focusItem()
					true
				} else {
					false
				}
			} else {
				false
			}
		}

		holder.binding.container.setOnClickListener {
			if(entitiesType == EntityTypes.GENRES) {
				helper.setupPageChange(
					true, GenreScreen::class.java, bundleOf(
						AppConstants.TAG to Screens.GENRE,
						"genreSlug" to "genre-"+entities[entityPosition].slug,
						"genreName" to entities[entityPosition].name
					), Screens.GENRE, true
				)
			} else if (entitiesType == EntityTypes.ARTIST) {
				helper.setupPageChange(
					true, ArtistScreen::class.java, bundleOf(
						AppConstants.TAG to Screens.ARTIST,
						"entityId" to entities[entityPosition].id,
						"entity" to entitiesType
					), Screens.ARTIST, true
				)
			} else if (entitiesType == EntityTypes.VENUE) {
				helper.setupPageChange(
					true, VenueScreen::class.java, bundleOf(
						AppConstants.TAG to Screens.VENUE,
						"entityId" to entities[entityPosition].id,
						"entity" to entitiesType
					), Screens.VENUE, true
				)
			} else if (screen == Screens.SHOWS) {
				if (isExpired) {
					helper.setupPageChange(
						true, EventScreen::class.java, bundleOf(
							AppConstants.TAG to Screens.EVENT,
							"entityId" to (entities[entityPosition].eventId
								?: entities[entityPosition].id ?: DEFAULT.EMPTY_STRING),
							"entity" to entitiesType
						), Screens.EVENT, true
					)
				} else {
					if (!entities[entityPosition].eventStreamStartsAt.isNullOrBlank() && AppUtil.compare(
							entities[entityPosition].eventStreamStartsAt
						) == DateTimeCompareDifference.GREATER_THAN
					) {
						action.onAction(entities[entityPosition])
					} else {
						helper.goToVideoPlayer(
							entities[entityPosition].eventId ?: entities[entityPosition].id
							?: DEFAULT.EMPTY_STRING
						)
					}
				}
			} else if (isContinueWatching) {
				if (!entities[entityPosition].eventStreamStartsAt.isNullOrBlank() && AppUtil.compare(
						entities[entityPosition].eventStreamStartsAt
					) == DateTimeCompareDifference.GREATER_THAN
				) {
					action.onAction(entities[entityPosition])
				} else {
					helper.goToVideoPlayer(
						entities[entityPosition].eventId ?: entities[entityPosition].id
						?: DEFAULT.EMPTY_STRING
					)
				}
			} else {
				helper.setupPageChange(
					true, EventScreen::class.java, bundleOf(
						AppConstants.TAG to Screens.EVENT,
						"entityId" to entities[entityPosition].id,
						"entity" to entitiesType
					), Screens.EVENT, true
				)
			}
		}

		if (isWatchList) holder.binding.container.setOnLongClickListener {
			helper.removeEventFromWatchList(entities[entityPosition].id ?: DEFAULT.EMPTY_STRING)
			true
		}

		holder.binding.container.setOnFocusChangeListener { _, hasFocus ->
			if (hasFocus && screen == Screens.BROWSE && adapterPosition > 0) helper.translateCarouselToTop(true)
			if (hasFocus && screen != Screens.BROWSE) helper.translateCarouselToTop(true)
			if (hasFocus && adapterPosition == 0 && cardType == CardTypes.HERO) {
				helper.setCarousel(entityPosition)
			}
			when (cardType) {
				CardTypes.CIRCLE -> {
					holder.binding.artistVenueThumbnailContainer.setImageResource(if (hasFocus) R.drawable.rounded_card_image_background_focused else (if (entitiesType == EntityTypes.ARTIST) R.drawable.rounded_card_image_background_white_10 else R.drawable.rounded_card_image_background_transparent))
				}
				CardTypes.LANDSCAPE, CardTypes.HERO -> {
					holder.binding.eventLandscapeThumbnailContainer.setImageResource(if (hasFocus) R.drawable.rounded_landscape_card_image_background_focused else R.drawable.rounded_card_image_background_transparent)
				} else -> {
				holder.binding.container.setCardBackgroundColor(
					if (hasFocus) context.getColor(R.color.white) else context.getColor(
						android.R.color.transparent
					)
				)
			}
			}
		}

		holder.binding.artistVenueFollow.setOnFocusChangeListener { _, hasFocus ->
			holder.binding.artistVenueFollowLabel.setTextColor(context.getColor(if (hasFocus) R.color.black else R.color.white))
			holder.binding.artistVenueFollowIcon.setImageResource(if (hasFocus) R.drawable.add_black else R.drawable.add_white)
		}

		holder.binding.eventForeground.visibility =
			if (entities[entityPosition].isOfType(EventTypes.EXPIRED)) View.VISIBLE else View.GONE

		when (entitiesType) {
			EntityTypes.ARTIST, EntityTypes.VENUE -> {
				if (cardType == CardTypes.CIRCLE) {
					val image = entities[entityPosition].landscapeUrl ?: DEFAULT.EMPTY_STRING
					val title = entities[entityPosition].name ?: DEFAULT.EMPTY_STRING
					holder.binding.artistVenueTitle.text = title
					if (entitiesType == EntityTypes.ARTIST) holder.binding.artistVenueThumbnailContainer.setImageResource(
						R.drawable.rounded_card_image_background_white_10
					)
					val newResource = image.replace(Image.DEFAULT, Image.CIRCLE)
					Glide.with(holder.binding.artistVenueThumbnail.context).load(newResource)
						.diskCacheStrategy(DiskCacheStrategy.ALL).override(
							holder.binding.artistVenueThumbnail.measuredWidth,
							holder.binding.artistVenueThumbnail.measuredHeight
						)
//						.transition(DrawableTransitionOptions.withCrossFade())
						.transform(
							CenterCrop(),
							RoundedCorners(context.resources.getDimensionPixelSize(R.dimen.row_width_circle) / 2)
						)
//						.placeholder(R.drawable.rounded_card_background_white_10)
						.error(R.drawable.rounded_card_background_white_10)
						.into(holder.binding.artistVenueThumbnail)
//					Logger.printMessage("Artist or Venue Image url with optimized ($newResource) is requested to load.")

//				holder.binding.container.nextFocusDownId = R.id.artist_venue_follow
					holder.binding.artistVenueFollow.visibility = View.GONE
					holder.binding.eventContainer.visibility = View.GONE
					holder.binding.eventLandscapeContainer.visibility = View.GONE
					holder.binding.genreContainer.visibility = View.GONE
					holder.binding.artistVenueContainer.visibility = View.VISIBLE
				} else {
					val image = entities[entityPosition].portraitUrl ?: DEFAULT.EMPTY_STRING
					val title = entities[entityPosition].name ?: DEFAULT.EMPTY_STRING
					holder.binding.eventTitle.visibility = View.GONE
					holder.binding.eventLogoLabel.text = title
					holder.binding.eventLogo.visibility = View.GONE

					val newResource = image.replace(Image.DEFAULT, Image.CARD)
					Glide.with(holder.binding.eventThumbnail.context).load(newResource)
						.diskCacheStrategy(DiskCacheStrategy.ALL).override(
							holder.binding.eventThumbnail.measuredWidth,
							holder.binding.eventThumbnail.measuredHeight
						)
//						.transition(DrawableTransitionOptions.withCrossFade())
						.transform(CenterCrop(), RoundedCorners(IntValue.NUMBER_10))
//						.placeholder(R.drawable.rounded_card_background_black)
						.error(R.drawable.rounded_card_background_black)
						.into(holder.binding.eventThumbnail)
//					Logger.printMessage("Card Image url with optimized ($newResource) is requested to load.")

					holder.binding.eventDateContainer.visibility = View.GONE
					holder.binding.artistVenueFollow.visibility = View.GONE
					holder.binding.artistVenueContainer.visibility = View.GONE
					holder.binding.genreContainer.visibility = View.GONE
					holder.binding.eventLandscapeContainer.visibility = View.GONE
					holder.binding.eventContainer.visibility = View.VISIBLE
				}
			}

			EntityTypes.GENRES -> {
				val image = entities[entityPosition].imageUrl ?: DEFAULT.EMPTY_STRING
				val title = entities[entityPosition].name ?: DEFAULT.EMPTY_STRING
				holder.binding.eventContainer.visibility = View.GONE
				holder.binding.eventLandscapeContainer.visibility = View.GONE
				holder.binding.artistVenueContainer.visibility = View.GONE
				holder.binding.genreContainer.visibility = View.VISIBLE
				holder.binding.genreLabel.text = title

				Glide.with(holder.binding.genreBackground.context)
					.asDrawable()
					.load(ColorDrawable(context.getColor(getGenreCardColor(title))))
					.diskCacheStrategy(DiskCacheStrategy.ALL)
					.transform(CenterCrop(), RoundedCorners(IntValue.NUMBER_10))
					.into(holder.binding.genreBackground)

				Glide.with(holder.binding.genreThumbnail.context).load(image)
					.diskCacheStrategy(DiskCacheStrategy.ALL)
					.transform(CenterCrop(), RoundedCorners(IntValue.NUMBER_10))
					.into(holder.binding.genreThumbnail)
			}

			else -> {
				if (cardType == CardTypes.LANDSCAPE || cardType == CardTypes.HERO) {
					var image: String
					entities[entityPosition].eventPosterUrl.let {
						image = it ?: DEFAULT.EMPTY_STRING
					}
					val title = entities[entityPosition].eventName
					val artistTitle =
						if (!entities[entityPosition].presentation.subtitle.isNullOrBlank()) entities[entityPosition].presentation.subtitle else DEFAULT.EMPTY_STRING

					holder.binding.eventLandscapeTitle.text = title
					holder.binding.eventLandscapeArtistName.text = artistTitle

					val newResource = image.replace(Image.DEFAULT, Image.CARD_HORIZONTAL)
					Glide.with(holder.binding.eventLandscapeThumbnail.context).load(newResource)
						.diskCacheStrategy(DiskCacheStrategy.ALL).override(
							holder.binding.eventLandscapeThumbnail.measuredWidth,
							holder.binding.eventLandscapeThumbnail.measuredHeight
						)
//					.transition(DrawableTransitionOptions.withCrossFade())
						.transform(CenterCrop(), RoundedCorners(IntValue.NUMBER_LANDSCAPE))
//					.placeholder(R.drawable.rounded_card_background_black)
						.error(R.drawable.rounded_card_background_black)
						.into(holder.binding.eventLandscapeThumbnail)
//				Logger.printMessage("Card Image url with optimized ($newResource) is requested to load.")

					holder.binding.artistVenueFollow.visibility = View.GONE
					holder.binding.artistVenueContainer.visibility = View.GONE
					holder.binding.genreContainer.visibility = View.GONE
					holder.binding.eventContainer.visibility = View.GONE
					holder.binding.eventLandscapeContainer.visibility = View.VISIBLE
				} else {
					when (val badgeStatus: String = AppUtil.getBadgeStatus(
						entities[entityPosition], isContinueWatching, screen == Screens.SHOWS
					)) {
						BadgeStatus.LIVE -> {
							holder.binding.eventLiveBadgeContainer.visibility = View.VISIBLE
							holder.binding.eventDateContainer.visibility = View.GONE
						}

						BadgeStatus.DO_NOT_SHOW, BadgeStatus.NOTHING,
							-> {
							holder.binding.eventLiveBadgeContainer.visibility = View.GONE
							holder.binding.eventDateContainer.visibility = View.GONE
						}

						else -> {
							holder.binding.eventLiveBadgeContainer.visibility = View.GONE
							holder.binding.eventDate.text = badgeStatus
							holder.binding.eventDateContainer.visibility =
								if (badgeStatus.isBlank()) View.GONE else View.VISIBLE
						}
					}

					if (isContinueWatching) {
						holder.binding.continueWatchingProgress.visibility = View.VISIBLE
						val stats =
							userStats.filter { it.eventId == entities[entityPosition].eventId }
						if (stats.size == 1 && ((stats[0].cursor / stats[0].duration) * 100) < 95) {
							holder.binding.continueWatchingProgress.max =
								stats[0].duration.roundToInt()
							holder.binding.continueWatchingProgress.progress =
								stats[0].cursor.roundToInt()
						}
					} else {
						holder.binding.continueWatchingProgress.visibility = View.GONE
					}
					var image: String
					entities[entityPosition].presentation.let {
						image = it.portraitUrl ?: DEFAULT.EMPTY_STRING
					}
					val title = entities[entityPosition].eventName
					val artistTitle =
						if (!entities[entityPosition].presentation.subtitle.isNullOrBlank()) entities[entityPosition].presentation.subtitle else DEFAULT.EMPTY_STRING
					entities[entityPosition].presentation.let {
						if (!it.badgeBgColor.isNullOrBlank()) holder.binding.eventBadgeContainer.background.colorFilter =
							BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
								Color.parseColor(it.badgeBgColor), BlendModeCompat.SRC_OVER
							)

						if (!it.badgeFgColor.isNullOrBlank()) holder.binding.eventBadge.setTextColor(
							Color.parseColor(
								it.badgeFgColor
							)
						)

						if (it.badgeLabel.isNullOrBlank()) {
							holder.binding.eventBadgeContainer.visibility = View.GONE
						} else {
							val firstLetter = it.badgeLabel!!.substring(0, 1).uppercase(Locale.ROOT)
							val labelWOFirstLetter = it.badgeLabel!!.substring(1)
							val label = "$firstLetter$labelWOFirstLetter"
							holder.binding.eventBadge.text = label
						}
					}
					holder.binding.eventTitle.text = title
					holder.binding.eventLogoLabel.text = artistTitle
					holder.binding.eventLogo.visibility = View.GONE

					val newResource = image.replace(Image.DEFAULT, Image.CARD)
					Glide.with(holder.binding.eventThumbnail.context).load(newResource)
						.diskCacheStrategy(DiskCacheStrategy.ALL).override(
							holder.binding.eventThumbnail.measuredWidth,
							holder.binding.eventThumbnail.measuredHeight
						)
//					.transition(DrawableTransitionOptions.withCrossFade())
						.transform(CenterCrop(), RoundedCorners(IntValue.NUMBER_10))
//					.placeholder(R.drawable.rounded_card_background_black)
						.error(R.drawable.rounded_card_background_black)
						.into(holder.binding.eventThumbnail)
//				Logger.printMessage("Card Image url with optimized ($newResource) is requested to load.")

					holder.binding.eventDateContainer.setupWith(holder.binding.container)
						.setBlurRadius(12.5f)
					holder.binding.eventDateContainer.outlineProvider =
						ViewOutlineProvider.BACKGROUND
					holder.binding.eventDateContainer.clipToOutline = true
					holder.binding.artistVenueFollow.visibility = View.GONE
					holder.binding.artistVenueContainer.visibility = View.GONE
					holder.binding.genreContainer.visibility = View.GONE
					holder.binding.eventLandscapeContainer.visibility = View.GONE
					holder.binding.eventContainer.visibility = View.VISIBLE
				}
			}
		}
	}

	override fun getItemId(position: Int): Long {
		var entityPosition = position
		if (entities.size > loopingLimit) entityPosition %= entities.size
		return entityPosition.toLong()
	}

	override fun getItemCount() = if (screen == Screens.BROWSE || screen == Screens.SHOWS) {
		if (entities.size <= loopingLimit) entities.size else Int.MAX_VALUE
	} else entities.size

	fun setEntities(entities: ArrayList<Entities>) {
		this.entities = entities
	}

	fun setCardType(cardType: String) {
		this.cardType = cardType
	}

	fun setEntityType(entitiesType: String) {
		this.entitiesType = entitiesType
	}

	fun setHelper(helper: AppHelper) {
		this.helper = helper
	}

	fun setAdapterPosition(adapterPosition: Int) {
		this.adapterPosition = adapterPosition
	}

	fun setScreen(screen: String) {
		this.screen = screen
	}

	fun setContinueWatching(isContinueWatching: Boolean) {
		this.isContinueWatching = isContinueWatching
	}

	fun setWatchList(isWatchList: Boolean) {
		this.isWatchList = isWatchList
	}

	fun setRailCount(railCount: Int) {
		this.railCount = railCount
	}

	fun setExpired(isExpired: Boolean) {
		this.isExpired = isExpired
	}

	fun setUserStats(userStats: ArrayList<UserStats>) {
		this.userStats = userStats
	}

	fun setIsRecommended(isRecommended: Boolean) {
		this.isRecommended = isRecommended
	}

	inner class ViewHolder(val binding: RowCardBinding) : RecyclerView.ViewHolder(binding.root)
}
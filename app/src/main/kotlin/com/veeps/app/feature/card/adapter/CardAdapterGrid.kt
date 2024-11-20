package com.veeps.app.feature.card.adapter

import android.content.Context
import android.graphics.Color
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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.veeps.app.R
import com.veeps.app.databinding.RowCardGridBinding
import com.veeps.app.extension.isOfType
import com.veeps.app.feature.artist.ui.ArtistScreen
import com.veeps.app.feature.contentRail.model.Entities
import com.veeps.app.feature.contentRail.model.UserStats
import com.veeps.app.feature.event.ui.EventScreen
import com.veeps.app.feature.venue.ui.VenueScreen
import com.veeps.app.util.AppAction
import com.veeps.app.util.AppConstants
import com.veeps.app.util.AppHelper
import com.veeps.app.util.AppUtil
import com.veeps.app.util.BadgeStatus
import com.veeps.app.util.CardTypes
import com.veeps.app.util.DEFAULT
import com.veeps.app.util.DateTimeCompareDifference
import com.veeps.app.util.EntityTypes
import com.veeps.app.util.EventTypes
import com.veeps.app.util.Image
import com.veeps.app.util.IntValue
import com.veeps.app.util.Logger
import com.veeps.app.util.Screens
import java.util.Locale
import kotlin.math.roundToInt

class CardAdapterGrid(private val action: AppAction) :
	RecyclerView.Adapter<CardAdapterGrid.ViewHolder>() {

	private lateinit var context: Context
	private var entities: ArrayList<Entities> = arrayListOf()
	private var cardType: String? = CardTypes.PORTRAIT
	private var entitiesType: String? = EntityTypes.EVENT
	private lateinit var helper: AppHelper
	private var adapterPosition: Int = 0
	private var screen: String = Screens.BROWSE
	private var isContinueWatching: Boolean = false
	private var isWatchList: Boolean = false
	private var isExpired: Boolean = false
	private var userStats: ArrayList<UserStats> = arrayListOf()
	override fun onCreateViewHolder(
		parent: ViewGroup, viewType: Int,
	): ViewHolder {
		context = parent.context
		return ViewHolder(
			RowCardGridBinding.inflate(
				LayoutInflater.from(parent.context), parent, false
			)
		)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.binding.container.setOnClickListener {
			if (entitiesType == EntityTypes.ARTIST) {
				helper.setupPageChange(
					true, ArtistScreen::class.java, bundleOf(
						AppConstants.TAG to Screens.ARTIST,
						"entityId" to entities[position].id,
						"entity" to entitiesType
					), Screens.ARTIST, true
				)
			} else if (entitiesType == EntityTypes.VENUE) {
				helper.setupPageChange(
					true, VenueScreen::class.java, bundleOf(
						AppConstants.TAG to Screens.VENUE,
						"entityId" to entities[position].id,
						"entity" to entitiesType
					), Screens.VENUE, true
				)
			} else if (screen == Screens.SHOWS) {
				if (isExpired) {
					helper.setupPageChange(
						true, EventScreen::class.java, bundleOf(
							AppConstants.TAG to Screens.EVENT,
							"entityId" to (entities[position].eventId ?: entities[position].id
							?: DEFAULT.EMPTY_STRING),
							"entity" to entitiesType
						), Screens.EVENT, true
					)
				} else {
					if (!entities[position].eventStreamStartsAt.isNullOrBlank().and(
							AppUtil.compare(
								entities[position].eventStreamStartsAt
							) == DateTimeCompareDifference.GREATER_THAN
						)
					) {
						action.onAction(entities[position])
					} else {
						helper.goToVideoPlayer(
							entities[position].eventId ?: entities[position].id
							?: DEFAULT.EMPTY_STRING
						)
					}
				}
			} else if (isContinueWatching) {
				if (!entities[position].eventStreamStartsAt.isNullOrBlank() && AppUtil.compare(
						entities[position].eventStreamStartsAt
					) == DateTimeCompareDifference.GREATER_THAN
				) {
					action.onAction(entities[position])
				} else {
					helper.goToVideoPlayer(
						entities[position].eventId ?: entities[position].id ?: DEFAULT.EMPTY_STRING
					)
				}
			} else {
				helper.setupPageChange(
					true, EventScreen::class.java, bundleOf(
						AppConstants.TAG to Screens.EVENT,
						"entityId" to entities[position].id,
						"entity" to entitiesType
					), Screens.EVENT, true
				)
			}
		}

		if (isWatchList) holder.binding.container.setOnLongClickListener {
			helper.removeEventFromWatchList(entities[position].id ?: DEFAULT.EMPTY_STRING)
			true
		}

		holder.binding.container.setOnFocusChangeListener { _, hasFocus ->
			if (cardType == CardTypes.CIRCLE) {
				holder.binding.artistVenueThumbnailContainer.setImageResource(if (hasFocus) R.drawable.rounded_card_image_background_focused else (if (entitiesType == EntityTypes.ARTIST) R.drawable.rounded_card_image_background_white_10 else R.drawable.rounded_card_image_background_transparent))
			} else {
				holder.binding.container.setCardBackgroundColor(
					if (hasFocus) context.getColor(R.color.white) else context.getColor(
						android.R.color.transparent
					)
				)
			}
		}

		holder.binding.artistVenueFollow.setOnFocusChangeListener { _, hasFocus ->
			holder.binding.artistVenueFollowLabel.setTextColor(context.getColor(if (hasFocus) R.color.black else R.color.white))
			holder.binding.artistVenueFollowIcon.setImageResource(if (hasFocus) R.drawable.add_black else R.drawable.add_white)
		}

		holder.binding.eventForeground.visibility =
			if (entities[position].isOfType(EventTypes.EXPIRED)) View.VISIBLE else View.GONE

		if (entitiesType == EntityTypes.ARTIST || entitiesType == EntityTypes.VENUE) {
			if (cardType == CardTypes.CIRCLE) {
				val image = entities[position].landscapeUrl ?: DEFAULT.EMPTY_STRING
				val title = entities[position].name ?: DEFAULT.EMPTY_STRING
				holder.binding.artistVenueTitle.text = title
				if (entitiesType == EntityTypes.ARTIST) holder.binding.artistVenueThumbnailContainer.setImageResource(
					R.drawable.rounded_card_image_background_white_10
				)

				val newResource = image.replace(Image.DEFAULT, Image.CIRCLE)
				Glide.with(holder.binding.artistVenueThumbnail.context).load(newResource)
					.diskCacheStrategy(DiskCacheStrategy.ALL)
					.transition(DrawableTransitionOptions.withCrossFade())
					.transform(CenterCrop(), RoundedCorners(IntValue.NUMBER_2000))
					.placeholder(R.drawable.rounded_card_background_white_10)
					.placeholder(R.drawable.rounded_card_background_white_10)
					.error(R.drawable.rounded_card_background_white_10)
					.into(holder.binding.artistVenueThumbnail)
				Logger.printMessage("Artist or Venue Image url with optimized ($newResource) is requested to load.")

//				holder.binding.container.nextFocusDownId = R.id.artist_venue_follow
				holder.binding.artistVenueFollow.visibility = View.GONE
				holder.binding.artistVenueContainer.visibility = View.VISIBLE
				holder.binding.eventContainer.visibility = View.GONE
			} else {
				val image = entities[position].portraitUrl ?: DEFAULT.EMPTY_STRING
				val title = entities[position].name ?: DEFAULT.EMPTY_STRING
				holder.binding.eventTitle.visibility = View.GONE
				holder.binding.eventLogoLabel.text = title
				holder.binding.eventLogo.visibility = View.GONE

				val newResource = image.replace(Image.DEFAULT, Image.CARD)
				Glide.with(holder.binding.eventThumbnail.context).load(newResource)
					.diskCacheStrategy(DiskCacheStrategy.ALL)
					.transition(DrawableTransitionOptions.withCrossFade())
					.transform(CenterCrop(), RoundedCorners(IntValue.NUMBER_10))
					.placeholder(R.drawable.rounded_card_background_black)
					.error(R.drawable.rounded_card_background_black)
					.into(holder.binding.eventThumbnail)
				Logger.printMessage("Card Image url with optimized ($newResource) is requested to load.")

				holder.binding.eventDateContainer.visibility = View.GONE
				holder.binding.artistVenueFollow.visibility = View.GONE
				holder.binding.artistVenueContainer.visibility = View.GONE
				holder.binding.eventContainer.visibility = View.VISIBLE
			}
		} else {

			when (val badgeStatus: String = AppUtil.getBadgeStatus(
				entities[position], isContinueWatching, screen == Screens.SHOWS
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
				val stats = userStats.filter { it.eventId == entities[position].eventId }
				if (stats.size == 1 && ((stats[0].cursor / stats[0].duration) * 100) < 95) {
					holder.binding.continueWatchingProgress.max = stats[0].duration.roundToInt()
					holder.binding.continueWatchingProgress.progress = stats[0].cursor.roundToInt()
				}
			} else {
				holder.binding.continueWatchingProgress.visibility = View.GONE
			}
			var image: String
			entities[position].presentation.let {
				image = it.portraitUrl ?: DEFAULT.EMPTY_STRING
			}
			val title = entities[position].eventName
			val artistTitle =
				if (!entities[position].presentation.subtitle.isNullOrBlank()) entities[position].presentation.subtitle else DEFAULT.EMPTY_STRING
			entities[position].presentation.let {
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
				.diskCacheStrategy(DiskCacheStrategy.ALL)
				.transition(DrawableTransitionOptions.withCrossFade())
				.transform(CenterCrop(), RoundedCorners(IntValue.NUMBER_10))
				.placeholder(R.drawable.rounded_card_background_black)
				.error(R.drawable.rounded_card_background_black)
				.into(holder.binding.eventThumbnail)
			Logger.printMessage("Card Image url with optimized ($newResource) is requested to load.")

			holder.binding.eventDateContainer.setupWith(holder.binding.container)
				.setBlurRadius(12.5f)
			holder.binding.eventDateContainer.outlineProvider = ViewOutlineProvider.BACKGROUND
			holder.binding.eventDateContainer.clipToOutline = true
			holder.binding.artistVenueFollow.visibility = View.GONE
			holder.binding.artistVenueContainer.visibility = View.GONE
			holder.binding.eventContainer.visibility = View.VISIBLE
		}
	}

	override fun getItemId(position: Int): Long {
		return position.toLong()
	}

	override fun getItemCount() = entities.size

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

	fun setExpired(isExpired: Boolean) {
		this.isExpired = isExpired
	}

	fun setUserStats(userStats: ArrayList<UserStats>) {
		this.userStats = userStats
	}

	inner class ViewHolder(val binding: RowCardGridBinding) : RecyclerView.ViewHolder(binding.root)
}
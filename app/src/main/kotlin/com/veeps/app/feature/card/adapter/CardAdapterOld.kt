package com.veeps.app.feature.card.adapter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.veeps.app.R
import com.veeps.app.databinding.RowCardBinding
import com.veeps.app.extension.isOfType
import com.veeps.app.feature.artist.ui.ArtistScreen
import com.veeps.app.feature.contentRail.model.Entities
import com.veeps.app.feature.contentRail.model.UserStats
import com.veeps.app.feature.event.ui.EventScreen
import com.veeps.app.feature.genre.ui.GenreScreen
import com.veeps.app.feature.venue.ui.VenueScreen
import com.veeps.app.util.*
import java.lang.ref.WeakReference

class CardAdapterOld(
	private val action: AppAction, private val context: Context, helper: AppHelper
) : RecyclerView.Adapter<CardAdapterOld.ViewHolder>() {

	private val entities = mutableListOf<Entities>()
	private val userStats = mutableListOf<UserStats>()

	private var cardType: String = CardTypes.PORTRAIT
	private var entityType: String = EntityTypes.EVENT
	private var screen: String = Screens.BROWSE

	private val helperRef = WeakReference(helper)

	private var isContinueWatching: Boolean = false
	private var isWatchList: Boolean = false
	private var railCount: Int = 0
	private var isExpired: Boolean = false
	private var isRecommended: Boolean = false
	private val loopingLimit = 7

	private val portraitOptions =
		RequestOptions().diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.AUTOMATIC)
			.transform(
				com.bumptech.glide.load.resource.bitmap.CenterCrop(),
				com.bumptech.glide.load.resource.bitmap.RoundedCorners(10)
			)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val binding = RowCardBinding.inflate(LayoutInflater.from(context), parent, false)
		return ViewHolder(binding)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val entityPosition = getEntityPosition(position)
		val entity = entities[entityPosition]

		holder.bind(entity, entityPosition)
	}

	override fun getItemCount(): Int {
		return if (entities.isEmpty()) {
			Log.i("CardAdapterOld", "No items available in the dataset.")
			0
		} else if (screen == Screens.BROWSE || screen == Screens.SHOWS) {
			if (entities.size <= loopingLimit) entities.size else Int.MAX_VALUE
		} else {
			entities.size
		}
	}

	override fun getItemId(position: Int): Long {
		val entityPosition = getEntityPosition(position)
		val entityId = entities[entityPosition].id
		return entityId?.hashCode()?.toLong() ?: position.toLong()
	}

	private fun getEntityPosition(position: Int): Int {
		return if (entities.size > loopingLimit) position % entities.size else position
	}

	fun updateEntities(newEntities: List<Entities>) {
		Handler(Looper.getMainLooper()).post {
			val diffCallback = EntitiesDiffCallback(entities, newEntities)
			val diffResult = DiffUtil.calculateDiff(diffCallback)

			entities.clear()
			entities.addAll(newEntities)
			diffResult.dispatchUpdatesTo(this)
		}
	}

	fun setCardType(cardType: String) {
		this.cardType = cardType
	}

	fun setEntityType(entityType: String) {
		this.entityType = entityType
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

	fun setUserStats(userStats: List<UserStats>) {
		this.userStats.clear()
		this.userStats.addAll(userStats)
	}

	fun setIsRecommended(isRecommended: Boolean) {
		this.isRecommended = isRecommended
	}

	inner class ViewHolder(private val binding: RowCardBinding) :
		RecyclerView.ViewHolder(binding.root) {

		fun bind(entity: Entities, entityPosition: Int) {
			setupListeners(entity, entityPosition)
			configureUI(entity, entityPosition)
		}

		private fun setupListeners(entity: Entities, entityPosition: Int) {
			// Key Event Listener
			binding.container.setOnKeyListener { _, keyCode, keyEvent ->
				handleKeyEvents(keyCode, keyEvent, entityPosition)
			}

			// Click Listener
			binding.container.setOnClickListener {
				handleItemClick(entity)
			}

			// Long Click Listener for WatchList
			if (isWatchList) {
				binding.container.setOnLongClickListener {
					handleLongClick(entity)
					true
				}
			} else {
				binding.container.setOnLongClickListener(null)
			}

			// Focus Change Listener for the Container
			binding.container.setOnFocusChangeListener { _, hasFocus ->
				handleFocusChange(hasFocus, entityPosition)
			}

			// Focus Change Listener for Follow Button (if applicable)
			binding.artistVenueFollow.setOnFocusChangeListener { _, hasFocus ->
				handleFollowFocusChange(hasFocus)
			}
		}

		private fun handleKeyEvents(
			keyCode: Int, keyEvent: KeyEvent, entityPosition: Int
		): Boolean {
			if (keyEvent.action == KeyEvent.ACTION_DOWN) {
				val helper = helperRef.get()
				if (bindingAdapterPosition == RecyclerView.NO_POSITION) return false

				return when {
					bindingAdapterPosition == 0 && keyCode == KeyEvent.KEYCODE_DPAD_LEFT -> {
						when (screen) {
							Screens.BROWSE, Screens.SHOWS, Screens.ARTIST, Screens.VENUE, Screens.EVENT -> {
								helper?.showNavigationMenu()
								true
							}

							Screens.SEARCH -> {
								helper?.focusItem()
								true
							}

							else -> false
						}
					}

					isRecommended && bindingAdapterPosition == 0 && keyCode == KeyEvent.KEYCODE_DPAD_UP -> {
						helper?.focusItem()
						true
					}

					screen != Screens.BROWSE && bindingAdapterPosition == 0 && keyCode == KeyEvent.KEYCODE_DPAD_UP -> {
						helper?.translateCarouselToBottom(true)
						true
					}

					screen == Screens.BROWSE && bindingAdapterPosition == 1 && keyCode == KeyEvent.KEYCODE_DPAD_UP -> {
						helper?.translateCarouselToBottom(true)
						false
					}

					bindingAdapterPosition == 0 && keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
						if (!isRecommended && screen == Screens.EVENT && railCount == 1) {
							helper?.focusItem()
							true
						} else if (screen == Screens.ARTIST || screen == Screens.VENUE) {
							action.focusDown()
							true
						} else false
					}

					screen == Screens.EVENT && railCount == 2 && bindingAdapterPosition == 1 && keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
						helper?.focusItem()
						true
					}

					else -> false
				}
			}
			return false
		}

		private fun handleItemClick(entity: Entities) {
			val helper = helperRef.get()
			if (helper != null) {
				when (entityType) {
					EntityTypes.GENRES -> navigateToGenreScreen(helper, entity)
					EntityTypes.ARTIST -> navigateToArtistScreen(helper, entity)
					EntityTypes.VENUE -> navigateToVenueScreen(helper, entity)
					else -> navigateToEventScreen(helper, entity)
				}
			} else {
				Toast.makeText(
					context, "Navigation not possible, helper unavailable.", Toast.LENGTH_SHORT
				).show()
			}
		}

		private fun handleLongClick(entity: Entities) {
			val entityId = entity.id ?: ""
			val helper = helperRef.get()
			if (helper != null) {
				helper.removeEventFromWatchList(entityId)
			} else {
				Toast.makeText(context, "Unable to remove from watchlist.", Toast.LENGTH_SHORT)
					.show()
			}
		}

		private fun handleFocusChange(hasFocus: Boolean, entityPosition: Int) {
			val helper = helperRef.get()
			if (hasFocus && screen == Screens.BROWSE && bindingAdapterPosition > 0) {
				helper?.translateCarouselToTop(true)
			}
			if (hasFocus && screen != Screens.BROWSE) {
				helper?.translateCarouselToTop(true)
			}
			if (hasFocus && bindingAdapterPosition == 0 && cardType == CardTypes.HERO) {
				helper?.setCarousel(entityPosition)
			}

			when (cardType) {
				CardTypes.CIRCLE -> {
					binding.artistVenueThumbnailContainer.setImageResource(
						if (hasFocus) R.drawable.rounded_card_image_background_focused else (if (entityType == EntityTypes.ARTIST) R.drawable.rounded_card_image_background_white_10
						else R.drawable.rounded_card_image_background_transparent)
					)
				}

				CardTypes.LANDSCAPE, CardTypes.HERO -> {
					binding.eventLandscapeThumbnailContainer.setImageResource(
						if (hasFocus) R.drawable.rounded_landscape_card_image_background_focused else R.drawable.rounded_card_image_background_transparent
					)
				}

				else -> {
					binding.container.setCardBackgroundColor(
						if (hasFocus) context.getColor(R.color.white) else context.getColor(android.R.color.transparent)
					)
				}
			}
		}

		private fun handleFollowFocusChange(hasFocus: Boolean) {
			binding.artistVenueFollowLabel.setTextColor(
				context.getColor(if (hasFocus) R.color.black else R.color.white)
			)
			binding.artistVenueFollowIcon.setImageResource(
				if (hasFocus) R.drawable.add_black else R.drawable.add_white
			)
		}

		private fun configureUI(entity: Entities, entityPosition: Int) {
			// Event foreground visibility for expired events
			binding.eventForeground.visibility =
				if (entity.isOfType(EventTypes.EXPIRED)) View.VISIBLE else View.GONE

			when (entityType) {
				EntityTypes.ARTIST, EntityTypes.VENUE -> configureArtistVenueUI(entity)
				EntityTypes.GENRES -> configureGenreUI(entity)
				else -> configureEventUI(entity, entityPosition)
			}
		}

		private fun configureArtistVenueUI(entity: Entities) {
			binding.artistVenueContainer.visibility = View.VISIBLE
			binding.eventContainer.visibility = View.GONE
			binding.genreContainer.visibility = View.GONE

			binding.artistVenueTitle.text = entity.name ?: DEFAULT.EMPTY_STRING

			val imageUrl = entity.landscapeUrl
			if (!imageUrl.isNullOrEmpty()) {
				loadImage(imageUrl, binding.artistVenueThumbnail, portraitOptions)
			} else {
				binding.artistVenueThumbnail.setImageResource(R.drawable.rounded_card_background_black)
			}

			binding.artistVenueFollow.visibility = View.GONE
		}

		private fun configureGenreUI(entity: Entities) {
			binding.genreContainer.visibility = View.VISIBLE
			binding.artistVenueContainer.visibility = View.GONE
			binding.eventContainer.visibility = View.GONE

			binding.genreLabel.text = entity.name ?: DEFAULT.EMPTY_STRING

			val imageUrl = entity.imageUrl
			if (!imageUrl.isNullOrEmpty()) {
				loadImage(imageUrl, binding.genreThumbnail, portraitOptions)
			} else {
				binding.genreThumbnail.setImageResource(R.drawable.rounded_card_background_black)
			}
		}

		private fun configureEventUI(entity: Entities, entityPosition: Int) {
			binding.eventContainer.visibility = View.VISIBLE
			binding.artistVenueContainer.visibility = View.GONE
			binding.genreContainer.visibility = View.GONE

			binding.eventTitle.text = entity.eventName ?: DEFAULT.EMPTY_STRING

			val imageUrl = entity.presentation.portraitUrl
			if (!imageUrl.isNullOrEmpty()) {
				loadImage(imageUrl, binding.eventThumbnail, portraitOptions)
			} else {
				binding.eventThumbnail.setImageResource(R.drawable.rounded_card_background_black)
			}

			// Configure live badge, progress bar for continue watching, etc.
			if (entity.isOfType(EventTypes.LIVE)) {
				binding.eventLiveBadgeContainer.visibility = View.VISIBLE
			} else {
				binding.eventLiveBadgeContainer.visibility = View.GONE
			}

			// Configure progress bar for continue watching
			if (isContinueWatching) {
				binding.continueWatchingProgress.visibility = View.VISIBLE
				val stats = userStats.firstOrNull { it.eventId == entity.eventId }
				if (stats != null && stats.duration > 0) {
					binding.continueWatchingProgress.max = stats.duration.toInt()
					binding.continueWatchingProgress.progress = stats.cursor.toInt()
				}
			} else {
				binding.continueWatchingProgress.visibility = View.GONE
			}
		}

		private fun loadImage(url: String, imageView: ImageView, options: RequestOptions) {
			Glide.with(imageView.context).load(url).apply(options)
				.placeholder(R.drawable.rounded_card_background_black)
				.error(R.drawable.rounded_card_background_black).into(imageView)
		}

		private fun navigateToGenreScreen(helper: AppHelper, entity: Entities) {
			val bundle = bundleOf(
				AppConstants.TAG to Screens.GENRE,
				"genreSlug" to "genre-${entity.slug}",
				"genreName" to entity.name
			)
			helper.setupPageChange(true, GenreScreen::class.java, bundle, Screens.GENRE, true)
		}

		private fun navigateToArtistScreen(helper: AppHelper, entity: Entities) {
			val bundle = bundleOf(
				AppConstants.TAG to Screens.ARTIST, "entityId" to entity.id, "entity" to entityType
			)
			helper.setupPageChange(true, ArtistScreen::class.java, bundle, Screens.ARTIST, true)
		}

		private fun navigateToVenueScreen(helper: AppHelper, entity: Entities) {
			val bundle = bundleOf(
				AppConstants.TAG to Screens.VENUE, "entityId" to entity.id, "entity" to entityType
			)
			helper.setupPageChange(true, VenueScreen::class.java, bundle, Screens.VENUE, true)
		}

		private fun navigateToEventScreen(helper: AppHelper, entity: Entities) {
			val bundle = bundleOf(
				AppConstants.TAG to Screens.EVENT, "entityId" to entity.id, "entity" to entityType
			)
			helper.setupPageChange(true, EventScreen::class.java, bundle, Screens.EVENT, true)
		}
	}

	class EntitiesDiffCallback(
		private val oldList: List<Entities>, private val newList: List<Entities>
	) : DiffUtil.Callback() {

		override fun getOldListSize(): Int = oldList.size
		override fun getNewListSize(): Int = newList.size

		override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
			return oldList[oldItemPosition].id == newList[newItemPosition].id
		}

		override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
			val oldEntity = oldList[oldItemPosition]
			val newEntity = newList[newItemPosition]
			return oldEntity == newEntity
		}
	}
}
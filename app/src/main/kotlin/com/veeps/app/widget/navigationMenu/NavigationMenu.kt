package com.veeps.app.widget.navigationMenu

import android.content.Context
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.veeps.app.R
import com.veeps.app.extension.fadeInNow
import com.veeps.app.extension.fadeOutNowWith
import com.veeps.app.extension.inflate
import com.veeps.app.extension.loadImage
import com.veeps.app.util.AppConstants
import com.veeps.app.util.AppPreferences
import com.veeps.app.util.DEFAULT
import com.veeps.app.util.ImageTags
import com.veeps.app.util.IntValue
import com.veeps.app.widget.navigationMenu.NavigationItems.NO_MENU
import com.veeps.app.widget.navigationMenu.NavigationItems.defaultResource
import com.veeps.app.widget.navigationMenu.NavigationItems.imageFilledResources
import com.veeps.app.widget.navigationMenu.NavigationItems.imageNonFilledResources

class NavigationMenu : LinearLayout {

	private var navigationItem: NavigationItem? = null
	private var currentSelectedItem: Int = NavigationItems.BROWSE_MENU
	private var isNavigationMenuVisible: Boolean = false

	constructor(context: Context) : super(context) {
		setupMenuUI(context)
	}

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		setupMenuUI(context)
	}

	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
		context, attrs, defStyleAttr
	) {
		setupMenuUI(context)
	}

	private fun setupMenuUI(context: Context) {
		this.navigationItem = context as NavigationItem
		val dp16 = context.resources.getDimensionPixelOffset(R.dimen.dp16)
		val dp32 = context.resources.getDimensionPixelOffset(R.dimen.dp32)
		orientation = VERTICAL
		gravity = Gravity.CENTER_HORIZONTAL
		setPadding(16, 32, 16, 32)
		setBackgroundColor(ContextCompat.getColor(this.context, android.R.color.transparent))

		val profile = this.inflate(R.layout.header_menu_item)
		profile.tag = NavigationItems.PROFILE
		profile.id = NavigationItems.PROFILE_MENU
		val profileImage = profile.findViewById<ImageView>(R.id.image)
		val profileImageLabel = profile.findViewById<TextView>(R.id.image_label)
		val profileLabel = profile.findViewById<TextView>(R.id.label)
		if (AppPreferences.get(AppConstants.userAvatar, DEFAULT.EMPTY_STRING).isNullOrBlank()) {
			profileImage.loadImage(R.drawable.menu_image_background_active, ImageTags.AVATAR)
			profileImage.imageTintList = ColorStateList.valueOf(
				ContextCompat.getColor(
					context, R.color.white
				)
			)
			profileImageLabel.text =
				AppPreferences.get(AppConstants.userFullName, "V")?.get(0).toString()
		} else {
			profileImage.loadImage(
				AppPreferences.get(AppConstants.userAvatar, DEFAULT.EMPTY_STRING)!!,
				ImageTags.AVATAR
			)
			profileImageLabel.text = DEFAULT.EMPTY_STRING
		}
		profile.setOnClickListener {
			navigationItem!!.select(NavigationItems.PROFILE_MENU)
		}
		addView(profile)

		val browse = this.inflate(R.layout.menu_item)
		browse.tag = NavigationItems.BROWSE
		browse.id = NavigationItems.BROWSE_MENU
		val browseImage = browse.findViewById<ImageView>(R.id.image)
		val browseLabel = browse.findViewById<TextView>(R.id.label)
		browseImage.setImageResource(R.drawable.home)
		browseImage.imageTintList = ColorStateList.valueOf(
			ContextCompat.getColor(
				context, R.color.white
			)
		)
		browse.setOnClickListener {
			navigationItem!!.select(NavigationItems.BROWSE_MENU)
		}
		addView(browse)

		val liveTv = this.inflate(R.layout.menu_item)
		liveTv.tag = NavigationItems.LIVE_TV
		liveTv.id = NavigationItems.LIVE_TV_MENU
		val liveTvImage = liveTv.findViewById<ImageView>(R.id.image)
		val liveTvLabel = liveTv.findViewById<TextView>(R.id.label)
		liveTvImage.setImageResource(R.drawable.live_tv)
		liveTvImage.imageTintList = ColorStateList.valueOf(
			ContextCompat.getColor(
				context, R.color.white
			)
		)
		liveTv.setOnClickListener {
			navigationItem!!.select(NavigationItems.LIVE_TV_MENU)
		}
		addView(liveTv)

		val myShows = this.inflate(R.layout.menu_item)
		myShows.tag = NavigationItems.MY_SHOWS
		myShows.id = NavigationItems.MY_SHOWS_MENU
		val myShowsImage = myShows.findViewById<ImageView>(R.id.image)
		val myShowsLabel = myShows.findViewById<TextView>(R.id.label)
		myShowsImage.setImageResource(R.drawable.my_shows)
		myShowsImage.imageTintList = ColorStateList.valueOf(
			ContextCompat.getColor(
				context, R.color.white
			)
		)
		myShows.setOnClickListener {
			navigationItem!!.select(NavigationItems.MY_SHOWS_MENU)
		}
		addView(myShows)

		val search = this.inflate(R.layout.menu_item)
		search.tag = NavigationItems.SEARCH
		search.id = NavigationItems.SEARCH_MENU
		val searchImage = search.findViewById<ImageView>(R.id.image)
		val searchLabel = search.findViewById<TextView>(R.id.label)
		searchImage.setImageResource(R.drawable.search)
		searchImage.imageTintList = ColorStateList.valueOf(
			ContextCompat.getColor(
				context, R.color.white
			)
		)
		search.setOnClickListener {
			navigationItem!!.select(NavigationItems.SEARCH_MENU)
		}
		addView(search)

	}

	private fun focusCurrentSelectedNavigationMenu() {
		var currentSelected = currentSelectedItem

		// If no menu is selected, default to BROWSE_MENU
		if (currentSelected == NavigationItems.NO_MENU) currentSelected =
			NavigationItems.BROWSE_MENU

		// Ensure the currentSelected index is within bounds
		if (currentSelected in 0 until childCount) {
			getChildAt(currentSelected).requestFocus()
		} else {
			// handle cases where currentSelected is invalid
			clearFocus()
		}
	}

	private fun updateMenuItemColors(view: View, position: Int, isExpanded: Boolean) {
		val avatar = AppPreferences.get(AppConstants.userAvatar, DEFAULT.EMPTY_STRING)
		val isAvatarMissing = avatar.isNullOrBlank()

		// Pre-cache colors for reuse
		val colorBlack = ContextCompat.getColor(view.context, R.color.black)
		val colorWhite = ContextCompat.getColor(view.context, R.color.white)
		val colorWhite50 = ContextCompat.getColor(view.context, R.color.white_50)
		val colorBlack10 = ContextCompat.getColor(view.context, R.color.black_10)
		val colorWhite10 = ContextCompat.getColor(view.context, R.color.white_10)

		val labelView = view.findViewById<TextView>(R.id.label)
		val imageView = view.findViewById<ImageView>(R.id.image)
		val hasFocus = view.hasFocus()

		val isCurrentItem = if (currentSelectedItem == NO_MENU) false else position == currentSelectedItem
		val isFirstItem = position == 0

		if (!isFirstItem) {
			val imageResource = if (isCurrentItem) {
				imageFilledResources.getOrNull(position) ?: defaultResource
			} else {
				imageNonFilledResources.getOrNull(position) ?: defaultResource
			}

			imageResource.let { imageView.setImageResource(it) }
		}

		// Determine the text color
		val textColor = when {
			isExpanded && isCurrentItem && !hasFocus -> colorWhite
			isExpanded && hasFocus -> colorBlack
			isExpanded -> colorWhite50
			isCurrentItem -> colorWhite
			else -> colorWhite50
		}
		labelView.setTextColor(textColor)

		// Determine the image tint color
		val imageTintColor = when {
			isExpanded && isFirstItem && isAvatarMissing && hasFocus -> colorBlack10
			isFirstItem && isAvatarMissing -> colorWhite10
			isExpanded && isCurrentItem && hasFocus -> colorBlack
			isExpanded && isCurrentItem -> colorWhite
			isExpanded && hasFocus -> colorBlack
			isExpanded -> colorWhite50
			isCurrentItem -> colorWhite
			else -> colorWhite50
		}
		if (!isFirstItem || isAvatarMissing) imageView.imageTintList = ColorStateList.valueOf(imageTintColor)

		// Handle image label for the first item when the avatar is missing
		if (isExpanded && isFirstItem && isAvatarMissing) {
			view.findViewById<TextView>(R.id.image_label)?.setTextColor(
				if (hasFocus) colorBlack else colorWhite
			)
		}
	}

	fun setCurrentSelected(currentSelected: Int) {
		this.currentSelectedItem = currentSelected
	}

	fun getCurrentSelected(): Int {
		return currentSelectedItem
	}

	fun getSelectedItem(selectedItem: Int): String {
		return getChildAt(selectedItem).tag.toString()
	}

	private fun clearNavigationMenuLabel() {
		for (i in 0 until childCount) {
			/*if (doesCompletelyHiddenRequired) {
				getChildAt(i).findViewById<View>(R.id.image).fadeOutNow(IntValue.NUMBER_100)
				if (i == 0) {
					getChildAt(i).findViewById<View>(R.id.image_label).fadeOutNow(IntValue.NUMBER_100)
				}
			}*/
			getChildAt(i).findViewById<View>(R.id.label)
				.fadeOutNowWith(IntValue.NUMBER_100, object : Animation.AnimationListener {
					override fun onAnimationStart(animation: Animation) {}
					override fun onAnimationEnd(animation: Animation) {
						(getChildAt(i).findViewById<View>(R.id.label) as TextView).text =
							DEFAULT.EMPTY_STRING
					}

					override fun onAnimationRepeat(animation: Animation) {}
				})
		}
	}

	private fun setNavigationMenuText(context: Context) {
		for (i in 0 until childCount) {
			when (getChildAt(i).tag) {
				NavigationItems.PROFILE -> (getChildAt(i).findViewById<View>(R.id.label) as TextView).text =
					context.resources.getString(R.string.profile_label)

				NavigationItems.BROWSE -> (getChildAt(i).findViewById<View>(R.id.label) as TextView).text =
					context.resources.getString(R.string.home_label)

				NavigationItems.LIVE_TV -> (getChildAt(i).findViewById<View>(R.id.label) as TextView).text =
					context.resources.getString(R.string.live_tv_label)

				NavigationItems.SEARCH -> (getChildAt(i).findViewById<View>(R.id.label) as TextView).text =
					context.resources.getString(R.string.search_label)

				NavigationItems.MY_SHOWS -> (getChildAt(i).findViewById<View>(R.id.label) as TextView).text =
					context.resources.getString(R.string.my_shows_label)
			}            /*getChildAt(i).findViewById<View>(R.id.image).fadeInNow(IntValue.NUMBER_333)
			if (i == 0) {
				getChildAt(i).findViewById<View>(R.id.image_label).fadeInNow(IntValue.NUMBER_333)
			}*/
			getChildAt(i).findViewById<View>(R.id.label).fadeInNow(IntValue.NUMBER_333)
		}
	}

	fun setupNavigationMenuExpandedUI(context: Context) {
		Handler(Looper.getMainLooper()).postDelayed({
			setNavigationMenuText(context)
			changeNavigationMenuFocusStatus(true)
			focusCurrentSelectedNavigationMenu()
			isNavigationMenuVisible = true
		}, IntValue.NUMBER_200.toLong())
	}

	fun setupNavigationMenuCollapsedUI() {
		clearNavigationMenuLabel()
		changeNavigationMenuFocusStatus(false)
		isNavigationMenuVisible = false
	}

	private fun changeNavigationMenuFocusStatus(isExpanded: Boolean) {
		for (position in 0 until childCount) {
			val childView = getChildAt(position)

			childView.apply {
				// Set focus properties
				nextFocusLeftId = this.id
				nextFocusUpId = if (position == 0) this.id else getChildAt(position - 1).id
				nextFocusDownId =
					if (position == childCount - 1) this.id else getChildAt(position + 1).id
				nextFocusForwardId = nextFocusDownId
				isFocusable = isExpanded
				isFocusableInTouchMode = isExpanded

				// Only set focus listener when the view is focusable (i.e., isExpanded is true)
				if (isExpanded) {
					setOnFocusChangeListener { view, _ ->
						updateMenuItemColors(view, position, isExpanded = true)
					}
				} else {
					// Clear focus if not expanded
					clearFocus()
				}

				// Apply the colors initially
				updateMenuItemColors(childView, position, isExpanded)
			}
		}
	}

	fun setupDefaultNavigationMenu(navigationMenuItemId: Int) {
		currentSelectedItem = navigationMenuItemId
		isNavigationMenuVisible = false
		changeNavigationMenuFocusStatus(false)
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
		val currentTime = System.currentTimeMillis()
		return if (currentTime - AppConstants.lastKeyPressTime < AppConstants.keyPressShortDelayTime) {
			true
		} else {
			AppConstants.lastKeyPressTime = currentTime
			super.onKeyDown(keyCode, event)
		}
	}
}
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
			profileImage.loadImage(R.drawable.menu_image_background, ImageTags.AVATAR)
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

	}

	private fun focusCurrentSelectedNavigationMenu() {
		getChildAt(currentSelectedItem).requestFocus()
	}

	private fun highlightCurrentSelectedNavigationMenu(isExpanded: Boolean) {
		for (position in 0 until childCount) {
			(getChildAt(position).findViewById<View>(R.id.label) as TextView).setTextColor(
				ContextCompat.getColor(
					context,
					if (isExpanded && getChildAt(position).hasFocus()) R.color.black else R.color.white
				)
			)
			if (position == 0) {
				if (AppPreferences.get(AppConstants.userAvatar, DEFAULT.EMPTY_STRING)
						.isNullOrBlank()
				) {
					(getChildAt(position).findViewById<View>(R.id.image) as ImageView).imageTintList =
						ColorStateList.valueOf(
							ContextCompat.getColor(
								context,
								if (isExpanded && getChildAt(position).hasFocus()) R.color.black else R.color.white
							)
						)
					(getChildAt(position).findViewById<View>(R.id.image_label) as TextView).setTextColor(
						ContextCompat.getColor(
							context,
							if (isExpanded && getChildAt(position).hasFocus()) R.color.black else R.color.white
						)
					)
				}
			} else {
				(getChildAt(position).findViewById<View>(R.id.image) as ImageView).imageTintList =
					ColorStateList.valueOf(
						ContextCompat.getColor(
							context,
							if (!isExpanded && position != currentSelectedItem) R.color.white_50 else if (isExpanded && getChildAt(
									position
								).hasFocus()
							) R.color.black else R.color.white
						)
					)
			}
		}
	}

	private fun removeHighlightCurrentSelectedNavigationMenu() {
		(getChildAt(currentSelectedItem).findViewById<View>(R.id.label) as TextView).setTextColor(
			ContextCompat.getColor(
				context, R.color.white
			)
		)
		if (currentSelectedItem == 0) {
			if (AppPreferences.get(AppConstants.userAvatar, DEFAULT.EMPTY_STRING).isNullOrBlank()) {
				(getChildAt(currentSelectedItem).findViewById<View>(R.id.image) as ImageView).imageTintList =
					ColorStateList.valueOf(
						ContextCompat.getColor(
							context, R.color.white
						)
					)
				(getChildAt(currentSelectedItem).findViewById<View>(R.id.image_label) as TextView).setTextColor(
					ContextCompat.getColor(
						context, R.color.white
					)
				)
			}
		} else {
			(getChildAt(currentSelectedItem).findViewById<View>(R.id.image) as ImageView).imageTintList =
				ColorStateList.valueOf(
					ContextCompat.getColor(
						context, R.color.white
					)
				)
		}
	}

	private fun updateHighlightCurrentSelectedNavigationMenu() {
		(getChildAt(currentSelectedItem).findViewById<View>(R.id.label) as TextView).setTextColor(
			ContextCompat.getColor(
				context, R.color.white
			)
		)
		if (currentSelectedItem == 0) {
			if (AppPreferences.get(AppConstants.userAvatar, DEFAULT.EMPTY_STRING).isNullOrBlank()) {
				(getChildAt(currentSelectedItem).findViewById<View>(R.id.image) as ImageView).imageTintList =
					ColorStateList.valueOf(
						ContextCompat.getColor(
							context, R.color.white
						)
					)
				(getChildAt(currentSelectedItem).findViewById<View>(R.id.image_label) as TextView).setTextColor(
					ContextCompat.getColor(
						context, R.color.white
					)
				)
			}
		} else {
			(getChildAt(currentSelectedItem).findViewById<View>(R.id.image) as ImageView).imageTintList =
				ColorStateList.valueOf(
					ContextCompat.getColor(
						context, R.color.white
					)
				)
		}
	}

	fun setCurrentSelectedItem(currentSelected: Int) {
		removeHighlightCurrentSelectedNavigationMenu()
		this.currentSelectedItem = currentSelected
		updateHighlightCurrentSelectedNavigationMenu()
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
		for (i in 0 until childCount) {            /*if (doesCompletelyHiddenRequired) {
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
		for (i in 0 until childCount) {
			getChildAt(i).apply {
				this.nextFocusLeftId = getChildAt(i).id
				this.nextFocusUpId = if (i == 0) getChildAt(i).id else getChildAt(i - 1).id
				this.nextFocusDownId =
					if (i == childCount - 1) getChildAt(i).id else getChildAt(i + 1).id
				this.nextFocusForwardId = getChildAt(i).nextFocusDownId
				this.isFocusable = isExpanded
				this.isFocusableInTouchMode = isExpanded
				this.setOnFocusChangeListener { view, hasFocus ->
					(view.findViewById<View>(R.id.label) as TextView).setTextColor(
						ContextCompat.getColor(
							context, if (hasFocus) R.color.black else R.color.white
						)
					)
					if (i == 0) {
						if (AppPreferences.get(AppConstants.userAvatar, DEFAULT.EMPTY_STRING)
								.isNullOrBlank()
						) {
							(view.findViewById<View>(R.id.image) as ImageView).imageTintList =
								ColorStateList.valueOf(
									ContextCompat.getColor(
										context, if (hasFocus) R.color.black else R.color.white
									)
								)
							(view.findViewById<View>(R.id.image_label) as TextView).setTextColor(
								ContextCompat.getColor(
									context, if (hasFocus) R.color.black else R.color.white
								)
							)
						}
					} else {
						(view.findViewById<View>(R.id.image) as ImageView).imageTintList =
							ColorStateList.valueOf(
								ContextCompat.getColor(
									context, if (hasFocus) R.color.black else R.color.white
								)
							)
					}
				}
				if (!isExpanded) this.clearFocus()
				highlightCurrentSelectedNavigationMenu(isExpanded)
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
package com.veeps.app.extension

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.Transformation
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.addListener
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.veeps.app.R
import com.veeps.app.util.DEFAULT
import com.veeps.app.util.Image
import com.veeps.app.util.ImageTags
import com.veeps.app.util.IntValue
import com.veeps.app.util.Logger
import com.veeps.app.widget.navigationMenu.NavigationMenu
import kotlin.math.roundToInt


fun ViewGroup.inflate(@LayoutRes resourceId: Int) =
	LayoutInflater.from(context).inflate(resourceId, this, false)!!

fun View.fadeInNow(duration: Int) {
	val fadeInAnimation = AlphaAnimation(0.0f, 1.0f)
	fadeInAnimation.duration = duration.toLong()
	fadeInAnimation.fillAfter = true
	this.startAnimation(fadeInAnimation)
}

fun ConstraintLayout.setHorizontalBias(
	@IdRes targetViewId: Int, bias: Float
) {
	val constraintSet = ConstraintSet()
	constraintSet.clone(this)
	constraintSet.setHorizontalBias(targetViewId, bias)
	constraintSet.applyTo(this)
	val transition = Slide()
	transition.interpolator = AccelerateInterpolator()
	TransitionManager.beginDelayedTransition(this, transition)
}

fun View.fadeOutNow(duration: Int) {
	val fadeOutAnimation = AlphaAnimation(1.0f, 0.0f)
	fadeOutAnimation.fillAfter = true
	fadeOutAnimation.duration = duration.toLong()
	this.startAnimation(fadeOutAnimation)
}

fun View.fadeInNowWith(duration: Int, listener: Animation.AnimationListener) {
	visibility = View.VISIBLE
	val fadeInAnimation = AlphaAnimation(0.0f, 1.0f)
	fadeInAnimation.fillAfter = true
	fadeInAnimation.duration = duration.toLong()
	fadeInAnimation.setAnimationListener(listener)
	this.startAnimation(fadeInAnimation)
}

fun View.fadeOutNowWith(duration: Int, listener: Animation.AnimationListener) {
	val fadeOutAnimation = AlphaAnimation(1.0f, 0.0f)
	fadeOutAnimation.fillAfter = true
	fadeOutAnimation.duration = duration.toLong()
	fadeOutAnimation.setAnimationListener(listener)
	this.startAnimation(fadeOutAnimation)
}

fun View.transformWidth(width: Int, doesCompletelyHiddenRequired: Boolean) {
	if (doesCompletelyHiddenRequired) {
		(this as NavigationMenu).setupNavigationMenuCollapsedUI()
		this.layoutParams.width = 1
		this.requestLayout()
	} else {
		val valueAnimator = ValueAnimator.ofInt(
			this.measuredWidth,
			resources.getDimensionPixelSize(width)
		)
		valueAnimator.addUpdateListener { animation ->
			this.layoutParams.width = animation.animatedValue as Int
			this.requestLayout()
		}

		valueAnimator.interpolator = AccelerateInterpolator()
		if (width == R.dimen.collapsed_navigation_menu_width) {
			valueAnimator.duration = IntValue.NUMBER_200.toLong()
			(this as NavigationMenu).setupNavigationMenuCollapsedUI()
		} else if (width == R.dimen.expanded_navigation_menu_width) {
			valueAnimator.duration = IntValue.NUMBER_333.toLong()
			(this as NavigationMenu).setupNavigationMenuExpandedUI(this.context)
		}
		valueAnimator.start()
	}
}

fun View.transformHeight(height: Int, shouldHide: Boolean) {
	val valueAnimator = ValueAnimator.ofInt(this.measuredHeight, height)
	valueAnimator.addListener(onStart = {
		if (!shouldHide) this.visibility = View.VISIBLE
	}, onEnd = {
		if (shouldHide) this.visibility = View.GONE
	})
	valueAnimator.addUpdateListener { animation ->
		this.layoutParams.height = animation.animatedValue as Int
		this.requestLayout()
	}

	valueAnimator.interpolator = AccelerateInterpolator()
	valueAnimator.duration = IntValue.NUMBER_333.toLong()
	valueAnimator.start()
}

fun View.transform(value: Int, duration: Int) {
	val animation: Animation = object : Animation() {
		override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
			val params: LinearLayout.LayoutParams = layoutParams as LinearLayout.LayoutParams
			params.topMargin = ((value * interpolatedTime).roundToInt())
			layoutParams = params
		}
	}
	animation.duration = duration.toLong()
	startAnimation(animation)
}

fun View.translateNow(from: Float, to: Float, type: String) {
	val valueAnimator = ValueAnimator.ofFloat(from, to)
	valueAnimator.addUpdateListener { animation ->
		when (type) {
			else -> {
				this.translationY = animation.animatedValue as Float
			}
		}
		this.requestLayout()
	}

	valueAnimator.interpolator = LinearInterpolator()
	valueAnimator.duration = IntValue.NUMBER_1000.toLong()
	valueAnimator.start()
}

fun View.translateNow(height: Float, listener: Animator.AnimatorListener) {
	val valueAnimator = ValueAnimator.ofFloat(this.measuredHeight.toFloat(), height)
	valueAnimator.addUpdateListener { animation ->
		this.translationY = animation.animatedValue as Float
		this.requestLayout()
	}

	valueAnimator.interpolator = LinearInterpolator()
	valueAnimator.duration = IntValue.NUMBER_100.toLong()
	valueAnimator.addListener(listener)
	valueAnimator.start()
}

fun EditText.clear() {
	this.setText(DEFAULT.EMPTY_STRING)
}

fun ImageView.loadImage(resource: Any, tag: Any) {

	when (resource) {
		is Drawable -> {
			if (resource.toString().isNotBlank()) this@loadImage.setImageDrawable(resource)
			Logger.printMessage("Image drawable ($resource) is requested to load.")
		}

		is String -> {
			when (tag) {
				ImageTags.CARD -> {
					val newResource = resource.replace(Image.DEFAULT, Image.CARD)
					Glide.with(this@loadImage.context).load(newResource)
						.diskCacheStrategy(DiskCacheStrategy.ALL)
						.transition(withCrossFade())
						.transform(CenterCrop(), RoundedCorners(IntValue.NUMBER_10))
						.placeholder(R.drawable.rounded_card_background_black)
						.error(R.drawable.rounded_card_background_black).into(this@loadImage)
					Logger.printMessage("Card Image url with optimized ($newResource) is requested to load.")
				}

				ImageTags.ARTIST_VENUE -> {
					val newResource = resource.replace(Image.DEFAULT, Image.CIRCLE)
					Glide.with(this@loadImage.context).load(newResource)
						.diskCacheStrategy(DiskCacheStrategy.ALL)
						.transition(withCrossFade())
						.transform(CenterCrop(), RoundedCorners(IntValue.NUMBER_2000))
						.placeholder(R.drawable.rounded_card_background_white_10)
						.error(R.drawable.rounded_card_background_white_10).into(this@loadImage)
					Logger.printMessage("Artist or Venue Image url with optimized ($newResource) is requested to load.")
				}

				ImageTags.HERO -> {
					val newResource = resource.replace(Image.DEFAULT, Image.HERO)
					Glide.with(this@loadImage.context).load(newResource)
						.diskCacheStrategy(DiskCacheStrategy.ALL)
						.transition(withCrossFade()).error(R.drawable.background_dark_black)
						.into(this@loadImage)
					Logger.printMessage("Hero Image url with optimized ($newResource) is requested to load.")
				}

				ImageTags.LOGO -> {
					val newResource = resource.replace(Image.DEFAULT, Image.LOGO)
					Glide.with(this@loadImage.context).load(newResource)
						.diskCacheStrategy(DiskCacheStrategy.ALL)
						.override(measuredWidth, measuredHeight)
						.transition(withCrossFade()).error(R.drawable.background_transparent)
						.into(this@loadImage)
					Logger.printMessage("Logo Image url with optimized ($newResource) is requested to load.")
				}

				ImageTags.DEFAULT -> {
					Glide.with(this@loadImage.context).load(resource)
						.diskCacheStrategy(DiskCacheStrategy.ALL)
						.transition(withCrossFade()).error(R.drawable.background_black)
						.into(this@loadImage)
					Logger.printMessage("Image url with optimized ($resource) is requested to load.")
				}

				ImageTags.QR -> {
					this@loadImage.clipToOutline = true
					Glide.with(this@loadImage.context).load(resource)
						.diskCacheStrategy(DiskCacheStrategy.ALL)
						.transition(withCrossFade())
						.transform(CenterInside(), RoundedCorners(IntValue.NUMBER_5))
						.placeholder(this@loadImage.drawable)
						.error(R.drawable.qr_code_background_transparent).into(this@loadImage)
					Logger.printMessage("QR image url ($resource) is requested to load.")
				}

				ImageTags.AVATAR -> {
					this@loadImage.clipToOutline = true
					Glide.with(this@loadImage.context).load(resource)
						.diskCacheStrategy(DiskCacheStrategy.ALL)
						.transition(withCrossFade()).transform(CircleCrop())
						.placeholder(this@loadImage.drawable)
						.error(R.drawable.qr_code_background_transparent).into(this@loadImage)
					Logger.printMessage("Profile avatar image url ($resource) is requested to load.")
				}
			}
		}

		is Int -> {
			if (resource.toString().isNotBlank()) this@loadImage.setImageResource(resource)
			Logger.printMessage("Image app resource ($resource) is requested to load.")
		}

		is Bitmap -> {
			if (resource.toString().isNotBlank()) this@loadImage.setImageBitmap(resource)
			Logger.printMessage("Image bitmap ($resource) is requested to load.")
		}

		else -> {
			Logger.printMessage("Unknown image resource is requested to load. Check resource type and add equivalent case.")
		}
	}

}
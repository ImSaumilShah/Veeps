package com.veeps.app.feature.contentRail.adapter

import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.BaseGridView
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.veeps.app.R
import com.veeps.app.databinding.RowContentRailBinding
import com.veeps.app.feature.card.adapter.CardAdapter
import com.veeps.app.feature.contentRail.model.RailData
import com.veeps.app.util.AppAction
import com.veeps.app.util.AppHelper
import com.veeps.app.util.CardTypes
import com.veeps.app.util.Screens


class ContentRailsAdapter(
	private var rails: ArrayList<RailData>,
	private val helper: AppHelper,
	private val screen: String,
	private val action: AppAction,
	private val isRecommended: Boolean = false,
) : RecyclerView.Adapter<ContentRailsAdapter.ViewHolder>() {

	lateinit var context: Context
	override fun onCreateViewHolder(
		parent: ViewGroup, viewType: Int,
	): ViewHolder {
		context = parent.context
		return ViewHolder(
			RowContentRailBinding.inflate(
				LayoutInflater.from(parent.context), parent, false
			)
		)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.binding?.title?.text = rails[position].name
		if (screen == Screens.BROWSE || screen == Screens.SHOWS || screen == Screens.ARTIST || screen == Screens.VENUE || screen == Screens.EVENT) {
			val params = holder.binding?.title?.layoutParams as ConstraintLayout.LayoutParams
			params.marginStart = context.resources.getDimensionPixelSize(R.dimen.dp75)
			params.marginEnd = context.resources.getDimensionPixelSize(R.dimen.dp75)
			holder.binding.title.layoutParams = params
		}
		holder.binding?.listing?.apply {
			itemAnimator = null
			setNumRows(1)
			setHasFixedSize(true)
			windowAlignment = BaseGridView.WINDOW_ALIGN_HIGH_EDGE
			windowAlignmentOffsetPercent =
				if (screen == Screens.BROWSE || screen == Screens.SHOWS || screen == Screens.ARTIST || screen == Screens.VENUE || screen == Screens.EVENT) 8f else 0f
			isItemAlignmentOffsetWithPadding = true
			itemAlignmentOffsetPercent = 0f
			setRowHeight(
				when (rails[position].cardType) {
					CardTypes.CIRCLE -> context.resources.getDimensionPixelSize(
						R.dimen.row_height_circle_without_follow
					)
					CardTypes.LANDSCAPE, CardTypes.HERO -> context.resources.getDimensionPixelSize(
						R.dimen.row_height_landscape
					)
					CardTypes.GENRE -> context.resources.getDimensionPixelSize(
						R.dimen.row_height_genre
					)
					else -> context.resources.getDimensionPixelSize(R.dimen.row_height_default)
				}
			)
//			setItemViewCacheSize(rails[position].entities.size)
			val cardAdapter = CardAdapter(action)
			cardAdapter.setEntities(rails[position].entities)
			rails[position].cardType?.let { cardAdapter.setCardType(it) }
			rails[position].entitiesType?.let { cardAdapter.setEntityType(it) }
			cardAdapter.setHelper(helper)
			cardAdapter.setAdapterPosition(position)
			cardAdapter.setScreen(screen)
			cardAdapter.setIsRecommended(isRecommended)
			cardAdapter.setContinueWatching(rails[position].isContinueWatching)
			cardAdapter.setWatchList(rails[position].isWatchList)
			cardAdapter.setRailCount(rails.size)
			cardAdapter.setExpired(rails[position].isExpired)
			cardAdapter.setUserStats(rails[position].userStats)

			adapter = cardAdapter
			onFlingListener = LinearSnapHelper()
			if (holder.binding.listing.itemDecorationCount != 0) {
				holder.binding.listing.removeItemDecorationAt(0)
			}
			holder.binding.listing.addItemDecoration(object : RecyclerView.ItemDecoration() {
				override fun getItemOffsets(
					outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State,
				) {
					val itemPosition = parent.getChildAdapterPosition(view)
					val isFirst = itemPosition == state.itemCount - state.itemCount
					val isLast = itemPosition == state.itemCount - 1
					with(outRect) {
						if (isFirst) {
							this.left =
								context.resources.getDimensionPixelSize(R.dimen.row_width_vertical)
						}
						if (isLast) {
							this.right = context.resources.getDimensionPixelSize(R.dimen.dp32)
						}
					}
				}
			})
		}
	}

	override fun getItemId(position: Int): Long {
		return position.toLong()
	}

	override fun getItemCount() = rails.size

	fun setRails(rails: ArrayList<RailData>) {
		this.rails = rails
		notifyDataSetChanged()
	}

	inner class ViewHolder(val binding: RowContentRailBinding?) :
		RecyclerView.ViewHolder(binding?.root!!)

}
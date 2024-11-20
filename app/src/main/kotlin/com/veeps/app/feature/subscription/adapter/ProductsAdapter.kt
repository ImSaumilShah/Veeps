package com.veeps.app.feature.subscription.adapter

import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.veeps.app.databinding.ProductItemBinding
import com.veeps.app.feature.contentRail.model.Products
import com.veeps.app.util.AppAction
import com.veeps.app.util.AppHelper
import com.veeps.app.util.DEFAULT


class ProductsAdapter(
	private val planList: ArrayList<Products>,
	val helper: AppHelper,
	val action: AppAction
) :
	RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {

	lateinit var context: Context

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		context = parent.context
		return ViewHolder(
			ProductItemBinding.inflate(
				LayoutInflater.from(parent.context), parent, false
			)
		)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.binding.planTitle.text = planList[position].name
		holder.binding.planPrice.text = planList[position].price
		holder.binding.planContainer.setOnClickListener {
			action.onAction(planList[position].id.toString())
		}
		holder.binding.planContainer.setOnFocusChangeListener { _, hasFocus ->
			holder.binding.unselectLabel.visibility = if (hasFocus) View.GONE else View.VISIBLE
			holder.binding.selectLabel.visibility = if (hasFocus) View.VISIBLE else View.GONE
		}
		holder.binding.planContainer.setOnKeyListener { _, keyCode, keyEvent ->
			keyEvent.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP || (position != DEFAULT.EMPTY_INT && (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)))
		}
	}

	override fun getItemId(position: Int): Long {
		return position.toLong()
	}

	override fun getItemCount(): Int {
		return planList.size
	}

	inner class ViewHolder(val binding: ProductItemBinding) : RecyclerView.ViewHolder(binding.root)

}
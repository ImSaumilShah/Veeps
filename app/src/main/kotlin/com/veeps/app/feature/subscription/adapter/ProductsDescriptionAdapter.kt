package com.veeps.app.feature.subscription.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.veeps.app.databinding.ProductDescriptionBinding


class ProductsDescriptionAdapter(private val planList: ArrayList<String>) :
    RecyclerView.Adapter<ProductsDescriptionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int,
    ): ProductsDescriptionAdapter.ViewHolder {
        return ViewHolder(
            ProductDescriptionBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.planDetail.text = planList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return planList.size
    }

    inner class ViewHolder(val binding: ProductDescriptionBinding) :
        RecyclerView.ViewHolder(binding.root)

}
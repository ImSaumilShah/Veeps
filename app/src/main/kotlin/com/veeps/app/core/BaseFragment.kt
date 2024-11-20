package com.veeps.app.core

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.ContentLoadingProgressBar
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.veeps.app.R
import com.veeps.app.feature.home.ui.HomeScreen
import com.veeps.app.feature.home.viewModel.HomeViewModel
import com.veeps.app.util.APIConstants
import com.veeps.app.util.AppHelper
import com.veeps.app.util.Logger
import java.lang.reflect.ParameterizedType

abstract class BaseFragment<VM : ViewModel, VB : ViewDataBinding> : Fragment() {

	lateinit var viewModel: VM
	lateinit var binding: VB
	private lateinit var screenLoader: ContentLoadingProgressBar
	private lateinit var layoutContainer: ConstraintLayout
	lateinit var helper: AppHelper
	lateinit var homeViewModel: HomeViewModel

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
	): View {
		super.onCreateView(inflater, container, savedInstanceState)
		homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
		viewModel = ViewModelProvider(requireActivity())[getViewModelClass()]
		binding = getViewBinding()
		binding.lifecycleOwner = viewLifecycleOwner
		layoutContainer = binding.root.findViewById(R.id.layout_container)
		screenLoader = binding.root.findViewById(R.id.loader)
		return binding.root
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)
		val activity: Activity = context as HomeScreen
		try {
			helper = activity as AppHelper
		} catch (e: ClassCastException) {
			throw ClassCastException("$activity must implement Interface")
		}
	}

	private fun getViewModelClass(): Class<VM> {
		val type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
		return type as Class<VM>
	}

	abstract fun getViewBinding(): VB

	open fun <T> fetch(
		dataResource: BaseDataSource.Resource<T>,
		isLoaderEnabled: Boolean = true,
		canUserAccessScreen: Boolean = false,
		shouldBeInBackground: Boolean = false,
		block: () -> Unit,
	) {
		when (dataResource.callStatus) {
			BaseDataSource.Resource.CallStatus.SUCCESS -> {
				if (isLoaderEnabled) {
					screenLoader.hide()
					screenLoader.visibility = View.GONE
					if (!shouldBeInBackground) {
						layoutContainer.visibility = View.VISIBLE
					}
				}
				block.invoke()
				Logger.print("API Call is successful for ${dataResource.tag}")
			}

			BaseDataSource.Resource.CallStatus.LOADING -> {
				if (isLoaderEnabled && !screenLoader.isVisible) {
					screenLoader.show()
					screenLoader.visibility = View.VISIBLE
					if (!canUserAccessScreen) screenLoader.requestFocus()
					if (!shouldBeInBackground) {
						layoutContainer.visibility = View.GONE
					}
				}
				Logger.print("API Call is initiating for ${dataResource.tag}. Loader should be visible.")
			}

			BaseDataSource.Resource.CallStatus.ERROR -> {
				if (isLoaderEnabled) {
					screenLoader.hide()
					screenLoader.visibility = View.GONE
					if (!shouldBeInBackground) {
						layoutContainer.visibility = View.VISIBLE
					}
				}
				when (dataResource.tag) {
					APIConstants.AUTHENTICATION_POLLING, APIConstants.VALIDATE_APP_VERSIONS, APIConstants.FETCH_COMPANIONS, APIConstants.FETCH_USER_STATS, APIConstants.FETCH_EVENT_STREAM_DETAILS, APIConstants.FETCH_EVENT_DETAILS, APIConstants.FETCH_EVENT_PRODUCT_DETAILS, APIConstants.CLEAR_ALL_RESERVATIONS, APIConstants.FETCH_STORY_BOARD, APIConstants.SUBSCRIPTION_MAPPING -> {
						block.invoke()
					}

					else -> {
						dataResource.message?.let { message ->
							helper.showErrorOnScreen(dataResource.tag, message)
						} ?: helper.showErrorOnScreen(
							dataResource.tag, getString(R.string.unknown_error)
						)
						if (dataResource.tag == APIConstants.SET_NEW_RESERVATION) {
							block.invoke()
						}
					}
				}
				Logger.print("Error While Calling API for ${dataResource.tag} - " + dataResource.message)
			}
		}
	}
}

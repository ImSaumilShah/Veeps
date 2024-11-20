package com.veeps.app.core

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.ContentLoadingProgressBar
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.veeps.app.R
import com.veeps.app.util.APIConstants
import com.veeps.app.util.Logger
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import java.lang.reflect.ParameterizedType

abstract class BaseActivity<VM : ViewModel, VB : ViewDataBinding> : FragmentActivity() {

	lateinit var viewModel: VM
	lateinit var binding: VB
	lateinit var backPressedCallback: OnBackPressedCallback
	private lateinit var layoutContainer: ConstraintLayout
	private lateinit var screenLoader: ContentLoadingProgressBar

	override fun attachBaseContext(newBase: Context?) {
		super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase!!))
	}

	override fun onCreate(savedInstanceState: Bundle?) {
//		if (isSplashScreenRequired()) {
//			installSplashScreen()
//		}
		super.onCreate(savedInstanceState)
		actionBar?.hide()
		viewModel = ViewModelProvider(this)[getViewModelClass()]
		binding = getViewBinding()
		binding.lifecycleOwner = this        /*loader = Loader.getLoader(this)*/
		setContentView(binding.root)
		layoutContainer = binding.root.findViewById(R.id.layout_container)
		screenLoader = binding.root.findViewById(R.id.loader)
		onRendered(viewModel, binding)

	}

//	abstract fun isSplashScreenRequired(): Boolean

//	abstract fun getBackCallback(): OnBackPressedCallback

	private fun getViewModelClass(): Class<VM> {
		val type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
		return type as Class<VM>
	}

	abstract fun getViewBinding(): VB

	open fun onRendered(viewModel: VM, binding: VB) {}

	open fun showError(tag: String, message: String, description: String = "") {}

	open fun <T> fetch(
		dataResource: BaseDataSource.Resource<T>,
		isLoaderEnabled: Boolean = true,
		canUserAccessScreen: Boolean = false,
		shouldBeInBackground: Boolean = true,
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
					APIConstants.AUTHENTICATION_POLLING,
					APIConstants.VALIDATE_APP_VERSIONS,
					APIConstants.FETCH_COMPANIONS,
					APIConstants.ADD_STATS,
					APIConstants.FETCH_EVENT_STREAM_DETAILS,
					APIConstants.FETCH_USER_STATS,
					APIConstants.FETCH_EVENT_DETAILS,
					APIConstants.FETCH_EVENT_PRODUCT_DETAILS,
					APIConstants.CLEAR_ALL_RESERVATIONS,
					APIConstants.FETCH_STORY_BOARD -> {
						block.invoke()
					}

					else -> {
						dataResource.message?.let { message ->
							showError(
								dataResource.tag, message
							)
						} ?: showError(dataResource.tag, getString(R.string.unknown_error))
					}
				}
				Logger.print("Error While Calling API for ${dataResource.tag} - " + dataResource.message)
			}
		}
	}
}

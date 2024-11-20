package com.veeps.app.feature.subscription.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.veeps.app.R
import com.veeps.app.core.BaseDataSource
import com.veeps.app.core.BaseFragment
import com.veeps.app.databinding.SubscriptionScreenBinding
import com.veeps.app.extension.isFireTV
import com.veeps.app.feature.contentRail.model.Products
import com.veeps.app.feature.subscription.adapter.ProductsAdapter
import com.veeps.app.feature.subscription.adapter.ProductsDescriptionAdapter
import com.veeps.app.feature.subscription.viewModel.SubscriptionModel
import com.veeps.app.util.APIConstants
import com.veeps.app.util.AppAction
import com.veeps.app.util.AppConstants
import com.veeps.app.util.AppPreferences
import com.veeps.app.util.DEFAULT
import com.veeps.app.util.IntValue.NUMBER_5000
import com.veeps.app.util.Logger
import com.veeps.app.util.PurchaseResponseStatus
import com.veeps.app.util.PurchaseResponseStatus.NOT_SUPPORTED
import com.veeps.app.util.SubscriptionPlanDetails
import com.veeps.app.util.SubscriptionPlanSKUs
import com.veeps.app.widget.navigationMenu.NavigationItems


class SubscriptionScreen : BaseFragment<SubscriptionModel, SubscriptionScreenBinding>() {
    private val action by lazy {
        object : AppAction {
            override fun onAction(action: String) {
                if (context?.isFireTV == true) {
                    if (!homeViewModel.isPaymentInProgress) {
                        homeViewModel.isPaymentInProgress = true
                        val requestId = PurchasingService.purchase(action)
                        AppPreferences.set(AppConstants.SKUId, action)
                        AppPreferences.set(AppConstants.requestId, requestId.toString())
                    }
                } else {
                    homeViewModel.purchaseAction.postValue(NOT_SUPPORTED)
                }
                Logger.print(
                    "Action performed on ${
                        this@SubscriptionScreen.javaClass.name.substringAfterLast(".")
                    }"
                )
            }
        }
    }

    override fun getViewBinding(): SubscriptionScreenBinding =
        SubscriptionScreenBinding.inflate(layoutInflater)

    override fun onDestroyView() {
        viewModelStore.clear()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            subscription = viewModel
            subscriptionScreen = this@SubscriptionScreen
            loader.visibility = View.VISIBLE
            paymentLoader.visibility = View.GONE
            lifecycleOwner = viewLifecycleOwner
            lifecycle.addObserver(viewModel)
        }
        notifyAppEvents()
        loadAppContent()
    }

    private fun loadAppContent() {
        Logger.print(homeViewModel.productsList)
        if (homeViewModel.productsList.isNotEmpty() && homeViewModel.productsList.size == 2) {
            viewModel.getPlanList.postValue(homeViewModel.productsList)
        } else {
            val productList = ArrayList<Products>().apply {
                add(
                    Products(
                        id = SubscriptionPlanSKUs.VEEPS_MONTHLY_SUBSCRIPTION,
                        name = SubscriptionPlanDetails.MONTHLY_PLAN_NAME,
                        price = SubscriptionPlanDetails.MONTHLY_PLAN_PRICE
                    )
                )
                add(
                    Products(
                        id = SubscriptionPlanSKUs.VEEPS_YEARLY_SUBSCRIPTION,
                        name = SubscriptionPlanDetails.YEARLY_PLAN_NAME,
                        price = SubscriptionPlanDetails.YEARLY_PLAN_PRICE
                    )
                )
            }
            viewModel.getPlanList.postValue(productList)
        }
    }

    private fun notifyAppEvents() {
        viewModel.isVisible.observeForever { isVisible ->
            if (isVisible) {
                helper.selectNavigationMenu(NavigationItems.NO_MENU)
                helper.completelyHideNavigationMenu()
            }
            Logger.print(
                "Visibility Changed to $isVisible On ${
                    this@SubscriptionScreen.javaClass.name.substringAfterLast(".")
                }"
            )
        }
        viewModel.getPlanList.observe(viewLifecycleOwner) { planList ->
            val adapter = ProductsAdapter(planList, helper, action)
            binding.planListing.adapter = adapter
            binding.planListing.requestFocus()
            binding.loader.visibility = View.GONE
        }
        viewModel.planDetailListing.observe(viewLifecycleOwner) { planDetail ->
            val adapter = ProductsDescriptionAdapter(planDetail)
            binding.planDetailListing.adapter = adapter
            binding.loader.visibility = View.GONE
        }
        homeViewModel.purchaseAction.observe(viewLifecycleOwner) {
            if (it.isNullOrBlank()) {
                binding.paymentLoader.visibility = View.GONE
            } else if (it == PurchaseResponseStatus.SUCCESS || it == PurchaseResponseStatus.SUCCESS_WITH_PENDING_PURCHASE) {
                if (homeViewModel.isSubscription) {
                    binding.paymentLoader.visibility = View.VISIBLE
                    subscriptionMapping()
                }
            } else {
                var errorMessage = getString(R.string.unknown_error)
                when (it) {
                    PurchaseResponseStatus.FAILED -> {
                        errorMessage = getString(R.string.payment_failed)
                    }

                    PurchaseResponseStatus.CANCELLED_BY_VEEPS -> {
                        errorMessage = getString(R.string.veeps_payment_api_failed)
                    }

                    PurchaseResponseStatus.INVALID_SKU -> {
                        errorMessage = getString(R.string.product_not_available)
                    }

                    NOT_SUPPORTED -> {
                        errorMessage = getString(R.string.iap_not_supported)
                    }

                    PurchaseResponseStatus.PENDING -> {
                        errorMessage = getString(R.string.payment_pending)
                    }

                    PurchaseResponseStatus.ALREADY_PURCHASED -> {
                        errorMessage = getString(R.string.subscription_already_purchased)
                    }
                }

                if (it != PurchaseResponseStatus.NONE) {
                    helper.showErrorOnScreen(APIConstants.GENERATE_NEW_ORDER, errorMessage)
                    homeViewModel.purchaseAction.postValue(PurchaseResponseStatus.NONE)
                }

                AppPreferences.remove(AppConstants.reservedId)
                AppPreferences.remove(AppConstants.receiptId)
                AppPreferences.remove(AppConstants.orderId)
                AppPreferences.remove(AppConstants.requestId)
                AppPreferences.remove(AppConstants.SKUId)
                homeViewModel.isPaymentInProgress = false
                binding.paymentLoader.visibility = View.GONE
            }
        }
    }

    private fun fetchUserDetails() {
        viewModel.fetchUserDetails().observe(viewLifecycleOwner) { userDetails ->
            fetch(
                userDetails,
                isLoaderEnabled = false,
                canUserAccessScreen = false,
                shouldBeInBackground = false
            ) {
                /* TODO: Handle 401 Unauthorized 422 Unprocessable Entity */
                userDetails.response?.let { userData ->
                    userData.data?.let { user ->
                        if (user.subscriptionStatus == "none") {
                            Handler(Looper.getMainLooper()).postDelayed({
                                fetchUserDetails()
                            }, NUMBER_5000.toLong())
                        } else {
                            homeViewModel.purchaseAction.postValue(PurchaseResponseStatus.NONE)
                            binding.paymentLoader.visibility = View.GONE
                            if (context?.isFireTV == true) {
                                PurchasingService.notifyFulfillment(AppPreferences.get(AppConstants.receiptId, DEFAULT.EMPTY_STRING).toString(), FulfillmentResult.FULFILLED)
                            }
                            AppPreferences.set(AppConstants.userSubscriptionStatus, user.subscriptionStatus)
                            helper.goBack()
                        }
                    } ?: helper.showErrorOnScreen(
                        userDetails.tag, getString(R.string.order_pending)
                    )
                } ?: helper.showErrorOnScreen(userDetails.tag, getString(R.string.order_pending))
            }
        }
    }

    private fun subscriptionMapping() {
        viewModel.subscriptionMapping(
            hashMapOf("partner_info" to hashMapOf("receipt_id" to AppPreferences.get(AppConstants.receiptId, DEFAULT.EMPTY_STRING).toString()))
        ).observe(viewLifecycleOwner) { subscriptionMapping ->
            fetch(
                subscriptionMapping,
                isLoaderEnabled = true,
                canUserAccessScreen = false,
                shouldBeInBackground = false
            ) {
                when (subscriptionMapping.callStatus) {
                    BaseDataSource.Resource.CallStatus.SUCCESS -> {
                        Handler(Looper.getMainLooper()).post {
                            fetchUserDetails()
                        }
                    }
                    BaseDataSource.Resource.CallStatus.ERROR -> {
                        PurchasingService.notifyFulfillment(AppPreferences.get(AppConstants.receiptId, DEFAULT.EMPTY_STRING).toString(), FulfillmentResult.UNAVAILABLE)
                        homeViewModel.purchaseAction.postValue(PurchaseResponseStatus.CANCELLED_BY_VEEPS)
                    }
                    else -> Logger.doNothing()
                }
            }
        }
    }
}
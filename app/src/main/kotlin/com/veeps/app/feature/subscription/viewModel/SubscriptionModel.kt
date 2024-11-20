package com.veeps.app.feature.subscription.viewModel

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.veeps.app.data.common.APIRepository
import com.veeps.app.feature.contentRail.model.Products
import com.veeps.app.util.SubscriptionPlanDetails

class SubscriptionModel : ViewModel(), DefaultLifecycleObserver {
    var isVisible = MutableLiveData(false)
    var getPlanList = MutableLiveData(ArrayList<Products>())
    var planDetailListing = MutableLiveData(ArrayList<String>().apply {
        add(SubscriptionPlanDetails.PLAN_BENEFIT_ONE)
        add(SubscriptionPlanDetails.PLAN_BENEFIT_TWO)
        add(SubscriptionPlanDetails.PLAN_BENEFIT_THREE)
        add(SubscriptionPlanDetails.PLAN_BENEFIT_FOUR)
    })

    fun fetchUserDetails() = APIRepository().fetchUserDetails()
    fun subscriptionMapping(subscriptionMappingRequest: HashMap<String, Any>) =
        APIRepository().subscriptionMapping(subscriptionMappingRequest)

}
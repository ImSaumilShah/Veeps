package com.veeps.app.feature.signIn.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.veeps.app.data.common.APIRepository
import com.veeps.app.util.AppConstants
import com.veeps.app.util.DEFAULT

class SignInViewModel : ViewModel() {
	var authenticationCode = MutableLiveData(DEFAULT.EMPTY_STRING)
	var authenticationLoginURL = MutableLiveData(DEFAULT.EMPTY_STRING)
	var authenticationQRCode = MutableLiveData(DEFAULT.EMPTY_STRING)
	var pollingInterval = MutableLiveData(0)
	var tokenExpiryTime = MutableLiveData(0)

	var contentHasLoaded = MutableLiveData(false)

	var deviceCode = MutableLiveData(DEFAULT.EMPTY_STRING)

	var errorMessage = MutableLiveData(DEFAULT.EMPTY_STRING)
	var errorNegativeLabel = MutableLiveData(DEFAULT.EMPTY_STRING)
	var errorPositiveLabel = MutableLiveData(DEFAULT.EMPTY_STRING)
	var isErrorNegativeApplicable = MutableLiveData(false)
	var isErrorPositiveApplicable = MutableLiveData(false)

	fun fetchAuthenticationDetails() = APIRepository().authenticationDetails(AppConstants.clientId)
	fun authenticationPolling() =
		APIRepository().authenticationPolling(if (deviceCode.value.isNullOrBlank()) DEFAULT.EMPTY_STRING else deviceCode.value.toString())

	fun fetchUserDetails() = APIRepository().fetchUserDetails()
}
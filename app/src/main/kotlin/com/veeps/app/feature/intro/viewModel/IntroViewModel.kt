package com.veeps.app.feature.intro.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.veeps.app.data.common.APIRepository
import com.veeps.app.data.network.APIUtil
import com.veeps.app.util.APIConstants
import com.veeps.app.util.DEFAULT

class IntroViewModel : ViewModel() {
	var contentHasLoaded = MutableLiveData(false)
	var errorMessage = MutableLiveData(DEFAULT.EMPTY_STRING)
	var errorPositiveLabel = MutableLiveData(DEFAULT.EMPTY_STRING)
	var errorNegativeLabel = MutableLiveData(DEFAULT.EMPTY_STRING)
	var isErrorPositiveApplicable = MutableLiveData(false)
	var isErrorNegativeApplicable = MutableLiveData(false)
	val isErrorVisible = MutableLiveData(false)
}
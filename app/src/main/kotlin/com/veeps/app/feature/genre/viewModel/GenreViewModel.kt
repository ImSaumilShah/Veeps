package com.veeps.app.feature.genre.viewModel

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.veeps.app.data.common.APIRepository
import com.veeps.app.feature.contentRail.model.RailData

class GenreViewModel : ViewModel(), DefaultLifecycleObserver {
    var isVisible = MutableLiveData(false)
    var railData = MutableLiveData(ArrayList<RailData>())
    var eventId: String = ""

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        isVisible.postValue(true)
    }

    override fun onPause(owner: LifecycleOwner) {
        isVisible.postValue(false)
        super.onPause(owner)
    }

    fun fetchEventDetails(eventId: String) = APIRepository().fetchEventDetails(eventId)

    fun fetchGenreRails(genreName: String) = APIRepository().fetchGenreRails(genreName)

}

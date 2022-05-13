package edu.vt.cs.cs5254.dreamcatcher

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.io.File
import java.util.*

class DreamDetailViewModel : ViewModel() {
    private val dreamRepository = DreamRepository.get()
    var dreamWithEntriesIdLiveData = MutableLiveData<UUID>()

    var dreamWithEntriesLiveData: LiveData<DreamWithEntries> =
        Transformations.switchMap(dreamWithEntriesIdLiveData) { dreamId ->
            dreamRepository.getDreamWithEntries(dreamId)
        }

    fun loadDream(dreamId: UUID) {
        dreamWithEntriesIdLiveData.value = dreamId
    }

    fun saveDreamWithEntries(dreamWithEntries: DreamWithEntries){
        dreamRepository.updateDreamWithEntries(dreamWithEntries)
    }

    fun getPhotoFile(dream: Dream) : File {
        return dreamRepository.getPhotoFile(dream)
    }
}
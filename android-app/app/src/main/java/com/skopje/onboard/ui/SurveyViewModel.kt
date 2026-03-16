package com.skopje.onboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skopje.onboard.data.AppDatabase
import com.skopje.onboard.data.Survey
import com.skopje.onboard.sync.SyncScheduler
import com.skopje.onboard.sync.SyncWorker
import com.skopje.onboard.util.LocationHelper
import com.skopje.onboard.util.Preferences
import com.skopje.onboard.util.checkServerOnline
import com.skopje.onboard.util.GpsStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SurveyUiState(
    val screen: Screen = Screen.Start,
    val stationName: String = "",
    val surveyorId: String = "",
    val passengerCount: Int = 0,
    val currentSurvey: Survey? = null,
    val gpsStatus: GpsStatus = GpsStatus.ACQUIRING,
    val serverOnline: Boolean = false,
    val showResumeDialog: Boolean = false,
    val showResetDialog: Boolean = false,
    val showSubmitDialog: Boolean = false,
)

enum class Screen { Start, Counting, Settings }

class SurveyViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val dao = db.surveyDao()
    private val prefs = Preferences(application)
    private val locationHelper = LocationHelper(application)

    private val _state = MutableStateFlow(SurveyUiState())
    val state: StateFlow<SurveyUiState> = _state.asStateFlow()

    init {
        SyncScheduler.schedule(application)
    }

    fun checkResumeOnStart() {
        viewModelScope.launch {
            val pending = dao.getPendingSurvey()
            if (pending != null) {
                _state.update {
                    it.copy(showResumeDialog = true, currentSurvey = pending)
                }
            }
        }
    }

    fun resumeSurvey() {
        val s = _state.value.currentSurvey ?: return
        _state.update {
            it.copy(
                showResumeDialog = false,
                screen = Screen.Counting,
                stationName = s.stationName,
                surveyorId = s.surveyorId,
                passengerCount = s.passengerCount,
                currentSurvey = s,
            )
        }
        viewModelScope.launch { refreshGpsAndServer() }
    }

    fun discardSurvey() {
        viewModelScope.launch {
            val s = _state.value.currentSurvey
            if (s != null) dao.delete(s.id)
            _state.update {
                it.copy(showResumeDialog = false, currentSurvey = null)
            }
        }
    }

    fun setStationName(v: String) { _state.update { it.copy(stationName = v) } }
    fun setSurveyorId(v: String) { _state.update { it.copy(surveyorId = v) } }

    fun startSurvey() {
        val s = _state.value
        if (s.stationName.isBlank() || s.surveyorId.isBlank()) return

        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val survey = Survey(
                surveyorId = s.surveyorId,
                stationName = s.stationName,
                startTime = now,
                submitTime = now,
                latitude = null,
                longitude = null,
                passengerCount = 0,
            )
            val id = dao.insert(survey)
            val created = dao.getById(id)!!
            _state.update {
                it.copy(
                    screen = Screen.Counting,
                    currentSurvey = created,
                    passengerCount = 0,
                )
            }
            refreshGpsAndServer()
        }
    }

    fun addCount(delta: Int) {
        val s = _state.value.currentSurvey ?: return
        val newCount = (s.passengerCount + delta).coerceAtLeast(0)
        viewModelScope.launch {
            val updated = s.copy(passengerCount = newCount, submitTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            dao.update(updated)
            _state.update { it.copy(passengerCount = newCount, currentSurvey = updated) }
        }
    }

    fun requestReset() { _state.update { it.copy(showResetDialog = true) } }
    fun dismissResetDialog() { _state.update { it.copy(showResetDialog = false) } }

    fun resetCounter() {
        viewModelScope.launch {
            val s = _state.value.currentSurvey ?: return@launch
            val updated = s.copy(passengerCount = 0, submitTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            dao.update(updated)
            _state.update { it.copy(passengerCount = 0, currentSurvey = updated, showResetDialog = false) }
        }
    }

    fun requestSubmit() { _state.update { it.copy(showSubmitDialog = true) } }
    fun dismissSubmitDialog() { _state.update { it.copy(showSubmitDialog = false) } }

    fun submitSurvey() {
        viewModelScope.launch {
            val s = _state.value.currentSurvey ?: return@launch
            val loc = locationHelper.getLocation()
            val submitTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val updated = s.copy(
                submitTime = submitTime,
                latitude = loc.lat,
                longitude = loc.lng,
                isSubmitted = true,
            )
            dao.update(updated)
            androidx.work.WorkManager.getInstance(getApplication()).enqueue(
                androidx.work.OneTimeWorkRequestBuilder<com.skopje.onboard.sync.SyncWorker>().build()
            )
            _state.update {
                it.copy(
                    screen = Screen.Start,
                    currentSurvey = null,
                    passengerCount = 0,
                    showSubmitDialog = false,
                )
            }
        }
    }

    fun refreshGpsAndServer() {
        viewModelScope.launch {
            _state.update { it.copy(gpsStatus = GpsStatus.ACQUIRING) }
            val loc = withContext(Dispatchers.IO) { locationHelper.getLocation() }
            val online = withContext(Dispatchers.IO) { checkServerOnline(prefs.getApiUrl()) }
            _state.update {
                it.copy(gpsStatus = loc.status, serverOnline = online)
            }
            val s = _state.value.currentSurvey
            if (s != null && (loc.lat != null || loc.lng != null)) {
                val updated = s.copy(latitude = loc.lat, longitude = loc.lng)
                dao.update(updated)
                _state.update { it.copy(currentSurvey = updated) }
            }
        }
    }

    fun navigateToSettings() { _state.update { it.copy(screen = Screen.Settings) } }
    fun navigateBack() { _state.update { it.copy(screen = Screen.Start) } }

    fun setLanguage(lang: String) {
        viewModelScope.launch { prefs.setLanguage(lang) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { prefs.setTheme(theme) }
    }

    fun setApiUrl(url: String) {
        viewModelScope.launch { prefs.setApiUrl(url) }
    }
}

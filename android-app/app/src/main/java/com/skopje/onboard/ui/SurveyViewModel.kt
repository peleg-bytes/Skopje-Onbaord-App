package com.skopje.onboard.ui

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skopje.onboard.data.AppDatabase
import com.skopje.onboard.data.Survey
import com.skopje.onboard.sync.SyncHelper
import com.skopje.onboard.sync.SyncScheduler
import com.skopje.onboard.sync.enqueueSubmitFollowUpSync
import com.skopje.onboard.util.LocationHelper
import com.skopje.onboard.util.Preferences
import com.skopje.onboard.R
import com.skopje.onboard.util.checkServerOnline
import com.skopje.onboard.util.GpsStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale as JavaLocale

internal fun Survey.isContinuableDraft(): Boolean = !isSubmitted && passengerCount > 1

data class SurveyUiState(
    val screen: Screen = Screen.Start,
    val stationName: String = "",
    val surveyorId: String = "",
    val passengerCount: Int = 0,
    val currentSurvey: Survey? = null,
    val gpsStatus: GpsStatus = GpsStatus.ACQUIRING,
    val serverOnline: Boolean = false,
    /** Latest in-progress row from DB for the home screen (not necessarily continuable). */
    val homeDraft: Survey? = null,
    val showStartNewSurveyConfirmDialog: Boolean = false,
    val showResetDialog: Boolean = false,
    val showSubmitDialog: Boolean = false,
    val showExitDialog: Boolean = false,
    val submitFeedback: SubmitFeedback? = null,
    val isSubmitting: Boolean = false,
    /** Shown under the submit dialog text while [isSubmitting] is true. */
    val submitProgressResId: Int? = null,
    /** True while a manual sync (start screen / history) is running. */
    val isSyncInProgress: Boolean = false,
    /** False until [checkResumeOnStart] finishes so the user cannot start a new survey before we know about a draft. */
    val resumeCheckComplete: Boolean = false,
)

enum class SubmitFeedback { SUCCESS, QUEUED_WAITING_NETWORK }

enum class Screen { Start, Counting, Settings, SubmittedSurveys }

class SurveyViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        /** Cap how long the quick server round-trip may block the submit flow. */
        private const val SUBMIT_QUICK_SYNC_TIMEOUT_MS = 6500L
    }

    private val db = AppDatabase.getInstance(application)
    private val dao = db.surveyDao()
    private val prefs = Preferences(application)
    private val locationHelper = LocationHelper(application)

    /** Avoid overlapping location/server refreshes (initial + periodic ticks). */
    private val locationRefreshMutex = Mutex()

    private val resumeCheckLock = Any()
    private var resumeCheckJob: Job? = null

    private val _state = MutableStateFlow(SurveyUiState())
    val state: StateFlow<SurveyUiState> = _state.asStateFlow()

    val submittedSurveys: StateFlow<List<Survey>> = dao.observeSubmittedSurveys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        SyncScheduler.schedule(application)
        viewModelScope.launch {
            val savedSurveyor = prefs.getSurveyorId().orEmpty()
            _state.update { it.copy(surveyorId = savedSurveyor) }
        }
        // Bulk sync only on IO — never blocks the main thread or starting new surveys.
        viewModelScope.launch(Dispatchers.IO) {
            SyncHelper.syncNow(application)
        }
    }

    fun checkResumeOnStart() {
        if (_state.value.resumeCheckComplete) return
        synchronized(resumeCheckLock) {
            if (_state.value.resumeCheckComplete) return
            if (resumeCheckJob?.isActive == true) return
            resumeCheckJob = viewModelScope.launch(Dispatchers.IO) {
                reloadHomeDraftLocked()
                _state.update { cur ->
                    cur.copy(
                        resumeCheckComplete = true,
                        currentSurvey = when {
                            cur.screen == Screen.Counting && cur.currentSurvey != null -> cur.currentSurvey
                            else -> null
                        },
                    )
                }
            }
        }
    }

    /**
     * Persists the in-memory in-progress survey to Room (same as after each +/- tap).
     * Used on a timer and when the activity stops so a kill shortly after the last tap loses at most one interval.
     */
    fun checkpointSurvey() {
        viewModelScope.launch(Dispatchers.IO) {
            val s = _state.value.currentSurvey?.takeIf { !it.isSubmitted } ?: return@launch
            dao.update(s)
        }
    }

    fun continueSurvey() {
        viewModelScope.launch {
            val draft = _state.value.homeDraft ?: return@launch
            if (!draft.isContinuableDraft()) return@launch
            val fresh = withContext(Dispatchers.IO) { dao.getById(draft.id) } ?: return@launch
            if (!fresh.isContinuableDraft()) return@launch
            _state.update {
                it.copy(
                    screen = Screen.Counting,
                    stationName = fresh.stationName,
                    surveyorId = fresh.surveyorId,
                    passengerCount = fresh.passengerCount,
                    currentSurvey = fresh,
                )
            }
            refreshGpsAndServer()
        }
    }

    fun refreshHomeDraft() {
        viewModelScope.launch(Dispatchers.IO) { reloadHomeDraftLocked() }
    }

    private suspend fun reloadHomeDraftLocked() {
        val pending = withContext(Dispatchers.IO) {
            dao.deleteStalePendingSurveys()
            dao.getPendingSurvey()
        }
        _state.update { it.copy(homeDraft = pending) }
    }

    fun setStationName(v: String) {
        _state.update { it.copy(stationName = v) }
    }

    fun setSurveyorId(v: String) {
        _state.update { it.copy(surveyorId = v) }
        viewModelScope.launch { prefs.setSurveyorId(v) }
    }

    fun startSurvey() {
        val s = _state.value
        if (s.stationName.isBlank() || s.surveyorId.isBlank()) return

        viewModelScope.launch {
            val existing = withContext(Dispatchers.IO) { dao.getPendingSurvey() }
            if (existing != null && existing.isContinuableDraft()) {
                _state.update { it.copy(showStartNewSurveyConfirmDialog = true) }
                return@launch
            }
            if (existing != null) {
                withContext(Dispatchers.IO) { dao.delete(existing.id) }
            }
            insertNewSurveyAndOpenCounting()
        }
    }

    fun dismissStartNewSurveyConfirmDialog() {
        _state.update { it.copy(showStartNewSurveyConfirmDialog = false) }
    }

    fun confirmStartNewSurveyReplacingDraft() {
        viewModelScope.launch {
            _state.update { it.copy(showStartNewSurveyConfirmDialog = false) }
            withContext(Dispatchers.IO) {
                dao.deleteStalePendingSurveys()
                val existing = dao.getPendingSurvey()
                if (existing != null) dao.delete(existing.id)
            }
            insertNewSurveyAndOpenCounting()
        }
    }

    private suspend fun insertNewSurveyAndOpenCounting() {
        val s = _state.value
        if (s.stationName.isBlank() || s.surveyorId.isBlank()) return
        val now = SimpleDateFormat("HH:mm:ss", JavaLocale.getDefault()).format(Date())
        val created = withContext(Dispatchers.IO) {
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
            dao.getById(id)!!
        }
        _state.update {
            it.copy(
                screen = Screen.Counting,
                currentSurvey = created,
                passengerCount = 0,
                homeDraft = created,
            )
        }
        refreshGpsAndServer()
    }

    fun addCount(delta: Int) {
        val s = _state.value.currentSurvey ?: return
        val newCount = (s.passengerCount + delta).coerceAtLeast(0)
        viewModelScope.launch {
            val updated = s.copy(passengerCount = newCount, submitTime = SimpleDateFormat("HH:mm:ss", JavaLocale.getDefault()).format(Date()))
            dao.update(updated)
            _state.update { it.copy(passengerCount = newCount, currentSurvey = updated) }
        }
    }

    fun requestReset() { _state.update { it.copy(showResetDialog = true) } }
    fun dismissResetDialog() { _state.update { it.copy(showResetDialog = false) } }

    fun resetCounter() {
        viewModelScope.launch {
            val s = _state.value.currentSurvey ?: return@launch
            val updated = s.copy(passengerCount = 0, submitTime = SimpleDateFormat("HH:mm:ss", JavaLocale.getDefault()).format(Date()))
            dao.update(updated)
            _state.update { it.copy(passengerCount = 0, currentSurvey = updated, showResetDialog = false) }
        }
    }

    fun requestSubmit() { _state.update { it.copy(showSubmitDialog = true) } }
    fun dismissSubmitDialog() { _state.update { it.copy(showSubmitDialog = false) } }
    fun requestExitSurvey() { _state.update { it.copy(showExitDialog = true) } }
    fun dismissExitDialog() { _state.update { it.copy(showExitDialog = false) } }
    fun confirmExitSurvey() {
        viewModelScope.launch {
            val s = _state.value.currentSurvey
            if (s != null) dao.delete(s.id)
            _state.update {
                it.copy(
                    showExitDialog = false,
                    screen = Screen.Start,
                    currentSurvey = null,
                    passengerCount = 0,
                    homeDraft = null,
                )
            }
            reloadHomeDraftLocked()
        }
    }

    fun submitSurvey() {
        if (_state.value.isSubmitting) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSubmitting = true,
                    submitProgressResId = R.string.submit_status_saving,
                )
            }
            try {
                val s = _state.value.currentSurvey
                if (s == null) return@launch
                val loc = withContext(Dispatchers.IO) { locationHelper.getLocation() }
                val submitTime = SimpleDateFormat("HH:mm:ss", JavaLocale.getDefault()).format(Date())
                val updated = s.copy(
                    submitTime = submitTime,
                    latitude = loc.lat,
                    longitude = loc.lng,
                    isSubmitted = true,
                )
                dao.update(updated)
                _state.update { it.copy(submitProgressResId = R.string.submit_status_sending) }
                val uploadedQuick = withTimeoutOrNull(SUBMIT_QUICK_SYNC_TIMEOUT_MS) {
                    SyncHelper.tryUploadSurveyQuick(getApplication(), updated)
                } == true
                if (!uploadedQuick) {
                    enqueueSubmitFollowUpSync(getApplication())
                }
                val feedback = if (uploadedQuick) SubmitFeedback.SUCCESS else SubmitFeedback.QUEUED_WAITING_NETWORK
                prefs.setStationName("")
                _state.update {
                    it.copy(
                        screen = Screen.Start,
                        currentSurvey = null,
                        passengerCount = 0,
                        stationName = "",
                        homeDraft = null,
                        showSubmitDialog = false,
                        submitFeedback = feedback,
                        submitProgressResId = null,
                    )
                }
                reloadHomeDraftLocked()
                withContext(Dispatchers.Main) {
                    val app = getApplication<Application>()
                    vibrateSubmitFeedback(app, feedback)
                    val msgResId = when (feedback) {
                        SubmitFeedback.SUCCESS -> R.string.submit_success
                        SubmitFeedback.QUEUED_WAITING_NETWORK -> R.string.submit_queued_waiting_network
                    }
                    showLocalizedToast(msgResId)
                }
                kotlinx.coroutines.delay(100)
                _state.update { it.copy(submitFeedback = null) }
            } finally {
                _state.update { it.copy(isSubmitting = false, submitProgressResId = null) }
            }
        }
    }

    /** Toasts must use this context so strings follow Settings → Language (mk/en), not only system locale. */
    private suspend fun localizedToastContext(): Context {
        val app = getApplication<Application>()
        val lang = prefs.language.first()
        val conf = Configuration(app.resources.configuration)
        conf.setLocale(JavaLocale(if (lang == "mk") "mk" else "en"))
        return app.createConfigurationContext(conf)
    }

    private suspend fun showLocalizedToast(messageResId: Int, length: Int = Toast.LENGTH_LONG) {
        Toast.makeText(localizedToastContext(), messageResId, length).show()
    }

    private fun vibrateSubmitFeedback(context: android.content.Context, feedback: SubmitFeedback) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = if (feedback == SubmitFeedback.SUCCESS) longArrayOf(0, 80, 80, 80) else longArrayOf(0, 120, 80, 120)
            val amplitudes = if (feedback == SubmitFeedback.SUCCESS) intArrayOf(0, 80, 0, 80) else intArrayOf(0, 80, 0, 80)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (feedback == SubmitFeedback.SUCCESS) 80L else 150L)
        }
    }

    fun refreshGpsAndServer() {
        viewModelScope.launch {
            locationRefreshMutex.withLock {
                _state.update { it.copy(gpsStatus = GpsStatus.ACQUIRING) }
                val (loc, online) = coroutineScope {
                    val locDef = async(Dispatchers.IO) { locationHelper.getLocation() }
                    val onlineDef = async(Dispatchers.IO) { checkServerOnline() }
                    Pair(locDef.await(), onlineDef.await())
                }
                _state.update {
                    it.copy(gpsStatus = loc.status, serverOnline = online)
                }
                applyLocationToCurrentSurvey(loc.lat, loc.lng)
            }
        }
    }

    /**
     * While on the counting screen, re-check location and server on a timer.
     * Skips if another refresh is running; does not flash "acquiring" or block submit/sync.
     */
    fun tickPeriodicLocationAndServerRefresh() {
        viewModelScope.launch {
            if (!locationRefreshMutex.tryLock()) return@launch
            try {
                val (loc, online) = coroutineScope {
                    val locDef = async(Dispatchers.IO) { locationHelper.getLocationLight() }
                    val onlineDef = async(Dispatchers.IO) { checkServerOnline() }
                    Pair(locDef.await(), onlineDef.await())
                }
                _state.update {
                    it.copy(gpsStatus = loc.status, serverOnline = online)
                }
                applyLocationToCurrentSurvey(loc.lat, loc.lng)
            } finally {
                locationRefreshMutex.unlock()
            }
        }
    }

    private suspend fun applyLocationToCurrentSurvey(lat: Double?, lng: Double?) {
        val s = _state.value.currentSurvey ?: return
        if (lat == null && lng == null) return
        val updated = s.copy(latitude = lat, longitude = lng)
        dao.update(updated)
        _state.update { it.copy(currentSurvey = updated) }
    }

    fun navigateToSettings() {
        _state.update { it.copy(screen = Screen.Settings) }
    }

    fun navigateToSubmittedSurveys() {
        _state.update { it.copy(screen = Screen.SubmittedSurveys) }
    }

    /**
     * Uploads only surveys that are submitted locally but not yet marked uploaded ([Survey.uploadedStatus] == false).
     * Each row is marked uploaded after a successful server response, so the same survey is not sent again from the app.
     * The server also treats rapid duplicates as success without a second insert.
     *
     * @param showFeedback If true, shows toasts for empty queue / result (for the start screen).
     */
    fun requestManualSync(showFeedback: Boolean = false) {
        viewModelScope.launch {
            if (_state.value.isSyncInProgress) return@launch
            _state.update { it.copy(isSyncInProgress = true) }
            try {
                val pending = withContext(Dispatchers.IO) { dao.getUnuploadedSubmittedSurveys() }
                if (pending.isEmpty()) {
                    if (showFeedback) {
                        withContext(Dispatchers.Main) {
                            showLocalizedToast(R.string.sync_toast_nothing_pending, Toast.LENGTH_SHORT)
                        }
                    }
                    return@launch
                }
                val result = withContext(Dispatchers.IO) { SyncHelper.syncNow(getApplication()) }
                if (showFeedback) {
                    withContext(Dispatchers.Main) {
                        val msg = if (result is SyncHelper.SyncResult.Success) {
                            R.string.sync_toast_success
                        } else {
                            R.string.sync_toast_failed
                        }
                        showLocalizedToast(msg)
                    }
                }
            } finally {
                _state.update { it.copy(isSyncInProgress = false) }
            }
        }
    }

    fun navigateBack() {
        _state.update { it.copy(screen = Screen.Start) }
        refreshHomeDraft()
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { prefs.setLanguage(lang) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { prefs.setTheme(theme) }
    }
}

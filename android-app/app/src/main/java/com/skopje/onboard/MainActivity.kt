package com.skopje.onboard

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import com.skopje.onboard.ui.ConfirmDialog
import com.skopje.onboard.ui.CountingScreen
import com.skopje.onboard.ui.ResumeDialog
import com.skopje.onboard.ui.SettingsScreen
import com.skopje.onboard.ui.StartSurveyScreen
import com.skopje.onboard.ui.SurveyViewModel
import com.skopje.onboard.ui.Screen
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val requestPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            LaunchedEffect(Unit) {
                requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<SurveyViewModel>()
            val state by viewModel.state.collectAsState()
            val prefs = com.skopje.onboard.util.Preferences(this)
            val theme by prefs.theme.collectAsState(initial = "system")
            val language by prefs.language.collectAsState(initial = "mk")
            val apiUrl by prefs.apiUrl.collectAsState(initial = "https://skopje-onboard-survey.vercel.app")

            LaunchedEffect(Unit) { viewModel.checkResumeOnStart() }

            val darkTheme = when (theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            val config = LocalConfiguration.current
            LaunchedEffect(language) {
                val locale = Locale(if (language == "mk") "mk" else "en")
                val newConfig = Configuration(config).apply { setLocale(locale) }
                resources.updateConfiguration(newConfig, resources.displayMetrics)
            }

            LaunchedEffect(state.screen) {
                if (state.screen == Screen.Counting) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
                when (state.screen) {
                    Screen.Start -> StartSurveyScreen(
                        stationName = state.stationName,
                        surveyorId = state.surveyorId,
                        onStationNameChange = viewModel::setStationName,
                        onSurveyorIdChange = viewModel::setSurveyorId,
                        onStartSurvey = viewModel::startSurvey,
                        onOpenSettings = viewModel::navigateToSettings,
                    )
                    Screen.Counting -> CountingScreen(
                        passengerCount = state.passengerCount,
                        stationName = state.stationName,
                        surveyorId = state.surveyorId,
                        gpsStatus = state.gpsStatus,
                        serverOnline = state.serverOnline,
                        onAdd = viewModel::addCount,
                        onReset = viewModel::requestReset,
                        onSubmit = viewModel::requestSubmit,
                        onVibrate = { },
                    )
                    Screen.Settings -> SettingsScreen(
                        language = language,
                        theme = theme,
                        apiUrl = apiUrl,
                        onLanguageChange = viewModel::setLanguage,
                        onThemeChange = viewModel::setTheme,
                        onApiUrlChange = viewModel::setApiUrl,
                        onBack = viewModel::navigateBack,
                    )
                }

                if (state.showResumeDialog) {
                    ResumeDialog(
                        onResume = viewModel::resumeSurvey,
                        onDiscard = viewModel::discardSurvey,
                    )
                }
                if (state.showResetDialog) {
                    ConfirmDialog.ResetConfirmDialog(
                        onConfirm = viewModel::resetCounter,
                        onDismiss = viewModel::dismissResetDialog,
                    )
                }
                if (state.showSubmitDialog) {
                    ConfirmDialog.SubmitConfirmDialog(
                        onConfirm = viewModel::submitSurvey,
                        onDismiss = viewModel::dismissSubmitDialog,
                    )
                }
            }
        }
    }
}

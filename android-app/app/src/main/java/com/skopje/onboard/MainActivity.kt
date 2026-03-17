package com.skopje.onboard

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import com.skopje.onboard.ui.CountingScreen
import com.skopje.onboard.ui.ExitSurveyConfirmDialog
import com.skopje.onboard.ui.ResetConfirmDialog
import com.skopje.onboard.ui.ResumeDialog
import com.skopje.onboard.ui.SettingsScreen
import com.skopje.onboard.ui.SubmitConfirmDialog
import com.skopje.onboard.ui.StartSurveyScreen
import com.skopje.onboard.ui.SurveyViewModel
import com.skopje.onboard.ui.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = com.skopje.onboard.util.Preferences(this)
        val lang = runBlocking { prefs.language.first() }
        val locale = Locale(if (lang == "mk") "mk" else "en")
        val config = Configuration(resources.configuration).apply { setLocale(locale) }
        resources.updateConfiguration(config, resources.displayMetrics)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val requestPermissions = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { }
            LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf<String>()
                if (this@MainActivity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                if (this@MainActivity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                if (permissionsToRequest.isNotEmpty()) {
                    requestPermissions.launch(permissionsToRequest.toTypedArray())
                }
            }
            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<SurveyViewModel>()
            val state by viewModel.state.collectAsState()
            val theme by prefs.theme.collectAsState(initial = "system")
            val language by prefs.language.collectAsState(initial = "mk")

            LaunchedEffect(Unit) { viewModel.checkResumeOnStart() }

            val darkTheme = when (theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            val currentConfig = LocalConfiguration.current
            LaunchedEffect(language) {
                val appLocale = Locale(if (language == "mk") "mk" else "en")
                val newConfig = Configuration(currentConfig).apply { setLocale(appLocale) }
                resources.updateConfiguration(newConfig, resources.displayMetrics)
            }

            LaunchedEffect(state.screen) {
                if (state.screen == Screen.Counting) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            BackHandler(enabled = state.screen == Screen.Counting) {
                viewModel.requestExitSurvey()
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
                        onExit = viewModel::requestExitSurvey,
                    )
                    Screen.Settings -> SettingsScreen(
                        language = language,
                        theme = theme,
                        onLanguageChange = viewModel::setLanguage,
                        onThemeChange = viewModel::setTheme,
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
                    ResetConfirmDialog(
                        onConfirm = viewModel::resetCounter,
                        onDismiss = viewModel::dismissResetDialog,
                    )
                }
                if (state.showSubmitDialog) {
                    SubmitConfirmDialog(
                        onConfirm = viewModel::submitSurvey,
                        onDismiss = viewModel::dismissSubmitDialog,
                        isSubmitting = state.isSubmitting,
                    )
                }
                if (state.showExitDialog) {
                    ExitSurveyConfirmDialog(
                        onConfirm = viewModel::confirmExitSurvey,
                        onDismiss = viewModel::dismissExitDialog,
                    )
                }
            }
        }
    }
}

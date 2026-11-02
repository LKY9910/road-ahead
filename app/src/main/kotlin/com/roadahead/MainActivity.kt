package com.roadahead

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import com.roadahead.core.model.DirectionConvention
import com.roadahead.ui.*

class MainActivity : ComponentActivity() {
    private var prefs by mutableStateOf(Prefs(AppMode.LIVE, DirectionConvention.APPROACH, false))
    private var overlayAllowed by mutableStateOf(false)
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        Log.i("RoadAhead", "permissions=$it")
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) startGuidance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = readPrefs(this)
        val missing = RoadAheadService.REQUIRED_PERMISSIONS.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) permissions.launch(missing.toTypedArray())
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val state by RoadAheadService.state.collectAsState()
                var settingsOpen by rememberSaveable { mutableStateOf(false) }
                var pickerEnabling by rememberSaveable { mutableStateOf<Boolean?>(null) }
                val closePicker = {
                    pickerEnabling = null
                    companionAction(RoadAheadService.ACTION_WITHDRAW_COMPANIONS)
                }
                LaunchedEffect(state.parked) {
                    if (!state.parked) {
                        if (pickerEnabling != null) closePicker()
                        settingsOpen = false
                    }
                }
                BackHandler(pickerEnabling != null || settingsOpen) {
                    if (pickerEnabling != null) closePicker() else settingsOpen = false
                }
                Surface {
                    if (state.parked && pickerEnabling != null) CompanionPicker(prefs.companions, pickerEnabling == true,
                        onComplete = { companions ->
                            if (RoadAheadService.state.value.parked) {
                                savePrefs(prefs.copy(companions = companions, travelMode = true))
                            }
                            closePicker()
                        }, onCancel = closePicker)
                    else if (state.parked && settingsOpen) Settings(prefs,
                        { if (RoadAheadService.state.value.parked) savePrefs(it) }, { settingsOpen = false },
                        onChooseCompanions = { enabling ->
                            if (RoadAheadService.state.value.parked) {
                                pickerEnabling = enabling
                                if (enabling) companionAction(RoadAheadService.ACTION_ASK_COMPANIONS)
                            }
                        })
                    else RoadAheadScreen(state.screen,
                        AppStatus(prefs.mode.label.takeIf { prefs.mode == AppMode.LIVE }, state.speechAvailable,
                            state.note ?: "위치·차량 속도 권한이 필요해요".takeIf { !hasInputPermissions() }),
                        state.parked, { settingsOpen = true }, overlayAllowed && !state.overlayPermissionMissing, ::openAppInfo)
                    if (state.parked && !prefs.disclaimerSeen) AlertDialog(
                        onDismissRequest = {}, text = { Text(getString(R.string.disclaimer)) },
                        confirmButton = { TextButton(onClick = {
                            savePrefs(prefs.copy(disclaimerSeen = true))
                        }) { Text("확인") } })
                }
            }
        }
    }

    private fun savePrefs(next: Prefs) {
        val restart = prefs.mode != next.mode || prefs.convention != next.convention || prefs.travelMode != next.travelMode ||
            prefs.companions != next.companions
        prefs = next
        writePrefs(this, next)
        if (restart) startGuidance()
    }

    private fun companionAction(action: String) {
        if (RoadAheadService.state.value.running) startService(Intent(this, RoadAheadService::class.java).setAction(action))
    }

    private fun startGuidance() {
        overlayAllowed = AndroidSettings.canDrawOverlays(this)
        if (!overlayAllowed) {
            stopService(Intent(this, RoadAheadService::class.java))
            return
        }
        if (!hasInputPermissions()) return
        startForegroundService(Intent(this, RoadAheadService::class.java)
            .putExtra("logOnlySpeech", intent.getBooleanExtra("logOnlySpeech", false)))
    }

    private fun openAppInfo() {
        startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    private fun hasInputPermissions() = RoadAheadService.REQUIRED_PERMISSIONS.all {
        checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        overlayAllowed = AndroidSettings.canDrawOverlays(this)
        if (!RoadAheadService.state.value.running || !overlayAllowed) startGuidance()
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); startGuidance() }
}

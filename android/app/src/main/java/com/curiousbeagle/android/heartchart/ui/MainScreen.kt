package com.curiousbeagle.android.heartchart.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curiousbeagle.android.heartchart.R
import com.curiousbeagle.android.heartchart.bluetooth.HeartRateMonitor
import com.curiousbeagle.android.heartchart.bluetooth.HeartRateMonitor.ConnectionState
import com.curiousbeagle.android.heartchart.data.HeartRateZones
import com.curiousbeagle.android.heartchart.data.SettingsStore
import kotlinx.coroutines.launch

/** The port of ContentView: live reading, chart with zone lines, status, banner. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(monitor: HeartRateMonitor, settings: SettingsStore) {
    val state by monitor.state.collectAsState()
    val currentHeartRate by monitor.currentHeartRate.collectAsState()
    val samples by monitor.samples.collectAsState()
    val pairedAddress by monitor.pairedSensorAddress.collectAsState()
    val connectionError by monitor.connectionError.collectAsState()
    val age by settings.age.collectAsState(initial = -1)

    var showingAgeEntry by remember { mutableStateOf(false) }
    var showingPairing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val hasAge = age in HeartRateZones.validAges
    // age == -1 means the DataStore read hasn't landed yet; don't prompt.
    LaunchedEffect(age) {
        if (age == 0) showingAgeEntry = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_title)) },
                actions = {
                    TextButton(onClick = { showingAgeEntry = true }) {
                        Icon(Icons.Filled.Person, contentDescription = null)
                        Text(
                            if (hasAge) stringResource(R.string.age_with_value, age)
                            else stringResource(R.string.set_age)
                        )
                    }
                    if (pairedAddress != null) {
                        TextButton(onClick = { monitor.unpair() }) {
                            Text(stringResource(R.string.unpair_button))
                        }
                    } else {
                        TextButton(onClick = { showingPairing = true }) {
                            Text(stringResource(R.string.pair_button))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CurrentReading(currentHeartRate)
                if (hasAge) {
                    HeartRateChart(
                        samples = samples,
                        zones = HeartRateZones(age),
                        modifier = Modifier.weight(1f),
                    )
                }
                StatusFooter(state)
            }

            AnimatedVisibility(
                visible = connectionError != null,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
            ) {
                connectionError?.let { error ->
                    ErrorBanner(error) { monitor.retryConnection() }
                }
            }
        }
    }

    if (showingAgeEntry) {
        AgeEntrySheet(
            initialAge = age,
            onSave = { newAge ->
                scope.launch { settings.setAge(newAge) }
                showingAgeEntry = false
            },
            onDismiss = { showingAgeEntry = false },
        )
    }
    if (showingPairing) {
        PairingSheet(monitor = monitor, onDismiss = { showingPairing = false })
    }
}

@Composable
private fun CurrentReading(bpm: Int?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(44.dp),
        )
        Column {
            Text(
                text = bpm?.toString() ?: "--",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.bpm_unit),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatusFooter(state: ConnectionState) {
    val text = when (state) {
        ConnectionState.BluetoothUnavailable -> stringResource(R.string.status_bluetooth_off)
        ConnectionState.Idle -> stringResource(R.string.status_not_paired)
        ConnectionState.Scanning -> stringResource(R.string.status_searching)
        is ConnectionState.Connecting ->
            stringResource(R.string.status_connecting, state.name ?: stringResource(R.string.unnamed_sensor))
        is ConnectionState.Connected ->
            stringResource(R.string.status_connected, state.name ?: stringResource(R.string.unnamed_sensor))
        ConnectionState.Reconnecting -> stringResource(R.string.status_reconnecting)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

package com.curiousbeagle.android.heartchart.ui

import android.content.res.Configuration
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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curiousbeagle.android.heartchart.R
import com.curiousbeagle.android.heartchart.bluetooth.HeartRateMonitor
import com.curiousbeagle.android.heartchart.bluetooth.HeartRateMonitor.ConnectionState
import com.curiousbeagle.android.heartchart.bluetooth.HeartRateMonitor.SensorError
import com.curiousbeagle.android.heartchart.data.HeartRateSample
import com.curiousbeagle.android.heartchart.data.HeartRateZones
import com.curiousbeagle.android.heartchart.data.SettingsStore
import com.curiousbeagle.android.heartchart.ui.theme.HeartChartTheme
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * The port of ContentView: live reading, chart with zone lines, status,
 * banner. Stateful entry point — collects the monitor and settings, owns
 * sheet visibility, and delegates to the stateless overload previews render.
 */
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

    // age == -1 means the DataStore read hasn't landed yet; don't prompt.
    LaunchedEffect(age) {
        if (age == 0) showingAgeEntry = true
    }

    MainScreen(
        state = state,
        currentHeartRate = currentHeartRate,
        samples = samples,
        age = age,
        isPaired = pairedAddress != null,
        connectionError = connectionError,
        onShowAgeEntry = { showingAgeEntry = true },
        onShowPairing = { showingPairing = true },
        onUnpair = monitor::unpair,
        onRetry = monitor::retryConnection,
    )

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

/** Stateless layer: a pure function of its inputs, previewable without a sensor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: ConnectionState,
    currentHeartRate: Int?,
    samples: List<HeartRateSample>,
    age: Int,
    isPaired: Boolean,
    connectionError: SensorError? = null,
    onShowAgeEntry: () -> Unit = {},
    onShowPairing: () -> Unit = {},
    onUnpair: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val hasAge = age in HeartRateZones.validAges

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_title)) },
                actions = {
                    TextButton(onClick = onShowAgeEntry) {
                        Icon(Icons.Filled.Person, contentDescription = null)
                        Text(
                            if (hasAge) {
                                stringResource(
                                    R.string.age_with_value,
                                    HeartRateZones(age).bracketLabel,
                                )
                            } else {
                                stringResource(R.string.set_age)
                            }
                        )
                    }
                    if (isPaired) {
                        TextButton(onClick = onUnpair) {
                            Text(stringResource(R.string.unpair_button))
                        }
                    } else {
                        TextButton(onClick = onShowPairing) {
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
                    ErrorBanner(error, onRetry)
                }
            }
        }
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

// MARK: Previews — mirroring the iOS set: default, small phone, tablet,
// plus dark, XL type, and the states a strapless canvas can't reach live.

internal fun previewChartSamples(): List<HeartRateSample> {
    val now = System.currentTimeMillis()
    return (0 until 120).map { second ->
        HeartRateSample(
            date = now - (120 - second) * 1000L,
            bpm = 80 + (55 * sin(second / 40.0)).toInt() + (-3..3).random(),
        )
    }
}

@Composable
private fun MainScreenPreviewContent(error: SensorError? = null) {
    HeartChartTheme {
        MainScreen(
            state = ConnectionState.Connected("Polar H9"),
            currentHeartRate = 96,
            samples = previewChartSamples(),
            age = 45,
            isPaired = true,
            connectionError = error,
        )
    }
}

@Preview(name = "Phone", showBackground = true)
@Composable
private fun MainScreenPreview() = MainScreenPreviewContent()

@Preview(name = "Small phone", showBackground = true, device = "spec:width=320dp,height=568dp,dpi=320")
@Composable
private fun MainScreenSmallPreview() = MainScreenPreviewContent()

@Preview(name = "Tablet", showBackground = true, device = Devices.TABLET)
@Composable
private fun MainScreenTabletPreview() = MainScreenPreviewContent()

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MainScreenDarkPreview() = MainScreenPreviewContent()

@Preview(name = "XL type", showBackground = true, fontScale = 2f)
@Composable
private fun MainScreenLargeTypePreview() = MainScreenPreviewContent()

@Preview(name = "Connection lost", showBackground = true)
@Composable
private fun MainScreenErrorPreview() =
    MainScreenPreviewContent(error = SensorError.CONNECTION_LOST)

@Preview(name = "No age set", showBackground = true)
@Composable
private fun MainScreenNoAgePreview() {
    HeartChartTheme {
        MainScreen(
            state = ConnectionState.Idle,
            currentHeartRate = null,
            samples = emptyList(),
            age = 0,
            isPaired = false,
        )
    }
}

package com.curiousbeagle.android.heartchart.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.curiousbeagle.android.heartchart.R
import com.curiousbeagle.android.heartchart.bluetooth.HeartRateMonitor

/**
 * Scans for heart rate sensors and pairs the one the user taps — the port
 * of PairingView, plus Android's runtime Bluetooth permissions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingSheet(
    monitor: HeartRateMonitor,
    onDismiss: () -> Unit,
) {
    val state by monitor.state.collectAsState()
    val discovered by monitor.discovered.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) monitor.startScanning()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredBlePermissions())
    }
    DisposableEffect(Unit) {
        onDispose { monitor.stopScanning() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.pairing_title),
                style = MaterialTheme.typography.titleLarge,
            )
            when {
                state == HeartRateMonitor.ConnectionState.BluetoothUnavailable -> {
                    Text(
                        stringResource(R.string.pairing_bt_off_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.pairing_bt_off_message),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                discovered.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            stringResource(R.string.pairing_searching),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.pairing_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> LazyColumn {
                    items(discovered, key = HeartRateMonitor.DiscoveredSensor::address) { sensor ->
                        ListItem(
                            headlineContent = {
                                Text(sensor.name ?: stringResource(R.string.unnamed_sensor))
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    monitor.pair(sensor)
                                    onDismiss()
                                },
                        )
                    }
                }
            }
        }
    }
}

/** The permission set Android requires for BLE scanning at this OS level. */
fun requiredBlePermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

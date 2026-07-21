package com.curiousbeagle.android.heartchart.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.curiousbeagle.android.heartchart.R
import com.curiousbeagle.android.heartchart.bluetooth.HeartRateMonitor
import com.curiousbeagle.android.heartchart.ui.theme.HeartChartTheme

/** Maps sensor errors to the localized copy users see. */
@Composable
fun errorMessage(error: HeartRateMonitor.SensorError): String = stringResource(
    when (error) {
        HeartRateMonitor.SensorError.CONNECTION_FAILED -> R.string.error_connection_failed
        HeartRateMonitor.SensorError.CONNECTION_LOST -> R.string.error_connection_lost
    }
)

/**
 * The single error surface: a red banner with the friendly message and a
 * Retry button that re-attempts the connection. Persists until the sensor
 * reconnects — the same pattern as TheMovieDBSwift.
 */
@Composable
fun ErrorBanner(error: HeartRateMonitor.SensorError, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.error,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onError,
            )
            Text(
                text = errorMessage(error),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onError,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.retry),
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorBannerPreview() {
    HeartChartTheme {
        ErrorBanner(HeartRateMonitor.SensorError.CONNECTION_LOST) {}
    }
}

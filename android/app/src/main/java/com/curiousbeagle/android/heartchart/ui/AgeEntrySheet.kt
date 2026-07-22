package com.curiousbeagle.android.heartchart.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.curiousbeagle.android.heartchart.R
import com.curiousbeagle.android.heartchart.data.HeartRateZones
import com.curiousbeagle.android.heartchart.ui.theme.HeartChartTheme

/**
 * Collects the user's age, which drives every zone line on the chart —
 * the port of AgeEntryView. Shown on first launch and editable any time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeEntrySheet(
    initialAge: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        AgeEntryContent(initialAge = initialAge, onSave = onSave)
    }
}

@Composable
fun AgeEntryContent(
    initialAge: Int,
    onSave: (Int) -> Unit,
) {
    var selectedAge by remember {
        mutableIntStateOf(if (initialAge in HeartRateZones.validAges) initialAge else 45)
    }
    val zones = HeartRateZones(selectedAge)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.age_section),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "$selectedAge",
                style = MaterialTheme.typography.displaySmall,
            )
            Slider(
                value = selectedAge.toFloat(),
                onValueChange = { selectedAge = it.toInt() },
                valueRange = HeartRateZones.validAges.first.toFloat()..HeartRateZones.validAges.last.toFloat(),
            )
        }
        Text(
            stringResource(R.string.age_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            stringResource(R.string.zones_section),
            style = MaterialTheme.typography.titleMedium,
        )
        LabeledRow(
            stringResource(R.string.target_zone_label),
            stringResource(R.string.zone_range_value, zones.targetLow, zones.targetHigh),
        )
        LabeledRow(
            stringResource(R.string.max_heart_rate_label),
            stringResource(R.string.bpm_value, zones.maxHeartRate),
        )

        Button(
            onClick = { onSave(selectedAge) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.save))
        }
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
private fun AgeEntryPreview() {
    HeartChartTheme {
        AgeEntryContent(initialAge = 45, onSave = {})
    }
}

@Preview(name = "Small phone", showBackground = true, device = "spec:width=320dp,height=568dp,dpi=320")
@Composable
private fun AgeEntrySmallPreview() {
    HeartChartTheme {
        AgeEntryContent(initialAge = 45, onSave = {})
    }
}

@Preview(name = "Tablet", showBackground = true, device = Devices.TABLET)
@Composable
private fun AgeEntryTabletPreview() {
    HeartChartTheme {
        AgeEntryContent(initialAge = 55, onSave = {})
    }
}

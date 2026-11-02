package com.shane.urovo.blindinventory.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.shane.urovo.blindinventory.R
import com.shane.urovo.blindinventory.session.SessionPhase
import com.shane.urovo.blindinventory.session.TagRecord
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val corner = RoundedCornerShape(8.dp)

@Composable
fun BlindInventoryScreen(
    state: UiState,
    onToggleScan: () -> Unit,
    onSimulate: () -> Unit,
    onClear: () -> Unit,
    onExport: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    val scanning = state.phase == SessionPhase.SCANNING
    val countDescription = stringResource(R.string.cd_unique_count, state.uniqueCount)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .systemBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Amber),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Header(state)
            CountHero(state, countDescription)
            Text(
                text = stringResource(R.string.continue_hint),
                color = Paper,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (state.initMessage.isNotEmpty()) {
                Text(
                    text = state.initMessage,
                    color = if (state.initOk) Muted else StopRed,
                    fontSize = if (state.initOk) 14.sp else 18.sp,
                    fontWeight = if (state.initOk) FontWeight.Medium else FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            RecentSection(
                state = state,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp),
            )
        }
        Dock(
            state = state,
            scanning = scanning,
            onToggleScan = onToggleScan,
            onSimulate = onSimulate,
            onExport = onExport,
            onClear = { confirmClear = true },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = PanelRaised,
            titleContentColor = Paper,
            textContentColor = Paper,
            title = {
                Text(
                    text = stringResource(R.string.clear_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.clear_body),
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.confirm_clear),
                        color = StopRed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = Amber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
        )
    }
}

@Composable
private fun Header(state: UiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                color = Paper,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(R.string.subtitle),
                color = Amber,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        StatusPill(state.phase)
    }
    Text(
        text = stringResource(R.string.offline_badge),
        color = Muted,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun StatusPill(phase: SessionPhase) {
    val scanning = phase == SessionPhase.SCANNING
    val color = when (phase) {
        SessionPhase.SCANNING -> OkGreen
        SessionPhase.PAUSED -> Amber
        SessionPhase.IDLE -> Muted
    }
    val label = when (phase) {
        SessionPhase.SCANNING -> stringResource(R.string.phase_scanning)
        SessionPhase.PAUSED -> stringResource(R.string.phase_paused)
        SessionPhase.IDLE -> stringResource(R.string.phase_idle)
    }
    val transition = rememberInfiniteTransition(label = "scan-pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "scan-alpha",
    )
    val pulse = if (scanning) pulseAlpha else 1f
    Row(
        modifier = Modifier
            .background(PanelRaised, corner)
            .border(1.dp, color, corner)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .alpha(pulse)
                .background(color, RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CountHero(state: UiState, countDescription: String) {
    Column(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .background(Panel, corner)
            .border(2.dp, Amber, corner)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics { contentDescription = countDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.uniqueCount.toString(),
            color = Amber,
            fontSize = 72.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = 76.sp,
        )
        Text(
            text = stringResource(R.string.unique_label),
            color = Paper,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.total_reads, state.totalReads),
            color = Paper,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun RecentSection(state: UiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.recent),
            color = Amber,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        if (state.uniqueCount > state.recent.size) {
            Text(
                text = stringResource(
                    R.string.recent_cap,
                    state.recent.size,
                    state.uniqueCount,
                ),
                color = Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (state.recent.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.recent, key = { it.epc }) { record ->
                        EpcRow(record)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Panel, corner)
            .border(1.dp, Line, corner)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.empty_title),
            color = Paper,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.empty_body),
            color = Paper,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun EpcRow(record: TagRecord) {
    val seen = timeFormatter.format(record.lastSeen.atZone(ZoneId.systemDefault()))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelRaised, corner)
            .border(1.dp, Line, corner)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.epc,
                color = Paper,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            )
            val rssi = record.lastRssi?.let { stringResource(R.string.rssi_dbm, it) }
            Text(
                text = if (rssi == null) seen else "$seen · $rssi",
                color = Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = stringResource(R.string.count_times, record.count),
            color = Amber,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun Dock(
    state: UiState,
    scanning: Boolean,
    onToggleScan: () -> Unit,
    onSimulate: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.banner?.let { banner ->
            Text(
                text = banner,
                color = Amber,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Button(
            onClick = onToggleScan,
            enabled = state.initOk,
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
            shape = corner,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (scanning) StopRed else StartLime,
                contentColor = if (scanning) Color.White else Color.Black,
                disabledContainerColor = Color(0xFF3A4148),
                disabledContentColor = Muted,
            ),
        ) {
            Text(
                text = stringResource(if (scanning) R.string.stop else R.string.start),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Button(
            onClick = onExport,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = corner,
            colors = ButtonDefaults.buttonColors(
                containerColor = Amber,
                contentColor = Color.Black,
            ),
        ) {
            Text(
                text = stringResource(R.string.export),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onSimulate,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = corner,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PanelRaised,
                    contentColor = Amber,
                ),
            ) {
                Text(
                    text = stringResource(R.string.simulate),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Button(
                onClick = onClear,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = corner,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PanelRaised,
                    contentColor = Paper,
                ),
            ) {
                Text(
                    text = stringResource(R.string.clear),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = stringResource(R.string.export_columns),
            color = Muted,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

package ro.plesiarazvan.profitride.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val Bg = Color(0xFF0E171E)
private val Card = Color(0xFF121E27)
private val Border = Color(0xFF293945)
private val White = Color(0xFFF7F9FA)
private val Gray = Color(0xFFB1BBC4)
private val Muted = Color(0xFF8F9BA5)
private val Green = Color(0xFF35E985)
private val Red = Color(0xFFE76770)

@Composable
fun ProfitRideOverlay(
    state: OverlayUiState,
    displayMode: OverlayDisplayMode,
    maxHeightDp: Float,
    onMinimize: () -> Unit,
    onDetails: () -> Unit,
    onHideDetails: () -> Unit,
    onRestoreCompact: () -> Unit,
    onSettings: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val safeMax = maxHeightDp.coerceAtLeast(96f).dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = safeMax)
            .pointerInput(displayMode) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onDrag(drag.x, drag.y)
                }
            },
        color = Bg,
        shape = shape,
        border = BorderStroke(1.dp, Border),
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        when (displayMode) {
            OverlayDisplayMode.MINIMIZED ->
                Minimized(
                    state = state,
                    onRestore = onRestoreCompact
                )

            OverlayDisplayMode.COMPACT ->
                Compact(
                    state = state,
                    onMinimize = onMinimize,
                    onDetails = onDetails,
                    onSettings = onSettings
                )

            OverlayDisplayMode.EXPANDED ->
                Expanded(
                    state = state,
                    onMinimize = onMinimize,
                    onHideDetails = onHideDetails,
                    onSettings = onSettings,
                    maxHeightDp = maxHeightDp
                )
        }
    }
}

@Composable
private fun Compact(
    state: OverlayUiState,
    onMinimize: () -> Unit,
    onDetails: () -> Unit,
    onSettings: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val narrow = maxWidth < 350.dp

        Column(
            Modifier.padding(
                horizontal = if (narrow) 10.dp else 12.dp,
                vertical = 9.dp
            )
        ) {
            Header(
                state = state,
                onSettings = onSettings,
                onMinimize = onMinimize
            )

            if (state.mode == OverlayMode.WAITING) {
                Waiting()
                return@Column
            }

            Spacer(Modifier.height(5.dp))

            // Payment and PROFIT are visually separated so profit can never look
            // like an amount added to the fare.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactValue(
                    modifier = Modifier.weight(1f),
                    label = "Plată cursă",
                    value = "${money(state.fare)} lei",
                    valueColor = White
                )
                CompactValue(
                    modifier = Modifier.weight(1f),
                    label = "Profit net",
                    value = "${money(state.netProfit)} lei",
                    valueColor = if (state.netProfit >= 0.0) Green else Red,
                    secondary = "după costuri"
                )
            }

            Spacer(Modifier.height(7.dp))

            CompactStats(
                state = state,
                narrow = narrow
            )

            Spacer(Modifier.height(5.dp))

            TextButton(
                onClick = onDetails,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(30.dp),
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 0.dp
                )
            ) {
                Text(
                    "Detalii",
                    color = Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Gray,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun Expanded(
    state: OverlayUiState,
    onMinimize: () -> Unit,
    onHideDetails: () -> Unit,
    onSettings: () -> Unit,
    maxHeightDp: Float
) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeightDp.coerceAtLeast(150f).dp)
    ) {
        val narrow = maxWidth < 350.dp

        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeightDp.coerceAtLeast(150f).dp)
                .padding(
                    horizontal = if (narrow) 10.dp else 12.dp,
                    vertical = 9.dp
                )
        ) {
            Header(
                state = state,
                onSettings = onSettings,
                onMinimize = onMinimize
            )

            if (state.mode == OverlayMode.WAITING) {
                Waiting()
                return@Column
            }

            Spacer(Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactValue(
                    modifier = Modifier.weight(1f),
                    label = "Plată cursă",
                    value = "${money(state.fare)} lei",
                    valueColor = White
                )
                CompactValue(
                    modifier = Modifier.weight(1f),
                    label = "Profit net",
                    value = "${money(state.netProfit)} lei",
                    valueColor = if (state.netProfit >= 0.0) Green else Red,
                    secondary = "după costuri"
                )
            }

            Spacer(Modifier.height(7.dp))

            CompactStats(
                state = state,
                narrow = narrow,
                showTimeBreakdown = !narrow
            )

            Spacer(Modifier.height(7.dp))

            // Only details/costs may scroll. Header + essential figures remain fixed.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                Costs(
                    state = state,
                    stack = maxWidth < 315.dp
                )

                Spacer(Modifier.height(6.dp))

                Advice(state.tip)

                Spacer(Modifier.height(2.dp))
            }

            TextButton(
                onClick = onHideDetails,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(30.dp),
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 0.dp
                )
            ) {
                Text(
                    "Ascunde",
                    color = Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Gray,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun Minimized(
    state: OverlayUiState,
    onRestore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRestore)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "R",
                color = Green,
                fontWeight = FontWeight.Black,
                fontSize = 21.sp
            )
            Spacer(Modifier.width(7.dp))
            Text(
                "Profit",
                color = White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
            Text(
                "Ride",
                color = Green,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }

        if (state.mode == OverlayMode.WAITING) {
            Spacer(Modifier.height(2.dp))
            Text(
                "Aștept cursă...",
                color = Gray,
                fontSize = 11.sp
            )
        } else {
            Spacer(Modifier.height(2.dp))
            Row {
                Text(
                    "Plată: ${money(state.fare)} lei",
                    color = White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "   Profit net: ${money(state.netProfit)} lei",
                    color = if (state.netProfit >= 0.0) Green else Red,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "${state.totalMinutes} min • ${distance(state.totalDistanceKm)} km",
                color = Gray,
                fontSize = 10.5.sp
            )
        }
    }
}

@Composable
private fun Header(
    state: OverlayUiState,
    onSettings: () -> Unit,
    onMinimize: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "R",
                color = Green,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            )
            Spacer(Modifier.width(7.dp))
            Text(
                "Profit",
                color = White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
            Text(
                "Ride",
                color = Green,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Setări",
                    tint = White,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = onMinimize,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Minimizează",
                    tint = White,
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        if (state.mode == OverlayMode.OFFER_READY) {
            val source = listOf(
                state.platform.trim().uppercase(),
                state.paymentType.trim().uppercase()
            ).filter { it.isNotBlank() }.joinToString("  •  ")

            if (source.isNotBlank()) {
                Text(
                    source,
                    color = Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun Waiting() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "ProfitRide activ • Aștept cursă...",
            color = Gray,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CompactValue(
    modifier: Modifier,
    label: String,
    value: String,
    valueColor: Color,
    secondary: String? = null
) {
    Column(
        modifier = modifier
            .background(Card, RoundedCornerShape(13.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = Gray,
            fontSize = 10.5.sp,
            maxLines = 1
        )
        Text(
            value,
            color = valueColor,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!secondary.isNullOrBlank()) {
            Text(
                secondary,
                color = Muted,
                fontSize = 8.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CompactStats(
    state: OverlayUiState,
    narrow: Boolean,
    showTimeBreakdown: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Card,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, Border),
        tonalElevation = 0.dp
    ) {
        if (!narrow) {
            Row(Modifier.fillMaxWidth()) {
                Stat(
                    Modifier.weight(1f),
                    "${state.totalMinutes} min",
                    "Timp total",
                    if (showTimeBreakdown)
                        "${state.pickupMinutes} + ${state.tripMinutes} min"
                    else null
                )
                DividerV()
                Stat(
                    Modifier.weight(1f),
                    "${distance(state.totalDistanceKm)} km",
                    "Distanță totală"
                )
                DividerV()
                Stat(
                    Modifier.weight(1f),
                    "${state.grossPerHour.roundToInt()} lei/h",
                    "Câștig/oră"
                )
                DividerV()
                Stat(
                    Modifier.weight(1f),
                    "${money(state.grossPerKm)} lei/km",
                    "Plată/km"
                )
            }
        } else {
            Column {
                Row(Modifier.fillMaxWidth()) {
                    Stat(
                        Modifier.weight(1f),
                        "${state.totalMinutes} min",
                        "Timp total"
                    )
                    DividerV()
                    Stat(
                        Modifier.weight(1f),
                        "${distance(state.totalDistanceKm)} km",
                        "Distanță totală"
                    )
                }
                DividerH()
                Row(Modifier.fillMaxWidth()) {
                    Stat(
                        Modifier.weight(1f),
                        "${state.grossPerHour.roundToInt()} lei/h",
                        "Câștig/oră"
                    )
                    DividerV()
                    Stat(
                        Modifier.weight(1f),
                        "${money(state.grossPerKm)} lei/km",
                        "Plată/km"
                    )
                }
            }
        }
    }
}

@Composable
private fun Stat(
    modifier: Modifier,
    value: String,
    label: String,
    detail: String? = null
) {
    Column(
        modifier = modifier.padding(
            horizontal = 5.dp,
            vertical = 6.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            color = White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            label,
            color = Gray,
            fontSize = 8.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!detail.isNullOrBlank()) {
            Text(
                detail,
                color = Muted,
                fontSize = 7.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun Costs(
    state: OverlayUiState,
    stack: Boolean
) {
    Text(
        "Costuri estimate cursă",
        color = White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black
    )

    Spacer(Modifier.height(4.dp))

    if (!stack) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Cost("⛽  Combustibil", state.fuelCost)
                Cost("🚗  Uzură + întreținere", state.maintenanceCost)
            }
            Column(Modifier.weight(1f)) {
                Cost("📄  Chirie / Rată", state.rentRateCost)
                Cost("⚙  Alte costuri", state.otherCost)
            }
        }
    } else {
        Cost("⛽  Combustibil", state.fuelCost)
        Cost("🚗  Uzură + întreținere", state.maintenanceCost)
        Cost("📄  Chirie / Rată", state.rentRateCost)
        Cost("⚙  Alte costuri", state.otherCost)
    }

    Spacer(Modifier.height(3.dp))
    DividerH()
    Spacer(Modifier.height(4.dp))

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Total costuri",
            color = White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.weight(1f))
        Text(
            "${money(state.totalCost)} lei",
            color = White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun Cost(
    label: String,
    value: Double
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Gray,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            "${money(value)} lei",
            color = White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun Advice(tip: String) {
    if (tip.isBlank()) return
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("💡", fontSize = 12.sp)
        Spacer(Modifier.width(5.dp))
        Text(
            tip,
            color = Gray,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DividerV() {
    Box(
        Modifier
            .width(1.dp)
            .height(38.dp)
            .background(Border)
    )
}

@Composable
private fun DividerH() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border)
    )
}

private fun money(v: Double): String {
    val n = if (abs(v) < 0.005) 0.0 else v
    return String.format(Locale.US, "%.2f", n).replace('.', ',')
}

private fun distance(v: Double): String =
    String.format(Locale.US, "%.2f", v).replace('.', ',')

package ro.plesiarazvan.profitride.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Locale

private val OverlayBg = Color(0xFF0E171E)
private val SecondaryCard = Color(0xFF121E27)
private val SecondaryCard2 = Color(0xFF101A22)
private val Border = Color(0xFF293945)
private val PrimaryText = Color(0xFFF7F9FA)
private val SecondaryText = Color(0xFFB1BBC4)
private val MutedText = Color(0xFF8F9BA5)
private val BrandGreen = Color(0xFF35E985)
private val ProfitGreen = Color(0xFF50E48E)
private val SoftRed = Color(0xFFE76770)
private val SoftAmber = Color(0xFFF0C35D)
private val SoftOrange = Color(0xFFE8984D)

@Composable
fun ProfitRideOverlay(
    state: OverlayUiState,
    compact: Boolean,
    onToggleCompact: () -> Unit,
    onSettings: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    val shape = RoundedCornerShape(20.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
        color = OverlayBg,
        shape = shape,
        border = BorderStroke(1.dp, Border),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        if (compact) {
            CompactOverlay(
                state = state,
                onExpand = onToggleCompact,
                onDrag = onDrag
            )
        } else {
            FullOverlay(
                state = state,
                onToggleCompact = onToggleCompact,
                onSettings = onSettings,
                onDrag = onDrag
            )
        }
    }
}

@Composable
private fun FullOverlay(
    state: OverlayUiState,
    onToggleCompact: () -> Unit,
    onSettings: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val narrow = maxWidth < 360.dp
        val veryNarrow = maxWidth < 330.dp
        val stackCosts = maxWidth < 315.dp

        Column(
            modifier = Modifier.padding(
                horizontal = if (narrow) 12.dp else 14.dp,
                vertical = if (narrow) 10.dp else 12.dp
            )
        ) {
            Header(
                state = state,
                onToggleCompact = onToggleCompact,
                onSettings = onSettings,
                onDrag = onDrag
            )

            if (state.mode == OverlayMode.WAITING) {
                WaitingState()
            } else {
                Spacer(Modifier.height(8.dp))

                PaymentProfitRow(state, narrow)

                Spacer(Modifier.height(8.dp))

                StatsSection(
                    state = state,
                    narrow = narrow,
                    veryNarrow = veryNarrow
                )

                Spacer(Modifier.height(7.dp))

                CostsSection(
                    state = state,
                    stackCosts = stackCosts
                )

                Spacer(Modifier.height(6.dp))

                AdviceRow(state.tip)
            }
        }
    }
}

@Composable
private fun Header(
    state: OverlayUiState,
    onToggleCompact: () -> Unit,
    onSettings: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "R",
                    color = BrandGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "Profit",
                    color = PrimaryText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Ride",
                    color = BrandGreen,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black
                )
            }

            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Setări ProfitRide",
                    tint = PrimaryText,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onToggleCompact,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Minimizează",
                    tint = PrimaryText,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (state.mode == OverlayMode.OFFER_READY) {
            val source = listOf(
                state.platform.trim().uppercase(),
                state.paymentType.trim().uppercase()
            ).filter { it.isNotBlank() }
                .joinToString("  •  ")

            if (source.isNotBlank()) {
                Text(
                    text = source,
                    color = SecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WaitingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "ProfitRide activ • Aștept cursă...",
            color = SecondaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PaymentProfitRow(
    state: OverlayUiState,
    narrow: Boolean
) {
    val profitColor = profitColor(state)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MainValueCard(
            modifier = Modifier.weight(1f),
            title = "Plată",
            value = "${money(state.fare)} lei",
            subtitle = "NET, taxe incluse",
            valueColor = PrimaryText,
            narrow = narrow
        )

        MainValueCard(
            modifier = Modifier.weight(1f),
            title = "Profit estimat",
            value = "${money(state.netProfit)} lei",
            subtitle = "${money(state.profitPerKm)} lei/km profit",
            valueColor = profitColor,
            narrow = narrow,
            positiveCard = state.netProfit > 0.0
        )
    }
}

@Composable
private fun MainValueCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    narrow: Boolean,
    positiveCard: Boolean = false
) {
    Surface(
        modifier = modifier,
        color = if (positiveCard) Color(0xFF10251A) else SecondaryCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (positiveCard) Color(0xFF1F5A3B) else Border
        ),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (narrow) 10.dp else 12.dp,
                vertical = if (narrow) 9.dp else 10.dp
            )
        ) {
            Text(
                text = title,
                color = if (title.startsWith("Profit")) ProfitGreen else SecondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = value,
                color = valueColor,
                fontSize = if (narrow) 24.sp else 28.sp,
                lineHeight = if (narrow) 27.sp else 31.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )

            Text(
                text = subtitle,
                color = SecondaryText,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatsSection(
    state: OverlayUiState,
    narrow: Boolean,
    veryNarrow: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SecondaryCard2,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, Border),
        tonalElevation = 0.dp
    ) {
        if (!narrow) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCell(
                    modifier = Modifier.weight(1f),
                    value = "${state.totalMinutes} min",
                    label = "Timp total",
                    detail = "${state.pickupMinutes} min + ${state.tripMinutes} min"
                )
                VerticalDivider()
                StatCell(
                    modifier = Modifier.weight(1f),
                    value = "${distance(state.totalDistanceKm)} km",
                    label = "Distanță totală",
                    detail = null
                )
                VerticalDivider()
                StatCell(
                    modifier = Modifier.weight(1f),
                    value = "${state.grossPerHour.roundToInt()} lei/oră",
                    label = "Câștig/oră",
                    detail = null
                )
                VerticalDivider()
                StatCell(
                    modifier = Modifier.weight(1f),
                    value = "${money(state.grossPerKm)} lei/km",
                    label = "Plată/km",
                    detail = null
                )
            }
        } else {
            Column {
                Row(Modifier.fillMaxWidth()) {
                    StatCell(
                        modifier = Modifier.weight(1f),
                        value = "${state.totalMinutes} min",
                        label = "Timp total",
                        detail = if (veryNarrow) null
                        else "${state.pickupMinutes} min + ${state.tripMinutes} min"
                    )
                    VerticalDivider()
                    StatCell(
                        modifier = Modifier.weight(1f),
                        value = "${distance(state.totalDistanceKm)} km",
                        label = "Distanță totală",
                        detail = null
                    )
                }

                HorizontalDivider()

                Row(Modifier.fillMaxWidth()) {
                    StatCell(
                        modifier = Modifier.weight(1f),
                        value = "${state.grossPerHour.roundToInt()} lei/oră",
                        label = "Câștig/oră",
                        detail = null
                    )
                    VerticalDivider()
                    StatCell(
                        modifier = Modifier.weight(1f),
                        value = "${money(state.grossPerKm)} lei/km",
                        label = "Plată/km",
                        detail = null
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCell(
    modifier: Modifier,
    value: String,
    label: String,
    detail: String?
) {
    Column(
        modifier = modifier.padding(
            horizontal = 7.dp,
            vertical = 8.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = PrimaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            color = SecondaryText,
            fontSize = 9.5.sp,
            maxLines = 1
        )
        if (!detail.isNullOrBlank()) {
            Text(
                text = detail,
                color = MutedText,
                fontSize = 8.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CostsSection(
    state: OverlayUiState,
    stackCosts: Boolean
) {
    Column {
        Text(
            text = "Costuri estimate cursă",
            color = PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(5.dp))

        if (!stackCosts) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    CostLine(
                        label = "⛽  Combustibil",
                        value = "${money(state.fuelCost)} lei"
                    )
                    CostLine(
                        label = "🚗  Uzură + întreținere",
                        value = "${money(state.maintenanceCost)} lei"
                    )
                }

                Column(Modifier.weight(1f)) {
                    CostLine(
                        label = "▤  Chirie / Rată",
                        value = "${money(state.rentRateCost)} lei"
                    )
                    CostLine(
                        label = "⚙  Alte costuri",
                        value = "${money(state.otherCost)} lei"
                    )
                }
            }
        } else {
            CostLine(
                label = "⛽  Combustibil",
                value = "${money(state.fuelCost)} lei"
            )
            CostLine(
                label = "🚗  Uzură + întreținere",
                value = "${money(state.maintenanceCost)} lei"
            )
            CostLine(
                label = "▤  Chirie / Rată",
                value = "${money(state.rentRateCost)} lei"
            )
            CostLine(
                label = "⚙  Alte costuri",
                value = "${money(state.otherCost)} lei"
            )
        }

        Spacer(Modifier.height(4.dp))

        HorizontalDivider()

        Spacer(Modifier.height(5.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total costuri",
                color = PrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "${money(state.totalCost)} lei",
                color = PrimaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun CostLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = SecondaryText,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = value,
            color = PrimaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun AdviceRow(tip: String) {
    if (tip.isBlank()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "💡",
            fontSize = 14.sp
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = tip,
            color = SecondaryText,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactOverlay(
    state: OverlayUiState,
    onExpand: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "R",
                color = BrandGreen,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = "Profit",
                color = PrimaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Ride",
                color = BrandGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }

        if (state.mode == OverlayMode.WAITING) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Aștept cursă...",
                color = SecondaryText,
                fontSize = 12.sp
            )
        } else {
            Spacer(Modifier.height(4.dp))

            Text(
                text = "${money(state.fare)} lei • ${signedMoney(state.netProfit)} lei profit",
                color = PrimaryText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${state.totalMinutes} min • ${distance(state.totalDistanceKm)} km",
                color = SecondaryText,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(46.dp)
            .background(Border)
    )
}

@Composable
private fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border)
    )
}

private fun profitColor(state: OverlayUiState): Color {
    if (state.netProfit < -0.005) return SoftRed
    return when (state.recommendation) {
        "ACCEPTA" -> ProfitGreen
        "ACCEPTABIL" -> SoftAmber
        "SLABA" -> SoftOrange
        "RESPINGE" -> SoftRed
        else -> if (state.netProfit > 0.0) ProfitGreen else SoftRed
    }
}

private fun money(value: Double): String {
    val clean = if (abs(value) < 0.005) 0.0 else value
    return String.format(Locale.US, "%.2f", clean).replace('.', ',')
}

private fun signedMoney(value: Double): String {
    val clean = if (abs(value) < 0.005) 0.0 else value
    return if (clean > 0.0) "+${money(clean)}" else money(clean)
}

private fun distance(value: Double): String =
    String.format(Locale.US, "%.2f", value).replace('.', ',')

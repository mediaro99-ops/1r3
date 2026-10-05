package ro.plesiarazvan.profitride.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF071016)
val Card = Color(0xFF0D1820)
val Card2 = Color(0xFF101B23)
val Green = Color(0xFF35F58A)
val Green2 = Color(0xFF28E978)
val Red = Color(0xFFFF5A63)
val Blue = Color(0xFF3EA6FF)
val Muted = Color(0xFFA9B4BC)

private val Scheme = darkColorScheme(
    primary = Green,
    secondary = Blue,
    background = Bg,
    surface = Card,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ProfitRideTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}

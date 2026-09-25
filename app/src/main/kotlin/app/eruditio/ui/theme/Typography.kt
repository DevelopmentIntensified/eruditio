package app.eruditio.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EditorialSerif = FontFamily.Serif
private val UtilitySans = FontFamily.SansSerif

/** Paper direction: serif for reading and titles, sans for controls and metadata. */
val EruditioTypography = Typography(
    displayLarge = TextStyle(fontFamily = EditorialSerif, fontSize = 52.sp, lineHeight = 58.sp),
    displayMedium = TextStyle(fontFamily = EditorialSerif, fontSize = 42.sp, lineHeight = 48.sp),
    displaySmall = TextStyle(fontFamily = EditorialSerif, fontSize = 34.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = EditorialSerif, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = EditorialSerif, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = EditorialSerif, fontSize = 23.sp, lineHeight = 29.sp),
    titleLarge = TextStyle(fontFamily = EditorialSerif, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = UtilitySans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = UtilitySans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = EditorialSerif, fontSize = 18.sp, lineHeight = 30.sp),
    bodyMedium = TextStyle(fontFamily = UtilitySans, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = UtilitySans, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = UtilitySans, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontFamily = UtilitySans, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 1.1.sp),
    labelSmall = TextStyle(fontFamily = UtilitySans, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.8.sp),
)

/** Keep surfaces quiet: the few pills in the app opt into rounded shapes explicitly. */
val EruditioShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
)

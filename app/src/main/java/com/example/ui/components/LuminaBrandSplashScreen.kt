package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.designsystem.LuminaBranding

/** Pixel-clean interim brand screen shown immediately after Android's system splash. */
@Composable
fun LuminaBrandSplashScreen(isDark: Boolean, modifier: Modifier = Modifier) {
    val background = if (isDark) Color.Black else Color.White
    val foreground = if (isDark) Color.White else Color(0xFF101828)
    val secondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF586477)
    Box(
        modifier = modifier.fillMaxSize().background(background).testTag("lumina_brand_splash"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.lumina_brand_mark_color),
                contentDescription = "Symbole Lumina",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(252.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Lumina",
                color = foreground,
                fontSize = 36.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.6).sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = LuminaBranding.SLOGAN,
                modifier = Modifier.widthIn(max = 320.dp),
                color = secondary,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

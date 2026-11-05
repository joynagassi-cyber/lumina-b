package com.example.ui.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R

/** Identité éditoriale partagée par les écrans d'accueil et d'ouverture de session. */
object LuminaBranding {
    const val SLOGAN = "Gérer avec confiance, servir avec transparence."
}

/**
 * Icône de marque intégrée à l'application : son fond suit l'accent du profil,
 * tandis que la marque blanche et la sphère indigo restent cohérentes.
 */
@Composable
fun LuminaBrandMark(
    modifier: Modifier = Modifier,
    contentDescription: String = "Icône Lumina",
    backgroundColor: Color = MaterialTheme.colorScheme.primary
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        lerp(backgroundColor, Color.White, 0.16f),
                        backgroundColor,
                        lerp(backgroundColor, Color.Black, 0.28f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.34f), shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = 180f
                    )
                )
        )
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground_asset),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.38f), CircleShape)
        )
    }
}

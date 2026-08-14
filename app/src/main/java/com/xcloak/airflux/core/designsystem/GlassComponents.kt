package com.xcloak.airflux.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xcloak.airflux.ui.theme.DeepSpace
import com.xcloak.airflux.ui.theme.DeepSpaceLight
import com.xcloak.airflux.ui.theme.ElectricCyan
import com.xcloak.airflux.ui.theme.GlassBorder
import com.xcloak.airflux.ui.theme.GlassWhite
import com.xcloak.airflux.ui.theme.MidnightBlue
import com.xcloak.airflux.ui.theme.TextPrimary

/** Full-screen gradient backdrop with soft ambient glow — use as the root of every screen.
 *  Applies status bar + navigation bar padding so content never draws under system UI. */
@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepSpace, DeepSpaceLight, MidnightBlue)
                )
            )
    ) {
        // Ambient glow blob, top-right
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(ElectricCyan.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(0.85f, 0.05f),
                        radius = 900f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            content()
        }
    }
}

/** Reusable frosted-glass card. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 20,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(cornerRadius.dp))
    ) {
        content()
    }
}

@Composable
fun GradientAppTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            brush = Brush.linearGradient(listOf(TextPrimary, ElectricCyan))
        )
    )
}
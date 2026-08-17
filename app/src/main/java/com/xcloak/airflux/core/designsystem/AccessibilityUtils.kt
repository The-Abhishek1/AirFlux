package com.xcloak.airflux.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*

/** Clickable modifier that announces correctly to TalkBack/screen readers as a button,
 *  with a description for the whole row (title + subtitle combined into one announcement
 *  instead of the reader stepping through each Text separately). */
@Composable
fun Modifier.accessibleClickableCard(
    description: String,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = onClick
        )
        .semantics {
            role = Role.Button
            this.contentDescription = description
        }
}
package com.ardia.design

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max

val LocalBarButtonScale = staticCompositionLocalOf { 1f }

object BarButtons {
    val baseHeight = 40.dp

    fun lift(scale: Float): Dp = baseHeight * (scale.coerceAtLeast(1f) - 1f) / 2

    fun rowHeight(scale: Float): Dp = max(BrowseLayout.hitTarget, baseHeight * scale)

    fun hitTarget(visual: Dp): Dp = max(BrowseLayout.hitTarget, visual)
}

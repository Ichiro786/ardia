package com.ardia.design

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Dp

private val pillShape = RoundedCornerShape(50)

@Composable
fun GlassCircleButton(
    symbol: ImageVector,
    label: String,
    backdrop: FrostedBackdrop?,
    classicStyle: FrostedStyle,
    classicTint: AdaptiveColor,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val dark = LocalArdiaDarkTheme.current
    val liquid = LocalLiquidGlass.current
    val scale = LocalBarButtonScale.current
    val circle = TabsLayout.barCircle * scale
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(BarButtons.hitTarget(circle))
            .combinedClickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                if (!enabled) disabled()
                this.onClick(label = null) { onClick(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .springPress(interaction)
                .size(circle)
                .glassShadow(CircleShape, liquid, dark)
                .frosted(backdrop, CircleShape, if (liquid) Glass.control else classicStyle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(symbol, null, Modifier.size(TabsLayout.barSymbol * scale), tint = if (liquid) SystemPalette.label.resolve(dark) else classicTint.resolve(dark))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassPlusButton(
    label: String,
    hint: String,
    backdrop: FrostedBackdrop?,
    classicStyle: FrostedStyle,
    iconSize: Dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    menu: @Composable () -> Unit,
) {
    val dark = LocalArdiaDarkTheme.current
    val liquid = LocalLiquidGlass.current
    val scale = LocalBarButtonScale.current
    val interaction = remember { MutableInteractionSource() }
    Box(Modifier.heightIn(min = BarButtons.rowHeight(scale)), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .springPress(interaction)
                .size(TabsLayout.barPlus * scale)
                .glassShadow(pillShape, liquid, dark)
                .frosted(if (liquid) null else backdrop, pillShape, if (liquid) Glass.activeStyle else classicStyle)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onLongClick = onLongClick,
                    onClick = onClick,
                )
                .clearAndSetSemantics {
                    contentDescription = label
                    role = Role.Button
                    if (!enabled) disabled()
                    this.onClick(label = null) { onClick(); true }
                    this.onLongClick(hint) { onLongClick(); true }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(ArdiaSymbols.plus, null, Modifier.size(iconSize * scale), tint = (if (liquid) Glass.onActive else SystemPalette.label).resolve(dark))
        }
        menu()
    }
}

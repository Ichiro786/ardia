package com.ardia.design

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

private val menuShape = RoundedCornerShape(22.dp)
private val menuRowShape = RoundedCornerShape(14.dp)
private val menuShadowRoom = 16.dp
private val menuGap = 6.dp
private val menuEdge = 10.dp
private const val MENU_FOLDED_SCALE = 0.8f

@Composable
fun GlassMenu(expanded: Boolean, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = expanded
    if (!visibility.currentState && !visibility.targetState) return
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(TransformOrigin(0.5f, 0f)) }
    val position = remember(density) { GlassMenuPosition(density) { origin = it } }
    Popup(popupPositionProvider = position, onDismissRequest = onDismissRequest, properties = PopupProperties(focusable = true)) {
        val dark = LocalArdiaDarkTheme.current
        val liquid = LocalLiquidGlass.current
        val reduceMotion = LocalReduceMotion.current
        val transition = rememberTransition(visibility, label = "glassMenu")
        val scale by transition.animateFloat(
            transitionSpec = {
                when {
                    reduceMotion -> Motion.reducedFade.spec()
                    targetState -> Motion.glassPop.spec()
                    else -> Motion.glassFold.spec()
                }
            },
            label = "glassMenuScale",
        ) { if (it || reduceMotion) 1f else MENU_FOLDED_SCALE }
        val alpha by transition.animateFloat(
            transitionSpec = { if (targetState) Motion.surfaceFade.spec() else Motion.glassFold.spec() },
            label = "glassMenuAlpha",
        ) { if (it) 1f else 0f }
        Box(Modifier.padding(menuShadowRoom)) {
            Column(
                modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha.coerceIn(0f, 1f)
                        transformOrigin = origin
                    }
                    .glassShadow(menuShape, liquid, dark)
                    .frosted(null, menuShape, Glass.panel())
                    .width(IntrinsicSize.Max)
                    .widthIn(min = 200.dp, max = 290.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                content = content,
            )
        }
    }
}

@Composable
fun GlassMenuItem(
    label: String,
    icon: ImageVector?,
    destructive: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    onClick: () -> Unit,
) {
    val dark = LocalArdiaDarkTheme.current
    val base = (if (destructive) SystemPalette.red else SystemPalette.label).resolve(dark)
    val color = if (enabled) base else base.copy(alpha = 0.35f)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val highlight by animateColorAsState(
        if (pressed) Glass.pressedRow.resolve(dark) else Color.Transparent,
        Motion.surfaceFade.spec(),
        label = "glassMenuRow",
    )
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .springPress(interaction, Glass.PRESSED_ROW_SCALE)
            .clip(menuRowShape)
            .background(highlight)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .then(if (selected != null) Modifier.semantics { this.selected = selected } else Modifier)
            .heightIn(min = 46.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = ArdiaType.body, color = color, modifier = Modifier.weight(1f), maxLines = 2)
        if (icon != null) {
            Spacer(Modifier.width(14.dp))
            Icon(icon, null, Modifier.size(18.dp), tint = color)
        }
    }
}

@Composable
fun GlassMenuHeading(text: String) {
    Text(
        text,
        style = ArdiaType.footnote,
        color = SystemPalette.secondaryLabel.current,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
fun GlassMenuDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 5.dp)
            .height(0.5.dp)
            .background(Glass.separator.current),
    )
}

private class GlassMenuPosition(private val density: Density, private val onOrigin: (TransformOrigin) -> Unit) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val room = with(density) { menuShadowRoom.roundToPx() }
        val gap = with(density) { menuGap.roundToPx() }
        val edge = with(density) { menuEdge.roundToPx() }
        val width = popupContentSize.width - room * 2
        val height = popupContentSize.height - room * 2
        val preferredX = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.left else anchorBounds.right - width
        val x = preferredX.coerceIn(edge, (windowSize.width - width - edge).coerceAtLeast(edge))
        val below = anchorBounds.bottom + gap
        val above = anchorBounds.top - gap - height
        val fitsBelow = below + height <= windowSize.height - edge
        val opensDown = fitsBelow || above < edge && windowSize.height - anchorBounds.bottom >= anchorBounds.top
        val y = (if (opensDown) below else above).coerceIn(edge, (windowSize.height - height - edge).coerceAtLeast(edge))
        val pivotX = if (width > 0) ((anchorBounds.center.x - x).toFloat() / width).coerceIn(0f, 1f) else 0.5f
        onOrigin(TransformOrigin(pivotX, if (opensDown) 0f else 1f))
        return IntOffset(x - room, y - room)
    }
}

@Composable
fun GlassAlert(
    title: String,
    message: String?,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    destructive: Boolean = true,
    confirmModifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val dark = LocalArdiaDarkTheme.current
        val liquid = LocalLiquidGlass.current
        val reduceMotion = LocalReduceMotion.current
        val view = LocalView.current
        val blurPx = with(LocalDensity.current) { 28.dp.roundToPx() }
        DisposableEffect(view, liquid) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null && liquid && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) blurBehind(window, blurPx)
            onDispose {}
        }
        val appear = remember { MutableTransitionState(false) }
        LaunchedEffect(Unit) { appear.targetState = true }
        val transition = rememberTransition(appear, label = "glassAlert")
        val scale by transition.animateFloat(
            transitionSpec = { if (reduceMotion) Motion.reducedFade.spec() else Motion.glassPop.spec() },
            label = "glassAlertScale",
        ) { if (it || reduceMotion) 1f else 0.88f }
        val alpha by transition.animateFloat(transitionSpec = { Motion.surfaceFade.spec() }, label = "glassAlertAlpha") { if (it) 1f else 0f }
        val shape = RoundedCornerShape(30.dp)
        val confirmColor = (if (destructive) SystemPalette.red else Palette.formAccent).resolve(dark)
        Column(
            Modifier
                .padding(28.dp)
                .widthIn(max = 320.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha.coerceIn(0f, 1f)
                }
                .glassShadow(shape, liquid, dark)
                .frosted(null, shape, Glass.panel())
                .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = ArdiaType.headline, color = SystemPalette.label.resolve(dark), textAlign = TextAlign.Center)
            if (message != null) {
                Text(message, style = ArdiaType.subheadline, color = SystemPalette.secondaryLabel.resolve(dark), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassPillButton(cancelLabel, SystemPalette.label.resolve(dark), Modifier.weight(1f), onDismissRequest)
                GlassPillButton(confirmLabel, confirmColor, Modifier.weight(1f).then(confirmModifier), onConfirm)
            }
        }
    }
}

@Composable
fun GlassPillButton(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .springPress(interaction)
            .clip(RoundedCornerShape(50))
            .background(Glass.chip.current)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = ArdiaType.body.semibold, color = color, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun blurBehind(window: Window, radius: Int) {
    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    window.attributes = window.attributes.apply {
        blurBehindRadius = radius
        dimAmount = 0.3f
    }
}

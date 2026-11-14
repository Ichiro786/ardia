package com.ardia.design

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

val LocalLiquidGlass = staticCompositionLocalOf { true }

object Glass {
    val rim = GlassRim(
        highlight = adaptive(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.34f)),
        lowlight = adaptive(Color.Black.copy(alpha = 0.05f), Color.White.copy(alpha = 0.07f)),
        sheen = adaptive(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.06f)),
    )

    val groupRim = GlassRim(
        highlight = adaptive(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.16f)),
        lowlight = adaptive(Color.Black.copy(alpha = 0.04f), Color.White.copy(alpha = 0.03f)),
        sheen = adaptive(Color.Transparent, Color.Transparent),
        width = 0.75.dp,
    )

    val sheetRim = GlassRim(
        highlight = adaptive(Color.White, Color.White.copy(alpha = 0.3f)),
        lowlight = adaptive(Color.Transparent, Color.Transparent),
        sheen = adaptive(Color.Transparent, Color.Transparent),
        width = 1.dp,
    )

    val control = FrostedStyle(
        blurRadius = 28.dp,
        saturation = 1.9f,
        tint = adaptive(ardiaColor(0xFFFFFF, 0.4f), ardiaColor(0x2A2A2E, 0.44f)),
        fallback = adaptive(ardiaColor(0xF2F2F5, 0.93f), ardiaColor(0x2A2A2E, 0.93f)),
        rim = rim,
    )

    val panel = FrostedStyle(
        blurRadius = 36.dp,
        saturation = 1.9f,
        tint = adaptive(ardiaColor(0xFFFFFF, 0.62f), ardiaColor(0x1F1F22, 0.62f)),
        fallback = adaptive(ardiaColor(0xF6F6F8, 0.97f), ardiaColor(0x242427, 0.97f)),
        rim = rim,
    )

    val overPage = control.copy(
        fallback = adaptive(ardiaColor(0xF4F4F7, 0.8f), ardiaColor(0x2A2A2E, 0.82f)),
    )

    val classicPanel = FrostedStyle.sheet.copy(edge = FrostedStyle.glass.edge)

    val active = adaptive(ardiaColor(0x1C1C1E), ardiaColor(0xECECEF))

    val activeStyle = FrostedStyle(
        blurRadius = 0.dp,
        saturation = 1f,
        tint = active,
        fallback = active,
        rim = GlassRim(
            highlight = adaptive(Color.White.copy(alpha = 0.28f), Color.White),
            lowlight = adaptive(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.12f)),
            sheen = adaptive(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.35f)),
        ),
    )

    val onActive = adaptive(Color.White, ardiaColor(0x111113))
    val pressedRow = adaptive(Color.Black.copy(alpha = 0.07f), Color.White.copy(alpha = 0.1f))
    val separator = adaptive(Color.Black.copy(alpha = 0.08f), Color.White.copy(alpha = 0.1f))
    val chip = adaptive(Color.Black.copy(alpha = 0.06f), Color.White.copy(alpha = 0.12f))
    val shadow = adaptive(Color.Black.copy(alpha = 0.22f), Color.Black.copy(alpha = 0.6f))
    val shadowElevation = 14.dp
    const val PRESSED_SCALE = 0.86f
    const val PRESSED_ROW_SCALE = 0.97f

    val isLiquid: Boolean
        @Composable @ReadOnlyComposable get() = LocalLiquidGlass.current

    @Composable
    @ReadOnlyComposable
    fun control(classic: FrostedStyle): FrostedStyle = if (LocalLiquidGlass.current) control else classic

    @Composable
    @ReadOnlyComposable
    fun panel(): FrostedStyle = if (LocalLiquidGlass.current) panel else classicPanel
}

fun Modifier.glassShadow(shape: Shape, enabled: Boolean, dark: Boolean): Modifier {
    if (!enabled) return this
    val color = Glass.shadow.resolve(dark)
    return shadow(Glass.shadowElevation, shape, clip = false, ambientColor = color, spotColor = color)
}

fun Modifier.glassRim(shape: Shape, enabled: Boolean, dark: Boolean, rim: GlassRim = Glass.rim): Modifier {
    if (!enabled) return this
    return drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val highlight = rim.highlight.resolve(dark)
        val stroke = Brush.verticalGradient(
            0f to highlight,
            0.3f to highlight.copy(alpha = highlight.alpha * 0.15f),
            1f to rim.lowlight.resolve(dark),
        )
        val width = rim.width.toPx()
        onDrawWithContent {
            drawContent()
            drawOutline(outline, stroke, style = Stroke(width))
        }
    }
}

fun Modifier.springPress(interactionSource: InteractionSource, pressedScale: Float = Glass.PRESSED_SCALE): Modifier =
    this then SpringPressElement(interactionSource, pressedScale)

private data class SpringPressElement(val interactionSource: InteractionSource, val pressedScale: Float) :
    ModifierNodeElement<SpringPressNode>() {
    override fun create(): SpringPressNode = SpringPressNode(interactionSource, pressedScale)

    override fun update(node: SpringPressNode) = node.update(interactionSource, pressedScale)

    override fun InspectorInfo.inspectableProperties() {
        name = "springPress"
        properties["pressedScale"] = pressedScale
    }
}

private class SpringPressNode(private var interactionSource: InteractionSource, private var pressedScale: Float) :
    Modifier.Node(),
    LayoutModifierNode,
    CompositionLocalConsumerModifierNode {

    private val scale = Animatable(1f)
    private var watcher: Job? = null

    override fun onAttach() = watch()

    override fun onDetach() {
        watcher = null
    }

    fun update(source: InteractionSource, pressed: Float) {
        pressedScale = pressed
        if (source == interactionSource) return
        interactionSource = source
        if (isAttached) watch()
    }

    private fun watch() {
        watcher?.cancel()
        watcher = coroutineScope.launch {
            var held = 0
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        held += 1
                        launch { pressIn() }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        held = (held - 1).coerceAtLeast(0)
                        if (held == 0) launch { release() }
                    }
                }
            }
        }
    }

    private fun stillMotion(): Boolean = currentValueOf(LocalReduceMotion)

    private suspend fun pressIn() {
        if (stillMotion()) return
        scale.animateTo(pressedScale, Motion.glassPressIn.spec())
    }

    private suspend fun release() {
        if (stillMotion()) {
            scale.snapTo(1f)
            return
        }
        val depth = 1f - pressedScale
        if (scale.value > 1f - depth * 0.6f) scale.animateTo(pressedScale + depth * 0.25f, Motion.glassPressIn.spec())
        scale.animateTo(1f, Motion.glassRelease.spec())
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                val value = scale.value
                scaleX = value
                scaleY = value
            }
        }
    }
}

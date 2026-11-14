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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

private val dialogShape = RoundedCornerShape(30.dp)
private val fieldShape = RoundedCornerShape(14.dp)
private val choiceShape = RoundedCornerShape(16.dp)
private const val DIALOG_FOLDED_SCALE = 0.88f

@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = dismissOnOutside),
    ) {
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
        val transition = rememberTransition(appear, label = "glassDialog")
        val scale by transition.animateFloat(
            transitionSpec = { if (reduceMotion) Motion.reducedFade.spec() else Motion.glassPop.spec() },
            label = "glassDialogScale",
        ) { if (it || reduceMotion) 1f else DIALOG_FOLDED_SCALE }
        val alpha by transition.animateFloat(transitionSpec = { Motion.surfaceFade.spec() }, label = "glassDialogAlpha") { if (it) 1f else 0f }
        Column(
            Modifier
                .padding(28.dp)
                .widthIn(max = 320.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha.coerceIn(0f, 1f)
                }
                .glassShadow(dialogShape, liquid, dark)
                .frosted(null, dialogShape, Glass.panel())
                .then(modifier)
                .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
fun GlassDialogText(title: String, message: String?) {
    val dark = LocalArdiaDarkTheme.current
    Text(title, style = ArdiaType.headline, color = SystemPalette.label.resolve(dark), textAlign = TextAlign.Center)
    if (message != null) {
        Text(message, style = ArdiaType.subheadline, color = SystemPalette.secondaryLabel.resolve(dark), textAlign = TextAlign.Center)
    }
}

@Composable
fun GlassDialogActions(
    confirmLabel: String,
    onConfirm: () -> Unit,
    cancelLabel: String?,
    onCancel: () -> Unit,
    destructive: Boolean = false,
    confirmModifier: Modifier = Modifier,
) {
    val dark = LocalArdiaDarkTheme.current
    val confirmColor = (if (destructive) SystemPalette.red else Palette.formAccent).resolve(dark)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (cancelLabel != null) GlassPillButton(cancelLabel, SystemPalette.label.resolve(dark), Modifier.weight(1f), onCancel)
        GlassPillButton(confirmLabel, confirmColor, Modifier.weight(1f).then(confirmModifier), onConfirm)
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
    GlassDialog(onDismissRequest) {
        GlassDialogText(title, message)
        GlassDialogActions(confirmLabel, onConfirm, cancelLabel, onDismissRequest, destructive, confirmModifier)
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

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val dark = LocalArdiaDarkTheme.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val ring by animateColorAsState(
        if (focused) Palette.formAccent.resolve(dark) else Color.Transparent,
        Motion.surfaceFade.spec(),
        label = "glassFieldRing",
    )
    val textColor = SystemPalette.label.resolve(dark)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = ArdiaType.body.copy(color = textColor),
        cursorBrush = SolidColor(Palette.formAccent.resolve(dark)),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .clip(fieldShape)
                    .background(Glass.chip.resolve(dark))
                    .border(1.5.dp, ring, fieldShape)
                    .heightIn(min = 46.dp)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, style = ArdiaType.body, color = SystemPalette.tertiaryLabel.resolve(dark), maxLines = 1)
                }
                inner()
            }
        },
    )
}

@Composable
fun GlassChoiceRow(title: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val dark = LocalArdiaDarkTheme.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill by animateColorAsState(
        if (pressed) Glass.pressedRow.resolve(dark) else Glass.chip.resolve(dark),
        Motion.surfaceFade.spec(),
        label = "glassChoice",
    )
    val color = SystemPalette.label.resolve(dark)
    Row(
        modifier
            .fillMaxWidth()
            .springPress(interaction, Glass.PRESSED_ROW_SCALE)
            .clip(choiceShape)
            .background(fill)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .heightIn(min = 50.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = Palette.formAccent.resolve(dark))
        Spacer(Modifier.width(12.dp))
        Text(title, style = ArdiaType.body, color = color, modifier = Modifier.weight(1f), maxLines = 2)
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

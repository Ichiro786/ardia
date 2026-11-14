package com.ardia.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val tileShape = RoundedCornerShape(16.dp)

@Composable
fun GlassSpaceSwitcher(
    spaces: List<String>,
    selected: Int,
    hint: String,
    backdrop: FrostedBackdrop?,
    choose: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = spaces.getOrNull(selected) ?: return
    val dark = LocalArdiaDarkTheme.current
    val ink = SystemPalette.label.resolve(dark)
    var menu by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    Box(modifier.heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .springPress(interaction, Glass.PRESSED_ROW_SCALE)
                .height(32.dp)
                .frosted(backdrop, CircleShape, Glass.control)
                .clickable(interactionSource = interaction, indication = null) { menu = true }
                .clearAndSetSemantics {
                    contentDescription = name
                    role = Role.Button
                    onClick(hint) { menu = true; true }
                }
                .padding(start = 6.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(20.dp).background(Glass.chip.resolve(dark), CircleShape), contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), style = ArdiaType.caption.semibold, color = ink)
            }
            Text(name, style = ArdiaType.subheadline.semibold, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(ArdiaSymbols.chevronDown, null, Modifier.size(10.dp), tint = SystemPalette.secondaryLabel.resolve(dark))
        }
        GlassMenu(expanded = menu, onDismissRequest = { menu = false }) {
            spaces.forEachIndexed { index, space ->
                GlassMenuItem(space, null, selected = index == selected) {
                    menu = false
                    if (index != selected) choose(index)
                }
            }
        }
    }
}

@Composable
fun GlassTile(
    label: String,
    backdrop: FrostedBackdrop?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = LocalArdiaDarkTheme.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .springPress(interaction)
            .height(52.dp)
            .glassShadow(tileShape, true, dark)
            .frosted(backdrop, tileShape, Glass.control)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                this.onClick(label = null) { onClick(); true }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    clearLabel: String,
    backdrop: FrostedBackdrop?,
    modifier: Modifier = Modifier,
) {
    val dark = LocalArdiaDarkTheme.current
    val ink = SystemPalette.label.resolve(dark)
    val secondary = SystemPalette.secondaryLabel.resolve(dark)
    val focus = LocalFocusManager.current
    Row(
        modifier
            .height(TabsLayout.barCircle * LocalBarButtonScale.current)
            .glassShadow(CircleShape, true, dark)
            .frosted(backdrop, CircleShape, Glass.control)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(ArdiaSymbols.magnifyingglass, null, Modifier.size(16.dp), tint = secondary)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, style = ArdiaType.subheadline, color = secondary, maxLines = 1, modifier = Modifier.clearAndSetSemantics {})
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = ArdiaType.subheadline.copy(color = ink),
                cursorBrush = SolidColor(ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
            )
        }
        if (value.isNotEmpty()) {
            Box(
                Modifier
                    .size(32.dp)
                    .clickable(onClick = { onValueChange("") })
                    .clearAndSetSemantics {
                        contentDescription = clearLabel
                        role = Role.Button
                        onClick { onValueChange(""); true }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(ArdiaSymbols.xmarkCircleFill, null, Modifier.size(16.dp), tint = secondary)
            }
        }
    }
}

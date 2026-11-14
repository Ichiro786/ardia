package com.ardia.browsing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.ardia.appmodel.AppModel
import com.ardia.design.ArdiaSymbols
import com.ardia.design.ArdiaType
import com.ardia.design.BrowsePalette
import com.ardia.design.FaviconImage
import com.ardia.design.FrostedBackdrop
import com.ardia.design.Glass
import com.ardia.design.LocalBarButtonScale
import com.ardia.design.Motion
import com.ardia.design.frosted
import com.ardia.store.BrowserTab
import com.ardia.store.BrowserUrl
import kotlin.math.min
import kotlin.math.roundToInt

internal object GlassBarLayout {
    val piece = 46.dp
    val sideInset = 14.dp
    val gap = 8.dp
    val addressPadding = 14.dp
    val moreSize = 40.dp
    val pillHeight = 30.dp
    val pillPadding = 12.dp
    val pillLift = 6.dp
    val favicon = 16.dp
    const val PILL_TEXT_SCALE = 0.84f
    const val SIDE_SHRUNK_SCALE = 0.55f

    fun pillCenterFromBottom(navigationInset: Dp): Dp = max(navigationInset, 8.dp) + pillLift + pillHeight / 2
}

private val addressShape = RoundedCornerShape(50)
private const val HIDDEN_ALPHA = 0.02f

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GlassPageBar(
    model: AppModel,
    tab: BrowserTab?,
    backdrop: FrostedBackdrop?,
    shrunk: Boolean,
    navigationBottom: Dp,
    reduceMotion: Boolean,
    onTabs: () -> Unit,
    onStep: (Int) -> Unit,
    onSelectTab: (String) -> Unit,
    onExpand: () -> Unit,
    addressDrag: Modifier,
    modifier: Modifier = Modifier,
) {
    val scale = LocalBarButtonScale.current
    val shrink by animateFloatAsState(
        targetValue = if (shrunk) 1f else 0f,
        animationSpec = if (reduceMotion) tween(200) else Motion.pillShrink.spec(),
        label = "pillShrink",
    )
    val order = model.deckOrder
    var showsRecent by remember { mutableStateOf(false) }
    val tabsLabel = stringResource(R.string.browsing_tabs)
    val tabCount = pluralStringResource(R.plurals.browsing_tab_count, order.size, order.size)
    val previous = stringResource(R.string.browsing_previous_tab)
    val next = stringResource(R.string.browsing_next_tab)
    val hint = stringResource(R.string.browsing_tabs_hint)
    val addressLabel = stringResource(R.string.browsing_address)
    val showBarLabel = stringResource(R.string.browsing_show_bar)
    val moreLabel = stringResource(R.string.browsing_page_menu)
    val plusLabel = stringResource(R.string.browsing_new_search)
    val host = tab?.let { BrowserUrl.host(it.url) ?: it.url }.orEmpty()
    val isSearchPresented = model.browse.search != null
    val pieceSize = GlassBarLayout.piece * scale
    val centerFromBottom = BarGeometry.centerFromBottom(navigationBottom, scale)
    val pillCenterFromBottom = GlassBarLayout.pillCenterFromBottom(navigationBottom)

    Layout(
        modifier = modifier,
        content = {
            Box(Modifier.size(pieceSize), contentAlignment = Alignment.Center) {
                val interaction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .testTag(BrowsingTags.TABS)
                        .barPress(interaction)
                        .size(pieceSize)
                        .frosted(backdrop, CircleShape, Glass.control)
                        .combinedClickable(
                            interactionSource = interaction,
                            indication = null,
                            enabled = !shrunk,
                            onClickLabel = tabsLabel,
                            onLongClickLabel = hint,
                            onLongClick = { showsRecent = true },
                            onClick = onTabs,
                        )
                        .semantics {
                            contentDescription = tabsLabel
                            stateDescription = tabCount
                            role = Role.Button
                            customActions = listOf(
                                CustomAccessibilityAction(previous) { onStep(-1); true },
                                CustomAccessibilityAction(next) { onStep(1); true },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    TabStackGlyph(model, order.takeLast(1))
                }
                RecentTabsMenu(model, order, showsRecent, onDismiss = { showsRecent = false }, onSelectTab = onSelectTab, onCloseTab = {
                    model.tabs.selectedTabId?.let(model::closeTab)
                })
            }
            val addressInteraction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .testTag(BrowsingTags.ADDRESS)
                    .then(addressDrag)
                    .barPress(addressInteraction)
                    .frosted(backdrop, addressShape, Glass.control)
                    .clickable(
                        interactionSource = addressInteraction,
                        indication = null,
                        onClickLabel = if (shrunk) showBarLabel else addressLabel,
                        role = Role.Button,
                    ) {
                        if (shrunk) onExpand() else tab?.let(model::editQuery)
                    }
                    .semantics { contentDescription = host.ifEmpty { addressLabel } },
            )
            Row(Modifier.clearAndSetSemantics {}, verticalAlignment = Alignment.CenterVertically) {
                if (tab != null) FaviconImage(iconUrl = tab.faviconUrl, pageUrl = tab.url, size = GlassBarLayout.favicon)
                Spacer(Modifier.width(7.dp))
                Text(
                    host,
                    style = ArdiaType.subheadline,
                    color = BrowsePalette.foregroundPrimary.current,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier
                    .testTag(BrowsingTags.MORE)
                    .size(GlassBarLayout.moreSize)
                    .clickable(enabled = !shrunk, role = Role.Button, onClickLabel = moreLabel) { model.browse.isToolbarPresented = true }
                    .semantics { contentDescription = moreLabel },
                contentAlignment = Alignment.Center,
            ) {
                Icon(ArdiaSymbols.ellipsis, null, Modifier.size(20.dp), tint = BrowsePalette.foregroundPrimary.current)
            }
            Box(Modifier.size(pieceSize), contentAlignment = Alignment.Center) {
                if (!isSearchPresented) {
                    val interaction = remember { MutableInteractionSource() }
                    Box(
                        Modifier
                            .testTag(BrowsingTags.PLUS)
                            .barPress(interaction)
                            .size(pieceSize)
                            .frosted(backdrop, CircleShape, Glass.control)
                            .clickable(interactionSource = interaction, indication = null, enabled = !shrunk, role = Role.Button, onClick = model::newTab)
                            .semantics { contentDescription = plusLabel },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(ArdiaSymbols.plus, null, Modifier.size(18.dp * scale), tint = BrowsePalette.foregroundPrimary.current)
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val s = shrink
        val visible = (1f - s).coerceIn(0f, 1f)
        val piece = pieceSize.roundToPx()
        val inset = GlassBarLayout.sideInset.roundToPx()
        val gap = GlassBarLayout.gap.roundToPx()
        val pad = GlassBarLayout.addressPadding.roundToPx()
        val more = GlassBarLayout.moreSize.roundToPx()
        val full = (width - 2 * inset - 2 * piece - 2 * gap).coerceAtLeast(0)
        val groupMax = (full - 2 * pad - more).coerceAtLeast(0)
        val group = measurables[2].measure(Constraints(maxWidth = groupMax))
        val textScale = lerp(1f, GlassBarLayout.PILL_TEXT_SCALE, s)
        val pillWidth = min(full, (group.width * GlassBarLayout.PILL_TEXT_SCALE).roundToInt() + 2 * GlassBarLayout.pillPadding.roundToPx())
        val addressWidth = lerp(full.toFloat(), pillWidth.toFloat(), s).roundToInt().coerceAtLeast(0)
        val addressHeight = lerp(piece.toFloat(), GlassBarLayout.pillHeight.toPx(), s).roundToInt().coerceAtLeast(0)
        val centerY = height - lerp(centerFromBottom.toPx(), pillCenterFromBottom.toPx(), s)
        val tabs = measurables[0].measure(Constraints.fixed(piece, piece))
        val address = measurables[1].measure(Constraints.fixed(addressWidth, addressHeight))
        val moreButton = measurables[3].measure(Constraints.fixed(more, more))
        val plus = measurables[4].measure(Constraints.fixed(piece, piece))
        val addressX = (width - addressWidth) / 2
        val sideScale = lerp(1f, GlassBarLayout.SIDE_SHRUNK_SCALE, s)
        layout(width, height) {
            val showsSides = visible > HIDDEN_ALPHA
            if (showsSides) {
                tabs.placeRelativeWithLayer(inset, (centerY - piece / 2f).roundToInt()) {
                    alpha = visible
                    scaleX = sideScale
                    scaleY = sideScale
                }
                plus.placeRelativeWithLayer(width - inset - piece, (centerY - piece / 2f).roundToInt()) {
                    alpha = visible
                    scaleX = sideScale
                    scaleY = sideScale
                }
            }
            address.placeRelative(addressX, (centerY - addressHeight / 2f).roundToInt())
            val expandedX = addressX + pad
            val shrunkX = addressX + ((addressWidth - group.width * textScale) / 2f).roundToInt()
            group.placeRelativeWithLayer(lerp(expandedX.toFloat(), shrunkX.toFloat(), s).roundToInt(), (centerY - group.height / 2f).roundToInt()) {
                transformOrigin = TransformOrigin(0f, 0.5f)
                scaleX = textScale
                scaleY = textScale
            }
            if (showsSides) {
                moreButton.placeRelativeWithLayer(addressX + addressWidth - more - pad / 4, (centerY - more / 2f).roundToInt()) {
                    alpha = visible
                }
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

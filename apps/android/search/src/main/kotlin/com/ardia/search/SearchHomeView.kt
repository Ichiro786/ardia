package com.ardia.search

import androidx.compose.animation.Crossfade
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.ardia.appmodel.AppModel
import com.ardia.design.ArdiaSymbols
import com.ardia.design.BarButtons
import com.ardia.design.BrowseLayout
import com.ardia.design.BrowsePalette
import com.ardia.design.FidgetLogo
import com.ardia.design.FrostedBackdrop
import com.ardia.design.FrostedStyle
import com.ardia.design.FaviconImage
import com.ardia.design.Glass
import com.ardia.design.GlassCircleButton
import com.ardia.design.GlassMenu
import com.ardia.design.GlassMenuItem
import com.ardia.design.GlassPlusButton
import com.ardia.design.GlassSpaceSwitcher
import com.ardia.design.GlassTile
import com.ardia.design.LocalArdiaDarkTheme
import com.ardia.design.LocalBarButtonScale
import com.ardia.design.LocalReduceMotion
import com.ardia.design.Motion
import com.ardia.design.ProfileBackdrop
import com.ardia.design.ProfileLook
import com.ardia.design.TabsDeckGeometry
import com.ardia.design.TabsLayout
import com.ardia.design.frostedBackdropSource
import com.ardia.design.rememberFrostedBackdrop
import com.ardia.store.BrowserUrl
import com.ardia.sync.PinnedTile
import com.ardia.sync.SyncedProfile

@Composable
fun SearchHomeView(model: AppModel, modifier: Modifier = Modifier) {
    val isTrayUp = model.browse.search != null
    val isIncognito = model.isIncognito
    val reduceMotion = LocalReduceMotion.current
    val shown = animateFloatAsState(
        targetValue = if (isTrayUp) 0f else 1f,
        animationSpec = (if (reduceMotion) Motion.reducedFade else Motion.searchRise).spec(),
        label = "restingStart",
    )
    val isOnboarding = model.settings.isShowingOnboarding
    LaunchedEffect(isOnboarding) {
        model.refreshRecents()
        if (!isOnboarding) model.openTrayIfWanted()
    }
    val dark = LocalArdiaDarkTheme.current || isIncognito
    CompositionLocalProvider(LocalArdiaDarkTheme provides dark) {
        val frost = rememberFrostedBackdrop()
        Box(modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().frostedBackdropSource(frost)) {
                Crossfade(
                    targetState = isIncognito,
                    animationSpec = (if (reduceMotion) Motion.reducedFade else Motion.surfaceSwap).spec(),
                    label = "homeBackdrop",
                ) { incognito ->
                    if (incognito) {
                        ProfileBackdrop(Modifier.fillMaxSize(), look = ProfileLook.incognito)
                    } else {
                        ProfileBackdrop(Modifier.fillMaxSize(), themed = true)
                    }
                }
            }
            val hidden = if (isTrayUp) Modifier.clearAndSetSemantics {} else Modifier
            val space = if (Glass.isLiquid && model.syncState.isEnabled && !isIncognito) model.selectedProfile else null
            val showsTiles = space != null && space.pinned.tiles.isNotEmpty()
            if (space != null) {
                SpaceStart(
                    model = model,
                    space = space,
                    frost = frost,
                    showsTiles = showsTiles,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = (TabsDeckGeometry.BAR_RESERVE * 0.5).dp + if (showsTiles) 0.dp else BrowseLayout.logoSize * 1.3f)
                        .graphicsLayer {
                            alpha = shown.value
                            val scale = if (reduceMotion) 1f else 0.92f + 0.08f * shown.value
                            scaleX = scale
                            scaleY = scale
                        }
                        .then(hidden),
                )
            }
            if (!showsTiles) FidgetLogo(
                Modifier
                    .align(Alignment.Center)
                    .padding(bottom = (TabsDeckGeometry.BAR_RESERVE * 0.5).dp)
                    .size(BrowseLayout.logoSize, BrowseLayout.logoSize * 0.82f)
                    .graphicsLayer {
                        alpha = shown.value
                        val scale = if (reduceMotion) 1f else 0.92f + 0.08f * shown.value
                        scaleX = scale
                        scaleY = scale
                    }
                    .then(hidden),
            )
            DeckBar(
                model = model,
                frost = frost,
                enabled = !isTrayUp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { alpha = shown.value }
                    .then(hidden),
            )
        }
    }
}

@Composable
private fun DeckBar(model: AppModel, frost: FrostedBackdrop, enabled: Boolean, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val navigationBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val safe = WindowInsets.safeDrawing
    val safeSide = with(density) { maxOf(safe.getLeft(this, direction), safe.getRight(this, direction)).toDp() }
    val scale = LocalBarButtonScale.current
    val circle = TabsLayout.barCircle * scale
    val rowHeight = BarButtons.rowHeight(scale)
    val centerFromBottom = BrowseLayout.barCenter(navigationBottom, scale)
    val side = safeSide + TabsLayout.barSideInset - (BarButtons.hitTarget(circle) - circle) / 2
    val opensTabs = model.deckTabs.isNotEmpty()
    val syncs = model.syncState.isEnabled
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = side)
            .padding(bottom = centerFromBottom - rowHeight / 2)
            .height(rowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeckCircleButton(
            symbol = when {
                opensTabs -> ArdiaSymbols.squareOnSquare
                syncs -> ArdiaSymbols.laptopcomputer
                else -> ArdiaSymbols.archivebox
            },
            label = stringResource(
                when {
                    opensTabs -> R.string.deck_tabs
                    syncs -> R.string.deck_synced_tabs
                    else -> R.string.deck_archive
                },
            ),
            frost = frost,
            enabled = enabled,
        ) {
            when {
                opensTabs -> model.showTabSwitcher()
                syncs -> model.openSyncedSpaces()
                else -> model.openArchive()
            }
        }
        Spacer(Modifier.weight(1f))
        PlusButton(model, frost, enabled)
        Spacer(Modifier.weight(1f))
        DeckCircleButton(ArdiaSymbols.gearshape, stringResource(R.string.deck_settings), frost, enabled) {
            model.sheet = AppModel.Sheet.Settings
        }
    }
}

@Composable
private fun PlusButton(model: AppModel, frost: FrostedBackdrop, enabled: Boolean) {
    var menuOpen by remember { mutableStateOf(false) }
    val isIncognito = model.isIncognito
    GlassPlusButton(
        label = stringResource(if (isIncognito) R.string.deck_new_incognito_tab else R.string.deck_new_tab),
        hint = stringResource(R.string.deck_more_options),
        backdrop = frost,
        classicStyle = FrostedStyle.glass,
        iconSize = 20.dp,
        enabled = enabled,
        onClick = { model.newTab() },
        onLongClick = { menuOpen = true },
    ) {
        GlassMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            if (isIncognito) {
                GlassMenuItem(stringResource(R.string.deck_new_tab), ArdiaSymbols.plus) {
                    menuOpen = false
                    model.newRegularTab()
                }
                GlassMenuItem(stringResource(R.string.deck_new_incognito_tab), ArdiaSymbols.eyes) {
                    menuOpen = false
                    model.newTab()
                }
            } else {
                GlassMenuItem(stringResource(R.string.deck_new_incognito_tab), ArdiaSymbols.eyes) {
                    menuOpen = false
                    model.newIncognitoTab()
                }
                if (model.tabs.canReopenClosed) {
                    GlassMenuItem(stringResource(R.string.deck_reopen_closed_tab), ArdiaSymbols.arrowUturnBackward) {
                        menuOpen = false
                        model.reopenLastClosed()
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckCircleButton(symbol: ImageVector, label: String, frost: FrostedBackdrop, enabled: Boolean, onClick: () -> Unit) {
    GlassCircleButton(
        symbol = symbol,
        label = label,
        backdrop = frost,
        classicStyle = FrostedStyle.glass,
        classicTint = BrowsePalette.foregroundSecondary,
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun SpaceStart(model: AppModel, space: SyncedProfile, frost: FrostedBackdrop, showsTiles: Boolean, modifier: Modifier = Modifier) {
    val profiles = model.syncSnapshot.profiles
    Column(
        modifier.widthIn(max = 360.dp).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GlassSpaceSwitcher(
            spaces = profiles.map { it.name },
            selected = profiles.indexOfFirst { it.syncId == space.syncId },
            hint = stringResource(R.string.deck_switch_space),
            backdrop = frost,
            choose = { index -> profiles.getOrNull(index)?.let { model.switchProfile(it.syncId) } },
        )
        if (showsTiles) {
            for (row in space.pinned.tiles.take(MAX_START_TILES).chunked(START_TILE_COLUMNS)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (tile in row) StartTile(model, space.syncId, tile, frost, Modifier.weight(1f))
                    repeat(START_TILE_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun StartTile(model: AppModel, spaceId: String, tile: PinnedTile, frost: FrostedBackdrop, modifier: Modifier = Modifier) {
    val folder = tile.folder?.takeIf { it.isNotEmpty() }
    var menu by remember { mutableStateOf(false) }
    val label = tile.title.ifEmpty { BrowserUrl.host(tile.url).orEmpty() }
    Box(modifier) {
        GlassTile(
            label = label,
            backdrop = frost,
            onClick = { if (folder != null) menu = true else model.openRemote(tile.url, tile.title, spaceId) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (folder != null) {
                Icon(ArdiaSymbols.folderFill, null, Modifier.size(20.dp), tint = BrowsePalette.foregroundSecondary.current)
            } else {
                FaviconImage(null, tile.url, size = 22.dp)
            }
        }
        if (folder != null) {
            GlassMenu(expanded = menu, onDismissRequest = { menu = false }) {
                for (tab in folder) {
                    GlassMenuItem(tab.title.ifEmpty { BrowserUrl.host(tab.url).orEmpty() }, null) {
                        menu = false
                        model.openRemote(tab.url, tab.title, spaceId)
                    }
                }
            }
        }
    }
}

private const val START_TILE_COLUMNS = 4
private const val MAX_START_TILES = 8

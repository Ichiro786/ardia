package com.ardia.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ardia.design.ArdiaType
import com.ardia.design.FaviconImage
import com.ardia.design.FrostedBackdrop
import com.ardia.design.Glass
import com.ardia.design.LocalArdiaDarkTheme
import com.ardia.design.SystemPalette
import com.ardia.design.frosted
import com.ardia.design.glassShadow
import com.ardia.design.medium
import com.ardia.store.BrowserTab

private val resultsShape = RoundedCornerShape(22.dp)
private const val MAX_RESULTS = 6

@Composable
internal fun TabSearchResults(found: List<BrowserTab>, backdrop: FrostedBackdrop?, open: (String) -> Unit, modifier: Modifier = Modifier) {
    val dark = LocalArdiaDarkTheme.current
    val newTab = stringResource(R.string.tabs_new_tab)
    Column(
        modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .glassShadow(resultsShape, true, dark)
            .frosted(backdrop, resultsShape, Glass.panel)
            .padding(vertical = 6.dp),
    ) {
        if (found.isEmpty()) {
            Text(
                stringResource(R.string.tabs_no_matching_tabs),
                style = ArdiaType.subheadline,
                color = SystemPalette.secondaryLabel.resolve(dark),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        for (tab in found.take(MAX_RESULTS)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { open(tab.id) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FaviconImage(tab.faviconUrl, tab.url, size = 18.dp)
                Column {
                    Text(TabText.title(tab, newTab), style = ArdiaType.subheadline.medium, color = SystemPalette.label.resolve(dark), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(TabText.host(tab, newTab), style = ArdiaType.caption, color = SystemPalette.secondaryLabel.resolve(dark), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

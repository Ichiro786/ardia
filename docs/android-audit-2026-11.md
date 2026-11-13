# Android stability audit, November 2026

What the audit of `apps/android` found, what this branch fixes, and what's left. Severity: High means a crash, data loss or a security gap; Medium a visible bug or likely failure; Low polish.

## Reported by the owner

| Problem | Cause | Status |
|---|---|---|
| Tab cards turn black at the bottom | The snapshot only covers the WebView, which stops above the bottom bar; the rest of the card showed the black card background (`tabs/TabCard.kt`, `DeckState.kt`, `TabSwitcherView.kt`) | Fixed: the card and the open/close cover fill with the snapshot's bottom-edge colour, and the cover's backdrop sits inside its fade |
| "Isn't 16 KB compatible" dialog | `libardiahermes` was linked by NDK r27 with 4 KB segments (`gradle/libs.versions.toml`, `shared/build.gradle.kts`) | Fixed: NDK r28 with flexible page sizes; CI checks every native library (`scripts/check-page-alignment.py`) |
| "App is debuggable" dialog | CI only published the debug APK | Fixed: CI also publishes `ardia-release-apk`, signed with a cached CI key so new builds install over old ones |
| Bottom buttons too small | Tab view buttons sat lower than the page bar; all bar buttons are 35 to 40 dp | Fixed: one bar line everywhere, and Settings › Bottom Button Size (Standard, Large, Larger; Large by default) |

## Fixed on this branch

### High
- Store that won't open (disk full, corrupt file) crashed every launch (`appmodel/AppModel.kt` `loadStore`, `store/BrowserStore.kt`). Now a toast and a retry when the app comes back.
- JS dialogs from background pages were never answered and could freeze the renderer (`browser/WebViewPageClients.kt`, `DevelopmentWebViewEngine.kt`).
- Renderer killed in the background left a blank tab and wiped its back/forward list; a crash reloaded every live tab and could loop (`browser/WebViewPage.kt`, `LivePageRoster.kt`).
- Long-press Copy/Share on a `data:` image threw TransactionTooLargeException (`WebViewPage.kt`, `appmodel/SystemActions.kt`).
- `intent:` links pointing at `file:` crashed with FileUriExposedException (`browser/PageCapture.kt`).
- A broken Android keystore crashed sync (`sync/KeystoreKeyStore.kt`); a throwing sync cycle stopped the loop for good (`SyncController.kt`, `SyncService.kt`).
- Very long shared or selected text crashed Open in Ardia (`app/share/ShareSheetModel.kt`, `ShareActivity.kt`, `SystemEntry.kt`).

### Medium
- Reopening from Recents replayed the link, shortcut or voice search that first opened the task (`app/MainActivity.kt`).
- Rotating the share sheet could send the same link twice (`AndroidManifest.xml`).
- Reader mode could spin forever (`WebViewPage.kt`).
- Sync settings buttons stuck in their busy state after an error (`settings/SyncPages.kt`); the code scanner didn't notice camera access granted in Settings (`SyncCodeScanner.kt`).
- Filter lists downloaded on metered or offline networks (`blocking/AndroidNetworkStatus.kt`).
- Private-tab previews could be evicted from the image cache; deleting many tabs removed files on the main thread (`store/TabSnapshotStore.kt`).
- Pending tab writes were dropped on close (`store/BrowserStore.kt`).
- Search tray restarted on density changes; toasts shared one drag state; tray and deck drags didn't reset when cancelled; voice search failed without an on-device language pack; double submit could open two tabs (`search/*`, `design/Toasts.kt`, `design/TrayDrag.kt`, `tabs/DeckModifiers.kt`, `tabs/DeckState.kt`).
- Widget favicon fetches kept running after being superseded and decoded without a size bound (`app/system/SystemSnapshotExport.kt`).
- Cloud backup ran on devices without encrypted backup (`res/xml/data_extraction_rules.xml`).
- Cookies are flushed to disk when the app goes to the background, so a fresh sign-in survives the process being killed (`AppModel.sceneEnteredBackground`).

### Low
- `intent:` links can no longer carry URI-grant or clear-task flags into the app they open (`browser/PageCapture.kt`).

### Motion
- Fades for sheet content, archive and synced-spaces rows, the search tray tint, the incognito backdrop and voice search phases.
- A sheet touched while it closed froze half-open over a dead scrim: its drag stays off once it starts closing, and the close always finishes (`app/Containers.kt`).
- Sheets keep the finger's speed when let go, both when they spring back and when they close (`Containers.kt`).
- The loading line no longer carries the last tab's progress into the next one (`browsing/BrowsingView.kt`).
- Find in Page slides in and out with the bar instead of popping (`BrowsingView.kt`).
- Reader mode fades instead of sliding when Reduce Motion is on, and no longer writes state while drawing (`BrowsingView.kt`).
- The Incognito label opens and closes its space smoothly instead of making the icon jump (`search/IncognitoToggle.kt`).

## Not fixed yet

From the second, wider pass:

- Any app or web page can open `ardia://voice`, `ardia://incognito` or `ardia://mac-tabs`, so voice search can start without a tap (`AndroidManifest.xml`, `app/SystemEntry.kt`, `appmodel/SystemLink.kt`). Medium-high, security. Fix: require an in-app extra on widget and shortcut intents for those links.
- The page view ignores side insets, so in landscape the bar and page can sit under a camera cutout or side nav bar (`browsing/BrowsingView.kt`). Medium.
- Clear Cookies with "Today" clears every cookie, since WebView can't clear by time; the dialog says "from today" (`settings/GeneralPages.kt`, `ClearDataWords.kt`). Medium, product call.
- Dismissing the Clear Data sheet mid-clear cancels the rest of it (`settings/GeneralPages.kt`). Medium.
- Downloads have no notification or foreground service, and `blob:`/`data:` downloads fail (`app/SystemDownloads.kt`). Medium, design work.
- Private-tab favicons are fetched outside the private session and cached on disk (`browsing/PageLayer.kt`, `TabPager.kt`, `design/Favicon.kt`); a favicon that fails once isn't retried until restart. Medium.
- Tab state saves typed form values to the database (`browser/WebViewPage.kt` `includeForms`). Medium, product call.
- Low: sheet drag can lose a delta at high touch rates (`app/Containers.kt`); Omnibox close blocks the main thread; saving page states on every background runs on main; closing a tab serialises its state first (`LivePageRoster.kt`); some English strings live in code; a few touch targets under 48 dp; settings rows lack a button role; no native debug symbols in release; "Open in Default Browser" can show when the default-browser role is unavailable; copied private URLs aren't marked sensitive; regular-profile clear-data could use `WebStorageCompat.deleteBrowsingData`.
- Motion pass, not fixed yet: after a committed back or forward swipe the old page flashes back for a moment before the new one loads, and a fast flick eases in from rest (`browsing/PageLayer.kt` `finishSwipe`); a second swipe or pull started during the last one's animation can cut it short (`PageLayer.kt`); a toast jumps between top and bottom when the keyboard opens (`design/Toasts.kt`); the Settings back gesture resets on a fixed 500 ms timer (`settings/SettingsView.kt`); sync and blocking settings sections appear and disappear without animation (`settings/SyncPages.kt`, `BlockingPages.kt`); the long-press menu and share sheet exit don't use the app's motion (`BrowsingView.kt`, `share/ShareSheetView.kt`); a replaced tab preview swaps without a crossfade (`tabs/TabCard.kt`).
- CI: no lint step, the emulator job rebuilds and re-downloads its image each run, and the Hermes download has no timeout.

From the first pass:

- Share to Mac fails when Ardia's process isn't running: the send gives up before sync starts (`AppModel.sendFromShare`). Medium.
- After Android kills the app in the background, it comes back to the start screen with the keyboard up instead of the page (`MainActivity`). Medium.
- The background sync job starts the whole browser (filters, Hermes, WebView) in a process with no screen (`ArdiaApplication.syncController`). Medium, battery.
- Status bar icon colour can be wrong after closing a sheet: two places write it (`app/StatusBarInk.kt`, `browsing/BrowsingView.kt`). Medium.
- Downloads fail on sites whose cookies have non-ASCII characters (`browser/HttpDownloadTransport.kt`). Medium.
- Reader lead image is only width-capped; ReaderView swallows cancellation (`browsing/ReaderView.kt`). Low.
- Camera captures for uploads from private tabs stay in the cache (`browsing/PageRequests.kt`). Low, privacy.
- Widevine protected-media ID is granted without asking. Low.
- WebViews keep running timers and media in the background; whether that's wanted is a product call.
- No Gradle dependency verification metadata. Low.

## What needs a phone

None of this could be run on a device here; CI builds, unit tests and emulator tests pass. Worth checking by hand: tab card previews in dark mode, the 16 KB dialog gone, the three button sizes on the start screen, a page and the tab view, and that a renderer crash on the front tab reloads it once.

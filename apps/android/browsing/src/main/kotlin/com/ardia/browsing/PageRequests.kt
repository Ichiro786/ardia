package com.ardia.browsing

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ardia.browser.Page
import com.ardia.browser.PageRequest
import com.ardia.browser.SitePermission
import com.ardia.design.ArdiaSymbols
import com.ardia.design.GlassChoiceRow
import com.ardia.design.GlassDialog
import com.ardia.design.GlassDialogActions
import com.ardia.design.GlassDialogText
import com.ardia.design.GlassPillButton
import com.ardia.design.GlassTextField
import com.ardia.design.Spacing
import com.ardia.design.SystemPalette
import java.io.File
import java.util.UUID

class CaptureFileProvider : FileProvider()

internal object CaptureFiles {
    fun create(context: Context, extension: String): Uri {
        val folder = File(context.cacheDir, "captures").apply { mkdirs() }
        folder.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > STALE_MILLIS }?.forEach { it.delete() }
        val file = File(folder, "${UUID.randomUUID()}.$extension")
        return FileProvider.getUriForFile(context, authority(context), file)
    }

    private const val STALE_MILLIS = 24L * 60 * 60 * 1000

    fun authority(context: Context): String = "${context.packageName}.browsing.captures"
}

internal object PickedFiles {
    @Suppress("DEPRECATION")
    private fun ownAuthorities(context: Context): Set<String> {
        val declared = runCatching {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PROVIDERS).providers.orEmpty()
                .flatMap { it.authority?.split(';').orEmpty() }
        }.getOrDefault(emptyList())
        return (declared + CaptureFiles.authority(context)).filter { it.isNotEmpty() }.toSet()
    }

    private fun isGranted(context: Context, uri: Uri): Boolean =
        context.checkUriPermission(uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun answer(context: Context, uris: List<Uri>, captureUri: Uri?): PageRequest.Answer {
        val own = ownAuthorities(context)
        val capture = captureUri?.toString()
        val files = uris.map { uri ->
            val isOwnCapture = capture != null && uri.toString() == capture
            PickedFile(uri.toString(), uri.scheme, uri.authority, isGranted = !isOwnCapture && isGranted(context, uri))
        }
        val accepted = PickedFileRule.filter(files, own, context.packageName, capture).map { it.uri }.toHashSet()
        return PageRequest.Answer.Files(uris.filter { it.toString() in accepted })
    }
}

private fun Context.holds(permissions: Array<String>): Map<String, Boolean> =
    permissions.associateWith { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

@Composable
internal fun PageRequestHost(page: Page?) {
    page ?: return
    val request by page.request.collectAsState()
    val current = request?.takeUnless { it.isAnswered } ?: return
    when (val kind = current.kind) {
        is PageRequest.Kind.Permission -> PermissionRequest(current, kind.permission)
        is PageRequest.Kind.FileChooser -> FileChooserRequest(current, kind)
        else -> RequestDialog(current)
    }
}

@Composable
private fun RequestDialog(request: PageRequest) {
    val context = LocalContext.current
    var text by remember(request.id) { mutableStateOf((request.kind as? PageRequest.Kind.Prompt)?.defaultText.orEmpty()) }
    var user by remember(request.id) { mutableStateOf("") }
    var password by remember(request.id) { mutableStateOf("") }
    val title = remember(request.id) { request.title(context) }
    val message = remember(request.id) { request.message(context) }
    val ok = stringResource(R.string.browsing_ok)
    val cancel = stringResource(R.string.browsing_cancel)
    val (confirmTitle, confirm) = when (request.kind) {
        is PageRequest.Kind.Alert, is PageRequest.Kind.Confirm -> ok to { request.answer(PageRequest.Answer.Accepted) }
        is PageRequest.Kind.Prompt -> ok to { request.answer(PageRequest.Answer.Text(text)) }
        is PageRequest.Kind.OpenExternal -> stringResource(R.string.browsing_open) to { request.answer(PageRequest.Answer.Accepted) }
        is PageRequest.Kind.HttpAuthentication -> stringResource(R.string.browsing_sign_in) to {
            request.answer(PageRequest.Answer.Credential(user, password))
        }
        else -> ok to { request.answer(PageRequest.Answer.Accepted) }
    }
    val cancellable = request.kind !is PageRequest.Kind.Alert
    GlassDialog(onDismissRequest = { request.dismiss() }, modifier = Modifier.testTag(BrowsingTags.REQUEST)) {
        GlassDialogText(title, message)
        when (request.kind) {
            is PageRequest.Kind.Prompt -> GlassTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.padding(top = Spacing.small).testTag("request.text"),
            )
            is PageRequest.Kind.HttpAuthentication -> {
                GlassTextField(
                    value = user,
                    onValueChange = { user = it },
                    placeholder = stringResource(R.string.browsing_user_name),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
                    modifier = Modifier.padding(top = Spacing.small).testTag("request.user"),
                )
                GlassTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = stringResource(R.string.browsing_password),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.testTag("request.password"),
                )
            }
            else -> Unit
        }
        GlassDialogActions(confirmTitle, confirm, if (cancellable) cancel else null, { request.dismiss() })
    }
}

@Composable
private fun PermissionRequest(request: PageRequest, permission: SitePermission) {
    val context = LocalContext.current
    val needed = remember(permission) { SitePermissions.android(permission) }
    var systemGranted by remember(request.id) { mutableStateOf(SitePermissions.granted(permission, context.holds(needed))) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val merged = context.holds(needed) + results.filterValues { it }
        if (SitePermissions.granted(permission, merged)) systemGranted = true else request.dismiss()
    }
    LaunchedEffect(request.id) { if (!systemGranted) launcher.launch(needed) }
    if (!systemGranted) return
    val title = remember(request.id) { request.title(context) }
    GlassDialog(onDismissRequest = { request.dismiss() }, modifier = Modifier.testTag(BrowsingTags.REQUEST)) {
        GlassDialogText(title, null)
        GlassDialogActions(
            stringResource(R.string.browsing_allow),
            { request.answer(PageRequest.Answer.Accepted) },
            stringResource(R.string.browsing_dont_allow),
            { request.dismiss() },
        )
    }
}

private enum class Capture { Photo, Video }

private fun pickerType(media: VisualMedia): ActivityResultContracts.PickVisualMedia.VisualMediaType = when (media) {
    VisualMedia.Images -> ActivityResultContracts.PickVisualMedia.ImageOnly
    VisualMedia.Videos -> ActivityResultContracts.PickVisualMedia.VideoOnly
    VisualMedia.ImagesAndVideos -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
}

@Composable
private fun FileChooserRequest(request: PageRequest, kind: PageRequest.Kind.FileChooser) {
    val context = LocalContext.current
    val plan = remember(request.id) { FileChooserPlan.of(kind.acceptTypes, kind.prefersCapture) }
    var choosing by remember(request.id) { mutableStateOf(!plan.capturesDirectly && (plan.offersPhoto || plan.offersVideo)) }
    var captureUri by remember(request.id) { mutableStateOf<Uri?>(null) }
    var pendingCapture by remember(request.id) { mutableStateOf<Capture?>(null) }
    val visualMedia = remember(request.id) { PickerChoice.visualMedia(plan.documentTypes) }
    fun answerPicked(uris: List<Uri>) {
        request.answer(if (uris.isEmpty()) PageRequest.Answer.Dismissed else PickedFiles.answer(context, uris, captureUri = null))
    }
    fun answerCaptured(saved: Boolean) {
        val uri = captureUri
        request.answer(if (saved && uri != null) PickedFiles.answer(context, listOf(uri), captureUri = uri) else PageRequest.Answer.Dismissed)
    }
    val single = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> answerPicked(listOfNotNull(uri)) }
    val multiple = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> answerPicked(uris) }
    val singleMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> answerPicked(listOfNotNull(uri)) }
    val multipleMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris -> answerPicked(uris) }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), ::answerCaptured)
    val video = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo(), ::answerCaptured)
    fun startCapture(capture: Capture) {
        val uri = runCatching { CaptureFiles.create(context, if (capture == Capture.Photo) "jpg" else "mp4") }.getOrNull()
        if (uri == null) {
            request.dismiss()
            return
        }
        captureUri = uri
        runCatching { if (capture == Capture.Photo) photo.launch(uri) else video.launch(uri) }.onFailure { request.dismiss() }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val capture = pendingCapture
        pendingCapture = null
        if (granted && capture != null) startCapture(capture) else request.dismiss()
    }
    fun capture(kind: Capture) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCapture(kind)
        } else {
            pendingCapture = kind
            camera.launch(Manifest.permission.CAMERA)
        }
    }
    fun documents() {
        runCatching {
            val media = visualMedia?.let(::pickerType)
            when {
                media != null && kind.allowsMultiple -> multipleMedia.launch(PickVisualMediaRequest(media))
                media != null -> singleMedia.launch(PickVisualMediaRequest(media))
                kind.allowsMultiple -> multiple.launch(plan.documentTypes)
                else -> single.launch(plan.documentTypes)
            }
        }.onFailure { request.dismiss() }
    }
    LaunchedEffect(request.id) {
        when {
            plan.capturesDirectly -> capture(if (plan.offersPhoto) Capture.Photo else Capture.Video)
            !choosing -> documents()
        }
    }
    if (!choosing) return
    val title = remember(request.id) { request.title(context) }
    GlassDialog(onDismissRequest = { request.dismiss() }, modifier = Modifier.testTag(BrowsingTags.REQUEST)) {
        GlassDialogText(title, null)
        Column(Modifier.padding(top = Spacing.small), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            if (plan.offersPhoto) {
                GlassChoiceRow(stringResource(R.string.browsing_take_photo), BrowsingSymbols.camera) {
                    choosing = false
                    capture(Capture.Photo)
                }
            }
            if (plan.offersVideo) {
                GlassChoiceRow(stringResource(R.string.browsing_record_video), BrowsingSymbols.video) {
                    choosing = false
                    capture(Capture.Video)
                }
            }
            GlassChoiceRow(
                stringResource(if (kind.allowsMultiple) R.string.browsing_choose_files else R.string.browsing_choose_file),
                ArdiaSymbols.folderFill,
            ) {
                choosing = false
                documents()
            }
        }
        Spacer(Modifier.height(Spacing.small))
        GlassPillButton(stringResource(R.string.browsing_cancel), SystemPalette.label.current, Modifier.fillMaxWidth()) { request.dismiss() }
    }
}

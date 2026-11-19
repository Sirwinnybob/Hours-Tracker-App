package com.example.timecard.ui.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * Profile photo plumbing shared by the themed settings layouts (standard, LCARS): the photo picker
 * (scaled to 512 px JPEG, saved through [ProfileViewModel.saveCustomAvatar]) and the current photo.
 */

/** Returns a launcher that opens the system photo picker and saves the chosen image as the avatar. */
@Composable
internal fun rememberAvatarPicker(profileViewModel: ProfileViewModel): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                val original = BitmapFactory.decodeStream(stream) ?: return@use null
                val scale = minOf(512f / original.width, 512f / original.height, 1f)
                val scaled = if (scale < 1f) Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true) else original
                java.io.ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
            }
            if (bytes != null) profileViewModel.saveCustomAvatar(bytes)
        }
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

/** The custom photo when one is set and loaded, else null (callers draw initials). */
@Composable
internal fun rememberAvatarBitmap(profileViewModel: ProfileViewModel): ImageBitmap? {
    val bytes = profileViewModel.avatarImage
    val avatar = profileViewModel.profile.avatar
    return remember(bytes, avatar) {
        if (avatar == "custom" && bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() else null
    }
}

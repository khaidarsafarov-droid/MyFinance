package com.truckerload.presentation.screens.camera

import com.truckerload.presentation.icons.AppIcons

import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R

enum class CameraFlashMode {
    OFF,
    AUTO,
    ON,
    ;

    fun next(): CameraFlashMode = when (this) {
        OFF -> AUTO
        AUTO -> ON
        ON -> OFF
    }

    fun toImageCaptureMode(): Int = when (this) {
        OFF -> ImageCapture.FLASH_MODE_OFF
        AUTO -> ImageCapture.FLASH_MODE_AUTO
        ON -> ImageCapture.FLASH_MODE_ON
    }
}

@Composable
fun CameraFlashButton(
    mode: CameraFlashMode,
    onCycle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, labelRes, cdRes) = when (mode) {
        CameraFlashMode.OFF -> Triple(
            AppIcons.FlashOff,
            R.string.camera_flash_off,
            R.string.camera_flash_cd_off,
        )
        CameraFlashMode.AUTO -> Triple(
            AppIcons.FlashAuto,
            R.string.camera_flash_auto,
            R.string.camera_flash_cd_auto,
        )
        CameraFlashMode.ON -> Triple(
            AppIcons.FlashOn,
            R.string.camera_flash_on,
            R.string.camera_flash_cd_on,
        )
    }
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                CircleShape,
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = onCycle) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(cdRes),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

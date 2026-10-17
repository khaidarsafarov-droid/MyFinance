package com.truckerload.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.BarChart as BarChartOutlined
import androidx.compose.material.icons.outlined.Build as BuildOutlined
import androidx.compose.material.icons.outlined.DocumentScanner as DocumentScannerOutlined
import androidx.compose.material.icons.outlined.Edit as EditOutlined
import androidx.compose.material.icons.outlined.Folder as FolderOutlined
import androidx.compose.material.icons.outlined.Info as InfoOutlined
import androidx.compose.material.icons.outlined.LocalGasStation as GasOutlined
import androidx.compose.material.icons.outlined.Map as MapOutlined
import androidx.compose.material.icons.outlined.Payments as PaymentsOutlined
import androidx.compose.material.icons.outlined.PhotoCamera as CameraOutlined
import androidx.compose.material.icons.outlined.Receipt as ReceiptOutlined
import androidx.compose.material.icons.outlined.Settings as SettingsOutlined
import androidx.compose.material.icons.outlined.TableChart as TableOutlined
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * One icon family for the drawer and the tablet rail.
 * Outlined at rest, filled only while that destination is open.
 */
internal fun drawerIcon(destination: DrawerDestination, selected: Boolean): ImageVector {
    val filled: ImageVector
    val outlined: ImageVector
    when (destination) {
        DrawerDestination.PAYCHECK -> {
            filled = Icons.Filled.Payments
            outlined = Icons.Outlined.PaymentsOutlined
        }
        DrawerDestination.DIESEL -> {
            filled = Icons.Filled.LocalGasStation
            outlined = Icons.Outlined.GasOutlined
        }
        DrawerDestination.MISC_EXPENSES -> {
            filled = Icons.Filled.Receipt
            outlined = Icons.Outlined.ReceiptOutlined
        }
        DrawerDestination.REPORTS -> {
            filled = Icons.Filled.BarChart
            outlined = Icons.Outlined.BarChartOutlined
        }
        DrawerDestination.TAX_TRACKER -> {
            filled = Icons.Filled.TableChart
            outlined = Icons.Outlined.TableOutlined
        }
        DrawerDestination.MAP -> {
            filled = Icons.Filled.Map
            outlined = Icons.Outlined.MapOutlined
        }
        DrawerDestination.CAMERA -> {
            filled = Icons.Filled.PhotoCamera
            outlined = Icons.Outlined.CameraOutlined
        }
        DrawerDestination.SCANNER -> {
            filled = Icons.Filled.DocumentScanner
            outlined = Icons.Outlined.DocumentScannerOutlined
        }
        DrawerDestination.MAINTENANCE -> {
            filled = Icons.Filled.Build
            outlined = Icons.Outlined.BuildOutlined
        }
        DrawerDestination.DOCUMENTS -> {
            filled = Icons.Filled.Folder
            outlined = Icons.Outlined.FolderOutlined
        }
        DrawerDestination.SETTINGS -> {
            filled = Icons.Filled.Settings
            outlined = Icons.Outlined.SettingsOutlined
        }
        DrawerDestination.IMPROVE -> {
            filled = Icons.Filled.Edit
            outlined = Icons.Outlined.EditOutlined
        }
        DrawerDestination.ABOUT -> {
            filled = Icons.Filled.Info
            outlined = Icons.Outlined.InfoOutlined
        }
    }
    return if (selected) filled else outlined
}

package com.truckerload.presentation.screens.scanner

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.appFormField

/** Documents search. Hint, typed text, cursor, and icon use the midnight field colors. */
@Composable
internal fun DocumentSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .appFormField(),
        placeholder = {
            Text(
                text = stringResource(R.string.scan_search_hint),
                color = AppTextFieldDefaults.hintColor(),
            )
        },
        leadingIcon = {
            Icon(
                imageVector = AppIcons.Search,
                contentDescription = stringResource(R.string.common_search),
                tint = AppTextFieldDefaults.iconColor(),
            )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = AppTextFieldDefaults.inputColor(),
        ),
        colors = AppTextFieldDefaults.outlined(),
    )
}

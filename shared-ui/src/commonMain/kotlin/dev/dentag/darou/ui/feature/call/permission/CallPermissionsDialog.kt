package dev.dentag.darou.ui.feature.call.permission

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import dev.dentag.darou.ui.resources.Res
import dev.dentag.darou.ui.resources.call_permissions_close
import dev.dentag.darou.ui.resources.call_permissions_denied
import dev.dentag.darou.ui.resources.call_permissions_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CallPermissionsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.call_permissions_title)) },
        text = { Text(stringResource(Res.string.call_permissions_denied)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.call_permissions_close))
            }
        },
    )
}

package dev.dentag.darou.ui.feature.call.permission

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberCallPermissionsLauncher(onResult: (Boolean) -> Unit): () -> Unit

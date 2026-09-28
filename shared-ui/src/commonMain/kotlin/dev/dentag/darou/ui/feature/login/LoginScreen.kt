package dev.dentag.darou.ui.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.dentag.darou.ui.feature.login.model.LoginUiState
import dev.dentag.darou.ui.resources.Res
import dev.dentag.darou.ui.resources.login_code
import dev.dentag.darou.ui.resources.login_loading
import dev.dentag.darou.ui.resources.login_submit
import dev.dentag.darou.ui.resources.login_title
import dev.dentag.darou.ui.resources.login_user
import dev.dentag.darou.ui.resources.login_user_hint
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LoginScreen(
    state: LoginUiState,
    onUserIdChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldsEnabled = !state.isLoading && state.authenticatedUser == null

    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.safeDrawingPadding().imePadding().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.login_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                OutlinedTextField(
                    value = state.userId,
                    onValueChange = onUserIdChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = fieldsEnabled,
                    label = { Text(stringResource(Res.string.login_user)) },
                    supportingText = { Text(stringResource(Res.string.login_user_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = state.code,
                    onValueChange = onCodeChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = fieldsEnabled,
                    label = { Text(stringResource(Res.string.login_code)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (state.canSubmit) onLogin() },
                    ),
                )
                state.error?.let { error ->
                    Text(
                        text = stringResource(error.messageResource()),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Button(
                    onClick = onLogin,
                    enabled = state.canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = LocalContentColor.current,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        stringResource(
                            if (state.isLoading) Res.string.login_loading else Res.string.login_submit,
                        ),
                    )
                }
            }
        }
    }
}

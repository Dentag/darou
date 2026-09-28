package dev.dentag.darou.ui.feature.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.dentag.darou.ui.resources.Res
import dev.dentag.darou.ui.resources.app_name
import dev.dentag.darou.ui.resources.welcome_description
import dev.dentag.darou.ui.resources.welcome_status
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WelcomeScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.safeDrawingPadding().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(Res.string.welcome_description),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(Res.string.welcome_status),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

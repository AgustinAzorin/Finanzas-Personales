package com.agustinazorin.finanzas.core.diagnostics.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agustinazorin.finanzas.R

/**
 * Se muestra al arrancar cuando [com.agustinazorin.finanzas.core.diagnostics.CrashDiagnostics]
 * encuentra un registro de un arranque anterior que falló. Es enteramente local (CLAUDE.md,
 * sección 2): la app nunca envía este texto a ningún lado — "Compartir" abre el selector normal
 * de Android y es el usuario quien elige, si quiere, a dónde mandarlo.
 */
@Composable
fun StartupDiagnosticsScreen(log: String, onDismiss: () -> Unit) {
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.diagnostics_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.diagnostics_description), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = log,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            )
            OutlinedButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, log)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.diagnostics_share))
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.diagnostics_continue))
            }
        }
    }
}

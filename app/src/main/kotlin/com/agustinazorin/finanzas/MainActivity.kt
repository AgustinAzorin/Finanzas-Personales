package com.agustinazorin.finanzas

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.agustinazorin.finanzas.core.diagnostics.CrashDiagnostics
import com.agustinazorin.finanzas.core.diagnostics.ui.StartupDiagnosticsScreen
import com.agustinazorin.finanzas.core.security.ui.AppLockGate
import com.agustinazorin.finanzas.core.ui.theme.FinanzasTheme
import com.agustinazorin.finanzas.navigation.FinanzasNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * [FragmentActivity] (no un simple `ComponentActivity`) porque `BiometricPrompt` lo exige para
 * mostrar el diálogo del sistema (CLAUDE.md, sección 43). Compose funciona igual: `FragmentActivity`
 * ya es un `ComponentActivity`.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Oculta el contenido en capturas de pantalla y en la lista de apps recientes (CLAUDE.md,
        // sección 43: "ocultar contenido sensible en screenshots cuando sea razonable"). Para una
        // app financiera, razonable es "siempre".
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        enableEdgeToEdge()
        setContent {
            // Si el arranque anterior falló, CrashDiagnostics dejó un registro local (ver
            // FinanzasApplication y DatabaseModule). Se muestra antes que cualquier otra cosa: es
            // la única forma de ver esa información sin `adb logcat`, que la mayoría de quienes
            // usan la app no tienen forma de correr.
            var diagnosticsLog by remember { mutableStateOf(CrashDiagnostics.readLog(this)) }

            FinanzasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val log = diagnosticsLog
                    if (log != null) {
                        StartupDiagnosticsScreen(
                            log = log,
                            onDismiss = {
                                CrashDiagnostics.clear(this)
                                diagnosticsLog = null
                            },
                        )
                    } else {
                        AppLockGate {
                            FinanzasNavHost()
                        }
                    }
                }
            }
        }
    }
}

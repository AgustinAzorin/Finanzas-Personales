package com.agustinazorin.finanzas

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
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
            FinanzasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppLockGate {
                        FinanzasNavHost()
                    }
                }
            }
        }
    }
}

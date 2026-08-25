package com.agustinazorin.finanzas.core.capture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.agustinazorin.finanzas.core.preferences.CapturePreferences
import com.agustinazorin.finanzas.engine.capture.NotificationParserRegistry
import com.agustinazorin.finanzas.engine.model.CaptureStatus
import com.agustinazorin.finanzas.feature.capture.domain.CapturedNotification
import com.agustinazorin.finanzas.feature.capture.domain.CapturedNotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Escucha notificaciones únicamente de las apps que el usuario habilitó explícitamente en la
 * pantalla de configuración de captura (CLAUDE.md, sección 37). Nunca crea una Transaction
 * directamente ni asume que un texto interpretado es un gasto real: sólo guarda la captura cruda
 * en estado PENDING_REVIEW para que el usuario la confirme desde la bandeja de revisión.
 *
 * Si ningún [NotificationParser][com.agustinazorin.finanzas.engine.capture.NotificationParser]
 * logra interpretar el texto, la notificación simplemente se ignora: no tiene sentido llenar la
 * bandeja de revisión con notificaciones promocionales de apps bancarias.
 */
@AndroidEntryPoint
class FinanzasNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var capturePreferences: CapturePreferences

    @Inject lateinit var parserRegistry: NotificationParserRegistry

    @Inject lateinit var capturedNotificationRepository: CapturedNotificationRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName !in capturePreferences.enabledPackages) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val postedAt = Instant.ofEpochMilli(sbn.postTime)

        serviceScope.launch {
            val parsed = parserRegistry.parse(packageName, title, text) ?: return@launch
            if (capturedNotificationRepository.existsExactDuplicate(packageName, text, postedAt)) return@launch

            capturedNotificationRepository.insert(
                CapturedNotification(
                    id = 0,
                    packageName = packageName,
                    parserId = parsed.parserId,
                    parserVersion = parsed.parserVersion,
                    postedAt = postedAt,
                    rawTitle = title,
                    rawText = text,
                    parsedAmount = parsed.transaction.amount.minorUnits,
                    parsedCurrency = parsed.transaction.amount.currency,
                    parsedDirection = parsed.transaction.direction,
                    parsedMerchant = parsed.transaction.merchant,
                    parsedMerchantNormalized = parsed.transaction.merchantNormalized,
                    status = CaptureStatus.PENDING_REVIEW,
                    linkedTransactionId = null,
                    createdAt = Instant.now(),
                ),
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

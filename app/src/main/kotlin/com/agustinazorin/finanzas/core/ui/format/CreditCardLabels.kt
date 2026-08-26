package com.agustinazorin.finanzas.core.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.engine.model.CreditCardStatementStatus

@Composable
fun CreditCardStatementStatus.label(): String = when (this) {
    CreditCardStatementStatus.OPEN -> stringResource(R.string.credit_card_statement_status_open)
    CreditCardStatementStatus.CLOSED -> stringResource(R.string.credit_card_statement_status_closed)
    CreditCardStatementStatus.PARTIALLY_PAID -> stringResource(R.string.credit_card_statement_status_partially_paid)
    CreditCardStatementStatus.PAID -> stringResource(R.string.credit_card_statement_status_paid)
}

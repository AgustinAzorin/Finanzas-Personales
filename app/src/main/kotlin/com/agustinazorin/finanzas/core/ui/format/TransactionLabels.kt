package com.agustinazorin.finanzas.core.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.engine.model.TransactionType

@Composable
fun TransactionType.label(): String = when (this) {
    TransactionType.EXPENSE -> stringResource(R.string.type_expense)
    TransactionType.INCOME -> stringResource(R.string.type_income)
    TransactionType.TRANSFER -> stringResource(R.string.type_transfer)
    TransactionType.ADJUSTMENT -> stringResource(R.string.type_adjustment)
}

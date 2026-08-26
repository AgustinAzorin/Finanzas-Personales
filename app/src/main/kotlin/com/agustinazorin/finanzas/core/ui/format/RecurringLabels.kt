package com.agustinazorin.finanzas.core.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.engine.model.Periodicity
import com.agustinazorin.finanzas.engine.model.RecurringType

@Composable
fun Periodicity.label(): String = when (this) {
    Periodicity.WEEKLY -> stringResource(R.string.periodicity_weekly)
    Periodicity.BIWEEKLY -> stringResource(R.string.periodicity_biweekly)
    Periodicity.MONTHLY -> stringResource(R.string.periodicity_monthly)
    Periodicity.ANNUAL -> stringResource(R.string.periodicity_annual)
}

@Composable
fun RecurringType.label(): String = when (this) {
    RecurringType.EXPENSE -> stringResource(R.string.type_expense)
    RecurringType.INCOME -> stringResource(R.string.type_income)
}

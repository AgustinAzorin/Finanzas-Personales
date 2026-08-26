package com.agustinazorin.finanzas.core.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.engine.model.AccountType

@Composable
fun AccountType.label(): String = when (this) {
    AccountType.CASH -> stringResource(R.string.account_type_cash)
    AccountType.BANK_ACCOUNT -> stringResource(R.string.account_type_bank_account)
    AccountType.SAVINGS_ACCOUNT -> stringResource(R.string.account_type_savings_account)
    AccountType.MERCADO_PAGO -> stringResource(R.string.account_type_mercado_pago)
    AccountType.CREDIT_CARD -> stringResource(R.string.account_type_credit_card)
    AccountType.INVESTMENT -> stringResource(R.string.account_type_investment)
    AccountType.DIGITAL_WALLET -> stringResource(R.string.account_type_digital_wallet)
    AccountType.OTHER_ASSET -> stringResource(R.string.account_type_other_asset)
    AccountType.LOAN -> stringResource(R.string.account_type_loan)
    AccountType.OTHER_LIABILITY -> stringResource(R.string.account_type_other_liability)
}

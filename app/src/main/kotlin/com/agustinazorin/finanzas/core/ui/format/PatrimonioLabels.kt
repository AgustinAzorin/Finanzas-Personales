package com.agustinazorin.finanzas.core.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.engine.model.AssetCategory
import com.agustinazorin.finanzas.engine.model.LiabilityType

@Composable
fun AssetCategory.label(): String = when (this) {
    AssetCategory.CASH -> stringResource(R.string.asset_category_cash)
    AssetCategory.VEHICLE -> stringResource(R.string.asset_category_vehicle)
    AssetCategory.REAL_ESTATE -> stringResource(R.string.asset_category_real_estate)
    AssetCategory.INVESTMENT -> stringResource(R.string.asset_category_investment)
    AssetCategory.OTHER -> stringResource(R.string.asset_category_other)
}

@Composable
fun LiabilityType.label(): String = when (this) {
    LiabilityType.LOAN -> stringResource(R.string.liability_type_loan)
    LiabilityType.PERSONAL_DEBT -> stringResource(R.string.liability_type_personal_debt)
    LiabilityType.OTHER -> stringResource(R.string.liability_type_other)
}

package com.agustinazorin.finanzas.core.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.engine.model.MemberType

@Composable
fun MemberType.label(): String = when (this) {
    MemberType.OWNER -> stringResource(R.string.member_type_owner)
    MemberType.MEMBER -> stringResource(R.string.member_type_member)
    MemberType.DEPENDENT -> stringResource(R.string.member_type_dependent)
    MemberType.OTHER -> stringResource(R.string.member_type_other)
}

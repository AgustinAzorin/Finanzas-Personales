package com.agustinazorin.finanzas.core.database

import androidx.room.TypeConverter
import com.agustinazorin.finanzas.engine.model.AccountType
import com.agustinazorin.finanzas.engine.model.CaptureStatus
import com.agustinazorin.finanzas.engine.model.MemberType
import com.agustinazorin.finanzas.engine.model.Periodicity
import com.agustinazorin.finanzas.engine.model.RecurringType
import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.model.TransactionSource
import com.agustinazorin.finanzas.engine.model.TransactionStatus
import com.agustinazorin.finanzas.engine.model.TransactionType
import java.time.Instant
import java.time.LocalDate

/**
 * Todos los enums se persisten como el nombre de su constante (String), nunca como
 * [Enum.ordinal]: así una migración que reordene o agregue valores nunca corrompe datos
 * existentes.
 */
class Converters {

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromMemberType(value: MemberType?): String? = value?.name

    @TypeConverter
    fun toMemberType(value: String?): MemberType? = value?.let(MemberType::valueOf)

    @TypeConverter
    fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? = value?.let(AccountType::valueOf)

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? = value?.let(TransactionType::valueOf)

    @TypeConverter
    fun fromTransactionDirection(value: TransactionDirection?): String? = value?.name

    @TypeConverter
    fun toTransactionDirection(value: String?): TransactionDirection? = value?.let(TransactionDirection::valueOf)

    @TypeConverter
    fun fromTransactionSource(value: TransactionSource?): String? = value?.name

    @TypeConverter
    fun toTransactionSource(value: String?): TransactionSource? = value?.let(TransactionSource::valueOf)

    @TypeConverter
    fun fromTransactionStatus(value: TransactionStatus?): String? = value?.name

    @TypeConverter
    fun toTransactionStatus(value: String?): TransactionStatus? = value?.let(TransactionStatus::valueOf)

    @TypeConverter
    fun fromRecurringType(value: RecurringType?): String? = value?.name

    @TypeConverter
    fun toRecurringType(value: String?): RecurringType? = value?.let(RecurringType::valueOf)

    @TypeConverter
    fun fromPeriodicity(value: Periodicity?): String? = value?.name

    @TypeConverter
    fun toPeriodicity(value: String?): Periodicity? = value?.let(Periodicity::valueOf)

    @TypeConverter
    fun fromCaptureStatus(value: CaptureStatus?): String? = value?.name

    @TypeConverter
    fun toCaptureStatus(value: String?): CaptureStatus? = value?.let(CaptureStatus::valueOf)
}

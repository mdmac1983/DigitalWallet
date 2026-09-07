package app.orionmd.digitalwallet.data

import androidx.room.TypeConverter

/** Room needs explicit converters to store our enum columns as plain text. */
class Converters {

    @TypeConverter
    fun fromIdDocumentType(value: IdDocumentType): String = value.name

    @TypeConverter
    fun toIdDocumentType(value: String): IdDocumentType =
        runCatching { IdDocumentType.valueOf(value) }.getOrDefault(IdDocumentType.DRIVERS_LICENSE)

    @TypeConverter
    fun fromPasswordCategory(value: PasswordCategory): String = value.name

    @TypeConverter
    fun toPasswordCategory(value: String): PasswordCategory =
        runCatching { PasswordCategory.valueOf(value) }.getOrDefault(PasswordCategory.OTHER)

    @TypeConverter
    fun fromFinanceType(value: FinanceType): String = value.name

    @TypeConverter
    fun toFinanceType(value: String): FinanceType =
        runCatching { FinanceType.valueOf(value) }.getOrDefault(FinanceType.EXPENSE)

    @TypeConverter
    fun fromStatementType(value: StatementType): String = value.name

    @TypeConverter
    fun toStatementType(value: String): StatementType =
        runCatching { StatementType.valueOf(value) }.getOrDefault(StatementType.BANK)

    @TypeConverter
    fun fromHoldingType(value: HoldingType): String = value.name

    @TypeConverter
    fun toHoldingType(value: String): HoldingType =
        runCatching { HoldingType.valueOf(value) }.getOrDefault(HoldingType.STOCK)

    @TypeConverter
    fun fromSaleStatus(value: SaleStatus): String = value.name

    @TypeConverter
    fun toSaleStatus(value: String): SaleStatus =
        runCatching { SaleStatus.valueOf(value) }.getOrDefault(SaleStatus.UNPAID)

    @TypeConverter
    fun fromContactCategory(value: ContactCategory): String = value.name

    @TypeConverter
    fun toContactCategory(value: String): ContactCategory =
        runCatching { ContactCategory.valueOf(value) }.getOrDefault(ContactCategory.OTHER)
}

package app.orionmd.digitalwallet.data

/** Plaintext, in-memory representation of a Wallet "Cards" entry - never persisted as-is. */
data class CardItem(
    val id: Long = 0,
    val issuer: String = "",
    val cardName: String = "",
    val cardholderName: String = "",
    val cardNumber: String = "",
    val expDate: String = "",
    val cvv: String = "",
    val websitePortal: String = "",
    val username: String = "",
    val password: String = "",
    val frontImagePath: String? = null,
    val backImagePath: String? = null,
    val creditLimit: String = "",
    val billingAddress: String = "",
    val billingCity: String = "",
    val billingState: String = "",
    val billingZip: String = ""
)

/** Plaintext, in-memory representation of a Wallet "IDs" entry - never persisted as-is. */
data class IdDocumentItem(
    val id: Long = 0,
    val documentType: IdDocumentType = IdDocumentType.DRIVERS_LICENSE,
    val fullName: String = "",
    val documentNumber: String = "",
    val issueDate: String = "",
    val expirationDate: String = "",
    val issuingRegion: String = "",
    val notes: String = "",
    val frontImagePath: String? = null,
    val backImagePath: String? = null
)

/** Plaintext, in-memory representation of a Passwords-tab entry - never persisted as-is. */
data class PasswordItem(
    val id: Long = 0,
    val category: PasswordCategory = PasswordCategory.OTHER,
    val accountName: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = ""
)

/** Plaintext, in-memory representation of a Contacts-tab entry - never persisted as-is. */
data class ContactItem(
    val id: Long = 0,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val company: String = "",
    val notes: String = "",
    val photoPath: String? = null,
    val category: ContactCategory = ContactCategory.OTHER
)

/** Plaintext, in-memory representation of a Finances-tab entry - never persisted as-is. */
data class FinanceItem(
    val id: Long = 0,
    val type: FinanceType = FinanceType.EXPENSE,
    val amount: String = "",
    val date: String = "",
    val category: String = "",
    val source: String = "",
    val notes: String = "",
    /** Optional override word (e.g. "Deposit", "Debit") shown instead of "Income"/"Expense". */
    val typeLabel: String = "",
    val statementId: Long? = null
)

/** The word to display for this entry's direction - [FinanceItem.typeLabel] if the user set one,
 * otherwise the default "Income"/"Expense" wording. */
fun FinanceItem.displayTypeLabel(defaultIncome: String, defaultExpense: String): String =
    typeLabel.ifBlank { if (type == FinanceType.INCOME) defaultIncome else defaultExpense }

/** Plaintext, in-memory representation of an uploaded statement - never persisted as-is.
 * beginningBalance/endingBalance are optional, typed in by the user straight off the statement -
 * the app never computes or infers a balance itself (see [app.orionmd.digitalwallet.ui.statements.MonthlyStatementPdfGenerator]
 * for why: a computed running balance would silently go wrong the moment one transaction is
 * missed or duplicated, whereas a number the user copied off their own statement can't drift). */
data class StatementItem(
    val id: Long = 0,
    val type: StatementType = StatementType.BANK,
    val institution: String = "",
    val statementMonth: String = "",
    val sourceFileName: String? = null,
    val sourceDisplayName: String = "",
    val sourceMimeType: String? = null,
    val createdAt: Long = 0,
    val beginningBalance: String = "",
    val endingBalance: String = ""
)

/** Plaintext, in-memory representation of a manually-entered stock/crypto holding - never
 * persisted as-is. costBasis is optional: leave it blank to just track current value with no
 * gain/loss. See [totalValue]/[totalCost]/[gainLoss] below for the derived figures. */
data class HoldingItem(
    val id: Long = 0,
    val holdingType: HoldingType = HoldingType.STOCK,
    val symbol: String = "",
    val name: String = "",
    val quantity: String = "",
    val costBasis: String = "",
    val currentPrice: String = "",
    val notes: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

fun HoldingItem.quantityValue(): Double = quantity.toDoubleOrNull() ?: 0.0

/** Current quantity * current price. Null if no price has been entered yet. */
fun HoldingItem.totalValue(): Double? = currentPrice.toDoubleOrNull()?.let { it * quantityValue() }

/** Current quantity * cost basis. Null if no cost basis was entered (gain/loss isn't tracked). */
fun HoldingItem.totalCost(): Double? = costBasis.toDoubleOrNull()?.let { it * quantityValue() }

/** totalValue - totalCost. Null unless both a current price and a cost basis are on file. */
fun HoldingItem.gainLoss(): Double? {
    val value = totalValue() ?: return null
    val cost = totalCost() ?: return null
    return value - cost
}

/** Plaintext, in-memory representation of a Sales-tab entry (a service sold to a client) - never
 * persisted as-is. See [netFee] - it's always derived from price/commissionFee/partnerFee, never
 * stored, so it can't drift out of sync with them. */
data class SaleItem(
    val id: Long = 0,
    val date: String = "",
    val clientId: String = "",
    val service: String = "",
    val price: String = "",
    val commissionFee: String = "",
    val partnerFee: String = "",
    val status: SaleStatus = SaleStatus.UNPAID,
    /** Only meaningful when [status] is PARTIAL - how much of the net fee has been paid so far. */
    val amountPaid: String = "",
    val notes: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

/** price - commissionFee - partnerFee. Blank fields count as 0. */
fun SaleItem.netFee(): Double {
    val price = this.price.toDoubleOrNull() ?: 0.0
    val commission = commissionFee.toDoubleOrNull() ?: 0.0
    val partner = partnerFee.toDoubleOrNull() ?: 0.0
    return price - commission - partner
}

/** How much of the net fee has actually been paid. UNPAID is always 0; PAID is the full net fee
 * unless a specific amount was typed in; PARTIAL is whatever amount was typed in (blank = 0, not
 * yet recorded). */
fun SaleItem.amountPaidValue(): Double = when (status) {
    SaleStatus.UNPAID -> 0.0
    SaleStatus.PAID -> amountPaid.toDoubleOrNull() ?: netFee()
    SaleStatus.PARTIAL -> amountPaid.toDoubleOrNull() ?: 0.0
}

/** netFee - amountPaidValue. What's still owed. */
fun SaleItem.balanceDue(): Double = netFee() - amountPaidValue()

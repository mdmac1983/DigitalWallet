package app.orionmd.digitalwallet.ui.statements

import app.orionmd.digitalwallet.data.FinanceType
import app.orionmd.digitalwallet.data.StatementType

/**
 * Heuristic, not a real parser for any specific institution's statement layout (those vary too
 * much bank to bank, card to card, to hard-code) - looks for lines that pair a date with a
 * dollar-shaped amount and treats the rest of the line as a description. This is deliberately
 * conservative and will miss things or misjudge income vs. expense sometimes; every result flows
 * through [ReviewParsedActivity] before anything reaches the Finances tab.
 *
 * A few real-world statement quirks this specifically tries to handle, beyond the simplest
 * "one dollar amount per line, sign tells you the direction" case:
 *  - Some issuers print the sign with a space before the currency symbol ("- $25.00" rather than
 *    "-$25.00"), which a sign-must-be-adjacent regex would miss entirely.
 *  - Multi-column activity tables (quantity / price / amount, or price / total / commission / net)
 *    can have more than one dollar-shaped number on the same line; the *last* one is usually the
 *    actual line total, and if any of them carries an explicit sign or parentheses that's almost
 *    always the one that matters (a net cash-flow figure), even if it isn't the last column.
 *  - Some bank statements never put a sign on the amount at all - they instead split activity into
 *    separate tables ("Electronic Credits" vs "Electronic Debits", "Deposits and Additions" vs
 *    "Withdrawals and Subtractions") where the section heading itself is the only signal. Section
 *    headings are recognized as such because they're a line with no dollar amount at all.
 *  - A "Daily Balance(s)" section is a repeating date/amount table that looks exactly like a
 *    transaction table but is only balance snapshots, not activity - it's skipped entirely once
 *    recognized, until a real activity section heading is seen again.
 */
object StatementParser {

    // Sign (+/-) may have a space before the optional parenthesis/currency symbol, e.g. "- $25.00".
    private val AMOUNT_REGEX = Regex("""[-+]?\s?\(?\$?\s?\d{1,3}(?:,\d{3})*\.\d{2}\)?-?""")
    private val DATE_REGEX = Regex(
        """\b\d{1,2}/\d{1,2}/\d{2,4}\b|\b\d{4}-\d{2}-\d{2}\b|\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{1,2},?\s*\d{0,4}\b""",
        RegexOption.IGNORE_CASE
    )

    private val BASE_INCOME_KEYWORDS = listOf(
        "deposit", "credit", "payment received", "payment - thank you", "refund", "interest paid",
        "interest earned", "dividend", "distribution", "payroll", "direct dep", "transfer from",
        "ach credit", "contribution", "reversal"
    )
    private val BASE_EXPENSE_KEYWORDS = listOf(
        "withdrawal", "purchase", "debit", "fee", "ach debit", "transfer to", "check card",
        "atm ", "advisory fee", "service charge", "overdraft"
    )

    // Section headings that imply every line under them is one direction, even though the
    // amount text itself carries no sign - matched only against lines with no dollar amount of
    // their own (i.e. actual headings, not transaction rows that happen to say "credit").
    private val INCOME_SECTION_HEADERS = listOf(
        "electronic credits", "deposits and additions", "deposits and credits",
        "deposits and other additions"
    )
    private val EXPENSE_SECTION_HEADERS = listOf(
        "electronic debits", "withdrawals and subtractions", "checks and debits",
        "withdrawals and debits", "withdrawals and other subtractions", "checks paid"
    )
    private val DAILY_BALANCE_HEADERS = listOf(
        "daily balance", "daily ledger balance", "average daily balance"
    )

    private const val MAX_RESULTS = 300
    private const val MIN_LINE_LENGTH = 4

    private fun incomeKeywordsFor(statementType: StatementType): List<String> =
        BASE_INCOME_KEYWORDS + when (statementType) {
            StatementType.BANK -> listOf("interest")
            StatementType.CREDIT_CARD -> listOf("statement credit")
            StatementType.INVESTMENT -> listOf("sell", "interest bank deposit")
        }

    private fun expenseKeywordsFor(statementType: StatementType): List<String> =
        BASE_EXPENSE_KEYWORDS + when (statementType) {
            StatementType.BANK -> emptyList()
            StatementType.CREDIT_CARD -> listOf("interest charge", "finance charge")
            StatementType.INVESTMENT -> listOf("buy")
        }

    /** True if the matched amount text itself carries a negative signal (leading/trailing minus,
     * or accounting-style parentheses). */
    private fun looksNegative(amountRaw: String): Boolean {
        val t = amountRaw.trim()
        return t.startsWith("-") || t.endsWith("-") || (t.startsWith("(") && t.endsWith(")"))
    }

    private fun looksPositive(amountRaw: String): Boolean = amountRaw.trim().startsWith("+")

    /** Picks which dollar-shaped match on a line is the "real" transaction amount when there's
     * more than one (quantity/price/amount style tables). A signed or parenthesized figure wins,
     * since that's almost always the actual net amount; otherwise falls back to the last match,
     * since running totals/line amounts are conventionally the rightmost column. */
    private fun pickAmount(matches: List<MatchResult>): MatchResult {
        val signed = matches.filter { looksNegative(it.value) || looksPositive(it.value) }
        return if (signed.isNotEmpty()) signed.last() else matches.last()
    }

    fun parse(statementText: String, statementType: StatementType): List<ParsedTransaction> {
        val incomeKeywords = incomeKeywordsFor(statementType)
        val expenseKeywords = expenseKeywordsFor(statementType)

        val results = mutableListOf<ParsedTransaction>()
        var lastSeenDate = ""
        var sectionHint: FinanceType? = null
        var skippingBalanceSection = false

        for (rawLine in statementText.lines()) {
            val line = rawLine.trim()
            if (line.length < MIN_LINE_LENGTH) continue

            DATE_REGEX.find(line)?.let { lastSeenDate = it.value }

            val allAmounts = AMOUNT_REGEX.findAll(line).toList()
            if (allAmounts.isEmpty()) {
                // No dollar amount on this line at all: it's either irrelevant text or a section
                // heading. Only headings change parsing state going forward.
                val lowerLine = line.lowercase()
                when {
                    DAILY_BALANCE_HEADERS.any { lowerLine.contains(it) } -> skippingBalanceSection = true
                    INCOME_SECTION_HEADERS.any { lowerLine.contains(it) } -> {
                        sectionHint = FinanceType.INCOME
                        skippingBalanceSection = false
                    }
                    EXPENSE_SECTION_HEADERS.any { lowerLine.contains(it) } -> {
                        sectionHint = FinanceType.EXPENSE
                        skippingBalanceSection = false
                    }
                }
                continue
            }

            if (skippingBalanceSection) continue
            if (results.size >= MAX_RESULTS) break

            val amountMatch = pickAmount(allAmounts)
            val amountRaw = amountMatch.value
            val isNegativeLooking = looksNegative(amountRaw)
            val isPositiveLooking = looksPositive(amountRaw)

            val cleanedAmount = amountRaw
                .replace("$", "").replace(",", "")
                .replace("(", "").replace(")", "")
                .removePrefix("-").removeSuffix("-").removePrefix("+")
                .trim()
            if (cleanedAmount.toDoubleOrNull() == null) continue

            val lowerLine = line.lowercase()
            val looksLikeIncome = when {
                isPositiveLooking -> true
                isNegativeLooking -> false
                incomeKeywords.any { lowerLine.contains(it) } -> true
                expenseKeywords.any { lowerLine.contains(it) } -> false
                sectionHint == FinanceType.INCOME -> true
                sectionHint == FinanceType.EXPENSE -> false
                // No sign, no keyword hit, no section heading seen yet: default to expense (a
                // credit card line with no other signal is almost always a purchase; for bank/
                // investment it's the safer guess since it's much easier for a user to notice and
                // flip a missed *deposit* than to notice everything was quietly counted as income).
                else -> false
            }

            val dateOnLine = DATE_REGEX.find(line)?.value ?: lastSeenDate
            val description = line
                .replace(amountMatch.value, "")
                .let { s -> if (dateOnLine.isNotEmpty()) s.replace(dateOnLine, "") else s }
                .trim(' ', '-', '|', '\t', ':')

            results += ParsedTransaction(
                include = true,
                type = if (looksLikeIncome) FinanceType.INCOME else FinanceType.EXPENSE,
                amount = cleanedAmount,
                date = dateOnLine,
                description = description
            )
        }
        return results
    }
}

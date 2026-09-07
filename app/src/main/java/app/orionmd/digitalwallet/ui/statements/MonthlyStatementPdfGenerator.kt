package app.orionmd.digitalwallet.ui.statements

import android.content.Context
import android.graphics.BitmapFactory
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.FinanceItem
import app.orionmd.digitalwallet.data.FinanceType
import app.orionmd.digitalwallet.data.displayTypeLabel
import app.orionmd.digitalwallet.data.HoldingItem
import app.orionmd.digitalwallet.data.HoldingType
import app.orionmd.digitalwallet.data.StatementItem
import app.orionmd.digitalwallet.data.StatementType
import app.orionmd.digitalwallet.data.gainLoss
import app.orionmd.digitalwallet.data.quantityValue
import app.orionmd.digitalwallet.data.totalValue
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds one multi-page "Finances at a Glance" consolidated statement PDF for a given month:
 * a summary (totals, by category, by source) followed by the full transaction detail. Uses
 * PdfBox-Android the same way the Top Notch Lock work-order app does for its PDFs, just without
 * that app's fixed one-page template - this one grows to however many pages the month needs.
 *
 * Table rows are drawn as real columns: each cell is placed at an explicit x-offset and
 * truncated to fit its column width (measured against the actual proportional font), rather
 * than padded with spaces - space-padding drifts out of alignment with a proportional font and
 * was the source of the "sloppy" columns this replaced. Every page also gets a faint logo
 * watermark in the top-right corner, matching the app's own branding.
 */
object MonthlyStatementPdfGenerator {

    private val PAGE_SIZE = PDRectangle.LETTER
    private const val MARGIN = 50f
    private const val LINE_HEIGHT = 14f
    private val CONTENT_WIDTH = PAGE_SIZE.width - 2 * MARGIN

    // Logo is 1120x955 - keep that aspect ratio at whatever watermark size we draw it.
    private const val LOGO_ASPECT = 1120f / 955f
    private const val WATERMARK_WIDTH = 64f
    private const val WATERMARK_HEIGHT = WATERMARK_WIDTH / LOGO_ASPECT
    private const val WATERMARK_INSET = 18f
    private const val WATERMARK_ALPHA = 0.16f

    private data class Cell(val text: String, val width: Float, val alignRight: Boolean = false)

    /** Returns the generated file, or null if there was nothing to report for [monthKey]. */
    suspend fun generate(
        context: Context,
        monthKey: String,
        allEntries: List<FinanceItem>,
        allStatements: List<StatementItem>,
        allHoldings: List<HoldingItem> = emptyList()
    ): File? {
        val statementMonthById = allStatements.associate { it.id to it.statementMonth }
        val monthEntries = allEntries.filter { entry ->
            val viaStatement = entry.statementId?.let { statementMonthById[it] == monthKey }
            viaStatement ?: (MonthUtil.keyFromFreeText(entry.date) == monthKey)
        }
        if (monthEntries.isEmpty()) return null
        val monthStatements = allStatements.filter { it.statementMonth == monthKey }

        val document = PDDocument()

        val logoImage: PDImageXObject? = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.logo_orionmd)
                ?.let { LosslessFactory.createFromImage(document, it) }
        }.getOrNull()
        val watermarkFadeIn = PDExtendedGraphicsState().apply { nonStrokingAlphaConstant = WATERMARK_ALPHA }
        val watermarkFadeOut = PDExtendedGraphicsState().apply { nonStrokingAlphaConstant = 1f }

        fun drawWatermark(cs: PDPageContentStream) {
            val img = logoImage ?: return
            runCatching {
                val x = PAGE_SIZE.width - WATERMARK_INSET - WATERMARK_WIDTH
                val yPos = PAGE_SIZE.height - WATERMARK_INSET - WATERMARK_HEIGHT
                cs.setGraphicsStateParameters(watermarkFadeIn)
                cs.drawImage(img, x, yPos, WATERMARK_WIDTH, WATERMARK_HEIGHT)
                cs.setGraphicsStateParameters(watermarkFadeOut)
            }
        }

        fun newPage(): Pair<PDPage, PDPageContentStream> {
            val pg = PDPage(PAGE_SIZE)
            document.addPage(pg)
            val cs = PDPageContentStream(document, pg)
            drawWatermark(cs)
            return pg to cs
        }

        var (page, stream) = newPage()
        var y = PAGE_SIZE.height - MARGIN

        fun ensureRoom() {
            if (y < MARGIN + LINE_HEIGHT) {
                stream.close()
                val created = newPage()
                page = created.first
                stream = created.second
                y = PAGE_SIZE.height - MARGIN
            }
        }

        fun widthOf(text: String, font: PDFont, size: Float): Float =
            font.getStringWidth(text) / 1000f * size

        fun truncate(text: String, font: PDFont, size: Float, maxWidth: Float): String {
            val clean = sanitizeForPdf(text)
            if (widthOf(clean, font, size) <= maxWidth) return clean
            var end = clean.length
            while (end > 0 && widthOf(clean.substring(0, end) + "..", font, size) > maxWidth) end--
            return if (end <= 0) "" else clean.substring(0, end) + ".."
        }

        fun wrapLines(text: String, font: PDFont, size: Float, maxWidth: Float): List<String> {
            val clean = sanitizeForPdf(text)
            // A leading indent (e.g. "  Freelance - income ...") is meaningful layout, not a
            // word boundary - splitting on every space would otherwise throw it away as a run of
            // empty tokens, so pull it off first and restore it onto the first wrapped line only.
            val indent = clean.takeWhile { it == ' ' }
            val words = clean.substring(indent.length).split(" ").filter { it.isNotEmpty() }
            val lines = mutableListOf<String>()
            var current = StringBuilder()
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (widthOf(candidate, font, size) > maxWidth && current.isNotEmpty()) {
                    lines.add(current.toString())
                    current = StringBuilder(word)
                } else {
                    current = StringBuilder(candidate)
                }
            }
            if (current.isNotEmpty()) lines.add(current.toString())
            if (lines.isEmpty()) lines.add("")
            lines[0] = indent + lines[0]
            return lines
        }

        fun line(text: String, font: PDFont = PDType1Font.HELVETICA, size: Float = 10f) {
            wrapLines(text, font, size, CONTENT_WIDTH).forEachIndexed { idx, subLine ->
                ensureRoom()
                val toDraw = if (idx == 0) subLine else "    $subLine"
                stream.beginText()
                stream.setFont(font, size)
                stream.newLineAtOffset(MARGIN, y)
                stream.showText(toDraw)
                stream.endText()
                y -= LINE_HEIGHT
            }
        }

        fun blank() {
            y -= LINE_HEIGHT / 2
        }

        fun tableRow(cells: List<Cell>, font: PDFont = PDType1Font.HELVETICA, size: Float = 8.5f) {
            ensureRoom()
            stream.beginText()
            stream.setFont(font, size)
            var x = MARGIN
            var penX = MARGIN
            stream.newLineAtOffset(x, y)
            cells.forEach { cell ->
                val fitted = truncate(cell.text, font, size, cell.width - 4f)
                val drawX = if (cell.alignRight) {
                    x + (cell.width - widthOf(fitted, font, size)) - 2f
                } else {
                    x
                }
                stream.newLineAtOffset(drawX - penX, 0f)
                stream.showText(fitted)
                penX = drawX
                x += cell.width
            }
            stream.endText()
            y -= LINE_HEIGHT
        }

        // --- Header ---
        line("Digital Wallet - Finances at a Glance", PDType1Font.HELVETICA_BOLD, 18f)
        line("Consolidated Statement - ${MonthUtil.label(monthKey)}", PDType1Font.HELVETICA, 12f)
        blank()

        // --- Summary ---
        val income = monthEntries.filter { it.type == FinanceType.INCOME }
        val expenses = monthEntries.filter { it.type == FinanceType.EXPENSE }
        val totalIncome = income.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val totalExpenses = expenses.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }

        line("SUMMARY", PDType1Font.HELVETICA_BOLD, 13f)
        line("Total income:    ${currency(totalIncome)}")
        line("Total expenses:  ${currency(totalExpenses)}")
        line("Net:             ${currency(totalIncome - totalExpenses)}")
        blank()

        // --- Portfolio (current holdings snapshot - not month-specific, but included in every
        // consolidated statement the same way a brokerage statement lists current positions
        // alongside that period's activity) ---
        if (allHoldings.isNotEmpty()) {
            val asOf = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
            line("PORTFOLIO (holdings as of $asOf)", PDType1Font.HELVETICA_BOLD, 13f)

            val symbolW = 60f
            val typeW = 55f
            val qtyW = 65f
            val priceW = 75f
            val valueW = 80f
            val gainW = 80f

            tableRow(
                listOf(
                    Cell("Symbol", symbolW), Cell("Type", typeW), Cell("Qty", qtyW, alignRight = true),
                    Cell("Price", priceW, alignRight = true), Cell("Value", valueW, alignRight = true),
                    Cell("Gain/Loss", gainW, alignRight = true)
                ),
                font = PDType1Font.HELVETICA_BOLD
            )
            allHoldings.sortedBy { it.symbol.uppercase(Locale.US) }.forEach { holding ->
                val value = holding.totalValue()
                val gainLoss = holding.gainLoss()
                tableRow(
                    listOf(
                        Cell(holding.symbol, symbolW),
                        Cell(if (holding.holdingType == HoldingType.CRYPTO) "Crypto" else if (holding.holdingType == HoldingType.CASH) "Cash" else "Stock", typeW),
                        Cell(formatQty(holding.quantityValue()), qtyW, alignRight = true),
                        Cell(holding.currentPrice.toDoubleOrNull()?.let { currency(it) } ?: "-", priceW, alignRight = true),
                        Cell(value?.let { currency(it) } ?: "-", valueW, alignRight = true),
                        Cell(gainLoss?.let { (if (it >= 0) "+" else "-") + currency(kotlin.math.abs(it)) } ?: "not tracked", gainW, alignRight = true)
                    )
                )
            }
            blank()
            val totalPortfolioValue = allHoldings.sumOf { it.totalValue() ?: 0.0 }
            line("Total portfolio value: ${currency(totalPortfolioValue)}")
            val trackedGainLoss = allHoldings.mapNotNull { it.gainLoss() }
            if (trackedGainLoss.isNotEmpty()) {
                val total = trackedGainLoss.sum()
                line("Total gain/loss:       ${if (total >= 0) "+" else "-"}${currency(kotlin.math.abs(total))}")
            }
            blank()
        }

        line("By category", PDType1Font.HELVETICA_BOLD, 12f)
        monthEntries.groupBy { it.category.ifBlank { "Uncategorized" } }.toSortedMap().forEach { (category, catEntries) ->
            val catIncome = catEntries.filter { it.type == FinanceType.INCOME }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
            val catExpense = catEntries.filter { it.type == FinanceType.EXPENSE }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
            line("  $category - income ${currency(catIncome)}, expenses ${currency(catExpense)}", size = 9.5f)
        }
        blank()

        line("By source / account", PDType1Font.HELVETICA_BOLD, 12f)
        monthEntries.groupBy { it.source.ifBlank { "(no source)" } }.toSortedMap().forEach { (source, srcEntries) ->
            val srcIncome = srcEntries.filter { it.type == FinanceType.INCOME }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
            val srcExpense = srcEntries.filter { it.type == FinanceType.EXPENSE }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
            line("  $source - income ${currency(srcIncome)}, expenses ${currency(srcExpense)}", size = 9.5f)
        }
        blank()

        // --- Full transaction detail ---
        line("TRANSACTION DETAIL (${monthEntries.size})", PDType1Font.HELVETICA_BOLD, 13f)

        val dateW = 62f
        val typeW2 = 48f
        val amountW = 68f
        val categoryW = 90f
        val sourceW = CONTENT_WIDTH - dateW - typeW2 - amountW - categoryW

        tableRow(
            listOf(
                Cell("Date", dateW), Cell("Type", typeW2), Cell("Amount", amountW, alignRight = true),
                Cell("Category", categoryW), Cell("Source / description", sourceW)
            ),
            font = PDType1Font.HELVETICA_BOLD
        )
        monthEntries.sortedBy { it.date }.forEach { entry ->
            val sign = if (entry.type == FinanceType.INCOME) "+" else "-"
            tableRow(
                listOf(
                    Cell(entry.date.take(10), dateW),
                    Cell(entry.displayTypeLabel("Income", "Expense"), typeW2),
                    Cell("$sign$${entry.amount}", amountW, alignRight = true),
                    Cell(entry.category, categoryW),
                    Cell(entry.notes.ifBlank { entry.source }, sourceW)
                )
            )
        }

        // --- Source statements ---
        if (monthStatements.isNotEmpty()) {
            blank()
            line("SOURCE STATEMENTS", PDType1Font.HELVETICA_BOLD, 13f)
            monthStatements.forEach { statement ->
                line("  ${statement.institution} (${typeLabel(statement.type)}) - ${statement.sourceDisplayName}", size = 9f)
                if (statement.beginningBalance.isNotBlank() || statement.endingBalance.isNotBlank()) {
                    val begin = statement.beginningBalance.toDoubleOrNull()?.let { currency(it) }
                        ?: statement.beginningBalance.ifBlank { "?" }
                    val end = statement.endingBalance.toDoubleOrNull()?.let { currency(it) }
                        ?: statement.endingBalance.ifBlank { "?" }
                    line("    Balance: $begin -> $end", size = 9f)
                }
            }
        }

        stream.close()

        val outDir = File(context.cacheDir, "statements_pdf").apply { if (!exists()) mkdirs() }
        val outFile = File(outDir, "finances-at-a-glance-$monthKey.pdf")
        document.save(outFile)
        document.close()
        return outFile
    }

    private fun typeLabel(type: StatementType) = when (type) {
        StatementType.BANK -> "Bank"
        StatementType.CREDIT_CARD -> "Credit Card"
        StatementType.INVESTMENT -> "Investment"
    }

    private fun currency(value: Double) = String.format(Locale.US, "$%,.2f", value)

    /** Quantities aren't always whole numbers (fractional shares, crypto) but shouldn't print a
     * pile of trailing zeros either. */
    private fun formatQty(value: Double): String = if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%,.4f", value).trimEnd('0').trimEnd('.')
    }

    /** Standard PDF fonts (Helvetica etc.) only support WinAnsi-range characters - strip
     * anything outside that so a stray OCR artifact or unicode symbol can't crash the render. */
    private fun sanitizeForPdf(text: String): String = text.map { c ->
        if (c.code in 32..126) c else '?'
    }.joinToString("")
}

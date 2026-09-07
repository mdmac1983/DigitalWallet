package app.orionmd.digitalwallet.ui.finances

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.FinanceItem
import app.orionmd.digitalwallet.data.FinanceRepository
import app.orionmd.digitalwallet.data.FinanceType
import app.orionmd.digitalwallet.data.HoldingItem
import app.orionmd.digitalwallet.data.HoldingRepository
import app.orionmd.digitalwallet.data.StatementRepository
import app.orionmd.digitalwallet.data.gainLoss
import app.orionmd.digitalwallet.data.totalValue
import app.orionmd.digitalwallet.databinding.FragmentFinancesBinding
import app.orionmd.digitalwallet.ui.statements.MonthUtil
import app.orionmd.digitalwallet.ui.statements.MonthlyStatementPdfGenerator
import app.orionmd.digitalwallet.ui.statements.StatementImportActivity
import app.orionmd.digitalwallet.ui.statements.StatementsListActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Tab 4: "Finances at a Glance" (Income and Expenses), fed either by manual entry or by
 * importing a statement - a bank/credit-card/investment PDF, or a photo of one - which is OCR'd
 * and heuristically parsed into candidate transactions (see [StatementImportActivity]) that the
 * user reviews before anything is saved. */
class FinancesFragment : Fragment() {

    private var _binding: FragmentFinancesBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: FinanceRepository
    private lateinit var statementRepository: StatementRepository
    private lateinit var holdingRepository: HoldingRepository
    private lateinit var adapter: FinanceAdapter
    private lateinit var holdingAdapter: HoldingAdapter

    /** Whether the Portfolio holdings list is currently shown - collapsing it is how the section
     * stays out of the way of the Import/Statements/PDF buttons and finance entries below it when
     * there are enough holdings to otherwise push those off screen. Resets to expanded each time
     * this view is (re)created, same as any other transient UI state here. */
    private var portfolioExpanded = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFinancesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = FinanceRepository(requireContext())
        statementRepository = StatementRepository(requireContext())
        holdingRepository = HoldingRepository(requireContext())

        adapter = FinanceAdapter(requireContext()) { entry ->
            startActivity(
                Intent(requireContext(), FinanceEditActivity::class.java)
                    .putExtra(FinanceEditActivity.EXTRA_FINANCE_ID, entry.id)
            )
        }
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        holdingAdapter = HoldingAdapter(requireContext()) { holding ->
            startActivity(
                Intent(requireContext(), HoldingEditActivity::class.java)
                    .putExtra(HoldingEditActivity.EXTRA_HOLDING_ID, holding.id)
            )
        }
        binding.listHoldings.layoutManager = LinearLayoutManager(requireContext())
        binding.listHoldings.adapter = holdingAdapter
        binding.listHoldings.isNestedScrollingEnabled = false

        binding.fab.setOnClickListener {
            startActivity(Intent(requireContext(), FinanceEditActivity::class.java))
        }
        binding.buttonAddHolding.setOnClickListener {
            startActivity(Intent(requireContext(), HoldingEditActivity::class.java))
        }
        binding.buttonTogglePortfolio.setOnClickListener {
            portfolioExpanded = !portfolioExpanded
            applyPortfolioExpanded()
        }
        applyPortfolioExpanded()
        binding.buttonImportStatement.setOnClickListener {
            startActivity(Intent(requireContext(), StatementImportActivity::class.java))
        }
        binding.buttonStatements.setOnClickListener {
            startActivity(Intent(requireContext(), StatementsListActivity::class.java))
        }
        binding.buttonMonthlyPdf.setOnClickListener { showMonthPickerForPdf() }

        viewLifecycleOwner.lifecycleScope.launch {
            repository.observeAll().collect { entries ->
                adapter.submitList(entries)
                binding.emptyState.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
                updateSummary(entries)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            holdingRepository.observeAll().collect { holdings ->
                holdingAdapter.submitList(holdings)
                binding.textHoldingsEmpty.visibility = if (holdings.isEmpty()) View.VISIBLE else View.GONE
                binding.listHoldings.visibility = if (holdings.isEmpty()) View.GONE else View.VISIBLE
                updatePortfolioSummary(holdings)
            }
        }
    }

    private fun applyPortfolioExpanded() {
        binding.groupHoldingsContent.visibility = if (portfolioExpanded) View.VISIBLE else View.GONE
        binding.buttonTogglePortfolio.rotation = if (portfolioExpanded) 0f else -90f
    }

    private fun updatePortfolioSummary(holdings: List<HoldingItem>) {
        val totalValue = holdings.sumOf { it.totalValue() ?: 0.0 }
        binding.textPortfolioValue.text = formatCurrency(totalValue)

        val tracked = holdings.mapNotNull { it.gainLoss() }
        if (tracked.isEmpty()) {
            binding.textPortfolioGainLoss.text = getString(R.string.gain_loss_not_tracked)
            binding.textPortfolioGainLoss.setTextColor(
                requireContext().getColor(R.color.wallet_divider)
            )
        } else {
            val totalGainLoss = tracked.sum()
            binding.textPortfolioGainLoss.text =
                (if (totalGainLoss >= 0) "+" else "-") + formatCurrency(kotlin.math.abs(totalGainLoss))
            binding.textPortfolioGainLoss.setTextColor(
                requireContext().getColor(if (totalGainLoss >= 0) R.color.wallet_income else R.color.wallet_expense)
            )
        }
    }

    private fun updateSummary(entries: List<FinanceItem>) {
        val income = entries.filter { it.type == FinanceType.INCOME }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val expenses = entries.filter { it.type == FinanceType.EXPENSE }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        binding.textTotalIncome.text = formatCurrency(income)
        binding.textTotalExpenses.text = formatCurrency(expenses)
        binding.textTotalNet.text = formatCurrency(income - expenses)
    }

    private fun formatCurrency(value: Double): String = String.format(Locale.US, "$%,.2f", value)

    private fun showMonthPickerForPdf() {
        val months = MonthUtil.recentMonths()
        val labels = months.map { it.second }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.monthly_pdf_pick_month_title)
            .setItems(labels) { _, which -> generateMonthlyPdf(months[which].first) }
            .show()
    }

    private fun generateMonthlyPdf(monthKey: String) {
        val context = requireContext()
        Toast.makeText(context, R.string.monthly_pdf_generating, Toast.LENGTH_SHORT).show()
        viewLifecycleOwner.lifecycleScope.launch {
            val allEntries = repository.observeAll().first()
            val allStatements = statementRepository.observeAll().first()
            val allHoldings = holdingRepository.observeAll().first()
            val file = withContext(Dispatchers.Default) {
                MonthlyStatementPdfGenerator.generate(context, monthKey, allEntries, allStatements, allHoldings)
            }
            if (file == null) {
                Toast.makeText(context, R.string.monthly_pdf_none_found, Toast.LENGTH_LONG).show()
            } else {
                openGeneratedPdf(file)
            }
        }
    }

    /** [DocumentStore.openExternally] expects an encrypted-store filename, but the generated PDF
     * is already a plain file on disk (nothing sensitive beyond what's already decrypted for this
     * session), so it gets its own tiny FileProvider share/open step here instead. */
    private fun openGeneratedPdf(file: java.io.File) {
        val context = requireContext()
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "app.orionmd.digitalwallet.fileprovider", file
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(viewIntent, file.name))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

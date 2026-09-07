package app.orionmd.digitalwallet.ui.sales

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.orionmd.digitalwallet.data.SaleItem
import app.orionmd.digitalwallet.data.SaleRepository
import app.orionmd.digitalwallet.data.balanceDue
import app.orionmd.digitalwallet.data.netFee
import app.orionmd.digitalwallet.databinding.FragmentSalesBinding
import app.orionmd.digitalwallet.ui.common.ListOptionsMenu
import app.orionmd.digitalwallet.ui.common.ViewMode
import app.orionmd.digitalwallet.ui.common.ViewModePrefs
import kotlinx.coroutines.launch
import java.util.Locale

/** Tab 5: services sold to clients - date, client ID, service, price, commission/partner fees,
 * and a paid/unpaid status. Net fee (price minus both fees) is always computed, never stored. */
class SalesFragment : Fragment() {

    private var _binding: FragmentSalesBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: SaleRepository
    private lateinit var adapter: SaleAdapter
    private var viewMode: ViewMode = ViewMode.TILES

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = SaleRepository(requireContext())
        viewMode = ViewModePrefs.get(requireContext(), ViewModePrefs.KEY_SALES)

        adapter = SaleAdapter(requireContext()) { sale ->
            startActivity(
                Intent(requireContext(), SaleEditActivity::class.java)
                    .putExtra(SaleEditActivity.EXTRA_SALE_ID, sale.id)
            )
        }
        adapter.setViewMode(viewMode)
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        binding.buttonViewMode.setOnClickListener {
            ListOptionsMenu.showViewMode(it, viewMode) { mode ->
                viewMode = mode
                ViewModePrefs.set(requireContext(), ViewModePrefs.KEY_SALES, mode)
                adapter.setViewMode(mode)
            }
        }

        binding.fab.setOnClickListener {
            startActivity(Intent(requireContext(), SaleEditActivity::class.java))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repository.observeAll().collect { sales ->
                // Display-only entry numbers, "(01)", "(02)"... always by creation order so a
                // sale's number never shifts just because something got edited (the list itself
                // stays sorted most-recently-updated-first, per SaleDao.getAll()).
                val numbering = sales.sortedBy { it.createdAt }
                    .mapIndexed { index, sale -> sale.id to (index + 1) }
                    .toMap()
                adapter.submitList(sales, numbering)
                binding.emptyState.visibility = if (sales.isEmpty()) View.VISIBLE else View.GONE
                updateSummary(sales)
            }
        }
    }

    private fun updateSummary(sales: List<SaleItem>) {
        val gross = sales.sumOf { it.price.toDoubleOrNull() ?: 0.0 }
        val net = sales.sumOf { it.netFee() }
        val unpaid = sales.sumOf { it.balanceDue() }

        binding.textTotalGross.text = formatCurrency(gross)
        binding.textTotalNet.text = formatCurrency(net)
        binding.textTotalUnpaid.text = formatCurrency(unpaid)
    }

    private fun formatCurrency(value: Double): String = String.format(Locale.US, "$%,.2f", value)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

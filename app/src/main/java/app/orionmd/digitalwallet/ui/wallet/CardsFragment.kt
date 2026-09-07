package app.orionmd.digitalwallet.ui.wallet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import app.orionmd.digitalwallet.data.CardRepository
import app.orionmd.digitalwallet.databinding.FragmentListWithFabBinding
import app.orionmd.digitalwallet.ui.common.DragReorderTouchHelperCallback
import app.orionmd.digitalwallet.ui.common.ListOptionsMenu
import app.orionmd.digitalwallet.ui.common.ViewMode
import app.orionmd.digitalwallet.ui.common.ViewModePrefs
import kotlinx.coroutines.launch

class CardsFragment : Fragment() {

    private var _binding: FragmentListWithFabBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: CardRepository
    private lateinit var adapter: CardAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var viewMode: ViewMode = ViewMode.TILES

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListWithFabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = CardRepository(requireContext())
        viewMode = ViewModePrefs.get(requireContext(), ViewModePrefs.KEY_CARDS)

        adapter = CardAdapter(
            requireContext(),
            onClick = { card ->
                startActivity(
                    Intent(requireContext(), CardEditActivity::class.java)
                        .putExtra(CardEditActivity.EXTRA_CARD_ID, card.id)
                )
            },
            onStartDrag = { holder -> itemTouchHelper.startDrag(holder) }
        )
        adapter.setViewMode(viewMode)
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        binding.buttonViewMode.setOnClickListener {
            ListOptionsMenu.showViewMode(it, viewMode) { mode ->
                viewMode = mode
                ViewModePrefs.set(requireContext(), ViewModePrefs.KEY_CARDS, mode)
                adapter.setViewMode(mode)
            }
        }

        itemTouchHelper = ItemTouchHelper(
            DragReorderTouchHelperCallback(
                onMove = { from, to -> adapter.moveItem(from, to) },
                onDragFinished = {
                    viewLifecycleOwner.lifecycleScope.launch {
                        repository.reorder(adapter.currentItems().map { it.id })
                    }
                }
            )
        )
        itemTouchHelper.attachToRecyclerView(binding.list)

        binding.fab.setOnClickListener {
            startActivity(Intent(requireContext(), CardEditActivity::class.java))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repository.observeAll().collect { cards ->
                adapter.submitList(cards)
                binding.emptyState.visibility = if (cards.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

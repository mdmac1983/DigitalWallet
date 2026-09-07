package app.orionmd.digitalwallet.ui.passwords

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import app.orionmd.digitalwallet.data.PasswordItem
import app.orionmd.digitalwallet.data.PasswordRepository
import app.orionmd.digitalwallet.databinding.FragmentListWithFabBinding
import app.orionmd.digitalwallet.ui.common.DragReorderTouchHelperCallback
import app.orionmd.digitalwallet.ui.common.ListOptionsMenu
import app.orionmd.digitalwallet.ui.common.SortMode
import app.orionmd.digitalwallet.ui.common.SortModePrefs
import app.orionmd.digitalwallet.ui.common.ViewMode
import app.orionmd.digitalwallet.ui.common.ViewModePrefs
import kotlinx.coroutines.launch

/** Tab 2: password bank for email, social media, website, banking and investment logins. */
class PasswordsFragment : Fragment() {

    private var _binding: FragmentListWithFabBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: PasswordRepository
    private lateinit var adapter: PasswordAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    private var viewMode: ViewMode = ViewMode.TILES
    private var sortMode: SortMode = SortMode.MANUAL
    private var latestEntries: List<PasswordItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListWithFabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = PasswordRepository(requireContext())
        viewMode = ViewModePrefs.get(requireContext(), ViewModePrefs.KEY_PASSWORDS)
        sortMode = SortModePrefs.get(requireContext(), SortModePrefs.KEY_PASSWORDS)

        adapter = PasswordAdapter(
            requireContext(),
            onClick = { entry ->
                startActivity(
                    Intent(requireContext(), PasswordEditActivity::class.java)
                        .putExtra(PasswordEditActivity.EXTRA_PASSWORD_ID, entry.id)
                )
            },
            onStartDrag = { holder -> itemTouchHelper.startDrag(holder) }
        )
        adapter.setViewMode(viewMode)
        adapter.setDragEnabled(sortMode == SortMode.MANUAL)
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        itemTouchHelper = ItemTouchHelper(
            DragReorderTouchHelperCallback(
                onMove = { from, to -> adapter.moveItem(from, to) },
                onDragFinished = {
                    if (sortMode == SortMode.MANUAL) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            repository.reorder(adapter.currentItems().map { it.id })
                        }
                    }
                }
            )
        )
        itemTouchHelper.attachToRecyclerView(binding.list)

        binding.buttonViewMode.setOnClickListener {
            ListOptionsMenu.showViewMode(it, viewMode) { mode ->
                viewMode = mode
                ViewModePrefs.set(requireContext(), ViewModePrefs.KEY_PASSWORDS, mode)
                adapter.setViewMode(mode)
            }
        }
        binding.buttonSort.visibility = View.VISIBLE
        binding.buttonSort.setOnClickListener {
            ListOptionsMenu.showSortMode(it, sortMode) { mode ->
                sortMode = mode
                SortModePrefs.set(requireContext(), SortModePrefs.KEY_PASSWORDS, mode)
                adapter.setDragEnabled(mode == SortMode.MANUAL)
                applySort()
            }
        }

        binding.fab.setOnClickListener {
            startActivity(Intent(requireContext(), PasswordEditActivity::class.java))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repository.observeAll().collect { entries ->
                latestEntries = entries
                applySort()
                binding.emptyState.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    /** Entries come from the repository already in Manual (persisted sortOrder) order;
     * Alphabetical/Type are computed here since the fields they sort by are encrypted at rest. */
    private fun applySort() {
        val sorted = when (sortMode) {
            SortMode.ALPHABETICAL -> latestEntries.sortedBy { it.accountName.lowercase() }
            SortMode.TYPE -> latestEntries.sortedWith(
                compareBy({ it.category.ordinal }, { it.accountName.lowercase() })
            )
            SortMode.MANUAL -> latestEntries
        }
        adapter.submitList(sorted)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package app.orionmd.digitalwallet.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.databinding.FragmentWalletBinding
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Tab 1 ("Wallet"). Hosts two sub-tabs: Cards (credit/debit cards + linked website login) and
 * IDs (driver's license, state ID, Social Security card, passport).
 */
class WalletFragment : Fragment() {

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.walletPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment =
                if (position == 0) CardsFragment() else IdsFragment()
        }

        TabLayoutMediator(binding.walletSubTabs, binding.walletPager) { tab, position ->
            tab.text = if (position == 0) getString(R.string.wallet_subtab_cards) else getString(R.string.wallet_subtab_ids)
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

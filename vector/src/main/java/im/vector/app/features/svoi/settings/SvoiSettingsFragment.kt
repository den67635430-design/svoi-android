/*
 * СВОи Settings Fragment — Центр управления СВОи
 */
package im.vector.app.features.svoi.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.databinding.FragmentSvoiSettingsBinding
import im.vector.app.features.svoi.SvoiConfig
import im.vector.app.features.svoi.donate.DonateBottomSheet
import im.vector.app.features.svoi.subscription.SubscriptionFragment
import im.vector.app.features.svoi.subscription.SubscriptionManager
import javax.inject.Inject

@AndroidEntryPoint
class SvoiSettingsFragment : Fragment() {

    private var _binding: FragmentSvoiSettingsBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var subscriptionManager: SubscriptionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSvoiSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isAdmin = subscriptionManager.isAdmin()
        binding.adminBadge.isVisible = isAdmin
        binding.demoBadge.isVisible = SvoiConfig.DEMO_MODE && !isAdmin

        binding.currentTierLabel.text = "Текущая подписка: ${subscriptionManager.getCurrentTier().displayName}"

        binding.buttonSubscriptions.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, SubscriptionFragment.newInstance())
                .addToBackStack("subscription")
                .commit()
        }

        binding.buttonDonate.setOnClickListener {
            DonateBottomSheet.newInstance().show(parentFragmentManager, DonateBottomSheet.TAG)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SvoiSettingsFragment()
    }
}

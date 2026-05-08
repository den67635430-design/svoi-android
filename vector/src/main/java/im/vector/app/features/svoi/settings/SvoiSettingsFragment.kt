/*
 * СВОи Settings Fragment — Центр управления СВОи
 */
package im.vector.app.features.svoi.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.databinding.FragmentSvoiSettingsBinding
import im.vector.app.features.svoi.SvoiConfig
import im.vector.app.features.svoi.donate.DonateBottomSheet
import im.vector.app.features.svoi.subscription.SubscriptionFragment
import im.vector.app.features.svoi.subscription.SubscriptionManager
import im.vector.app.features.svoi.subscription.TestModeManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SvoiSettingsFragment : Fragment() {

    private var _binding: FragmentSvoiSettingsBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var subscriptionManager: SubscriptionManager
    @Inject lateinit var testModeManager: TestModeManager
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSvoiSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isAdmin = isMatrixAdmin() || subscriptionManager.isAdmin()
        binding.adminBadge.isVisible = isAdmin

        // Обновляем кеш TestMode из API при открытии настроек
        lifecycleScope.launch {
            testModeManager.refresh()
            renderTestModeUi(isAdmin)
        }
        renderTestModeUi(isAdmin)

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

        // SVOi: переключатель тестового режима — только для админов
        binding.adminTestModeSwitch.setOnCheckedChangeListener { _, _ ->
            // ничего не делаем здесь — toggle только по клику пользователя
        }
        binding.adminTestModeSwitch.setOnClickListener {
            val newValue = binding.adminTestModeSwitch.isChecked
            val token = activeSessionHolder.getSafeActiveSession()?.sessionParams?.credentials?.accessToken
            if (token.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Нет активной сессии Synapse", Toast.LENGTH_SHORT).show()
                binding.adminTestModeSwitch.isChecked = !newValue
                return@setOnClickListener
            }
            binding.adminTestModeSwitch.isEnabled = false
            lifecycleScope.launch {
                val current = testModeManager.isEnabled()
                if (current == newValue) {
                    binding.adminTestModeSwitch.isEnabled = true
                    return@launch
                }
                val result = testModeManager.toggle(token)
                binding.adminTestModeSwitch.isEnabled = true
                if (result == null) {
                    Toast.makeText(requireContext(), "Не удалось переключить — проверьте права админа", Toast.LENGTH_LONG).show()
                    binding.adminTestModeSwitch.isChecked = !newValue
                } else {
                    val msg = if (result) "Тестовый режим ВКЛ — все платные функции бесплатны"
                              else "Тестовый режим ВЫКЛ — платные функции снова платные"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    renderTestModeUi(true)
                    binding.currentTierLabel.text = "Текущая подписка: ${subscriptionManager.getCurrentTier().displayName}"
                }
            }
        }
    }

    private fun renderTestModeUi(isAdmin: Boolean) {
        val enabled = testModeManager.isEnabled()
        binding.adminTestModeBlock.isVisible = isAdmin
        binding.demoBadge.isVisible = enabled && !isAdmin
        binding.adminTestModeSwitch.isChecked = enabled
        binding.testModeStatusLabel.text = if (enabled) {
            "Сейчас: ВКЛ — все платные функции бесплатны для всех пользователей"
        } else {
            "Сейчас: ВЫКЛ — пользователи без подписки видят ограничения"
        }
    }

    /** Проверяет является ли текущий Matrix-юзер админом сервера. */
    private fun isMatrixAdmin(): Boolean {
        val userId = activeSessionHolder.getSafeActiveSession()?.myUserId ?: return false
        return userId in ADMIN_MATRIX_IDS
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SvoiSettingsFragment()

        private val ADMIN_MATRIX_IDS = setOf(
                "@denis:svo.kodkontenta.ru",
                "@admin:svo.kodkontenta.ru",
        )
    }
}

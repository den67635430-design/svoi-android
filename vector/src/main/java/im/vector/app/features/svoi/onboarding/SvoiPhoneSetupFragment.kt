/*
 * SVOi: экран ввода номера телефона.
 * Запускается автоматически при первом старте после регистрации.
 * Сохраняет номер в Matrix-профиль через /api/users/me/phone.
 */
package im.vector.app.features.svoi.onboarding

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.svoi.api.SvoiPhoneApi
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SvoiPhoneSetupFragment : Fragment() {

    @Inject lateinit var activeSessionHolder: ActiveSessionHolder

    private lateinit var phoneInput: EditText
    private lateinit var saveButton: Button
    private lateinit var skipButton: Button
    private lateinit var errorLabel: TextView
    private lateinit var progressBar: ProgressBar

    interface Listener { fun onPhoneSetupComplete() }
    private var listener: Listener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = (parentFragment as? Listener) ?: (activity as? Listener)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_svoi_phone_setup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        phoneInput = view.findViewById(R.id.phoneInput)
        saveButton = view.findViewById(R.id.saveButton)
        skipButton = view.findViewById(R.id.skipButton)
        errorLabel = view.findViewById(R.id.errorLabel)
        progressBar = view.findViewById(R.id.progressBar)

        saveButton.setOnClickListener { savePhone() }
        skipButton.setOnClickListener {
            markCompleted(skipped = true)
            listener?.onPhoneSetupComplete()
        }
    }

    private fun savePhone() {
        val raw = phoneInput.text?.toString().orEmpty().trim()
        val digits = raw.filter { it.isDigit() }
        if (digits.length < 10 || digits.length > 15) {
            showError("Введите корректный номер с кодом страны (например, +7 999 123 45 67)")
            return
        }

        val token = activeSessionHolder.getSafeActiveSession()?.sessionParams?.credentials?.accessToken
        if (token.isNullOrBlank()) {
            showError("Сессия не активна. Войдите снова.")
            return
        }

        hideKeyboard()
        setBusy(true)
        errorLabel.isVisible = false

        lifecycleScope.launch {
            val ok = SvoiPhoneApi.setPhone(token, raw)
            setBusy(false)
            if (ok) {
                Toast.makeText(requireContext(), "Номер сохранён. Ищем ваших друзей в СВОи…", Toast.LENGTH_LONG).show()
                markCompleted(skipped = false)
                listener?.onPhoneSetupComplete()
            } else {
                showError("Не удалось сохранить номер. Попробуйте ещё раз.")
            }
        }
    }

    private fun markCompleted(skipped: Boolean) {
        val session = activeSessionHolder.getSafeActiveSession() ?: return
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(prefKey(session.myUserId), true)
                .putBoolean(prefKey(session.myUserId) + "_skipped", skipped)
                .apply()
    }

    private fun setBusy(busy: Boolean) {
        saveButton.isEnabled = !busy
        skipButton.isEnabled = !busy
        phoneInput.isEnabled = !busy
        progressBar.isVisible = busy
    }

    private fun showError(msg: String) {
        errorLabel.text = msg
        errorLabel.isVisible = true
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    companion object {
        private const val PREFS_NAME = "svoi_onboarding"
        private fun prefKey(userId: String) = "phone_setup_done_$userId"

        fun newInstance() = SvoiPhoneSetupFragment()

        /** Был ли уже показан экран phone setup для этого юзера. */
        fun isCompleted(context: Context, userId: String): Boolean =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getBoolean(prefKey(userId), false)
    }
}

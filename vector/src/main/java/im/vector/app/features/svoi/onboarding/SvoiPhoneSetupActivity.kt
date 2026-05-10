/*
 * SVOi onboarding: один раз после регистрации показать экран ввода телефона.
 */
package im.vector.app.features.svoi.onboarding

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R

@AndroidEntryPoint
class SvoiPhoneSetupActivity : AppCompatActivity(), SvoiPhoneSetupFragment.Listener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_svoi_phone_setup)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.svoiPhoneSetupContainer, SvoiPhoneSetupFragment.newInstance())
                    .commit()
        }
        // Не даём закрыть экран кнопкой Назад — нужен setup или "Пропустить"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* swallow */ }
        })
    }

    override fun onPhoneSetupComplete() {
        finish()
    }
}

/*
 * SVOi onboarding: один раз после регистрации показать экран ввода телефона.
 */
package im.vector.app.features.svoi.onboarding

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R

@AndroidEntryPoint
class SvoiPhoneSetupActivity : AppCompatActivity(), SvoiPhoneSetupFragment.Listener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Используем готовый контейнер simple_fragment если есть — иначе создаём программно
        setContentView(R.layout.activity_svoi_phone_setup)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.svoiPhoneSetupContainer, SvoiPhoneSetupFragment.newInstance())
                    .commit()
        }
    }

    override fun onPhoneSetupComplete() {
        finish()
    }

    override fun onBackPressed() {
        // Не даём закрыть экран кнопкой Назад — должен пройти setup или нажать "Пропустить"
    }
}

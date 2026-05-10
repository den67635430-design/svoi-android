/*
 * SVOi: обёртка-Activity для SvoiSettingsFragment.
 * Открывается из FAB на главном экране (только админ).
 */
package im.vector.app.features.svoi.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R

@AndroidEntryPoint
class SvoiSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_svoi_settings)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.svoiSettingsContainer, SvoiSettingsFragment.newInstance())
                    .commit()
        }
    }

    companion object {
        fun getIntent(ctx: Context) = Intent(ctx, SvoiSettingsActivity::class.java)
    }
}

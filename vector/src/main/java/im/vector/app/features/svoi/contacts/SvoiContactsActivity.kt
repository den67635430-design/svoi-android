/*
 * SVOi: автоматическая синхронизация телефонной книги.
 * Показывает все контакты с разделением:
 *   - "В СВОи" — кто зарегистрирован (есть Matrix ID), тап → открыть чат
 *   - "Пригласить" — кто не зарегистрирован, тап → отправить SMS / share invite link
 *
 * При первом запуске вызывается из HomeActivity (после phone setup).
 */
package im.vector.app.features.svoi.contacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.contacts.ContactsDataSource
import im.vector.app.core.contacts.MappedContact
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.createdirect.CreateDirectRoomActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.identity.ThreePid
import javax.inject.Inject

@AndroidEntryPoint
class SvoiContactsActivity : AppCompatActivity() {

    @Inject lateinit var contactsDataSource: ContactsDataSource
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder

    private lateinit var recycler: RecyclerView
    private lateinit var emptyState: View
    private lateinit var loadingState: View
    private lateinit var permissionState: View
    private lateinit var grantButton: View
    private lateinit var refreshButton: View

    private val adapter = SvoiContactsAdapter(::onContactClicked)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_svoi_contacts)

        recycler = findViewById(R.id.svoiContactsRecycler)
        emptyState = findViewById(R.id.svoiContactsEmpty)
        loadingState = findViewById(R.id.svoiContactsLoading)
        permissionState = findViewById(R.id.svoiContactsNoPermission)
        grantButton = findViewById(R.id.svoiContactsGrantButton)
        refreshButton = findViewById(R.id.svoiContactsRefreshButton)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        grantButton.setOnClickListener { requestContactsPermission() }
        refreshButton.setOnClickListener { loadContacts() }

        findViewById<View>(R.id.svoiContactsBack).setOnClickListener { finish() }

        if (hasContactsPermission()) loadContacts() else showPermissionRequest()
    }

    override fun onResume() {
        super.onResume()
        if (hasContactsPermission() && permissionState.visibility == View.VISIBLE) loadContacts()
    }

    private fun hasContactsPermission(): Boolean = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    private val permLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) loadContacts() }

    private fun requestContactsPermission() {
        permLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    private fun showPermissionRequest() {
        permissionState.visibility = View.VISIBLE
        loadingState.visibility = View.GONE
        recycler.visibility = View.GONE
        emptyState.visibility = View.GONE
    }

    private fun loadContacts() {
        permissionState.visibility = View.GONE
        emptyState.visibility = View.GONE
        recycler.visibility = View.GONE
        loadingState.visibility = View.VISIBLE

        val session = activeSessionHolder.getSafeActiveSession()
        if (session == null) {
            Toast.makeText(this, "Сессия не активна. Войдите заново.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            // 1. Читаем телефонную книгу
            val rawContacts = withContext(Dispatchers.IO) {
                contactsDataSource.getContacts(withEmails = true, withMsisdn = true)
            }
            // 2. Делаем batch lookup через identity server (ma1sd)
            val threePids = mutableListOf<ThreePid>()
            rawContacts.forEach { c ->
                c.msisdns.forEach { m -> threePids += ThreePid.Msisdn(m.phoneNumber) }
                c.emails.forEach { e -> threePids += ThreePid.Email(e.email) }
            }
            val matched: Map<ThreePid, String> = if (threePids.isNotEmpty()) {
                runCatching {
                    withContext(Dispatchers.IO) {
                        // identityService().getUserConsent() игнорируем для авто-синхронизации
                        runCatching { session.identityService().setUserConsent(true) }
                        session.identityService().lookUp(threePids).associate { it.threePid to it.matrixId }
                    }
                }.getOrElse { emptyMap() }
            } else emptyMap()

            // 3. Разделяем на зарегистрированных и нет
            val registered = mutableListOf<SvoiContactItem.Registered>()
            val invitable = mutableListOf<SvoiContactItem.Invitable>()
            for (c in rawContacts) {
                val matrixId = findMatrixId(c, matched)
                val phone = c.msisdns.firstOrNull()?.phoneNumber
                if (matrixId != null) {
                    registered += SvoiContactItem.Registered(
                            id = c.id, displayName = c.displayName, phone = phone, matrixId = matrixId
                    )
                } else if (phone != null) {
                    invitable += SvoiContactItem.Invitable(
                            id = c.id, displayName = c.displayName, phone = phone
                    )
                }
            }

            registered.sortBy { it.displayName.lowercase() }
            invitable.sortBy { it.displayName.lowercase() }

            val items = buildList {
                add(SvoiContactItem.Header("В СВОи (${registered.size})"))
                addAll(registered)
                add(SvoiContactItem.Header("Пригласить (${invitable.size})"))
                addAll(invitable)
            }

            loadingState.visibility = View.GONE
            if (registered.isEmpty() && invitable.isEmpty()) {
                emptyState.visibility = View.VISIBLE
            } else {
                recycler.visibility = View.VISIBLE
                adapter.submit(items)
            }
        }
    }

    private fun findMatrixId(contact: MappedContact, matched: Map<ThreePid, String>): String? {
        for (m in contact.msisdns) {
            val key = matched.keys.firstOrNull { it is ThreePid.Msisdn && it.msisdn.endsWith(m.phoneNumber.filter { it.isDigit() }.takeLast(10)) }
            if (key != null) return matched[key]
        }
        for (e in contact.emails) {
            val key = matched.keys.firstOrNull { it is ThreePid.Email && it.email.equals(e.email, ignoreCase = true) }
            if (key != null) return matched[key]
        }
        return null
    }

    private fun onContactClicked(item: SvoiContactItem) {
        when (item) {
            is SvoiContactItem.Registered -> {
                // Открываем экран создания прямого чата с этим юзером
                startActivity(CreateDirectRoomActivity.getIntent(this))
                Toast.makeText(this, "Найдите ${item.matrixId} в списке для начала чата", Toast.LENGTH_LONG).show()
            }
            is SvoiContactItem.Invitable -> {
                // Отправляем SMS-приглашение
                val text = "Привет! Я в новом мессенджере СВОи. Скачай: https://kodkontenta.ru/apk/svoi.apk"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("smsto:${item.phone}")).apply {
                    putExtra("sms_body", text)
                }
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    startActivity(Intent.createChooser(share, "Пригласить ${item.displayName}"))
                }
            }
            else -> Unit
        }
    }

    companion object {
        fun getIntent(ctx: android.content.Context) = Intent(ctx, SvoiContactsActivity::class.java)
    }
}

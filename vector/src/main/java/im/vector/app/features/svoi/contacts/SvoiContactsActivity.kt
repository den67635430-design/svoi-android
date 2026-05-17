/*
 * SVOi: автоматическая синхронизация телефонной книги.
 * Telegram-style: показывает все контакты с разделением:
 *   - "В СВОи" — кто зарегистрирован, можно открыть чат
 *   - "Пригласить" — кто не зарегистрирован
 * Юзер ставит галочки → "Пригласить выбранных" → SMS пакетно.
 */
package im.vector.app.features.svoi.contacts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
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
    private lateinit var selectAllButton: TextView
    private lateinit var actionsBar: View
    private lateinit var inviteBtn: Button

    private val adapter = SvoiContactsAdapter(
            onItemClick = ::onContactLongPress,
            onSelectionChanged = ::updateActionsBar,
    )
    private var allSelected = false

    private val permLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) loadContacts() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_svoi_contacts)

        recycler = findViewById(R.id.svoiContactsRecycler)
        emptyState = findViewById(R.id.svoiContactsEmpty)
        loadingState = findViewById(R.id.svoiContactsLoading)
        permissionState = findViewById(R.id.svoiContactsNoPermission)
        grantButton = findViewById(R.id.svoiContactsGrantButton)
        refreshButton = findViewById(R.id.svoiContactsRefreshButton)
        selectAllButton = findViewById(R.id.svoiContactsSelectAll)
        actionsBar = findViewById(R.id.svoiContactsActions)
        inviteBtn = findViewById(R.id.svoiContactsInviteBtn)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        grantButton.setOnClickListener { requestContactsPermission() }
        refreshButton.setOnClickListener { loadContacts() }
        selectAllButton.setOnClickListener {
            allSelected = !allSelected
            selectAllButton.text = if (allSelected) "Снять" else "Все"
            adapter.selectAll(allSelected)
        }
        inviteBtn.setOnClickListener { inviteSelected() }

        findViewById<View>(R.id.svoiContactsBack).setOnClickListener { finish() }

        if (hasContactsPermission()) loadContacts() else showPermissionRequest()
    }

    override fun onResume() {
        super.onResume()
        if (hasContactsPermission() && permissionState.visibility == View.VISIBLE) loadContacts()
    }

    private fun hasContactsPermission(): Boolean = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.READ_CONTACTS
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun requestContactsPermission() {
        permLauncher.launch(android.Manifest.permission.READ_CONTACTS)
    }

    private fun showPermissionRequest() {
        permissionState.visibility = View.VISIBLE
        loadingState.visibility = View.GONE
        recycler.visibility = View.GONE
        emptyState.visibility = View.GONE
        actionsBar.visibility = View.GONE
    }

    private fun loadContacts() {
        permissionState.visibility = View.GONE
        emptyState.visibility = View.GONE
        recycler.visibility = View.GONE
        actionsBar.visibility = View.GONE
        loadingState.visibility = View.VISIBLE

        val session = activeSessionHolder.getSafeActiveSession()
        if (session == null) {
            Toast.makeText(this, "Сессия не активна.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val rawContacts = withContext(Dispatchers.IO) {
                contactsDataSource.getContacts(withEmails = true, withMsisdn = true)
            }

            val threePids = mutableListOf<ThreePid>()
            rawContacts.forEach { c ->
                c.msisdns.forEach { m -> threePids += ThreePid.Msisdn(m.phoneNumber) }
                c.emails.forEach { e -> threePids += ThreePid.Email(e.email) }
            }
            val matched: Map<ThreePid, String> = if (threePids.isNotEmpty()) {
                runCatching {
                    withContext(Dispatchers.IO) {
                        runCatching { session.identityService().setUserConsent(true) }
                        session.identityService().lookUp(threePids).associate { it.threePid to it.matrixId }
                    }
                }.getOrElse { emptyMap() }
            } else emptyMap()

            val registered = mutableListOf<SvoiContactItem.Registered>()
            val invitable = mutableListOf<SvoiContactItem.Invitable>()
            for (c in rawContacts) {
                val matrixId = findMatrixId(c, matched)
                val phone = c.msisdns.firstOrNull()?.phoneNumber
                if (matrixId != null) {
                    registered += SvoiContactItem.Registered(c.id, c.displayName, phone, matrixId)
                } else if (phone != null) {
                    invitable += SvoiContactItem.Invitable(c.id, c.displayName, phone)
                }
            }

            registered.sortBy { it.displayName.lowercase() }
            invitable.sortBy { it.displayName.lowercase() }

            val items = buildList {
                if (registered.isNotEmpty()) {
                    add(SvoiContactItem.Header("В СВОи (${registered.size})"))
                    addAll(registered)
                }
                if (invitable.isNotEmpty()) {
                    add(SvoiContactItem.Header("Пригласить (${invitable.size})"))
                    addAll(invitable)
                }
            }

            loadingState.visibility = View.GONE
            if (registered.isEmpty() && invitable.isEmpty()) {
                emptyState.visibility = View.VISIBLE
            } else {
                recycler.visibility = View.VISIBLE
                actionsBar.visibility = View.VISIBLE
                adapter.submit(items)
                updateActionsBar()
            }
        }
    }

    private fun findMatrixId(contact: MappedContact, matched: Map<ThreePid, String>): String? {
        for (m in contact.msisdns) {
            val tail = m.phoneNumber.filter { it.isDigit() }.takeLast(10)
            val key = matched.keys.firstOrNull { it is ThreePid.Msisdn && it.msisdn.endsWith(tail) }
            if (key != null) return matched[key]
        }
        for (e in contact.emails) {
            val key = matched.keys.firstOrNull { it is ThreePid.Email && it.email.equals(e.email, ignoreCase = true) }
            if (key != null) return matched[key]
        }
        return null
    }

    private fun updateActionsBar() {
        val invSel = adapter.selectedInvitable()
        val regSel = adapter.selectedRegistered()
        val total = invSel.size + regSel.size
        inviteBtn.text = if (invSel.isEmpty()) {
            if (regSel.isEmpty()) "Выберите контакты" else "Открыть чат (${regSel.size})"
        } else {
            "Пригласить (${invSel.size})"
        }
        inviteBtn.isEnabled = total > 0
        inviteBtn.alpha = if (total > 0) 1f else 0.5f
    }

    private fun inviteSelected() {
        val invSel = adapter.selectedInvitable()
        val regSel = adapter.selectedRegistered()
        when {
            invSel.isNotEmpty() -> {
                val phones = invSel.map { it.phone }.joinToString(",")
                val text = "Привет! Я в новом мессенджере СВОи. Скачай: https://kodkontenta.ru/apk/svoi.apk"
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phones")).apply {
                    putExtra("sms_body", text)
                }
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "$text\n\nНомера: $phones")
                    }
                    startActivity(Intent.createChooser(share, "Пригласить ${invSel.size}"))
                }
            }
            regSel.isNotEmpty() -> {
                Toast.makeText(this, "Открой чат с ${regSel.first().matrixId} вручную в списке бесед", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun onContactLongPress(item: SvoiContactItem) {
        if (item is SvoiContactItem.Invitable) {
            val text = "Привет! Я в новом мессенджере СВОи. Скачай: https://kodkontenta.ru/apk/svoi.apk"
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${item.phone}")).apply {
                putExtra("sms_body", text)
            }
            runCatching { startActivity(intent) }
        }
    }

    companion object {
        fun getIntent(ctx: android.content.Context) = Intent(ctx, SvoiContactsActivity::class.java)
    }
}

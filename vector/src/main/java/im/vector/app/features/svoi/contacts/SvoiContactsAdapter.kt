package im.vector.app.features.svoi.contacts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.R

class SvoiContactsAdapter(
        private val onItemClick: (SvoiContactItem) -> Unit,
        private val onSelectionChanged: () -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<SvoiContactItem>()

    fun submit(newItems: List<SvoiContactItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun selectedRegistered(): List<SvoiContactItem.Registered> =
            items.filterIsInstance<SvoiContactItem.Registered>().filter { it.selected }

    fun selectedInvitable(): List<SvoiContactItem.Invitable> =
            items.filterIsInstance<SvoiContactItem.Invitable>().filter { it.selected }

    fun selectAll(value: Boolean) {
        items.forEach {
            when (it) {
                is SvoiContactItem.Registered -> it.selected = value
                is SvoiContactItem.Invitable -> it.selected = value
                else -> Unit
            }
        }
        notifyDataSetChanged()
        onSelectionChanged()
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is SvoiContactItem.Header -> 0
        is SvoiContactItem.Registered -> 1
        is SvoiContactItem.Invitable -> 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> HeaderVH(inflater.inflate(R.layout.item_svoi_contact_header, parent, false))
            1 -> RegisteredVH(inflater.inflate(R.layout.item_svoi_contact_registered, parent, false))
            else -> InvitableVH(inflater.inflate(R.layout.item_svoi_contact_invitable, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is HeaderVH -> holder.bind(item as SvoiContactItem.Header)
            is RegisteredVH -> holder.bind(item as SvoiContactItem.Registered, onItemClick, onSelectionChanged)
            is InvitableVH -> holder.bind(item as SvoiContactItem.Invitable, onItemClick, onSelectionChanged)
        }
    }

    private class HeaderVH(v: View) : RecyclerView.ViewHolder(v) {
        private val title: TextView = v.findViewById(R.id.title)
        fun bind(h: SvoiContactItem.Header) { title.text = h.title }
    }

    private class RegisteredVH(v: View) : RecyclerView.ViewHolder(v) {
        private val name: TextView = v.findViewById(R.id.name)
        private val sub: TextView = v.findViewById(R.id.sub)
        private val cb: CheckBox? = v.findViewById(R.id.checkbox)
        fun bind(c: SvoiContactItem.Registered, onItemClick: (SvoiContactItem) -> Unit, onSel: () -> Unit) {
            name.text = c.displayName
            sub.text = c.matrixId + (c.phone?.let { " · $it" } ?: "")
            cb?.setOnCheckedChangeListener(null)
            cb?.isChecked = c.selected
            cb?.setOnCheckedChangeListener { _, checked -> c.selected = checked; onSel() }
            itemView.setOnClickListener {
                cb?.toggle()
            }
            itemView.setOnLongClickListener { onItemClick(c); true }
        }
    }

    private class InvitableVH(v: View) : RecyclerView.ViewHolder(v) {
        private val name: TextView = v.findViewById(R.id.name)
        private val phone: TextView = v.findViewById(R.id.phone)
        private val cb: CheckBox? = v.findViewById(R.id.checkbox)
        fun bind(c: SvoiContactItem.Invitable, onItemClick: (SvoiContactItem) -> Unit, onSel: () -> Unit) {
            name.text = c.displayName
            phone.text = c.phone
            cb?.setOnCheckedChangeListener(null)
            cb?.isChecked = c.selected
            cb?.setOnCheckedChangeListener { _, checked -> c.selected = checked; onSel() }
            itemView.setOnClickListener {
                cb?.toggle()
            }
            itemView.setOnLongClickListener { onItemClick(c); true }
        }
    }
}

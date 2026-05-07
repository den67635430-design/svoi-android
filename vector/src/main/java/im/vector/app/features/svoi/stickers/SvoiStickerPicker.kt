/*
 * СВОи Sticker Picker — UI компонент для выбора стикеров в чате.
 * Показывает BottomSheet с двумя табами: Builtin / My stickers.
 *
 * Использование:
 *   val picker = SvoiStickerPicker(activity, userId)
 *   picker.show { sticker -> /* отправить sticker.url в чат */ }
 */
package im.vector.app.features.svoi.stickers

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import im.vector.app.features.svoi.api.Sticker
import im.vector.app.features.svoi.api.SvoiStickerApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URL

class SvoiStickerPicker(
        private val activity: Activity,
        private val userId: String,
        private val scope: CoroutineScope,
) {
    private val api = SvoiStickerApi()
    private var dialog: BottomSheetDialog? = null
    private var builtinStickers: List<Sticker> = emptyList()
    private var userStickers: List<Sticker> = emptyList()
    private var currentTab = 0  // 0=builtin, 1=user

    /** Показать пикер. onSelected получает выбранный стикер. */
    fun show(onSelected: (Sticker) -> Unit) {
        if (dialog?.isShowing == true) return
        dialog = BottomSheetDialog(activity)
        val root = createView(onSelected)
        dialog?.setContentView(root)
        dialog?.show()
        loadStickers()
    }

    private fun createView(onSelected: (Sticker) -> Unit): View {
        val ctx = activity
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val tabs = TabLayout(ctx)
        tabs.addTab(tabs.newTab().setText("Встроенные"))
        tabs.addTab(tabs.newTab().setText("Мои"))
        container.addView(tabs)

        val recycler = RecyclerView(ctx)
        recycler.layoutManager = GridLayoutManager(ctx, 4)
        val adapter = StickerAdapter(emptyList()) { sticker ->
            onSelected(sticker)
            dialog?.dismiss()
        }
        recycler.adapter = adapter
        recycler.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                500,
        )
        container.addView(recycler)

        tabs.addOnTabSelectedListener(
                object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab) {
                        currentTab = tab.position
                        adapter.update(if (currentTab == 0) builtinStickers else userStickers)
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab) {}
                    override fun onTabReselected(tab: TabLayout.Tab) {}
                }
        )

        return container
    }

    private fun loadStickers() {
        scope.launch {
            try {
                val builtin = api.getBuiltinStickers()
                val user = api.getUserStickers(userId)
                builtinStickers = builtin
                userStickers = user
                Timber.i(
                        "Stickers loaded: %d builtin, %d user",
                        builtin.size,
                        user.size,
                )
                withContext(Dispatchers.Main) {
                    val recycler =
                            ((dialog?.findViewById<View>(android.R.id.content) as? ViewGroup)
                                    ?.getChildAt(0) as? ViewGroup)
                                    ?.getChildAt(1)
                                    as? RecyclerView
                    (recycler?.adapter as? StickerAdapter)?.update(
                            if (currentTab == 0) builtinStickers else userStickers
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load stickers")
            }
        }
    }

    private class StickerAdapter(
            private var items: List<Sticker>,
            private val onClick: (Sticker) -> Unit,
    ) : RecyclerView.Adapter<StickerAdapter.VH>() {

        class VH(val img: ImageView) : RecyclerView.ViewHolder(img)

        fun update(newItems: List<Sticker>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val img = ImageView(parent.context)
            img.layoutParams =
                    ViewGroup.LayoutParams(120, 120)
            img.scaleType = ImageView.ScaleType.FIT_CENTER
            return VH(img)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val sticker = items[position]
            holder.img.setOnClickListener { onClick(sticker) }
            // Async load image
            Thread {
                try {
                    val bytes = URL(sticker.url).readBytes()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    holder.img.post { holder.img.setImageBitmap(bitmap) }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to load sticker image: %s", sticker.url)
                }
            }
                    .start()
        }

        override fun getItemCount() = items.size
    }
}

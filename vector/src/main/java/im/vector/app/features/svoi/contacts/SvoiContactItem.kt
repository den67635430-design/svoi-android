package im.vector.app.features.svoi.contacts

sealed class SvoiContactItem {
    abstract val stableKey: String

    data class Header(val title: String) : SvoiContactItem() {
        override val stableKey: String = "h_$title"
    }
    data class Registered(
            val id: Long,
            val displayName: String,
            val phone: String?,
            val matrixId: String,
            var selected: Boolean = false,
    ) : SvoiContactItem() {
        override val stableKey: String = "r_$id"
    }
    data class Invitable(
            val id: Long,
            val displayName: String,
            val phone: String,
            var selected: Boolean = false,
    ) : SvoiContactItem() {
        override val stableKey: String = "i_$id"
    }
}

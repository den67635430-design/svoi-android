package im.vector.app.features.svoi.contacts

sealed class SvoiContactItem {
    data class Header(val title: String) : SvoiContactItem()
    data class Registered(
            val id: Long,
            val displayName: String,
            val phone: String?,
            val matrixId: String,
    ) : SvoiContactItem()
    data class Invitable(
            val id: Long,
            val displayName: String,
            val phone: String,
    ) : SvoiContactItem()
}

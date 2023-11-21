package kr.young.firertc.util

data class RecyclerViewNotifier(
    val position: Int,
    val count: Int,
    val modifierCategory: ModifierCategory
) {
    enum class ModifierCategory {
        Insert,
        Removed,
        Changed,
    }
}

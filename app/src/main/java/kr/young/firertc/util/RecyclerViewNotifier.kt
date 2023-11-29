package kr.young.firertc.util

data class RecyclerViewNotifier(
    val position: Int,
    val count: Int,
    val modifierCategory: ModifierCategory,
    val isBottom: Boolean = false
) {
    enum class ModifierCategory {
        Insert,
        Removed,
        Changed,
    }
}

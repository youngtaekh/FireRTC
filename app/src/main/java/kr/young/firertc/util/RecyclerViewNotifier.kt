package kr.young.firertc.util

data class RecyclerViewNotifier<E>(
    val position: Int,
    val count: Int,
    val modifierCategory: ModifierCategory,
    val isBottom: Boolean = false,
    val list: MutableList<E> = mutableListOf()
) {
    enum class ModifierCategory {
        Insert,
        Removed,
        Changed,
    }
}

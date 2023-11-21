package kr.young.firertc.model

import com.google.firebase.firestore.FieldValue
import kr.young.common.Crypto
import kr.young.firertc.util.Config.Companion.CREATED_AT
import kr.young.firertc.util.Config.Companion.FROM
import kr.young.firertc.util.Config.Companion.TO
import kr.young.firertc.vm.MyDataViewModel
import java.util.Date

data class Relation(
    val from: String? = MyDataViewModel.instance.myData?.id,
    val to: String? = null,
    val id: String = Crypto().getHash("$from$to"),
    var type: Type = Type.Friend,
    val createdAt: Date? = null
) {

    fun toMap(): Map<String, Any> {
        val at = createdAt ?: FieldValue.serverTimestamp()
        return mapOf(
            FROM to from!!,
            TO to to!!,
            "id" to id,
            CREATED_AT to at
        )
    }

    override fun toString(): String {
        return "from $from, to $to, id ${id.isNotEmpty()}"
    }

    enum class Type {
        Friend,
        Hide,
        Block
    }
}
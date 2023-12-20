package kr.young.firertc.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.FieldValue
import com.google.gson.JsonObject
import kr.young.common.Crypto
import kr.young.firertc.util.Config.Companion.BODY
import kr.young.firertc.util.Config.Companion.CHAT_ID
import kr.young.firertc.util.Config.Companion.CREATED_AT
import kr.young.firertc.util.Config.Companion.FROM
import kr.young.firertc.util.Config.Companion.SEQUENCE
import kr.young.firertc.vm.MyDataViewModel
import org.json.JSONObject
import java.util.*

@Entity(tableName = "messages")
data class Message(
    val from: String = MyDataViewModel.instance.getMyId(),
    val chatId: String? = null,
    @PrimaryKey
    val id: String = Crypto().getHash("$from$chatId${System.currentTimeMillis()}"),
    val body: String? = null,
    var sequence: Long = -1,
    val createdAt: Date? = null,
    var timeFlag: Boolean = true,
    var isDate: Boolean = false,
) {
    fun toMap(): Map<String, Any> {
        val createdAt = this.createdAt ?: FieldValue.serverTimestamp()
        return mapOf(
            "id" to id,
            FROM to from,
            CHAT_ID to chatId!!,
            BODY to body!!,
            SEQUENCE to sequence,
            CREATED_AT to createdAt
        )
    }

    override fun toString(): String {
        val json = JsonObject()
        json.addProperty(FROM, from)
        json.addProperty(BODY, body)
        json.addProperty(SEQUENCE, sequence)
        json.addProperty(CREATED_AT, createdAt?.time)
        json.addProperty("id", id)
        json.addProperty(CHAT_ID, chatId)
        return json.toString()
    }

    companion object {
        private const val TAG = "Message"

        fun fromJson(json: String): Message {
            val obj = JSONObject(json)
            return Message(
                from = obj.getString(FROM),
                id = obj.getString("id"),
                chatId = obj.getString(CHAT_ID),
                body = obj.getString(BODY),
                sequence = obj.getLong(SEQUENCE),
                createdAt = Date(obj.getLong(CREATED_AT))
            )
        }
    }
}
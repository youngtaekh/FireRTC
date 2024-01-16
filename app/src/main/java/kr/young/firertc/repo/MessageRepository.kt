package kr.young.firertc.repo

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.firertc.model.Message
import kr.young.firertc.util.Config.Companion.CHAT_ID
import kr.young.firertc.util.Config.Companion.MAX_LONG
import kr.young.firertc.util.Config.Companion.MESSAGE_PAGE_SIZE
import kr.young.firertc.util.Config.Companion.MIN_LONG
import kr.young.firertc.util.Config.Companion.SEQUENCE
import kr.young.firertc.vm.CallViewModel

class MessageRepository {
    companion object {
        fun getMessages(
            chatId: String,
            min: Long = MIN_LONG,
            max: Long = MAX_LONG,
            failure: OnFailureListener = OnFailureListener {
                CallViewModel.instance.setResponseCode(MESSAGE_READ_FAILURE)
                e(TAG, "get message failure", it)
            },
            success: OnSuccessListener<QuerySnapshot> = OnSuccessListener<QuerySnapshot> {
                CallViewModel.instance.setResponseCode(MESSAGE_READ_SUCCESS)
                for (document in it) {
                    val message = document.toObject<Message>()
                    d(TAG, "message $message")
                }
                d(TAG, "get message success")
            }
        ) {
            Firebase.firestore.collection(COLLECTION)
                .whereEqualTo(CHAT_ID, chatId)
                .whereGreaterThan(SEQUENCE, min)
                .whereLessThan(SEQUENCE, max)
                .orderBy(SEQUENCE, Query.Direction.DESCENDING)
                .limit(MESSAGE_PAGE_SIZE)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun getLastMessage(
            chatId: String,
            failure: OnFailureListener = OnFailureListener {
                e(TAG, "get message failure", it)
            },
            success: OnSuccessListener<QuerySnapshot> = OnSuccessListener<QuerySnapshot> {
                for (document in it) {
                    val message = document.toObject<Message>()
                    d(TAG, "getLastMessage $message")
                }
                d(TAG, "get message success")
            }
        ) {
            Firebase.firestore.collection(COLLECTION)
                .whereEqualTo(CHAT_ID, chatId)
                .orderBy(SEQUENCE, Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun post(
            message: Message,
            failure: OnFailureListener = OnFailureListener {
                e(TAG, "post message failure")
            },
            success: OnSuccessListener<Void> = OnSuccessListener {
                d(TAG, "post message success")
            }
        ) {
            d(TAG, "post message")
            Firebase.firestore.collection(COLLECTION).document(message.id)
                .set(message.toMap())
                .addOnFailureListener(failure)
                .addOnSuccessListener(success)
        }

        const val MESSAGE_CREATE_SUCCESS = 20051
        const val MESSAGE_READ_SUCCESS = 20052
        const val MESSAGE_UPDATE_SUCCESS = 20053
        const val MESSAGE_DELETE_SUCCESS = 20054

        const val MESSAGE_CREATE_FAILURE = 10051
        const val MESSAGE_READ_FAILURE = 10052
        const val MESSAGE_UPDATE_FAILURE = 10053
        const val MESSAGE_DELETE_FAILURE = 10054

        private const val TAG = "MessageRepository"
        private const val COLLECTION = "messages"
    }
}
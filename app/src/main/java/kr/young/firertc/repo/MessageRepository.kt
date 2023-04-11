package kr.young.firertc.repo

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.firertc.model.Message
import kr.young.firertc.util.Config.Companion.CREATED_AT
import kr.young.firertc.vm.CallViewModel

class MessageRepository {
    companion object {
        fun getMessages(
            chatId: String,
            failure: OnFailureListener = OnFailureListener {
                CallViewModel.instance.setResponseCode(MESSAGE_READ_FAILURE)
                e(TAG, "get message failure", it)
            },
            success: OnSuccessListener<QuerySnapshot> = OnSuccessListener<QuerySnapshot> {
                CallViewModel.instance.setResponseCode(MESSAGE_READ_SUCCESS)
                d(TAG, "get message success")
            }
        ) {
            d(TAG, "get message by chat id")
            Firebase.firestore.collection(COLLECTION)
                .whereEqualTo("chatId", chatId)
                .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                .limit(100)
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
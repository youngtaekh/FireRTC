package kr.young.firertc.repo

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.firertc.model.Chat
import kr.young.firertc.vm.ChatViewModel

class ChatRepository {
    companion object {
        fun getChat(
            id: String,
            failure: OnFailureListener = OnFailureListener {
                e(TAG, "get chat failure", it)
                ChatViewModel.instance.setResponseCode(CHAT_READ_FAILURE)
            },
            success: OnSuccessListener<DocumentSnapshot> = OnSuccessListener<DocumentSnapshot> {
                d(TAG, "get chat success")
                ChatViewModel.instance.setResponseCode(CHAT_READ_SUCCESS)
            }
        ) {
            d(TAG, "getById")
            Firebase.firestore.collection(COLLECTION).document(id)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun getChats(
            id: String,
            failure: OnFailureListener = OnFailureListener {
                e(TAG, "get chats failure", it)
                ChatViewModel.instance.setResponseCode(CHAT_READ_FAILURE)
            },
            success: OnSuccessListener<QuerySnapshot> = OnSuccessListener {
                d(TAG, "get chats success")
                ChatViewModel.instance.setResponseCode(CHAT_READ_SUCCESS)
            }
        ) {
            d(TAG, "getChats by my id")
            Firebase.firestore.collection(COLLECTION)
                .whereArrayContains("participants", id)
                .orderBy("modifiedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun post(
            chat: Chat,
            failure: OnFailureListener = OnFailureListener {
                e(TAG, "post chat failure")
                ChatViewModel.instance.setResponseCode(CHAT_CREATE_FAILURE)
            },
            success: OnSuccessListener<Void> = OnSuccessListener {
                d(TAG, "post chat success")
                ChatViewModel.instance.setResponseCode(CHAT_CREATE_SUCCESS)
            }
        ) {
            d(TAG, "post chat")
            Firebase.firestore.collection(COLLECTION).document(chat.id!!)
                .set(chat.toMap())
                .addOnFailureListener(failure)
                .addOnSuccessListener(success)
        }

        fun updateModifiedAt(
            chat: Chat,
            failure: OnFailureListener = OnFailureListener {
                e(TAG, "update chat modified at failure")
                ChatViewModel.instance.setResponseCode(CHAT_UPDATE_FAILURE)
            },
            success: OnSuccessListener<Void> = OnSuccessListener {
                d(TAG, "update chat modified at success")
                ChatViewModel.instance.setResponseCode(CHAT_UPDATE_SUCCESS)
            }
        ) {
            d(TAG, "update modified at")
            Firebase.firestore.collection(COLLECTION).document(chat.id!!)
                .update("modifiedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun updateLastMessage(
            chat: Chat,
            failure: OnFailureListener = OnFailureListener {
                e(TAG, "update chat last message failure")
                ChatViewModel.instance.setResponseCode(CHAT_UPDATE_FAILURE)
            },
            success: OnSuccessListener<Void> = OnSuccessListener {
                d(TAG, "update chat last message success")
                ChatViewModel.instance.setResponseCode(CHAT_UPDATE_SUCCESS)
            }
        ) {
            d(TAG, "update last message")
            val map = mapOf(
                "modifiedAt" to FieldValue.serverTimestamp(),
                "lastMessage" to chat.lastMessage
            )
            Firebase.firestore.collection(COLLECTION).document(chat.id!!)
                .update(map)
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        const val CHAT_CREATE_SUCCESS = 20041
        const val CHAT_READ_SUCCESS = 20042
        const val CHAT_UPDATE_SUCCESS = 20043
        const val CHAT_DELETE_SUCCESS = 20044

        const val CHAT_CREATE_FAILURE = 10041
        const val CHAT_READ_FAILURE = 10042
        const val CHAT_UPDATE_FAILURE = 10043
        const val CHAT_DELETE_FAILURE = 10044

        private const val TAG = "ChatRepository"
        private const val COLLECTION = "chats"
    }
}
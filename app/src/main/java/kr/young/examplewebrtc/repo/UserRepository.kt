package kr.young.examplewebrtc.repo

import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.examplewebrtc.model.Call
import kr.young.examplewebrtc.model.User
import kr.young.examplewebrtc.util.Config.Companion.FCM_TOKEN

class UserRepository {
    companion object {
        fun get(id: String, success: OnSuccessListener<DocumentSnapshot>) {
            d(TAG, "get id $id")
            Firebase.firestore.collection(COLLECTION).document(id)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener { e -> e(TAG, "get user by id is fail", e) }
        }

        fun post(user: User, success: OnSuccessListener<Void>) {
            d(TAG, "post id ${user.id}")
            Firebase.firestore.collection(COLLECTION).document(user.id)
                .set(user)
                .addOnSuccessListener(success)
                .addOnFailureListener { e -> e(TAG, "post fail", e) }
        }

        fun updateFCMToken(user: User) {
            d(TAG, "updateToken id ${user.id}")
            Firebase.firestore.collection(COLLECTION).document(user.id)
                .update(FCM_TOKEN, user.fcmToken)
                .addOnFailureListener { e -> e(TAG, "update call state fail", e)
                }
        }

        private const val TAG= "UserRepository"
        private const val COLLECTION = "users"
    }
}
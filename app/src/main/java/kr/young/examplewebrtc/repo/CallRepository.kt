package kr.young.examplewebrtc.repo

import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kr.young.common.DateUtil
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.examplewebrtc.model.Call
import kr.young.examplewebrtc.model.Call.Companion.STATE
import kr.young.examplewebrtc.model.Call.Companion.TERMINATED_AT

class CallRepository {
    companion object {
        fun getById(id: String, success: OnSuccessListener<DocumentSnapshot>) {
            d(TAG, "getById")
            Firebase.firestore.collection(Call.COLLECTION).document(id)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener { e -> e(TAG, "get call fail", e) }
        }

        fun getActiveCalls(spaceId: String, success: OnSuccessListener<QuerySnapshot>) {
            d(TAG, "getActiveCalls")
            Firebase.firestore.collection(Call.COLLECTION)
                .whereEqualTo(Call.SPACE_ID, spaceId)
                .whereEqualTo(TERMINATED_AT, null)
                .limit(2)
                .get()
                .addOnSuccessListener(success)
        }

        fun post(call: Call) {
            d(TAG, "post")
            Firebase.firestore.collection(Call.COLLECTION).document(call.id)
                .set(call)
                .addOnFailureListener { e -> e(TAG, "post fail", e) }
        }

        fun updateTerminatedAt(call: Call) {
            d(TAG, "end")
            Firebase.firestore.collection(Call.COLLECTION).document(call.id)
                .update(TERMINATED_AT, call.terminatedAt)
                .addOnFailureListener { e -> e(TAG, "update call state fail", e) }
        }

        private const val TAG = "CallRepository"
    }
}
package kr.young.examplewebrtc.repo

import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.examplewebrtc.model.Call
import kr.young.examplewebrtc.util.Config.Companion.CANDIDATES
import kr.young.examplewebrtc.util.Config.Companion.SDP
import kr.young.examplewebrtc.util.Config.Companion.SPACE_ID
import kr.young.examplewebrtc.util.Config.Companion.TERMINATED
import kr.young.examplewebrtc.util.Config.Companion.TERMINATED_AT

class CallRepository {
    companion object {
        fun getById(id: String, success: OnSuccessListener<DocumentSnapshot>) {
            d(TAG, "getById")
            Firebase.firestore.collection(COLLECTION).document(id)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener { e -> e(TAG, "get call fail", e) }
        }

        fun getBySpaceId(id: String, success: OnSuccessListener<QuerySnapshot>) {
            d(TAG, "getsBySpaceId")
            Firebase.firestore.collection(COLLECTION)
                .whereEqualTo(SPACE_ID, id)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener { e -> e(TAG, "get by space id is failed") }
        }

        fun getActiveCalls(spaceId: String, success: OnSuccessListener<QuerySnapshot>) {
            d(TAG, "getActiveCalls")
            Firebase.firestore.collection(COLLECTION)
                .whereEqualTo(SPACE_ID, spaceId)
                .whereEqualTo(TERMINATED, false)
                .limit(2)
                .get()
                .addOnSuccessListener(success)
        }

        fun post(call: Call) {
            d(TAG, "post call user ${call.userId}")
            Firebase.firestore.collection(COLLECTION).document(call.id)
                .set(call)
                .addOnFailureListener { e -> e(TAG, "post fail", e) }
        }

        fun updateCandidates(call: Call, candidate: String) {
            Firebase.firestore.collection(COLLECTION).document(call.id)
                .update(CANDIDATES, FieldValue.arrayUnion(candidate))
                .addOnSuccessListener { d(TAG, "updateCandidates success") }
                .addOnFailureListener { e -> e(TAG, "updateCandidates failure", e) }
        }

        fun updateSDP(call: Call) {
            Firebase.firestore.collection(COLLECTION).document(call.id)
                .update(SDP, call.sdp)
                .addOnSuccessListener { d(TAG, "updateSDP success") }
                .addOnFailureListener { e -> e(TAG, "updateSDP failure", e) }
        }

        fun updateTerminatedAt(call: Call) {
            d(TAG, "end call user ${call.userId}")
            val update = hashMapOf<String, Any> (
                TERMINATED_AT to call.terminatedAt!!,
                TERMINATED to true,
//                SDP to "",
//                CANDIDATES to mutableListOf<String>()
            )
            Firebase.firestore.collection(COLLECTION).document(call.id)
                .update(update)
                .addOnFailureListener { e -> e(TAG, "update call state fail", e) }
        }

        private const val TAG = "CallRepository"
        private const val COLLECTION = "calls"
    }
}
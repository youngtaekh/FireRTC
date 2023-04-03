package kr.young.examplewebrtc.repo

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
import kr.young.examplewebrtc.model.Call
import kr.young.examplewebrtc.util.Config.Companion.CANDIDATES
import kr.young.examplewebrtc.util.Config.Companion.SDP
import kr.young.examplewebrtc.util.Config.Companion.SPACE_ID
import kr.young.examplewebrtc.util.Config.Companion.TERMINATED
import kr.young.examplewebrtc.util.Config.Companion.TERMINATED_AT
import kr.young.examplewebrtc.util.Config.Companion.USER_ID
import kr.young.examplewebrtc.vm.CallViewModel

class CallRepository {
    companion object {
        fun getCall(
            id: String,
            failure: OnFailureListener = OnFailureListener {
                CallViewModel.instance.setResponseCode(CALL_READ_FAILURE)
                e(TAG, "get call fail", it)
            },
            success: OnSuccessListener<DocumentSnapshot> = OnSuccessListener<DocumentSnapshot> {
                CallViewModel.instance.setResponseCode(CALL_READ_SUCCESS)
            }
        ) {
            d(TAG, "getById")
            Firebase.firestore.collection(COLLECTION).document(id)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun getBySpaceId(
            id: String,
            success: OnSuccessListener<QuerySnapshot> = OnSuccessListener {
                CallViewModel.instance.setResponseCode(CALL_READ_SUCCESS)
            },
            failure: OnFailureListener = OnFailureListener {
                CallViewModel.instance.setResponseCode(CALL_READ_FAILURE)
                e(TAG, "get by space id is failed")
            }
        ) {
            d(TAG, "getsBySpaceId")
            Firebase.firestore.collection(COLLECTION)
                .whereEqualTo(SPACE_ID, id)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun getByUserId(
            userId: String,
            failure: OnFailureListener = OnFailureListener {
                CallViewModel.instance.setResponseCode(CALL_READ_FAILURE)
                e(TAG, "get call by user id is failed")
            },
            success: OnSuccessListener<QuerySnapshot> = OnSuccessListener {
                CallViewModel.instance.setResponseCode(CALL_READ_SUCCESS)
                d(TAG, "get calls by user id is success")
            }
        ) {
            d(TAG, "getByUserId")
            Firebase.firestore.collection(COLLECTION)
                .whereEqualTo(USER_ID, userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun getActiveCalls(
            spaceId: String,
            success: OnSuccessListener<QuerySnapshot> = OnSuccessListener {
                CallViewModel.instance.setResponseCode(CALL_READ_SUCCESS)
            },
            failure: OnFailureListener = OnFailureListener {
                CallViewModel.instance.setResponseCode(CALL_READ_FAILURE)
            }
        ) {
            d(TAG, "getActiveCalls")
            Firebase.firestore.collection(COLLECTION)
                .whereEqualTo(SPACE_ID, spaceId)
                .whereEqualTo(TERMINATED, false)
                .limit(2)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun post(
            call: Call,
            failure: OnFailureListener = OnFailureListener {
                CallViewModel.instance.setResponseCode(CALL_CREATE_FAILURE)
                e(TAG, "post fail", it)
            },
            success: OnSuccessListener<Void> = OnSuccessListener {
                CallViewModel.instance.setResponseCode(CALL_CREATE_SUCCESS)
                d(TAG, "post call success")
            }
        ) {
            d(TAG, "post call user ${call.userId}")
            Firebase.firestore.collection(COLLECTION).document(call.id)
                .set(call.toMap())
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun update(
            id: String,
            map: Map<String, Any>,
            failure: OnFailureListener = OnFailureListener {
                CallViewModel.instance.setResponseCode(CALL_UPDATE_FAILURE)
                e(TAG, "update call failure", it)
            },
            success: OnSuccessListener<Void> = OnSuccessListener {
                CallViewModel.instance.setResponseCode(CALL_UPDATE_SUCCESS)
                d(TAG, "update call success")
            },
        ) {
            Firebase.firestore.collection(COLLECTION).document(id)
                .update(map)
                .addOnFailureListener(failure)
                .addOnSuccessListener(success)
        }

        fun updateCandidates(
            call: Call,
            candidate: String,
            failure: OnFailureListener = OnFailureListener {
                CallViewModel.instance.setResponseCode(CALL_UPDATE_FAILURE)
                e(TAG, "updateCandidates failure", it)
            },
            success: OnSuccessListener<Void> = OnSuccessListener {
                CallViewModel.instance.setResponseCode(CALL_UPDATE_SUCCESS)
                d(TAG, "updateCandidates success")
            },
        ) {
            d(TAG, "updateCandidates")
            Firebase.firestore.collection(COLLECTION).document(call.id)
                .update(CANDIDATES, FieldValue.arrayUnion(candidate))
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun updateSDP(
            call: Call,
            failure: OnFailureListener = OnFailureListener {
                CallViewModel.instance.setResponseCode(CALL_UPDATE_FAILURE)
                e(TAG, "updateSDP failure", it)
            },
            success: OnSuccessListener<Void> = OnSuccessListener {
                CallViewModel.instance.setResponseCode(CALL_UPDATE_SUCCESS)
                d(TAG, "updateSDP success")
            }
        ) {
            d(TAG, "updateSDP")
            Firebase.firestore.collection(COLLECTION).document(call.id)
                .update(SDP, call.sdp)
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun updateTerminatedAt(
            call: Call,
            failure: OnFailureListener = OnFailureListener {
                CallViewModel.instance.setResponseCode(CALL_UPDATE_FAILURE)
                e(TAG, "updateTerminatedAt failure", it)
            },
            success: OnSuccessListener<Void> = OnSuccessListener {
                CallViewModel.instance.setResponseCode(CALL_UPDATE_SUCCESS)
                d(TAG, "updateTerminatedAt success")
            },
        ) {
            d(TAG, "end call user ${call.userId}")
            val update = hashMapOf(
                TERMINATED_AT to FieldValue.serverTimestamp(),
                TERMINATED to true,
//                SDP to "",
//                CANDIDATES to mutableListOf<String>()
            )
            Firebase.firestore.collection(COLLECTION).document(call.id)
                .update(update)
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        const val CALL_CREATE_SUCCESS = 20001
        const val CALL_READ_SUCCESS = 20002
        const val CALL_UPDATE_SUCCESS = 20003
        const val CALL_DELETE_SUCCESS = 20004

        const val CALL_CREATE_FAILURE = 10001
        const val CALL_READ_FAILURE = 10002
        const val CALL_UPDATE_FAILURE = 10003
        const val CALL_DELETE_FAILURE = 10004

        private const val TAG = "CallRepository"
        private const val COLLECTION = "calls"
    }
}
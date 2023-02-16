package kr.young.examplewebrtc.repo

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kr.young.common.DateUtil
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.examplewebrtc.model.Call.Companion.TERMINATED_AT
import kr.young.examplewebrtc.model.Space
import kr.young.examplewebrtc.model.Space.Companion.STATUS

class SpaceRepository {
    companion object {
        fun getActiveSpace(name: String, success: OnSuccessListener<QuerySnapshot>, failure: OnFailureListener) {
            d(TAG, "getActiveSpace")
            Firebase.firestore.collection(Space.COLLECTION)
                .whereEqualTo(Space.NAME, name)
                .whereIn(STATUS, Space.notTerminated())
                .limit(1)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun post(space: Space) {
            d(TAG, "post")
            Firebase.firestore.collection(Space.COLLECTION)
                .document(space.id)
                .set(space)
                .addOnFailureListener { e -> e(TAG, "post space fail", e) }
        }

        fun updateStatus(space: Space) {
            d(TAG, "updateStatus")
            if (space.status == Space.SpaceStatus.TERMINATED) {
                val map = mapOf(
                    STATUS to Space.SpaceStatus.TERMINATED,
                    TERMINATED_AT to DateUtil.toFormattedString(System.currentTimeMillis())
                )
                Firebase.firestore.collection(Space.COLLECTION)
                    .document(space.id)
                    .update(map)
                    .addOnFailureListener { e -> e(TAG, "update space status to TERMINATED fail", e) }
            } else {
                Firebase.firestore.collection(Space.COLLECTION)
                    .document(space.id)
                    .update(STATUS, space.status)
                    .addOnFailureListener { e -> e(TAG, "update space status fail", e) }
            }
        }

        fun updateCallList(spaceId: String, callId: String) {
            d(TAG, "updateCallList")
            Firebase.firestore.collection(Space.COLLECTION).document(spaceId)
                .update(Space.CALLS, FieldValue.arrayUnion(callId))
                .addOnFailureListener { e -> e(TAG, "add call id fail", e) }
        }

        private const val TAG = "SpaceRepository"
    }
}
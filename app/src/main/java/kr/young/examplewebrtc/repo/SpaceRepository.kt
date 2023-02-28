package kr.young.examplewebrtc.repo

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kr.young.common.DateUtil
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.examplewebrtc.model.Space
import kr.young.examplewebrtc.util.Config.Companion.CALLS
import kr.young.examplewebrtc.util.Config.Companion.NAME
import kr.young.examplewebrtc.util.Config.Companion.STATUS
import kr.young.examplewebrtc.util.Config.Companion.TERMINATED_AT

class SpaceRepository {
    companion object {
        fun getSpace(id: String, successListener: OnSuccessListener<DocumentSnapshot>) {
            d(TAG, "getSpaceById id ${id.substring(0, 5)}")
            Firebase.firestore.collection(COLLECTION)
                .document(id)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener { e -> e(TAG, "get space by id fail", e) }
        }

        fun getActiveSpace(name: String, success: OnSuccessListener<QuerySnapshot>) {
            d(TAG, "getActiveSpace name $name")
            Firebase.firestore.collection(COLLECTION)
                .whereEqualTo(NAME, name)
                .whereIn(STATUS, Space.notTerminated())
                .limit(1)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener { e -> e(TAG, "getActiveSpaceFailure", e) }
        }

        fun post(space: Space) {
            d(TAG, "post space name ${space.name}")
            Firebase.firestore.collection(COLLECTION)
                .document(space.id)
                .set(space)
                .addOnFailureListener { e -> e(TAG, "post space fail", e) }
        }

        fun updateStatus(space: Space) {
            d(TAG, "updateStatus space name ${space.name}")
            if (space.status == Space.SpaceStatus.TERMINATED) {
                val map = mapOf(
                    STATUS to Space.SpaceStatus.TERMINATED,
                    TERMINATED_AT to DateUtil.toFormattedString(System.currentTimeMillis())
                )
                Firebase.firestore.collection(COLLECTION)
                    .document(space.id)
                    .update(map)
                    .addOnFailureListener { e -> e(TAG, "update space status to TERMINATED fail", e) }
            } else {
                Firebase.firestore.collection(COLLECTION)
                    .document(space.id)
                    .update(STATUS, space.status)
                    .addOnFailureListener { e -> e(TAG, "update space status fail", e) }
            }
        }

        fun addCallList(spaceId: String, callId: String) {
            d(TAG, "updateCallList")
            Firebase.firestore.collection(COLLECTION).document(spaceId)
                .update(CALLS, FieldValue.arrayUnion(callId))
                .addOnFailureListener { e -> e(TAG, "add call fail", e) }
        }

        fun removeCallList(spaceId: String, callId: String) {
            d(TAG, "removeCallList")
            Firebase.firestore.collection(COLLECTION).document(spaceId)
                .update(CALLS, FieldValue.arrayRemove(callId))
                .addOnFailureListener { e -> e(TAG, "remove call fail", e) }
        }

        private const val TAG = "SpaceRepository"
        private const val COLLECTION = "spaces"
    }
}
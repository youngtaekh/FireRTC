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
import kr.young.examplewebrtc.vm.MyDataViewModel
import kr.young.examplewebrtc.vm.SpaceViewModel

class SpaceRepository {
    companion object {
        fun post(
            space: Space,
            success: OnSuccessListener<Void> = OnSuccessListener {
                SpaceViewModel.instance.setResponseCode(SPACE_CREATE_SUCCESS)
                d(TAG, "post space success")
            },
            failure: OnFailureListener = OnFailureListener {
                SpaceViewModel.instance.setResponseCode(SPACE_CREATE_FAILURE)
                e(TAG, "post space failure", it)
            }
        ) {
            d(TAG, "post space name ${space.name}")
            Firebase.firestore.collection(COLLECTION)
                .document(space.id)
                .set(space.toMap())
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun getSpace(
            id: String,
            success: OnSuccessListener<DocumentSnapshot> = OnSuccessListener {
                SpaceViewModel.instance.setResponseCode(SPACE_READ_SUCCESS)
                d(TAG, "get space success")
            },
            failure: OnFailureListener = OnFailureListener {
                SpaceViewModel.instance.setResponseCode(SPACE_READ_FAILURE)
                e(TAG, "get space by id fail", it)
            }
        ) {
            d(TAG, "getSpaceById id ${id.substring(0, 5)}")
            Firebase.firestore.collection(COLLECTION)
                .document(id)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun getActiveSpace(
            name: String,
            success: OnSuccessListener<QuerySnapshot> = OnSuccessListener {
                SpaceViewModel.instance.setResponseCode(SPACE_READ_SUCCESS)
                d(TAG, "get active space success")
            },
            failure: OnFailureListener = OnFailureListener {
                SpaceViewModel.instance.setResponseCode(SPACE_READ_FAILURE)
                e(TAG, "get active space failure", it)
            }
        ) {
            d(TAG, "getActiveSpace name $name")
            Firebase.firestore.collection(COLLECTION)
                .whereEqualTo(NAME, name)
                .whereIn(STATUS, Space.notTerminated())
                .limit(1)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun updateStatus(
            space: Space,
            success: OnSuccessListener<Void> = OnSuccessListener {
                d(TAG, "update space status success")
                SpaceViewModel.instance.setResponseCode(SPACE_UPDATE_SUCCESS)
            },
            failure: OnFailureListener = OnFailureListener {
                e(TAG, "update space status failure", it)
                SpaceViewModel.instance.setResponseCode(SPACE_UPDATE_FAILURE)
            }
        ) {
            d(TAG, "updateStatus space name ${space.name}")
            if (space.terminated) {
                val map = mapOf(
                    "terminated" to true,
                    "terminatedBy" to MyDataViewModel.instance.getMyId(),
                    TERMINATED_AT to DateUtil.toFormattedString(System.currentTimeMillis())
                )
                Firebase.firestore.collection(COLLECTION)
                    .document(space.id)
                    .update(map)
                    .addOnSuccessListener(success)
                    .addOnFailureListener(failure)
            } else {
                Firebase.firestore.collection(COLLECTION)
                    .document(space.id)
                    .update("connected", space.connected)
                    .addOnSuccessListener(success)
                    .addOnFailureListener(failure)
            }
        }

        fun addCallList(
            spaceId: String,
            callId: String,
            success: OnSuccessListener<Void> = OnSuccessListener {
                SpaceViewModel.instance.setResponseCode(SPACE_UPDATE_SUCCESS)
                d(TAG, "update call list success")
            },
            failure: OnFailureListener = OnFailureListener {
                SpaceViewModel.instance.setResponseCode(SPACE_UPDATE_FAILURE)
            }
        ) {
            d(TAG, "updateCallList")
            Firebase.firestore.collection(COLLECTION).document(spaceId)
                .update(CALLS, FieldValue.arrayUnion(callId))
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun removeCallList(
            spaceId: String,
            callId: String,
            success: OnSuccessListener<Void> = OnSuccessListener {
                d(TAG, "remove space success")
                SpaceViewModel.instance.setResponseCode(SPACE_DELETE_SUCCESS)
            },
            failure: OnFailureListener = OnFailureListener {
                e(TAG, "remove space failure", it)
                SpaceViewModel.instance.setResponseCode(SPACE_DELETE_FAILURE)
            }
        ) {
            d(TAG, "removeCallList")
            Firebase.firestore.collection(COLLECTION).document(spaceId)
                .update(CALLS, FieldValue.arrayRemove(callId))
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        const val SPACE_CREATE_FAILURE = 10021
        const val SPACE_READ_FAILURE = 10022
        const val SPACE_UPDATE_FAILURE = 10023
        const val SPACE_DELETE_FAILURE = 10024

        const val SPACE_CREATE_SUCCESS = 20021
        const val SPACE_READ_SUCCESS = 20022
        const val SPACE_UPDATE_SUCCESS = 20023
        const val SPACE_DELETE_SUCCESS = 20024

        private const val TAG = "SpaceRepository"
        private const val COLLECTION = "spaces"
    }
}
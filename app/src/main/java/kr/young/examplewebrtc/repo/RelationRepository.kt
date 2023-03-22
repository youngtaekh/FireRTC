package kr.young.examplewebrtc.repo

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kr.young.common.Crypto
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.examplewebrtc.model.Relation
import kr.young.examplewebrtc.util.Config.Companion.FROM
import kr.young.examplewebrtc.util.Config.Companion.TO
import kr.young.examplewebrtc.vm.MyDataViewModel
import kr.young.examplewebrtc.vm.UserViewModel

class RelationRepository {
    companion object {
        fun post(
            relation: Relation,
            success: OnSuccessListener<Void> = OnSuccessListener<Void> {
                UserViewModel.instance.setResponseCode(RELATION_CREATE_SUCCESS)
                d(TAG, "relation post success")
            },
            failure: OnFailureListener = OnFailureListener {
                UserViewModel.instance.setResponseCode(RELATION_CREATE_FAILURE)
                e(TAG, "relation post failure", it)
            }
        ) {
            d(TAG, "post relation(${relation.from} -> ${relation.to})")
            Firebase.firestore.collection(COLLECTION).document(relation.id)
                .set(relation.toMap())
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun getAll(
            success: OnSuccessListener<QuerySnapshot> = OnSuccessListener {
                d(TAG, "getAll success")
                UserViewModel.instance.setResponseCode(RELATION_READ_SUCCESS)
                val list = mutableListOf<String>()
                for (document in it) {
                    val relation = document.toObject<Relation>()
                    list.add(relation.to!!)
                }
                if (list.isEmpty()) {
                    UserViewModel.instance.removeAllContact()
                    UserViewModel.instance.setRefreshContacts()
                } else {
                    UserRepository.getUsers(list)
                }
            },
            failure: OnFailureListener = OnFailureListener {
                e(TAG, "getAll failure", it)
                UserViewModel.instance.setResponseCode(RELATION_READ_FAILURE)
            }
        ) {
            d(TAG, "getAll")
            Firebase.firestore.collection(COLLECTION)
                .whereEqualTo(FROM, MyDataViewModel.instance.getMyId())
                .orderBy(TO)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun remove(id: String) {
            Firebase.firestore.collection(COLLECTION)
                .document(Crypto().getHash("${MyDataViewModel.instance.getMyId()}$id"))
                .delete()
                .addOnSuccessListener { d(TAG, "remove relation success") }
                .addOnFailureListener { e(TAG, "remove relation failure") }
        }

        const val RELATION_CREATE_FAILURE = 10011
        const val RELATION_READ_FAILURE = 10012
        const val RELATION_UPDATE_FAILURE = 10013
        const val RELATION_DELETE_FAILURE = 10014

        const val RELATION_CREATE_SUCCESS = 20011
        const val RELATION_READ_SUCCESS = 20012
        const val RELATION_UPDATE_SUCCESS = 20013
        const val RELATION_DELETE_SUCCESS = 20014

        private const val TAG = "RelationRepository"
        private const val COLLECTION = "relations"
    }
}
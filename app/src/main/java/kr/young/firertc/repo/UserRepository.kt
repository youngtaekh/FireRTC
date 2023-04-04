package kr.young.firertc.repo

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.firertc.model.User
import kr.young.firertc.util.Config.Companion.FCM_TOKEN
import kr.young.firertc.vm.MyDataViewModel
import kr.young.firertc.vm.UserViewModel
import kotlin.math.min

class UserRepository {
    companion object {
        fun post(
            user: User,
            success: OnSuccessListener<Void> = OnSuccessListener {
                d(TAG, "post user success")
                UserViewModel.instance.setResponseCode(USER_CREATE_SUCCESS)
            },
            failure: OnFailureListener = OnFailureListener {
                e(TAG, "post user failure", it)
                UserViewModel.instance.setResponseCode(USER_CREATE_FAILURE)
            }
        ) {
            d(TAG, "post user id ${user.id}")
            Firebase.firestore.collection(COLLECTION).document(user.id)
                .set(user.toMap())
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun getUser(
            id: String,
            failure: OnFailureListener = OnFailureListener {
                e(TAG, "get user by id is fail", it)
                UserViewModel.instance.setResponseCode(USER_READ_FAILURE)
            },
            success: OnSuccessListener<DocumentSnapshot> = OnSuccessListener {
                d(TAG, "get user success")
                UserViewModel.instance.setResponseCode(USER_READ_SUCCESS)
            }
        ) {
            d(TAG, "get id $id")
            Firebase.firestore.collection(COLLECTION).document(id)
                .get()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun getUsers(
            list: List<String>,
            success: OnSuccessListener<QuerySnapshot> = OnSuccessListener {
                d(TAG, "get users success")
                val userViewModel = UserViewModel.instance
                userViewModel.setResponseCode(USER_READ_SUCCESS)
                val userList = mutableListOf<User>()
                for (document in it) {
                    val user = document.toObject<User>()
                    userList.add(user)
                }
                userViewModel.addContacts(userList)

                if (userViewModel.checkPage()) {
                    userViewModel.contacts.sortBy { user -> user.name }
                    userViewModel.setRefreshContacts()
                }
            },
            failure: OnFailureListener = OnFailureListener {
                e(TAG, "get users failure", it)
                UserViewModel.instance.setResponseCode(USER_READ_FAILURE)
            }
        ) {
            d(TAG, "getUsers list size ${list.size}")
            UserViewModel.instance.removeAllContact()
            var start = 0
            var end = 10
            UserViewModel.instance.initPage(list.size / 10)
            while (list.size > start) {
                end = min(end + start, list.size)
                val subList = list.subList(start, end)
                start = end
                Firebase.firestore.collection(COLLECTION)
                    .whereIn("id", subList)
                    .get()
                    .addOnSuccessListener(success)
                    .addOnFailureListener(failure)
            }
        }

        fun updateFCMToken(
            user: User,
            success: OnSuccessListener<Void> = OnSuccessListener {
                d(TAG, "my date update success")
                MyDataViewModel.instance.setResponseCode(USER_UPDATE_SUCCESS)
            },
            failure: OnFailureListener = OnFailureListener {
                e(TAG, "my data update failure", it)
                MyDataViewModel.instance.setResponseCode(USER_UPDATE_FAILURE)
            }
        ) {
            d(TAG, "updateToken id ${user.id}")
            Firebase.firestore.collection(COLLECTION).document(user.id)
                .update(FCM_TOKEN, user.fcmToken)
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        fun delete(
            id: String,
            success: OnSuccessListener<Void> = OnSuccessListener {
                d(TAG, "my data delete success")
                MyDataViewModel.instance.setResponseCode(USER_DELETE_SUCCESS)
            },
            failure: OnFailureListener = OnFailureListener {
                e(TAG, "my data delete failure", it)
                MyDataViewModel.instance.setResponseCode(USER_DELETE_FAILURE)
            }
        ) {
            d(TAG, "remove")
            Firebase.firestore.collection(COLLECTION).document(id)
                .delete()
                .addOnSuccessListener(success)
                .addOnFailureListener(failure)
        }

        const val USER_CREATE_FAILURE = 10031
        const val USER_READ_FAILURE = 10032
        const val USER_UPDATE_FAILURE = 10033
        const val USER_DELETE_FAILURE = 10034

        const val USER_CREATE_SUCCESS = 20031
        const val USER_READ_SUCCESS = 20032
        const val USER_UPDATE_SUCCESS = 20033
        const val USER_DELETE_SUCCESS = 20034

        private const val TAG= "UserRepository"
        private const val COLLECTION = "users"
    }

    enum class ResponseCode {
        UserCreateFailure,
    }
}
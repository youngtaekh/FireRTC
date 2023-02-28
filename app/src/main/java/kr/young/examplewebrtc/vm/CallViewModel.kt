package kr.young.examplewebrtc.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import com.google.gson.JsonObject
import kr.young.common.Crypto
import kr.young.common.DateUtil
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.common.UtilLog.Companion.i
import kr.young.common.UtilLog.Companion.w
import kr.young.examplewebrtc.repo.AppSP
import kr.young.examplewebrtc.fcm.ApiClient
import kr.young.examplewebrtc.model.Call
import kr.young.examplewebrtc.model.Call.CallDirection.Answer
import kr.young.examplewebrtc.model.Call.CallDirection.Offer
import kr.young.examplewebrtc.model.Space
import kr.young.examplewebrtc.model.Space.SpaceStatus
import kr.young.examplewebrtc.model.Space.SpaceStatus.INACTIVE
import kr.young.examplewebrtc.model.Space.SpaceStatus.TERMINATED
import kr.young.examplewebrtc.repo.CallRepository
import kr.young.examplewebrtc.repo.SpaceRepository
import retrofit2.Callback
import retrofit2.Response
import java.lang.System.currentTimeMillis

class CallViewModel: ViewModel() {
    val myCall = MutableLiveData<Call>()

    //implementation fire store result interface
    private val checkLastParticipant = OnSuccessListener<QuerySnapshot> { documents ->
        d(TAG, "checkLastParticipant size - ${documents.size()}")
        if (documents.size() == 0) {
            SpaceViewModel.instance.updateSpaceStatus(TERMINATED)
        }
        for (document in documents) {
            val call = document.toObject<Call>()
            if (documents.size() == 1) {
                d(TAG, "call.userId - ${call.userId}")
                if (call.userId == AppSP.instance.getUserId()) {
                    SpaceViewModel.instance.updateSpaceStatus(TERMINATED)
                } else {
                    SpaceViewModel.instance.updateSpaceStatus(INACTIVE)
                }
            }
        }
    }

    private fun setMyCall(call: Call) {
        this.myCall.value = call
    }

    fun release() {
        d(TAG, "release")
        myCall.value = null
    }

    fun terminateSpace() {
        d(TAG, "terminateSpace")
        if (myCall.value == null) {
            e(TAG, "terminateSpace() My Call is null")
            return
        }
        CallRepository.getActiveCalls(myCall.value!!.spaceId!!, checkLastParticipant)
    }

    fun getCallsBySpaceId(id: String) {
        CallRepository.getBySpaceId(id) { documents ->
            val callList = mutableListOf<Call>()
            d(TAG, "getCallsBySpaceIdSuccess documents.size ${documents.size()}")
            for (document in documents) {
                val call = document.toObject<Call>()
                d(TAG, "getCallsBySpaceId userId ${call.userId}")
                callList.add(call)
            }
            SpaceViewModel.instance.setCalls(callList)
            SpaceViewModel.instance.setOffer(false)
        }
    }

    fun refreshCalls(id: String) {
        CallRepository.getBySpaceId(id) { documents ->
            val callList = mutableListOf<Call>()
            d(TAG, "refreshCallsSuccess documents.size ${documents.size()}")
            for (document in documents) {
                val call = document.toObject<Call>()
                d(TAG, "refreshCalls userId ${call.userId}")
                callList.add(call)
            }
            SpaceViewModel.instance.setCalls(callList)
        }
    }

    fun setCall(call: Call) {
        setMyCall(call)
        CallRepository.post(myCall.value!!)
    }

    fun endCall() {
        d(TAG, "endCall")
        if (myCall.value == null) {
            e(TAG, "My Call is null")
            return
        }
        val call = myCall.value!!
        call.terminatedAt = DateUtil.toFormattedString(currentTimeMillis())
        call.terminated = true
        setMyCall(call)
        CallRepository.updateTerminatedAt(call)
    }

    private object Holder {
        val INSTANCE = CallViewModel()
    }

    companion object {
        private const val TAG = "CallViewModel"
        val instance: CallViewModel by lazy { Holder.INSTANCE }
    }
}
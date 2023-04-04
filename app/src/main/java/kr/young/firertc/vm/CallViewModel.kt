package kr.young.firertc.vm

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.firertc.model.Call
import kr.young.firertc.model.Space.SpaceStatus.INACTIVE
import kr.young.firertc.model.Space.SpaceStatus.TERMINATED
import kr.young.firertc.repo.AppSP
import kr.young.firertc.repo.CallRepository
import kr.young.firertc.repo.CallRepository.Companion.CALL_READ_SUCCESS

class CallViewModel: ViewModel() {
    val myCall = MutableLiveData<Call>()
    val mute = MutableLiveData(false)
    val speaker = MutableLiveData(false)

    internal val responseCode = MutableLiveData<Int> ()

    fun setResponseCode(value: Int) {
        d(TAG, "setResponseCode $value")
        Handler(Looper.getMainLooper()).post { responseCode.value = value }
    }

    //implementation fire store result interface
    private val checkLastParticipant = OnSuccessListener<QuerySnapshot> { documents ->
        d(TAG, "checkLastParticipant size - ${documents.size()}")
        setResponseCode(CALL_READ_SUCCESS)
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

    fun setMute() {
        if (mute.value == null) {
            mute.value = true
        } else {
            mute.value = !mute.value!!
        }
    }

    fun getMute(): Boolean {
        return if (mute.value == null) {
            false
        } else {
            mute.value!!
        }
    }

    fun setSpeaker() {
        if (speaker.value == null) {
            speaker.value = true
        } else {
            speaker.value = !speaker.value!!
        }
    }

    fun getSpeaker(): Boolean {
        return if (speaker.value == null) {
            false
        } else {
            speaker.value!!
        }
    }

    fun release() {
        d(TAG, "release")
        myCall.value = null
        mute.value = null
        speaker.value = null
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
        CallRepository.getBySpaceId(id = id, success = { documents ->
            setResponseCode(CALL_READ_SUCCESS)
            val callList = mutableListOf<Call>()
            d(TAG, "getCallsBySpaceIdSuccess documents.size ${documents.size()}")
            for (document in documents) {
                val call = document.toObject<Call>()
                d(TAG, "getCallsBySpaceId userId ${call.userId}")
                callList.add(call)
            }
            SpaceViewModel.instance.setCalls(callList)
            SpaceViewModel.instance.setOffer(false)
        })
    }

    fun refreshCalls(id: String) {
        CallRepository.getBySpaceId(id = id, success =  { documents ->
            setResponseCode(CALL_READ_SUCCESS)
            val callList = mutableListOf<Call>()
            d(TAG, "refreshCallsSuccess documents.size ${documents.size()}")
            for (document in documents) {
                val call = document.toObject<Call>()
                d(TAG, "refreshCalls userId ${call.userId}")
                callList.add(call)
            }
            SpaceViewModel.instance.setCalls(callList)
        })
    }

    fun setCall(call: Call) {
        setMyCall(call)
        CallRepository.post(call = myCall.value!!)
    }

    fun updateSDP(sdp: String) {
        val call = myCall.value!!
        call.sdp = sdp
        setMyCall(call)
        CallRepository.updateSDP(call)
    }

    fun updateCandidate(candidate: String) {
        val call = myCall.value!!
        call.candidates.add(candidate)
        setMyCall(call)
        CallRepository.updateCandidates(call, candidate)
    }

    fun endCall() {
        d(TAG, "endCall")
        if (myCall.value == null) {
            e(TAG, "My Call is null")
            return
        }
        val call = myCall.value!!
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
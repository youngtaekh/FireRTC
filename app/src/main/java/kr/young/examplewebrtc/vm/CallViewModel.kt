package kr.young.examplewebrtc.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import kr.young.common.Crypto
import kr.young.common.DateUtil
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.common.UtilLog.Companion.i
import kr.young.examplewebrtc.AppSP
import kr.young.examplewebrtc.model.Call
import kr.young.examplewebrtc.model.Call.CallDirection.Answer
import kr.young.examplewebrtc.model.Call.CallDirection.Offer
import kr.young.examplewebrtc.model.Space
import kr.young.examplewebrtc.model.Space.SpaceStatus
import kr.young.examplewebrtc.model.Space.SpaceStatus.TERMINATED
import kr.young.examplewebrtc.repo.CallRepository
import kr.young.examplewebrtc.repo.SpaceRepository
import java.lang.System.currentTimeMillis

class CallViewModel: ViewModel() {
    val hasExistSpace = MutableLiveData<Boolean?>()
    val space = MutableLiveData<Space>()
    val myCall = MutableLiveData<Call>()
    val remoteCall = MutableLiveData<MutableList<Call>>()

    //implementation fire store result interface
    private val getCallSuccess =
        OnSuccessListener<DocumentSnapshot> { document ->
            val remoteCall = document!!.toObject<Call>()
            addRemoteCall(remoteCall!!)
        }
    private val checkExistSpaceSuccess = OnSuccessListener<QuerySnapshot> { documents ->
        d(TAG, "checkExistSpaceSuccess")
        if (documents.isEmpty) {
            hasExistSpace.value = false
        } else {
            hasExistSpace.value = true
            for (document in documents) {
                this.space.value = document.toObject()
                break
            }
        }
    }
    private val checkExistSpaceFailure = OnFailureListener {
        d(TAG, "checkExistSpaceFailure")
        hasExistSpace.value = false
    }
    private val checkLastParticipant = OnSuccessListener<QuerySnapshot> { documents ->
        d(TAG, "checkLastParticipant size - ${documents.size()}")
        if (documents.size() == 0) {
            updateSpaceStatus(TERMINATED)
        }
        for (document in documents) {
            val call = document.toObject<Call>()
            if (documents.size() == 1 && call.userId == AppSP.instance.getUserId()) {
                d(TAG, "call.userId - ${call.userId}")
                updateSpaceStatus(TERMINATED)
            }
        }
        release()
    }

    private fun setSpace(space: Space) {
        this.space.value = space
    }

    private fun setMyCall(call: Call) {
        this.myCall.value = call
    }

    private fun addRemoteCall(call: Call) {
        if (remoteCall.value == null) {
            remoteCall.value = mutableListOf()
        }
        val callList = remoteCall.value!!
        callList.add(call)
        remoteCall.value = callList
    }

    private fun release() {
        d(TAG, "release")
        space.value = null
        myCall.value = null
        remoteCall.value = null
    }

    private fun getMyId(): String {
        var userId = AppSP.instance.getUserId()
        if (userId == null) {
            userId = Crypto().getHash(currentTimeMillis().toString())
            AppSP.instance.setUserId(userId)
        }
        return userId
    }

    fun createSpace(name: String) {
        d(TAG, "createSpace")
        setSpace(Space(name = name, createdBy = getMyId()))
        hasExistSpace.value = true
        SpaceRepository.post(space.value!!)
    }

    fun checkExistSpace(name: String) {
        d(TAG, "checkExistSpace")
        SpaceRepository.getActiveSpace(name, checkExistSpaceSuccess, checkExistSpaceFailure)
    }

    fun updateSpaceStatus(status: SpaceStatus) {
        i(TAG, "${space.value!!.status} -> $status")
        val space = space.value!!
        space.status = status
        setSpace(space)
        SpaceRepository.updateStatus(space)
    }

    fun terminateSpace() {
        d(TAG, "terminateSpace")
        if (myCall.value == null) {
            e(TAG, "terminateSpace() My Call is null")
            return
        }
        CallRepository.getActiveCalls(myCall.value!!.spaceId!!, checkLastParticipant)
    }

    fun makeCall() {
        d(TAG, "makeCall")
        val call = Call(userId = getMyId(), spaceId = space.value!!.id, direction = Offer)

        setMyCall(call)
        CallRepository.post(myCall.value!!)
        val space = this.space.value!!
        space.calls.add(call.id)
        this.space.value = space
        SpaceRepository.updateCallList(space.id, call.id)
    }

    fun answerCall() {
        d(TAG, "answerCall")
        val call = Call(userId = getMyId(), spaceId = space.value!!.id, direction = Answer)

        for (callId in space.value!!.calls) {
            i(TAG, "answerCall() callId - $callId")
            CallRepository.getById(callId, getCallSuccess)
        }

        setMyCall(call)
        CallRepository.post(myCall.value!!)
        val space = this.space.value!!
        space.calls.add(call.id)
        this.space.value = space
        SpaceRepository.updateCallList(space.id, call.id)
    }

    fun endCall() {
        d(TAG, "endCall")
        if (myCall.value == null) {
            e(TAG, "My Call is null")
            return
        }
        val call = myCall.value!!
        call.terminatedAt = DateUtil.toFormattedString(currentTimeMillis())
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
package kr.young.examplewebrtc.vm

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.MODE_IN_COMMUNICATION
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.toObject
import kr.young.common.DateUtil
import kr.young.common.UtilLog.Companion.d
import kr.young.examplewebrtc.CallService
import kr.young.examplewebrtc.fcm.SendFCM
import kr.young.examplewebrtc.model.Call
import kr.young.examplewebrtc.model.Space
import kr.young.examplewebrtc.model.User
import kr.young.examplewebrtc.observer.CallSignalImpl
import kr.young.examplewebrtc.repo.CallRepository
import kr.young.examplewebrtc.repo.CallRepository.Companion.CALL_READ_SUCCESS
import kr.young.examplewebrtc.repo.SpaceRepository
import kr.young.examplewebrtc.repo.SpaceRepository.Companion.SPACE_READ_SUCCESS
import kr.young.examplewebrtc.repo.UserRepository
import kr.young.rtp.RTPManager
import org.webrtc.SessionDescription
import java.util.*

open class CallVM internal constructor(): ViewModel() {
    var space: Space? = null
    var call: Call? = null
    var counterpart: User? = null
    var callType: Call.Type? = null
        get() { return field ?: Call.Type.AUDIO }
    var callDirection: Call.Direction? = null
        get() { return field ?: Call.Direction.Offer }
    var historyList = mutableListOf<Call>()
    var selectedCall: Call? = null

    val responseCode = MutableLiveData<Int>()
    val terminatedCall = MutableLiveData<Boolean>()
    val mute = MutableLiveData<Boolean>()
    val speaker = MutableLiveData<Boolean>()

    var remoteSDP: SessionDescription? = null
    var remoteIce: String? = null

    internal fun setResponseCode(value: Int) {
        Handler(Looper.getMainLooper()).post { responseCode.value = value }
    }

    fun setTerminatedCall(value: Boolean) {
        Handler(Looper.getMainLooper()).post { terminatedCall.value = value }
    }

    fun mute() {
        RTPManager.instance.setMute(!mute.value!!)
        RTPManager.instance.setAudioEnable(mute.value!!)
        Handler(Looper.getMainLooper()).post { mute.value = !mute.value!! }
    }

    fun speaker(audioManager: AudioManager) {
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        audioManager.isSpeakerphoneOn = !speaker.value!!
        audioManager.mode = MODE_IN_COMMUNICATION
//        RTPManager.instance.setSpeaker(!speaker.value!!)
        Handler(Looper.getMainLooper()).post { speaker.value = !speaker.value!! }
    }

    open fun release() {
        d(TAG, "release()")
        space = null
        call = null
        counterpart = null
        setResponseCode(0)
        setTerminatedCall(false)
        Handler(Looper.getMainLooper()).post { mute.value = false }
        Handler(Looper.getMainLooper()).post { speaker.value = false }
    }

    open fun startOffer(counterpart: User, type: Call.Type, callCreateSuccess: OnSuccessListener<Void>) {
        d(TAG, "startOffer")
        this.counterpart = counterpart
        space = Space()
        SpaceRepository.post(space!!) {
            d(TAG, "create space success")
            call = Call(spaceId = space!!.id, type = type, direction = Call.Direction.Offer, counterpartName = counterpart.name)
            CallRepository.post(call!!, {
                setResponseCode(CallRepository.CALL_CREATE_FAILURE)
            }, callCreateSuccess)
        }
    }

    fun sendOffer(sdp: String) {
        call!!.sdp = sdp
        CallRepository.updateSDP(call!!)
        SendFCM.sendMessage(
            to = counterpart!!.fcmToken!!,
            type = SendFCM.FCMType.Offer,
            callType = Call.Type.AUDIO,
            spaceId = space!!.id,
            callId = call!!.id,
            sdp = sdp
        )
    }

    open fun startAnswer() {
        d(TAG, "startAnswer")
    }

    fun sendAnswer(sdp: String) {
        call!!.sdp = sdp
        CallRepository.updateSDP(call!!)
        SendFCM.sendMessage(
            to = counterpart!!.fcmToken!!,
            type = SendFCM.FCMType.Answer,
            callType = Call.Type.AUDIO,
            spaceId = space!!.id,
            callId = call!!.id,
            sdp = sdp
        )
    }

    open fun updateCallList() {
        space!!.calls.add(call!!.id)
        SpaceRepository.addCallList(space!!.id, call!!.id)
    }

    open fun updateParticipantList() {
        val userId = MyDataViewModel.instance.getMyId()
        space!!.participants.add(userId)
        SpaceRepository.addParticipantList(space!!.id, userId)
    }

    open fun updateLeaveList() {
        val userId = MyDataViewModel.instance.getMyId()
        space!!.leaves.add(userId)
        SpaceRepository.addLeaveList(space!!.id, userId)
    }

    open fun busy(space: Space) {
        space.terminated = true
        SpaceRepository.updateStatus(space, "Busy")
        SpaceRepository.addLeaveList(space.id, MyDataViewModel.instance.getMyId())
    }

    open fun end(fcmType: SendFCM.FCMType = SendFCM.FCMType.Bye) {
        d(TAG, "end")
        space!!.terminated = true
        SpaceRepository.updateStatus(space!!, fcmType.toString())
        SendFCM.sendMessage(
            to = counterpart!!.fcmToken!!,
            type = fcmType,
            callType = call!!.type,
            spaceId = space!!.id,
            callId = call!!.id
        )
        onTerminatedCall()
    }

    open fun onIncomingCall(context: Context, userId: String?, spaceId: String?, type: String?, sdp: String?) {
        d(TAG, "onIncomingCall")
        if (spaceId == null || type == null) {
            return
        }

        callType = Call.Type.valueOf(type)
        callDirection = Call.Direction.Answer
        remoteSDP = SessionDescription(SessionDescription.Type.OFFER, sdp!!)
        SpaceRepository.getSpace(id = spaceId, success = {
            d(TAG, "get space success")
            this.space = it.toObject<Space>()!!
            setResponseCode(SPACE_READ_SUCCESS)
            UserRepository.getUser(id = userId!!) { user ->
                counterpart = user.toObject<User>()
                call = Call(spaceId = space!!.id, type = callType!!, direction = Call.Direction.Answer, counterpartName = counterpart!!.name)
                d(TAG, "call id ${call!!.id}")
                CallRepository.post(call!!) {
                    d(TAG, "call post success")
                    updateCallList()
                    updateParticipantList()
                    startForegroundService(context, Intent(context, CallService::class.java))
                    CallSignalImpl.instance.onIncomingObserver()
                }
            }
        })
    }

    open fun onAnswerCall(sdp: String?) {
        d(TAG, "onAnswerCall")
        space!!.connected = true
        SpaceRepository.update(space!!.id, mapOf<String, Any>("connected" to true))
        RTPManager.instance.setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp!!))
    }

    fun onIceCandidate(ice: String?) {
        if (ice == null) return
        call!!.candidates.add(ice)
        CallRepository.updateCandidates(call!!, ice)
        SendFCM.sendMessage(
            to = counterpart!!.fcmToken!!,
            type = SendFCM.FCMType.Ice,
            callType = call!!.type,
            spaceId = call!!.spaceId,
            callId = call!!.id,
            sdp = ice
        )
    }

    fun onPCConnected() {
        call!!.connected = true
        CallRepository.update(call!!.id, mapOf("connected" to true))
    }

    open fun onTerminatedCall() {
        d(TAG, "onTerminatedCall")
        updateLeaveList()
        setTerminatedCall(true)
        if (call != null) {
            call!!.terminated = true
            CallRepository.updateTerminatedAt(call!!)
        }
    }

    fun getSpace(id: String, success: OnSuccessListener<DocumentSnapshot>) {
        SpaceRepository.getSpace(id = id, success = success)
    }

    fun getHistory() {
        if (MyDataViewModel.instance.myData != null) {
            CallRepository.getByUserId(MyDataViewModel.instance.getMyId()) {
                d(TAG, "getHistory success size ${it.size()}")
                var savedDate = ""
                for (i in it) {
                    val call = i.toObject<Call>()
                    val newDate = DateUtil.toFormattedString(call.createdAt!!, "yy.MM.dd", TimeZone.getDefault())
                    if (savedDate != newDate) {
                        val header = Call(isHeader = true, createdAt = call.createdAt)
                        historyList.add(header)
                        savedDate = newDate
                    }
                    historyList.add(call)
                }
                setResponseCode(CALL_READ_SUCCESS)
            }
        }
    }

    init {
        setResponseCode(0)
        setTerminatedCall(false)
        Handler(Looper.getMainLooper()).post { mute.value = false }
        Handler(Looper.getMainLooper()).post { speaker.value = false }
    }

    private object Holder {
        val INSTANCE = CallVM()
    }

    companion object {
        private const val TAG = "CallVM"
        val instance: CallVM by lazy { Holder.INSTANCE }
    }
}
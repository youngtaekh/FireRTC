package kr.young.firertc.vm

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
import kr.young.firertc.CallService
import kr.young.firertc.fcm.SendFCM
import kr.young.firertc.model.Call
import kr.young.firertc.model.Space
import kr.young.firertc.model.User
import kr.young.firertc.observer.CallSignalImpl
import kr.young.firertc.repo.CallRepository
import kr.young.firertc.repo.CallRepository.Companion.CALL_READ_SUCCESS
import kr.young.firertc.repo.SpaceRepository
import kr.young.firertc.repo.SpaceRepository.Companion.SPACE_READ_SUCCESS
import kr.young.firertc.repo.UserRepository
import kr.young.rtp.RTPManager
import org.webrtc.RendererCommon.ScalingType
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

    val cameraMute = MutableLiveData<Boolean>()
    val sd = MutableLiveData<Boolean>()
    val scaleType = MutableLiveData<ScalingType>()
    val swapScreen = MutableLiveData<Boolean> ()

    val rtpManager = RTPManager.instance

    var remoteSDP: SessionDescription? = null
    var remoteIce: String? = null

    internal fun setResponseCode(value: Int) {
        Handler(Looper.getMainLooper()).post { responseCode.value = value }
    }

    fun setTerminatedCall(value: Boolean) {
        Handler(Looper.getMainLooper()).post { terminatedCall.value = value }
    }

    fun mute() {
        rtpManager.setMute(!mute.value!!)
        rtpManager.setAudioEnable(mute.value!!)
        Handler(Looper.getMainLooper()).post { mute.value = !mute.value!! }
    }

    fun speaker(audioManager: AudioManager, value: Boolean) {
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        audioManager.isSpeakerphoneOn = value
        audioManager.mode = MODE_IN_COMMUNICATION
//        rtpManager.setSpeaker(!speaker.value!!)
        Handler(Looper.getMainLooper()).post { speaker.value = value }
    }

    fun cameraMute(value: Boolean) {
        if (value) {
//            rtpManager.setVideoEnable(false)
            rtpManager.stopVideoSource()
        } else {
//            rtpManager.setVideoEnable(true)
            rtpManager.startVideoSource()
        }
        Handler(Looper.getMainLooper()).post { cameraMute.value = value }
    }

    fun cameraSwitch() {
        rtpManager.switchCamera()
    }

    fun changeDefinition() {
        if (sd.value!!) {
            rtpManager.captureFormatChange(720, 1280, 30)
        } else {
            rtpManager.captureFormatChange(360, 360, 10)
        }
        Handler(Looper.getMainLooper()).post { sd.value = !sd.value!! }
    }

    fun changeScalingType() {
        scaleType.value = when (scaleType.value!!) {
            ScalingType.SCALE_ASPECT_FIT -> ScalingType.SCALE_ASPECT_FILL
            ScalingType.SCALE_ASPECT_FILL -> ScalingType.SCALE_ASPECT_BALANCED
            ScalingType.SCALE_ASPECT_BALANCED -> ScalingType.SCALE_ASPECT_FIT
        }
        rtpManager.setScaleType(scaleType.value!!)
    }

    fun setSwappedFeeds(value: Boolean) {
        rtpManager.setSwappedFeeds(value)
        Handler(Looper.getMainLooper()).post { swapScreen.value = value }
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

        Handler(Looper.getMainLooper()).post { cameraMute.value = false }
        Handler(Looper.getMainLooper()).post { sd.value = false }
        Handler(Looper.getMainLooper()).post { scaleType.value = ScalingType.SCALE_ASPECT_FILL }
        Handler(Looper.getMainLooper()).post { swapScreen.value = false }
    }

    open fun startOffer(counterpart: User, type: Call.Type, callCreateSuccess: OnSuccessListener<Void>) {
        d(TAG, "startOffer")
        callDirection = Call.Direction.Offer
        this.counterpart = counterpart
        space = Space(callType = type)
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
            callType = call!!.type,
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
            callType = call!!.type,
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
        if (space != null) {
            val userId = MyDataViewModel.instance.getMyId()
            space!!.leaves.add(userId)
            SpaceRepository.addLeaveList(space!!.id, userId)
        }
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
                    if (callType == Call.Type.MESSAGE) {
                        context.startService(Intent(context, CallService::class.java))
                    } else {
                        startForegroundService(context, Intent(context, CallService::class.java))
                    }
                    CallSignalImpl.instance.onIncomingObserver()
                }
            }
        })
    }

    open fun onAnswerCall(sdp: String?) {
        d(TAG, "onAnswerCall")
        space!!.connected = true
        SpaceRepository.update(space!!.id, mapOf<String, Any>("connected" to true))
        rtpManager.setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp!!))
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
        if (space != null) {
            updateLeaveList()
            setTerminatedCall(true)
            if (call != null) {
                call!!.terminated = true
                CallRepository.updateTerminatedAt(call!!)
            }
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
                historyList.removeAll { true }
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

        Handler(Looper.getMainLooper()).post { cameraMute.value = false }
        Handler(Looper.getMainLooper()).post { sd.value = false }
        Handler(Looper.getMainLooper()).post { scaleType.value = ScalingType.SCALE_ASPECT_FILL }
        Handler(Looper.getMainLooper()).post { swapScreen.value = false }
    }

    private object Holder {
        val INSTANCE = CallVM()
    }

    companion object {
        private const val TAG = "CallVM"
        val instance: CallVM by lazy { Holder.INSTANCE }
    }
}
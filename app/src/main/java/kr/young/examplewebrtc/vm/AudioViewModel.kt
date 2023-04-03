package kr.young.examplewebrtc.vm

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnSuccessListener
import kr.young.common.UtilLog.Companion.d
import kr.young.examplewebrtc.fcm.SendFCM
import kr.young.examplewebrtc.model.Call
import kr.young.examplewebrtc.model.Space
import kr.young.examplewebrtc.model.User
import kr.young.examplewebrtc.repo.CallRepository
import kr.young.examplewebrtc.repo.CallRepository.Companion.CALL_CREATE_FAILURE
import kr.young.examplewebrtc.repo.SpaceRepository

class AudioViewModel private constructor(): ViewModel() {
    var remoteCall: Call? = null
    val vm = CallVM.instance
    val counterpart: User? get() { return vm.counterpart }
    val call: Call? get() { return vm.call }
    val space: Space? get() { return vm.space }

    val responseCode = vm.responseCode
    val terminatedCall = vm.terminatedCall
    val mute = vm.mute
    val speaker = vm.speaker

    fun release() {
        vm.release()
    }

    fun startOffer(counterpart: User, type: Call.Type, callCreateSuccess: OnSuccessListener<Void>) {
        vm.startOffer(counterpart, type, callCreateSuccess)
    }

    fun startAnswer() {
        vm.startAnswer()
    }

    fun updateCallList() {
        vm.updateCallList()
    }

    fun updateParticipantList() {
        vm.updateParticipantList()
    }

    fun mute() {
        vm.mute()
    }

    fun speaker(audioManager: AudioManager) {
        vm.speaker(audioManager)
    }

    fun end(fcmType: SendFCM.FCMType = SendFCM.FCMType.Bye) {
        vm.end(fcmType)
    }

    fun onIncomingCall(
        context: Context,
        userId: String?,
        spaceId: String?,
        type: String?,
        sdp: String?
    ) {
        vm.onIncomingCall(context, userId, spaceId, type, sdp)
    }

    fun onAnswerCall(sdp: String?) {
        vm.onAnswerCall(sdp)
    }

    fun onTerminatedCall() {
        vm.onTerminatedCall()
    }

    private object Holder {
        val INSTANCE = AudioViewModel()
    }

    companion object {
        private const val TAG = "AudioViewModel"
        val instance: AudioViewModel by lazy { Holder.INSTANCE }
    }
}
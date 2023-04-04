package kr.young.firertc.vm

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnSuccessListener
import kr.young.firertc.fcm.SendFCM
import kr.young.firertc.model.Call
import kr.young.firertc.model.Space
import kr.young.firertc.model.User

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
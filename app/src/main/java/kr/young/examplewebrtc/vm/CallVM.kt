package kr.young.examplewebrtc.vm

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.ktx.toObject
import kr.young.common.UtilLog.Companion.d
import kr.young.examplewebrtc.CallService
import kr.young.examplewebrtc.fcm.SendFCM
import kr.young.examplewebrtc.model.Call
import kr.young.examplewebrtc.model.Space
import kr.young.examplewebrtc.model.User
import kr.young.examplewebrtc.repo.CallRepository
import kr.young.examplewebrtc.repo.SpaceRepository
import kr.young.examplewebrtc.repo.SpaceRepository.Companion.SPACE_READ_SUCCESS
import kr.young.examplewebrtc.repo.UserRepository

open class CallVM internal constructor(): ViewModel() {
    var space: Space? = null
    var call: Call? = null
    var counterpart: User? = null
    var callType: Call.Type? = null
        get() { return field ?: Call.Type.AUDIO }
    var callDirection: Call.Direction? = null
        get() { return field ?: Call.Direction.Offer }

    val responseCode = MutableLiveData<Int>()
    val terminatedCall = MutableLiveData<Boolean>()

    internal fun setResponseCode(value: Int) {
        Handler(Looper.getMainLooper()).post { responseCode.value = value }
    }

    fun setTerminatedCall(value: Boolean) {
        Handler(Looper.getMainLooper()).post { terminatedCall.value = value }
    }

    open fun release() {
        d(TAG, "release()")
        space = null
        call = null
        counterpart = null
        setResponseCode(0)
        setTerminatedCall(false)
    }

    open fun startOffer(counterpart: User, type: Call.Type, callCreateSuccess: OnSuccessListener<Void>) {
        d(TAG, "startOffer")
        this.counterpart = counterpart
        space = Space()
        SpaceRepository.post(space!!) {
            d(TAG, "create space success")
            call = Call(spaceId = space!!.id, type = type, direction = Call.Direction.Offer)
            CallRepository.post(call!!, {
                setResponseCode(CallRepository.CALL_CREATE_FAILURE)
            }, callCreateSuccess)
        }
    }

    open fun startAnswer() {
        d(TAG, "startAnswer")
    }

    open fun updateCallList() {
        space!!.calls.add(call!!.id)
        SpaceRepository.addCallList(space!!.id, call!!.id)
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

    open fun onIncomingCall(context: Context, userId: String?, spaceId: String?, type: String?) {
        d(TAG, "onIncomingCall")
        if (spaceId == null || type == null) {
            return
        }

        callType = Call.Type.valueOf(type)
        callDirection = Call.Direction.Answer
        SpaceRepository.getSpace(id = spaceId, success = {
            d(TAG, "get space success")
            this.space = it.toObject<Space>()!!
            setResponseCode(SPACE_READ_SUCCESS)
            call = Call(spaceId = space!!.id, type = callType!!, direction = Call.Direction.Answer)
            d(TAG, "call id ${call!!.id}")
            UserRepository.getUser(id = userId!!) { user ->
                counterpart = user.toObject<User>()
            }
            CallRepository.post(call!!) {
                d(TAG, "call post success")
                updateCallList()
                startForegroundService(context, Intent(context, CallService::class.java))
            }
        })
    }

    open fun onAnswerCall() {
        d(TAG, "onAnswerCall")
    }

    open fun onTerminatedCall() {
        d(TAG, "onTerminatedCall")
        setTerminatedCall(true)
        call!!.terminated = true
        CallRepository.updateTerminatedAt(call!!)
    }

    init {
        setResponseCode(0)
        setTerminatedCall(false)
    }

    private object Holder {
        val INSTANCE = CallVM()
    }

    companion object {
        private const val TAG = "CallVM"
        val instance: CallVM by lazy { Holder.INSTANCE }
    }
}
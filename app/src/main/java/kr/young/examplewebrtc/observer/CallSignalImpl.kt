package kr.young.examplewebrtc.observer

import android.content.Context
import kr.young.examplewebrtc.model.Call

class CallSignalImpl private constructor(): CallSignal.Publisher {
    private val callSignals = mutableListOf<CallSignal.Observer>()

    override fun add(observer: CallSignal.Observer) {
        callSignals.add(observer)
    }

    override fun remove(observer: CallSignal.Observer) {
        callSignals.remove(observer)
    }

    override fun onIncomingObserver() {
        for (observer in callSignals) {
            observer.onIncomingCall()
        }
    }

    override fun onAnswerObserver(sdp: String) {
        for (observer in callSignals) {
            observer.onAnswerCall(sdp)
        }
    }

    override fun onTerminatedObserver() {
        for (observer in callSignals) {
            observer.onTerminatedCall()
        }
    }

    private object Holder {
        val INSTANCE = CallSignalImpl()
    }

    companion object {
        private const val TAG = "CallSignalImpl"
        val instance: CallSignalImpl by lazy { Holder.INSTANCE }
    }
}
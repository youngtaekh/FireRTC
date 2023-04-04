package kr.young.examplewebrtc.observer

import android.content.Context
import kr.young.examplewebrtc.model.Call.Type

class CallSignal {
    interface Observer {
        fun onIncomingCall()
        fun onAnswerCall(sdp: String)
        fun onTerminatedCall()
    }

    interface Publisher {
        fun add(observer: Observer)
        fun remove(observer: Observer)
        fun onIncomingObserver()
        fun onAnswerObserver(sdp: String)
        fun onTerminatedObserver()
    }
}
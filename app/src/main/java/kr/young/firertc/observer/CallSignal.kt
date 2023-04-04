package kr.young.firertc.observer

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
package kr.young.rtp.observer

import kr.young.common.UtilLog.Companion.e
import kr.young.common.UtilLog.Companion.i
import kr.young.rtp.observer.PCObserverImpl.Companion.instance
import org.webrtc.*
import java.nio.charset.Charset
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PCListener(private val pcObserverImpl: PCObserverImpl)
    : PeerConnection.Observer {

    private var executor: ExecutorService? = null

    init {
        this.executor = Executors.newSingleThreadExecutor()
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        i(TAG, "onSignalingChange($p0)")
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        i(TAG, "onIceConnectionChange($p0)")
        this.executor!!.execute {
            when (p0) {
                PeerConnection.IceConnectionState.CONNECTED -> {
                    i(TAG, "ICE connection CONNECTED")
                    pcObserverImpl.onICEConnectedObserver()
                }
                PeerConnection.IceConnectionState.DISCONNECTED -> {
                    i(TAG, "ICE connection DISCONNECTED")
                    pcObserverImpl.onICEDisconnectedObserver()
                }
                PeerConnection.IceConnectionState.FAILED -> {
                    e(TAG, "ICE connection FAILED")
                }
                PeerConnection.IceConnectionState.NEW -> {
                    i(TAG, "ICE connection NEW")
                }
                PeerConnection.IceConnectionState.CHECKING -> {
                    i(TAG, "ICE connection CHECKING")
                }
                PeerConnection.IceConnectionState.COMPLETED -> {
                    i(TAG, "ICE connection COMPLETED")
                }
                PeerConnection.IceConnectionState.CLOSED -> {
                    i(TAG, "ICE connection CLOSED")
                }
                else -> {
                    e(TAG, "ICE connection UNKNOWN")
                }
            }
        }
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        i(TAG, "onIceConnectionReceivingChange($p0)")
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        i(TAG, "onIceGatheringChange($p0)")
    }

    override fun onIceCandidate(p0: IceCandidate?) {
        i(TAG, "onIceCandidate($p0)")
        this.executor!!.execute { pcObserverImpl.onICECandidateObserver(p0) }
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        i(TAG, "onIceCandidatesRemoved($p0)")
        this.executor!!.execute { pcObserverImpl.onICECandidatesRemovedObserver(p0) }
    }

    override fun onAddStream(p0: MediaStream?) {
        i(TAG, "onAddStream($p0)")
    }

    override fun onRemoveStream(p0: MediaStream?) {
        i(TAG, "onRemoveStream($p0)")
    }

    override fun onDataChannel(dc: DataChannel?) {
        dc!!.registerObserver(object: DataChannel.Observer {
            override fun onBufferedAmountChange(p0: Long) {
                i(TAG, "onDataChannel.onBufferedAmountChange" +
                        "label: ${dc.label()}, state: ${dc.state()}")
            }

            override fun onStateChange() {
                i(TAG, "onDataChannel.onStateChange" +
                        "label: ${dc.label()}, state: ${dc.state()}")
            }

            override fun onMessage(p0: DataChannel.Buffer?) {
                i(TAG, "onDataChannel.onMessage" +
                        "label: ${dc.label()}, state: ${dc.state()}")
                if (p0 == null || p0.binary) {
                    i(TAG, "Received binary msg over $dc")
                    return
                }
                val data = p0.data
                val bytes = ByteArray(data.capacity())
                data[bytes]
                val strData = String(bytes, Charset.forName("UTF-8"))
                i(TAG, "Got msg: $strData over $dc")
                instance.onMessageObserver(strData)
            }
        })
    }

    override fun onRenegotiationNeeded() {
        i(TAG, "onRenegotiationNeeded()")
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        i(TAG, "onAddTrack($p0, $p1)")
    }

    companion object {
        private const val TAG = "PCListener"
    }
}
